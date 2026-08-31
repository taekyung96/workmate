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
| Git                     | `db/init/*.sql` 이 필요해 클론한다                              |
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

## 4. ⚠️ 스키마 변경을 반영하는 법

**`db/init/*.sql` 은 DB 볼륨을 처음 만들 때만 실행된다.** 이미 볼륨이 있는 서버에는
나중에 추가된 스크립트가 자동 적용되지 않고, `ddl-auto: validate` 가 실패하며 WAS 가 기동하지 못한다.

누락분만 수동으로 적용한다. 각 스크립트 머리말에 적용 명령이 적혀 있다.

```bash
docker exec -i workmate-db psql -U <POSTGRES_USER> -d <POSTGRES_DB> < db/init/16-llm-usage.sql
```

`.sh` 스크립트는 환경변수가 필요하다.

```bash
docker exec -i -e GRAFANA_DB_PASSWORD="$GRAFANA_DB_PASSWORD" workmate-db \
  bash /docker-entrypoint-initdb.d/19-grafana-readonly.sh
```

> 지금은 마이그레이션 도구 없이 SQL 파일로 관리한다. Flyway 도입은 후속 과제다.

---

## 5. Cloudflare Tunnel (외부 노출)

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
- `grafana_ro` 롤이 없으면 DB 데이터소스가 인증 실패한다 → §4 의 `.sh` 적용

---

## 9. 자주 막히는 곳

| 증상                            | 원인                                                                                        |
| ------------------------------- | ------------------------------------------------------------------------------------------- |
| WAS 가 기동 직후 죽음           | AES 키 미주입. `Illegal base64 character 24` 는 `${AES_SECRET_KEY}` 가 치환되지 않았다는 뜻 |
| `ddl-auto: validate` 실패       | 기존 볼륨에 신규 스키마 미적용 → §4                                                         |
| 소셜 로그인 버튼이 안 됨        | 콜백 URL 미등록(§6) 또는 자격증명 미주입                                                    |
| OAuth 리다이렉트가 `http://`    | `SERVER_FORWARD_HEADERS_STRATEGY` 누락                                                      |
| Grafana DB 데이터소스 인증 실패 | `grafana_ro` 비밀번호와 `.env` 불일치 → §4 의 `.sh` 재실행                                  |
| 이미지가 옛 버전                | `pull` 을 안 했다. `latest` 는 자동 갱신되지 않는다                                         |
