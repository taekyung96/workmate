# 12. 운영 가이드 (Operations)

> **지금 실제로 돌고 있는 배포**를 어떻게 다루는지 적는다.
> 처음 배포하는 절차는 [11. 배포 가이드](11_DEPLOYMENT_GUIDE.md)에 있다. 이 문서는 그 **다음** 이야기다.

- **관련**: [11. 배포 가이드](11_DEPLOYMENT_GUIDE.md) · [08. WSL2·Docker 셋업](08_DOCKER_WSL2_SETUP_GUIDE.md) · [HANDOVER](../project/HANDOVER.md)

---

## 1. 지금 어떤 구성인가

정식 도메인을 아직 붙이지 않아서, **Cloudflare Quick Tunnel**(무료 임시 주소)로 공개하고 있다.

```
인터넷
  │  https://<랜덤>.trycloudflare.com     ← 주소가 고정이 아니다 (§5)
Cloudflare Quick Tunnel
  │  아웃바운드 연결만 사용 — 포트포워딩·DDNS·공인IP 불필요
wm-quicktunnel (컨테이너)
  │  같은 도커 네트워크
workmate-web  :8080     ← 호스트에는 127.0.0.1 로만 바인딩
  │
workmate-was  :8081     ← 호스트에 포트 없음
  │
workmate-db   :5432     ← 호스트에 포트 없음
```

### 어디에 무엇이 있나

| 항목 | 값 |
| --- | --- |
| 실행 위치 | **WSL2 Ubuntu** (Windows 가 아니다 — `wsl -d Ubuntu` 안에서 도커가 돈다) |
| 저장소 경로 | `~/workmate` (WSL 홈. GitHub 에서 클론한 것) |
| compose 프로젝트명 | `workmate` |
| 컨테이너 | `workmate-db` · `workmate-was` · `workmate-web` · `wm-quicktunnel` |
| 네트워크 | `workmate_default` |
| 볼륨 | `workmate_workmate-db-data` (DB) · `workmate_workmate-uploads` (업로드 파일) |
| 이미지 | GHCR 에서 pull (`ghcr.io/taekyung96/workmate-{was,web}:latest`) — 서버에서 빌드하지 않는다 |
| 비밀값 | `~/workmate/.env` — **git 에 없다.** 잃어버리면 DB 의 이메일·전화번호를 복호화할 수 없다 |

> **개발용 DB 와 헷갈리지 말 것.** `workmate-v3_workmate-db-data` 볼륨은 예전 개발 환경
> (`/mnt/d/ClaudeCode/workmate-v3-ws/workmate-v3`)의 것으로 **별개**다. 이 배포와 데이터를 공유하지 않는다.

---

## 2. 일상 작업

전부 **WSL 안에서** 실행한다. Windows PowerShell 이나 Git Bash 에는 `docker` 가 없다.

```bash
wsl -d Ubuntu          # WSL 진입 후 아래를 실행
cd ~/workmate
```

| 하고 싶은 것 | 명령 |
| --- | --- |
| 상태 보기 | `docker compose -f docker-compose.deploy.yml -p workmate ps` |
| 기동 | `docker compose -f docker-compose.deploy.yml -p workmate up -d` |
| 정지 (데이터 유지) | `docker compose -f docker-compose.deploy.yml -p workmate stop` |
| 로그 보기 | `docker compose -f docker-compose.deploy.yml -p workmate logs -f web` |
| **완전 삭제 (데이터까지)** | `docker compose -f docker-compose.deploy.yml -p workmate down -v` ⚠️ |

`-v` 는 볼륨까지 지운다. **DB 가 초기화되므로 §4 의 부트스트랩을 다시 해야 한다.**

---

## 3. 재부팅 후 복구

**WSL2 는 부팅 시 도커 데몬을 자동으로 켜지 않는다.** 컨테이너에 `restart: unless-stopped` 가
걸려 있어도 데몬이 안 뜨면 소용없다. 그래서 재부팅 후에는 수동으로 한 번 올려야 한다.

```bash
# 1) 스택 기동
wsl -d Ubuntu -e bash -lc 'cd ~/workmate && docker compose -f docker-compose.deploy.yml -p workmate up -d'

# 2) 터널 기동 (주소가 새로 발급된다)
wsl -d Ubuntu -e bash -lc 'docker start wm-quicktunnel'

# 3) 바뀐 주소를 README 에 반영 (§5)
```

DB 볼륨은 남아 있으므로 **부트스트랩(§4)은 다시 하지 않아도 된다.**

> 매번 하기 번거로우면 Windows 작업 스케줄러에 등록한다 —
> [11. 배포 가이드 §7](11_DEPLOYMENT_GUIDE.md) 참고.

---

## 4. 데모 계정 부트스트랩 (DB 를 새로 만들었을 때만)

`db/init/13-seed-demo-data.sql` 의 이메일·전화번호는 **개발 환경 AES 키**로 암호화된 값이라,
이 배포의 키로는 조회되지 않아 로그인이 실패한다. 한 번만 맞춰주면 된다.

```bash
cd ~/workmate && ./scripts/bootstrap-demo-login.sh
```

```
demo.admin@example.com / Workmate!2026
hong@example.com       / Workmate!2026
kim@example.com        / Workmate!2026
```

세 계정 모두 **`ROLE_USER`** 다. 비밀번호가 공개 README 에 있어서 공개 인스턴스에서는 관리자로 두지 않는다.
관리자 화면 시연이 필요한 **비공개** 환경에서만 `--grant-admin` 을 붙인다.

> 배경과 근거: [11. 배포 가이드 §2 '데모 계정은 어떻게 되나'](11_DEPLOYMENT_GUIDE.md)

---

## 5. 데모 주소 관리 (Quick Tunnel 을 쓰는 동안)

### 주소는 언제 바뀌나

`cloudflared` 프로세스가 **다시 뜰 때마다** 새로 발급된다. 실측으로 확인했다.

| 상황 | 주소 |
| --- | --- |
| `docker restart` · `stop`→`start` | **바뀜** |
| 도커 데몬 재시작 · WSL 종료 · PC 재부팅 | **바뀜** |
| 그냥 오래 켜두기 | 유지 |
| 공인 IP 변경 | 무관 (아웃바운드 연결이라) |

Windows 업데이트 재부팅이 월 1~2 회 강제되므로 **실질적으로 월 1~2 회는 바뀐다**고 보면 된다.
알림이 없어서 **조용히** 바뀐다.

### 그래서 이렇게 운영한다

```
이력서·지원서  →  github.com/taekyung96/workmate     절대 안 바뀜
README         →  🔗 라이브 데모: https://xxx...      바뀌면 한 줄 갱신
```

데모가 죽어도 포트폴리오 본체는 멀쩡하고, 링크만 고치면 복구된다.

### 갱신 방법

```bash
./scripts/update-demo-url.sh                       # 터널 컨테이너에서 자동 탐지
./scripts/update-demo-url.sh https://xxx...        # 주소를 직접 지정
```

저장소가 Windows(`D:\...`)에 있고 도커가 WSL 에 있으면 자동 탐지가 안 된다. 그때는 주소를 넘긴다.

```bash
# Windows 쪽 저장소에서 쓰는 형태
./scripts/update-demo-url.sh "$(wsl -d Ubuntu -e bash -lc \
  'docker logs wm-quicktunnel 2>&1 | grep -oE "https://[a-z0-9-]+\.trycloudflare\.com" | tail -1')"
git add README.md && git commit -m "docs: 라이브 데모 주소 갱신" && git push
```

스크립트는 **쓰기 전에 `HTTP 200` 을 확인**하고, 마커(`<!-- demo-url:start/end -->`) 사이만 갈아끼운다(멱등).

### 주소가 발급됐는데 접속이 안 될 때

Quick Tunnel 은 **호스트명 발급이 간헐적으로 실패한다.** 터널 로그에는
`Registered tunnel connection` 이 찍히는데 DNS 가 끝내 해석되지 않는 경우가 실제로 있었다.
몇 분 기다려도 그대로면 터널만 다시 만든다.

```bash
docker rm -f wm-quicktunnel
docker run -d --name wm-quicktunnel --restart unless-stopped --network workmate_default \
    cloudflare/cloudflared:latest tunnel --url http://web:8080
docker logs wm-quicktunnel | grep trycloudflare.com
```

---

## 6. 앱 갱신 (main 에 머지한 내용을 반영)

`main` 에 머지되면 CI 가 GHCR 에 새 이미지를 올린다. 서버는 **받아서 다시 띄우기만** 하면 된다.

```bash
cd ~/workmate
git pull                                                    # db/init 변경분을 받기 위해
docker compose -f docker-compose.deploy.yml -p workmate pull
docker compose -f docker-compose.deploy.yml -p workmate up -d
```

> **스키마가 바뀌었다면** `db/init/*.sql` 은 볼륨 최초 생성 시에만 실행되므로 수동 적용해야 한다 —
> [11. 배포 가이드 §4](11_DEPLOYMENT_GUIDE.md). 안 하면 `ddl-auto: validate` 가 실패해 WAS 가 안 뜬다.

---

## 7. 지금 안 되는 것

| 항목 | 이유 | 언제 풀리나 |
| --- | --- | --- |
| **소셜 로그인** (네이버·카카오·구글) | 콜백 URL 은 고정 도메인에만 등록할 수 있다 | 도메인 붙이면 (§8) |
| **주소 고정** | Quick Tunnel 의 특성 | 도메인 붙이면 (§8) |
| 관리자 화면 | 의도된 제한 — 데모 계정은 `ROLE_USER` | 비공개 환경에서 `--grant-admin` |

이메일 로그인·채팅(SSE·RAG)·영수증·가이드·회의록은 **전부 동작한다.**

---

## 8. 정식 도메인으로 전환할 때

Quick Tunnel 을 정식(Named) 터널로 바꾸는 것뿐이라, **앱과 DB 는 손대지 않는다.**

1. 도메인을 Cloudflare 에 등록하고 상태가 **Active** 가 되게 한다
2. Zero Trust → Networks → Tunnels → Create a tunnel → **토큰** 복사
3. Public hostname 추가 — Type `HTTP`, URL **`web:8080`** (`localhost` 아님)
4. 적용

```bash
cd ~/workmate
echo 'TUNNEL_TOKEN=eyJ...' >> .env
docker rm -f wm-quicktunnel                                            # Quick Tunnel 제거
docker compose -f docker-compose.deploy.yml -p workmate --profile tunnel up -d
```

5. 각 제공자 콘솔에 콜백 등록 → 소셜 로그인이 살아난다

```
https://<도메인>/login/oauth2/code/naver
https://<도메인>/login/oauth2/code/kakao
https://<도메인>/login/oauth2/code/google
```

6. README 를 고정 주소로 갱신한다. 이후로는 `update-demo-url.sh` 가 필요 없다

상세는 [11. 배포 가이드 §5·§6](11_DEPLOYMENT_GUIDE.md).

---

## 9. 문제가 생기면

증상별 원인은 [11. 배포 가이드 §9 '자주 막히는 곳'](11_DEPLOYMENT_GUIDE.md)에 모아두었다. 자주 보는 것만 옮기면:

| 증상 | 먼저 볼 것 |
| --- | --- |
| 공개 주소가 안 열림 | 터널이 떠 있나 → `docker ps --filter name=quicktunnel`. 떠 있으면 §5 의 재생성 |
| 로컬은 되는데 밖에서 안 됨 | 터널 문제다. `curl -I http://127.0.0.1:8080/` 로 앱 자체를 먼저 확인 |
| 데모 로그인 401 | 볼륨을 새로 만들었다 → §4 부트스트랩 |
| WEB 이 재시작 반복 | `.env` 의 소셜 자격증명이 **빈 값**은 아닌지 (주석 처리해야 한다) |
| DB unhealthy · init 중단 | `db/init/*.sh` 실행권한. `.gitattributes` 가 LF 를 강제하는지 |
