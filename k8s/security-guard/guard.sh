#!/usr/bin/env bash
# Security guard (runs as the security-guard CronJob).
#
# 1. Scans the image digest each workload is actually running for fixable CVEs (Trivy).
# 2. PATCH_TARGETS (infra StatefulSets): if a newer build of the SAME tag has fewer fixable
#    CVEs, pins the workload to that digest. MySQL is dumped first. A failed rollout is undone.
#    The tag itself (mysql:8.0, redis:7-alpine, ...) is never changed: major/minor upgrades
#    can change the on-disk data format, so they stay manual and are only reported.
# 3. REPORT_TARGETS (the app): report only. Library fixes come from Dependabot PRs (CI-tested).
#    If GH_TOKEN is set and OS packages are fixable, re-runs the CD workflow once per app
#    digest so the image is rebuilt on the newest base image.
# 4. Audits MySQL accounts (report only, never changes users).
# 5. Mails ADMIN_EMAIL only when the report differs from the previous run.
set -uo pipefail

NS="${NAMESPACE:-maeilkanji}"
PATCH_TARGETS="${PATCH_TARGETS:-statefulset/mysql statefulset/redis statefulset/rabbitmq}"
REPORT_TARGETS="${REPORT_TARGETS:-deployment/maeilkanji}"
AUTO_PATCH="${AUTO_PATCH:-true}"
MAIL_ENABLED="${MAIL_ENABLED:-true}"
SEVERITY="${SEVERITY:-HIGH,CRITICAL}"
PLATFORM="${PLATFORM:-linux/arm64}"
DATA_DIR="${DATA_DIR:-/data}"
BACKUP_KEEP="${BACKUP_KEEP:-7}"
ROLLOUT_TIMEOUT="${ROLLOUT_TIMEOUT:-600s}"
GH_REPO="${GH_REPO:-dockerel/MaeilKanji}"
GH_TOKEN="${GH_TOKEN:-}"
export TRIVY_CACHE_DIR="${TRIVY_CACHE_DIR:-$DATA_DIR/trivy-cache}"

BACKUP_DIR="$DATA_DIR/backup"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$BACKUP_DIR" "$TRIVY_CACHE_DIR"

REPORT=""
ATTENTION=0
log() { echo "[$(date '+%F %T')] $*" >&2; }
note() { REPORT+="$*"$'\n'; log "$*"; }
k() { kubectl -n "$NS" "$@"; }

# "mysql:8.0" -> "docker.io library/mysql 8.0" (digest suffix ignored)
split_ref() {
  local ref="${1%@*}" reg repo tag
  if [[ "$ref" == */* && "${ref%%/*}" == *[.:]* ]]; then
    reg="${ref%%/*}"; ref="${ref#*/}"
  else
    reg="docker.io"
  fi
  [[ "$reg" == "docker.io" && "$ref" != */* ]] && ref="library/$ref"
  if [[ "${ref##*/}" == *:* ]]; then tag="${ref##*:}"; repo="${ref%:*}"; else tag="latest"; repo="$ref"; fi
  echo "$reg $repo $tag"
}

# Digest the registry currently serves for reg/repo:tag (HEAD request, no pull-rate cost).
remote_digest() {
  local reg="$1" repo="$2" tag="$3" token api
  case "$reg" in
    docker.io)
      token="$(curl -fsS "https://auth.docker.io/token?service=registry.docker.io&scope=repository:$repo:pull" | jq -r .token)"
      api="registry-1.docker.io" ;;
    ghcr.io)
      token="$(curl -fsS "https://ghcr.io/token?scope=repository:$repo:pull" | jq -r .token)"
      api="ghcr.io" ;;
    *) return 1 ;;
  esac
  curl -fsSI \
    -H "Authorization: Bearer $token" \
    -H 'Accept: application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json, application/vnd.oci.image.manifest.v1+json, application/vnd.docker.distribution.manifest.v2+json' \
    "https://$api/v2/$repo/manifests/$tag" \
    | tr -d '\r' | awk -F': ' 'tolower($1)=="docker-content-digest"{print $2}'
}

# Image digest of the first running pod of a workload.
running_digest() {
  local app
  app="$(k get "$1" -o jsonpath='{.spec.selector.matchLabels.app}')"
  k get pods -l "app=$app" --field-selector=status.phase=Running \
    -o jsonpath='{.items[0].status.containerStatuses[0].imageID}' | sed 's/.*@//'
}

# scan <ref> <outfile>: writes Trivy JSON; prints the number of fixable findings.
scan() {
  local ref="$1" out="$2"
  if ! trivy image --quiet --image-src remote --platform "$PLATFORM" --scanners vuln \
      --severity "$SEVERITY" --ignore-unfixed --timeout 30m --format json --output "$out" "$ref" >&2; then
    return 1
  fi
  jq '[.Results[]?.Vulnerabilities[]? | .VulnerabilityID + "|" + .PkgName] | unique | length' "$out"
}

top_findings() {
  jq -r '[.Results[]?.Vulnerabilities[]?] | unique_by(.VulnerabilityID + .PkgName) | sort_by(.Severity) | .[:10][]
    | "    - \(.Severity) \(.VulnerabilityID) \(.PkgName) \(.InstalledVersion) -> \(.FixedVersion)"' "$1"
}

backup_mysql() {
  local pod="$1" f
  f="$BACKUP_DIR/mysql-$(date +%Y%m%d-%H%M%S).sql.gz"
  log "MySQL backup -> $f"
  if ! k exec "$pod" -c mysql -- sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --all-databases --single-transaction --routines --events --triggers 2>/dev/null' \
      | gzip > "$f"; then
    rm -f "$f"; return 1
  fi
  if ! gzip -t "$f" || ! zcat "$f" | tail -n 1 | grep -q 'Dump completed'; then
    rm -f "$f"; return 1
  fi
  ls -1t "$BACKUP_DIR"/mysql-*.sql.gz | tail -n +"$((BACKUP_KEEP + 1))" | xargs -r rm -f
  note "  backup: $(basename "$f") ($(du -h "$f" | cut -f1))"
}

patch_workload() {
  local w="$1" name="${1#*/}" img cname running reg repo tag cur_n latest new_n
  img="$(k get "$w" -o jsonpath='{.spec.template.spec.containers[0].image}')"
  cname="$(k get "$w" -o jsonpath='{.spec.template.spec.containers[0].name}')"
  running="$(running_digest "$w")"
  read -r reg repo tag < <(split_ref "$img")
  note "[$w] ${img%@*} (running ${running:7:12})"

  if [[ -z "$running" ]]; then note "  ! no running pod, skipped"; ATTENTION=1; return; fi
  if ! cur_n="$(scan "$reg/$repo@$running" "$WORK/$name-cur.json")"; then
    note "  ! scan failed"; ATTENTION=1; return
  fi
  if [[ "$cur_n" -eq 0 ]]; then note "  ok: no fixable $SEVERITY CVEs"; return; fi
  note "  fixable $SEVERITY CVEs: $cur_n"
  note "$(top_findings "$WORK/$name-cur.json")"
  ATTENTION=1

  latest="$(remote_digest "$reg" "$repo" "$tag")"
  if [[ -z "$latest" ]]; then note "  ! could not resolve latest digest of $repo:$tag"; return; fi
  if [[ "$latest" == "$running" ]]; then
    note "  no newer build of tag '$tag'. Fix needs a manual tag upgrade (check data compatibility first)."
    return
  fi
  if ! new_n="$(scan "$reg/$repo@$latest" "$WORK/$name-new.json")"; then
    note "  ! scan of newer build ${latest:7:12} failed"; return
  fi
  if [[ "$new_n" -ge "$cur_n" ]]; then
    note "  newer build ${latest:7:12} has $new_n fixable CVEs (not better), not patched"
    return
  fi
  if [[ "$AUTO_PATCH" != "true" ]]; then
    note "  AUTO_PATCH=false: would patch to ${latest:7:12} ($cur_n -> $new_n)"
    return
  fi

  if [[ "$name" == "mysql" ]] && ! backup_mysql "$name-0"; then
    note "  ! MySQL backup failed, not patched"; return
  fi

  k set image "$w" "$cname=${img%@*}@$latest" >&2
  if k rollout status "$w" --timeout="$ROLLOUT_TIMEOUT" >&2; then
    note "  PATCHED: ${running:7:12} -> ${latest:7:12} (fixable $cur_n -> $new_n)"
  else
    note "  ! rollout failed, rolling back to ${running:7:12}"
    k rollout undo "$w" >&2
    # A StatefulSet keeps a broken pod until it is deleted (OrderedReady).
    [[ "$w" == statefulset/* ]] && k delete pod "$name-0" --wait=false >&2
    if k rollout status "$w" --timeout="$ROLLOUT_TIMEOUT" >&2; then
      note "  rollback ok"
    else
      note "  !! ROLLBACK FAILED, check now: kubectl -n $NS describe $w"
    fi
  fi
}

report_workload() {
  local w="$1" name="${1#*/}" img running reg repo tag n os_n state
  img="$(k get "$w" -o jsonpath='{.spec.template.spec.containers[0].image}')"
  running="$(running_digest "$w")"
  read -r reg repo tag < <(split_ref "$img")
  note "[$w] $img (report only)"
  if [[ -z "$running" ]]; then note "  ! no running pod"; ATTENTION=1; return; fi
  if ! n="$(scan "$reg/$repo@$running" "$WORK/$name.json")"; then note "  ! scan failed"; ATTENTION=1; return; fi
  if [[ "$n" -eq 0 ]]; then note "  ok: no fixable $SEVERITY CVEs"; return; fi
  ATTENTION=1
  os_n="$(jq '[.Results[]? | select(.Class == "os-pkgs") | .Vulnerabilities[]?] | length' "$WORK/$name.json")"
  note "  fixable $SEVERITY CVEs: $n (OS packages: $os_n, libraries: $((n - os_n)))"
  note "$(top_findings "$WORK/$name.json")"
  [[ $((n - os_n)) -gt 0 ]] && note "  libraries: merge the Dependabot PRs (build.gradle)."

  state="$DATA_DIR/last-rebuild-digest"
  if [[ "$os_n" -gt 0 ]]; then
    if [[ -z "$GH_TOKEN" ]]; then
      note "  OS packages: re-run the CD workflow to rebuild on the latest base image (GH_TOKEN not set)."
    elif [[ "$AUTO_PATCH" != "true" ]]; then
      note "  AUTO_PATCH=false: would trigger CD rebuild"
    elif [[ "$(cat "$state" 2>/dev/null)" == "$running" ]]; then
      note "  CD rebuild already triggered for this image; base image has no fix yet."
    elif curl -fsS -X POST -H "Authorization: Bearer $GH_TOKEN" -H "Accept: application/vnd.github+json" \
        "https://api.github.com/repos/$GH_REPO/actions/workflows/cd.yml/dispatches" -d '{"ref":"main"}'; then
      echo "$running" > "$state"
      note "  CD rebuild triggered (cd.yml on main)."
    else
      note "  ! CD dispatch failed"
    fi
  fi
}

audit_mysql() {
  local q out
  q="SELECT CONCAT(user, '@', host, ' (', plugin, ')') FROM mysql.user
     WHERE (authentication_string = '' AND plugin NOT IN ('auth_socket', 'unix_socket'))
        OR (user = 'root' AND host NOT IN ('localhost', '127.0.0.1', '::1'))"
  note "[mysql audit]"
  if ! out="$(k exec mysql-0 -c mysql -- sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -B -e "$1" 2>/dev/null' _ "$q")"; then
    note "  ! audit query failed"; ATTENTION=1; return
  fi
  if [[ -z "$out" ]]; then note "  ok"; return; fi
  ATTENTION=1
  while IFS= read -r line; do
    note "  - risky account: $line (empty password or root reachable from the network)"
  done <<< "$out"
}

send_mail() {
  local subject="$1" from rcpt
  from="$(sed -n 's/.*<\(.*\)>.*/\1/p' <<< "$MAIL_FROM")"; from="${from:-$MAIL_FROM}"
  rcpt="$ADMIN_EMAIL"
  {
    printf 'From: %s\r\nTo: %s\r\nSubject: =?UTF-8?B?%s?=\r\n' "$from" "$rcpt" "$(printf '%s' "$subject" | base64 | tr -d '\n')"
    printf 'MIME-Version: 1.0\r\nContent-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: 8bit\r\n\r\n'
    printf '%s\n' "$REPORT"
  } > "$WORK/mail.txt"
  curl -fsS --url "smtp://$SMTP_HOST:$SMTP_PORT" --ssl-reqd \
    --user "$SMTP_USERNAME:$SMTP_PASSWORD" \
    --mail-from "$from" --mail-rcpt "$rcpt" -T "$WORK/mail.txt"
}

main() {
  log "trivy $(trivy --version | head -1), namespace $NS, AUTO_PATCH=$AUTO_PATCH"
  for w in $PATCH_TARGETS; do patch_workload "$w"; done
  for w in $REPORT_TARGETS; do report_workload "$w"; done
  audit_mysql

  # Mail only when something needs attention and the report changed since the last mail.
  local hash last
  hash="$(grep -v '^  backup:' <<< "$REPORT" | sed 's/(running [0-9a-f]*)//' | sha256sum | cut -d' ' -f1)"
  last="$(cat "$DATA_DIR/last-report-hash" 2>/dev/null)"
  if [[ "$ATTENTION" -eq 1 && "$hash" != "$last" && "$MAIL_ENABLED" == "true" ]]; then
    if send_mail "[매일한자] 보안 점검 $(date +%F)"; then
      echo "$hash" > "$DATA_DIR/last-report-hash"; log "report mailed"
    else
      log "mail failed"
    fi
  fi
  printf '\n===== report =====\n%s' "$REPORT"
}

main
