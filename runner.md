# Self-hosted Runner 설치 가이드 (Raspberry Pi / k3s)

CD 파이프라인(`.github/workflows/cd.yml`)의 `deploy` job은 Pi에 설치된 self-hosted runner에서 실행됩니다.
GitHub 서버는 집 안의 k3s API(`127.0.0.1:6443`)에 접근할 수 없기 때문에, Pi에 runner가 꼭 있어야 합니다.

## 현재 상태 (2026-10-31 기준)

- runner v2.338.2(arm64)는 `~/actions-runner`에 압축만 풀어 둔 상태입니다. sudo 없이 실행되는 것까지 확인했습니다.
- GitHub에 runner 등록은 아직 하지 않았습니다.
- 서비스 등록(재부팅 후 자동 시작)도 아직 하지 않았습니다.

## 전체 흐름

```
main push
  → ci pipeline (GitHub ubuntu-latest)
  → 성공 시 cd pipeline
      build  : GitHub ubuntu-24.04-arm 에서 bootJar → 이미지 빌드 → ghcr.io/dockerel/maeilkanji:<sha> push
      deploy : Pi runner 에서 k8s/app.yaml apply → rollout status 대기
```

---

## 1. Runner 등록

1. 브라우저에서 GitHub `Dockerel/MaeilKanji` → **Settings → Actions → Runners → New self-hosted runner**로 갑니다.
2. OS는 Linux, Architecture는 ARM64를 고릅니다.
3. 화면의 `./config.sh ... --token XXXX` 줄에서 **토큰 값만** 복사합니다. 토큰은 **1시간 뒤 만료**되므로 받은 뒤 바로 진행하세요.
4. Pi에서 아래를 실행합니다. 다운로드 단계는 이미 끝났으니 건너뜁니다.

```bash
cd ~/actions-runner
./config.sh --unattended \
  --url https://github.com/Dockerel/MaeilKanji \
  --token <복사한 토큰> \
  --name rpi4b \
  --labels k3s
```

`--labels k3s`는 꼭 넣어야 합니다. `cd.yml`이 `runs-on: [self-hosted, linux, ARM64, k3s]`로 runner를 찾기 때문입니다.
`self-hosted`, `linux`, `ARM64` 라벨은 자동으로 붙습니다.

## 2. 상시 실행 등록 (재부팅 후 자동 시작)

시스템 서비스로 등록하는 방법을 추천합니다(sudo 필요).

```bash
cd ~/actions-runner
sudo ./svc.sh install $USER
sudo ./svc.sh start
sudo ./svc.sh status
```

서비스는 `$USER` 권한으로 실행되므로 `~/.kube/config`를 그대로 사용합니다.

테스트로 잠깐만 돌려 보려면 아래처럼 실행합니다. 터미널을 닫거나 재부팅하면 종료됩니다.

```bash
cd ~/actions-runner && ./run.sh
```

## 3. 등록 확인

1. GitHub **Settings → Actions → Runners**에서 `rpi4b`가 **Idle**(초록색)로 보이는지 확인합니다.
2. Pi에서 runner가 kubectl을 쓸 수 있는지 확인합니다.

```bash
kubectl get nodes
kubectl -n maeilkanji get pods
```

---

## 4. GitHub 저장소 보안 설정 (꼭 할 것)

이 저장소는 **public**입니다. 외부인이 fork에서 PR을 열고 워크플로 파일을 고쳐 `runs-on: self-hosted`를 지정하면, **Pi에서 임의 코드가 실행될 수 있습니다.**

1. **Settings → Actions → General → Fork pull request workflows from outside collaborators**를 **"Require approval for all external contributors"** 로 설정합니다.
2. **Settings → Environments → New environment**에서 이름이 `production`인 환경을 만듭니다.
   - **Deployment branches and tags**를 "Selected branches"로 바꾸고 `main`만 허용합니다.
   - 필요하면 **Required reviewers**에 본인을 넣어, 배포 전에 수동 승인을 받게 할 수 있습니다.

## 5. 첫 배포 전 준비

### 5-1. 앱용 Secret 만들기

메일, 관리자, 서비스 URL 값은 `maeilkanji-app` Secret에 넣습니다. 이 파일은 gitignore 대상이라 커밋되지 않습니다.

```bash
cd ~/MaeilKanji
cp k8s/app-secret.example.yaml k8s/app-secret.yaml
vi k8s/app-secret.yaml      # SMTP_USERNAME, SMTP_PASSWORD(Gmail 앱 비밀번호), ADMIN_EMAIL, SERVICE_BASE_URL
kubectl apply -f k8s/app-secret.yaml
```

DB, Redis, RabbitMQ 계정은 이미 적용된 `maeilkanji-infra` Secret(`k8s/secret.yaml`)을 그대로 씁니다.

### 5-2. 커밋하고 push

```bash
git add .gitignore .dockerignore Dockerfile .github/workflows/cd.yml k8s/ runner.md
git status            # k8s/secret.yaml, k8s/app-secret.yaml 이 목록에 없는지 확인
git commit -m "ci: k8s 매니페스트 및 CD 파이프라인 추가"
git push origin main
```

### 5-3. GHCR 이미지를 Public으로 바꾸기

첫 CD에서 build job이 끝나면 GHCR에 이미지가 생깁니다. 새 패키지는 기본이 **private**이라서 k3s가 이미지를 받지 못하고, 첫 배포는 `ImagePullBackOff`로 실패합니다.

1. GitHub 프로필 → **Packages → maeilkanji → Package settings**로 갑니다.
2. **Change visibility**를 **Public**으로 바꿉니다.
3. Actions 탭에서 실패한 cd pipeline의 **Re-run failed jobs**를 누르거나, **Run workflow**(workflow_dispatch)로 다시 실행합니다.

## 6. 배포 확인

```bash
kubectl -n maeilkanji get pods -l app=maeilkanji
kubectl -n maeilkanji logs deploy/maeilkanji --tail=50
kubectl -n maeilkanji port-forward svc/maeilkanji 8080:80   # 브라우저에서 http://localhost:8080
```

---

## 문제 해결

| 증상 | 원인과 해결 |
|---|---|
| deploy job이 "Waiting for a runner" 상태로 멈춤 | runner가 꺼져 있거나 `k3s` 라벨이 없습니다. `sudo ./svc.sh status`와 GitHub의 Runners 화면을 확인하세요. |
| `x509: certificate has expired` | k3s 인증서는 1년마다 만료됩니다. `sudo systemctl restart k3s`를 실행한 뒤 `sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config && sudo chown $USER ~/.kube/config`로 kubeconfig를 다시 복사하세요. |
| `ImagePullBackOff` | GHCR 패키지가 private 상태입니다. 5-3을 참고하세요. |
| `CreateContainerConfigError` | `maeilkanji-app` Secret이 없습니다. 5-1을 참고하세요. |
| `config.sh` 실행 시 토큰 오류 | 토큰이 만료됐습니다(1시간). 1단계에서 새 토큰을 받으세요. |
| runner를 다시 등록하고 싶음 | `sudo ./svc.sh stop && sudo ./svc.sh uninstall`을 실행합니다. 그다음 GitHub에서 remove 토큰을 받아 `./config.sh remove --token <토큰>`을 실행하고, 1단계부터 다시 진행합니다. |

## 참고

- JDK 17(Temurin)은 `~/.local/jdk-17`에 설치되어 있습니다. 로컬에서 `./gradlew bootRun`을 하려면 먼저 `export JAVA_HOME=~/.local/jdk-17 PATH=~/.local/jdk-17/bin:$PATH`를 실행하세요.
- 앱 Deployment는 `replicas: 1`로 고정입니다. `@Scheduled` 작업이 파드마다 실행되기 때문에 늘리면 메일이 중복 발송됩니다.
