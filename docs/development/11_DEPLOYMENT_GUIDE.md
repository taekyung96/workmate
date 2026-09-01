# 11. 배포 가이드 (Deployment Guide)

> 서버는 GHCR 이미지를 **받기만** 한다. 소스 빌드가 없으므로 **JDK·Node·Gradle 이 필요 없다.**
> 개발 환경 구축은 [08. WSL2·Docker 셋업](08_DOCKER_WSL2_SETUP_GUIDE.md) 참고.

- **관련**: [ADR-0001](../project/adr/0001-hybrid-ssr-to-vue3-spa.md)(3-tier·WAS 은닉) · [CI 워크플로](../../.github/workflows/ci.yml)

---

## 0. 배포 구조

```
인터넷
  │  HTTPS
Cloudflare Tunnel  ── 아웃바운드 연결만 사용 (포트포워딩·DDNS·고정IP 불필요)
  │
workmate-web (:8080, 루프백 바인딩)   ← 브라우저가 보는 유일한 지점
  │  내부 도커 네트워크
workmate-was (:8081, 포트 미공개)
  │
PostgreSQL 17 + pgvector (:5432, 포트 미공개)
```

**호스트에 여는 포트는 `127.0.0.1:8080` 하나뿐이다.** 외부 노출은 터널이 담당하므로
공유기나 방화벽에 구멍을 뚫지 않는다.

---

## 1. 사전 준비

| 항목                    | 비고                                                            |
| ----------------------- | --------------------------------------------------------------- |
| Docker + Docker Compose | 서버에 설치. WSL2 라면 [08 문서](08_DOCKER_WSL2_SETUP_GUIDE.md) |
| Git                     | `db/init/*.sh`(롤 생성·데모 승격 스크립트)가 필요해 클론한다    |
| 도메인                  | Cloudflare 에 등록(네임서버 이전)                               |
| Cloudflare 계정         | 무료로 충분                                                     |
| Gemini API 키           | Google AI Studio                                                |
| 소셜 로그인 앱          | 네이버·카카오·구글 (선택 — 미설정 시 버튼만 동작 안 함)         |

---

## 2. 클론과 환경변수

```bash
git clone https://github.com/taekyung96/workmate.git
cd workmate
cp .env.example .env
```

`.env` 를 채운다. 비밀값은 직접 생성한다.

```bash
openssl rand -base64 32   # AES_SECRET_KEY
openssl rand -base64 16   # AES_SECRET_IV
openssl rand -base64 24   # GRAFANA_* (관측 스택 쓸 때만)
```

> **AES 키는 한 번 정하면 바꿀 수 없다.** 이메일·전화번호를 이 키로 암호화해 저장하므로,
> 키가 바뀌면 기존 데이터를 복호화하지 못한다. 운영 시작 후 교체하려면 재암호화 마이그레이션이 필요하다.

### 데모 계정은 어떻게 되나

Flyway 시드(`V2__seed_reference_data.sql`)는 데모 계정을 **넣지 않는다** — 이메일은 고정 IV 의
결정적 AES 로 암호화해 저장하고(`AesCipher`), 로그인은 입력 이메일을 같은 방식으로 암호화해
**암호문끼리** 비교한다(`UserRepository.findByEmail`). 마이그레이션에 특정 키로 암호화한 고정
암호문을 넣으면 다른 키를 쓰는 배포에서는 영원히 조회가 안 된다 — 그래서 계정 생성 자체를
**실제 AES 키를 쥔** 배포 스크립트가 맡는다.

> **계정을 새로 만드는 다른 방법은 막혀 있다.** `POST /api/auth/signup` 은 `ROLE_ADMIN` 전용인데
> (`SecurityConfig`) 신규 배포에는 관리자가 한 명도 없다. `scripts/seed-demo-accounts.js` 도
> 같은 엔드포인트를 쓰므로 함께 막힌다. 즉 아래 스크립트가 유일한 진입 경로다.

**해결 — 배포 직후 한 번 실행한다.**

```bash
./scripts/bootstrap-demo-data.sh
```

이 배포의 AES 키로 데모 계정 3종을 **직접 암호화해 생성**하고(이미 있으면 email·phone 만
현재 키로 재암호화), 캡처용 채팅·영수증·회의록·감사로그 콘텐츠까지 채운다. 비밀번호는
BCrypt(단방향, 키와 무관)라 고정 해시를 그대로 쓴다. 전부 존재 검사 기반이라 여러 번
실행해도 안전하다(멱등). 계정이 기대한 건수만큼 만들어지지 않으면 스크립트가 비정상
종료한다 — 조용히 아무 일도 안 하고 성공하는 일은 없다.

비밀번호(`Workmate!2026`)가 공개 README 에 적혀 있어, 계정은 기본적으로 `ROLE_USER` 로 둔다 —
관리자 권한이면 누구나 사용자 관리·감사로그에 들어올 수 있기 때문이다. 관리자 화면 시연이
필요한 **비공개** 환경에서만 `--grant-admin` 을 붙인다(공개 인스턴스에는 쓰지 말 것). 자동으로
켜지는 경로는 없다 — 개발·배포 어느 쪽 compose 도 승격을 자동으로 하지 않는다. 공개
인스턴스에서는 이 플래그를 쓰지 않으므로, 방문자는 채팅·영수증·가이드·회의록까지 체험하고
관리자 화면에는 들어오지 못한다.

> **로컬·데모 전용 — 공개 배포 인스턴스에서 이 스크립트를 실행하지 말 것.** 비밀번호가
> README 에 공개돼 있어, 실행하는 순간 그 비밀번호로 로그인 가능한 계정이 생긴다.

```
demo.admin@example.com / Workmate!2026
hong@example.com       / Workmate!2026
kim@example.com        / Workmate!2026
```

---

## 3. 기동

```bash
docker compose -f docker-compose.deploy.yml up -d
```

이미지는 GHCR 에서 받는다(공개 패키지라 로그인 불필요).

```
ghcr.io/taekyung96/workmate-was:latest
ghcr.io/taekyung96/workmate-web:latest
```

확인:

```bash
docker compose -f docker-compose.deploy.yml ps      # 3개 컨테이너
curl -I http://127.0.0.1:8080/                      # 200
```

### 갱신

`main` 에 머지되면 CI 가 새 이미지를 올린다. 서버에서는 받아서 다시 띄우기만 하면 된다.

```bash
docker compose -f docker-compose.deploy.yml pull
docker compose -f docker-compose.deploy.yml up -d
```

### 롤백

이미지에 `sha-<커밋해시>` 태그가 함께 붙는다.

```bash
IMAGE_TAG=sha-2877133077c022f4f3791a29133395390c9d1c8a \
  docker compose -f docker-compose.deploy.yml up -d
```

---

## 4. 스키마는 어떻게 적용되나 (Flyway)

**스키마·시드는 `db/init/*.sql` 이 아니라 Flyway 가 관리한다.** 마이그레이션 파일은
`workmate-was/src/main/resources/db/migration/` 에 있고 **WAS jar 안에 포함되어 배포된다** —
서버에 저장소를 클론하지 않아도 스키마가 따라온다. WAS 가 기동할 때(Spring Boot 자동설정)
`flyway_schema_history` 테이블을 보고 **아직 적용되지 않은 마이그레이션만 순서대로** 적용한다.

```
V1__baseline_schema.sql       -- 테이블·인덱스·제약·코멘트 (Flyway 도입 시점 스냅샷)
V2__seed_reference_data.sql   -- guide·vector_store·common_code(_group) 참조 데이터
```

세 가지 경로(새 컨테이너 / CI / 이미 떠 있는 서버)가 전부 이 한 경로로 합쳐진다 —
더 이상 "이 서버에 몇 번까지 넣었나"를 사람이 기억하거나 DB 를 뒤져 확인할 필요가 없다.

### 새 스키마 변경을 추가하려면

`db/migration/` 에 `V3__설명.sql` 처럼 다음 버전 번호로 새 파일을 추가한다.
**이미 적용된 V1·V2 파일은 절대 손으로 고치지 않는다** — Flyway 는 각 파일의 체크섬을
`flyway_schema_history` 에 저장해 두고, 내용이 바뀌면 다음 기동 시 `FlywayValidateException`
으로 기동 자체를 막는다. 새 테이블·컬럼도 프로젝트 규칙대로 `COMMENT ON` 을 **같은 파일 안에** 넣는다.

### 이미 볼륨이 있는(Flyway 이전) 서버는

`baseline-on-migrate: true` / `baseline-version: 1` 설정 덕분에, 기존 DB 는 처음 기동할 때
"V1 은 이미 적용된 것"으로 자동 도장이 찍히고 V2 부터 적용된다. V2 의 모든 INSERT 는
`ON CONFLICT DO NOTHING` 으로 돼 있어 이미 같은 데이터가 들어 있어도 실패하지 않는다.
별도 수동 작업이 필요 없다 — WAS 를 한 번 새로 띄우기만 하면 된다.

### 체크섬 불일치·baseline 문제가 생기면

`docs/development/12_OPERATIONS.md` §9 참고.

---

## 5. Cloudflare Tunnel (외부 노출)

### 도메인 없이 임시로 열기 — Quick Tunnel

도메인을 아직 안 정했다면 `trycloudflare.com` 임시 주소로 먼저 열어볼 수 있다. 계정도 토큰도 필요 없다.

```bash
docker run -d --name wm-quicktunnel --restart unless-stopped \
    --network <compose 네트워크명> \
    cloudflare/cloudflared:latest tunnel --url http://web:8080

docker logs wm-quicktunnel | grep trycloudflare.com   # 발급된 주소 확인
```

**주소는 cloudflared 가 다시 뜰 때마다 새로 발급된다** — 재부팅·도커 재시작·WSL 종료가 모두 해당한다.
실측으로 `docker restart` 와 `stop`→`start` 양쪽에서 바뀌는 것을 확인했다. 프로세스가 살아 있는
동안은 유지된다.

바뀐 주소를 README 에 반영하려면:

```bash
./scripts/update-demo-url.sh          # 터널 컨테이너에서 자동 탐지
./scripts/update-demo-url.sh <주소>   # 저장소와 docker 가 다른 환경일 때
```

Quick Tunnel 은 Cloudflare 가 **운영용이 아니라고 명시**한다(가용성 보장 없음). 고정 도메인에만
등록할 수 있는 소셜 로그인 콜백도 쓸 수 없다. 상시 공개용으로는 아래 정식 터널로 간다.

가정용 회선은 인바운드가 막혀 있는 경우가 많고, 공유기에 포트를 여는 것도 바람직하지 않다.
터널은 **아웃바운드 연결만** 쓰므로 이 문제를 통째로 우회한다.

1. Cloudflare 대시보드 → Zero Trust → Networks → Tunnels → **Create a tunnel**
2. 터널 토큰을 복사해 `.env` 에 넣는다 — `TUNNEL_TOKEN=...`
3. Public hostname 을 추가한다
    - Subdomain/Domain: 원하는 주소
    - Service: `HTTP` → `web:8080` (같은 compose 네트워크의 서비스명)
4. 기동

```bash
docker compose -f docker-compose.deploy.yml --profile tunnel up -d
```

HTTPS 인증서는 Cloudflare 가 처리하므로 Let's Encrypt 갱신을 관리할 필요가 없다.

> WEB 은 `SERVER_FORWARD_HEADERS_STRATEGY: framework` 로 `X-Forwarded-*` 를 신뢰하게 돼 있다.
> 이게 없으면 프록시 뒤에서 OAuth 리다이렉트 URI 가 `http` 로 만들어지고 세션 쿠키의 Secure 판정도 틀어진다.

---

## 6. 소셜 로그인 콜백 등록

도메인이 정해진 뒤 각 제공자 콘솔에 **콜백 URL 을 추가**해야 버튼이 동작한다.

```
https://<도메인>/login/oauth2/code/naver
https://<도메인>/login/oauth2/code/kakao
https://<도메인>/login/oauth2/code/google
```

미등록 시 로그인 시도에서 제공자가 거부한다. 이메일 로그인은 영향을 받지 않는다.

---

## 7. 자동 시작 (재부팅 생존)

컨테이너에는 이미 `restart: unless-stopped` 가 걸려 있다. 남은 것은 **도커 데몬 자체**다.

- **Linux 서버**: `sudo systemctl enable docker`
- **Windows + WSL2**: WSL 과 도커 데몬이 부팅 시 자동 실행되도록 작업 스케줄러에 등록한다.
  WSL2 는 기본적으로 데몬을 자동 시작하지 않는다(08 문서 §4.2 참고).

---

## 8. 관측 스택 (선택)

```bash
docker compose -f docker-compose.deploy.yml --profile obs up -d
```

- Prometheus 는 호스트에 포트를 열지 않는다. Grafana 만 접근한다
- Grafana 는 `127.0.0.1:3000` 루프백만 바인딩한다. 밖에서 보려면 **터널에 별도 호스트명 + Access 인증**을 붙인다. 앱과 같은 호스트명으로 열지 않는다
- `grafana_ro` 롤이 없으면 DB 데이터소스가 인증 실패한다 → `db/init/19-grafana-readonly.sh` 는 롤만 만든다.
  llm_usage 조회 권한은 WAS 가 한 번 뜬 뒤(Flyway 적용 후) `./scripts/grant-grafana-readonly.sh` 를 실행해야 붙는다

---

## 9. 자주 막히는 곳

### 기동이 안 될 때

| 증상                                                                           | 원인                                                                                                                                                           |
| ------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| WAS 가 기동 직후 죽음                                                          | AES 키 미주입. `Illegal base64 character 24` 는 `${AES_SECRET_KEY}` 가 치환되지 않았다는 뜻                                                                    |
| WEB 이 기동 직후 죽음<br>`Client id of registration 'kakao' must not be empty` | `.env` 에 소셜 자격증명을 **빈 값**으로 두면 안 된다. `${VAR:not-configured}` 기본값은 변수가 _없을 때만_ 적용된다. 주석 처리해 아예 없애거나 실제 값을 넣는다 |
| DB 가 unhealthy · init 이 중간에 끊김                                          | `db/init/*.sh` 에 실행권한이 없으면 엔트리포인트가 source 로 읽어, 스크립트의 `exit` 가 초기화 전체를 끊는다. `chmod +x` 로 커밋돼 있어야 한다                 |
| `ddl-auto: validate` 실패                                                      | Flyway 마이그레이션이 실패했거나 아직 적용 전이다 → §4, `docs/development/12_OPERATIONS.md` §9                                                                 |

### 로그인·계정

| 증상                           | 원인                                                                                                   |
| ------------------------------ | ------------------------------------------------------------------------------------------------------ |
| 데모 계정으로 로그인이 안 됨   | 배포 후 `./scripts/bootstrap-demo-data.sh` 를 아직 안 돌렸다(Flyway 는 데모 계정을 만들지 않는다) → §2 |
| 데모 계정에 관리자 메뉴가 없음 | 정상이다. 공개 배포에서는 `ROLE_USER` 로 둔다 → §2                                                     |
| 회원가입이 403                 | 의도된 동작. `/api/auth/signup` 은 `ROLE_ADMIN` 전용이다                                               |
| 소셜 로그인 버튼이 안 됨       | 콜백 URL 미등록(§6) 또는 자격증명 미주입                                                               |
| OAuth 리다이렉트가 `http://`   | `SERVER_FORWARD_HEADERS_STRATEGY` 누락                                                                 |

### 그 밖에

| 증상                            | 원인                                                                                            |
| ------------------------------- | ----------------------------------------------------------------------------------------------- |
| Grafana DB 데이터소스 인증 실패 | `grafana_ro` 비밀번호와 `.env` 불일치 → §8 참고, 필요시 `db/init/19-grafana-readonly.sh` 재실행 |
| 이미지가 옛 버전                | `pull` 을 안 했다. `latest` 는 자동 갱신되지 않는다                                             |
