# Official mysql:8.4 with the scanner findings that upstream leaves in place removed.
# Built and pushed to ghcr.io/dockerel/maeilkanji:mysql-8.4 by .github/workflows/mysql-image.yml
# (weekly, so upstream 8.4 patches arrive; security-guard rolls out a new digest only if it has fewer CVEs).

# gosu upstream pins an old Go on purpose, so its stdlib CVEs never go away.
# Rebuild the same gosu release with a current Go.
FROM golang:1.27-alpine AS gosu
ARG GOSU_VERSION=1.20
RUN CGO_ENABLED=0 GOTOOLCHAIN=local go install -trimpath -ldflags='-s -w' github.com/tianon/gosu@${GOSU_VERSION}

FROM mysql:8.4
# mysql-shell (mysqlsh) bundles its own Python libraries (certifi, cryptography, pyOpenSSL, urllib3).
# Nothing here uses mysqlsh: the entrypoint and security-guard use mysqld, mysql and mysqldump.
RUN microdnf remove -y mysql-shell \
 && microdnf clean all \
 && rm -rf /usr/lib/mysqlsh \
 && ! command -v mysqlsh
COPY --from=gosu /go/bin/gosu /usr/local/bin/gosu
RUN gosu --version && gosu mysql true && mysqldump --version
