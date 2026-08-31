# 07. 프로젝트 지도 — "뭘 하려면 어디를 봐야 하나"

- **목적**: 이 구조(Vue3 SPA + 얇은 WEB(BFF) + WAS 3-tier)를 처음 다룰 때, **어떤 작업을 하려면 어느 파일을 열어야 하는지** 빠르게 찾는 지도.
- **함께 보기**: [01_ARCHITECTURE](01_ARCHITECTURE.md) · [02_FRONTEND_STRUCTURE_GUIDE](02_FRONTEND_STRUCTURE_GUIDE.md) · [07_BUILD_WIRING](07_BUILD_WIRING.md)

---

## 0. 전체 구조 한눈에

```
workmate/
├── workmate-vue/     프론트엔드 (Vue3 SPA) — 화면·라우팅·상태·API호출
├── workmate-web/     얇은 BFF (:8080) — 세션인증·CSRF·/api 프록시·SSE중계·SPA서빙
├── workmate-was/     비즈니스/AI (:8081) — Spring AI·JPA·DB (주인공, 대체로 유지·필요 시 수정)
├── db/init/*.sql     DB 스키마 (초기화 스크립트)
├── docker-compose.yml  DB·WAS·WEB 컨테이너 정의
├── .env              비밀값(DB비번·Gemini키·AES키) — git 미추적
├── settings.gradle / gradlew / gradle/   Gradle 루트(멀티모듈: web·was)
└── docs/             설계·개발 문서
```

**요청이 흐르는 순서** (개발 모드): `브라우저 → Vite(5173) → WEB(8080) → WAS(8081) → DB(5432)`
운영 모드: Vite 없이 `브라우저 → WEB(8080, SPA도 서빙) → WAS → DB`

---

## 1. 실행 방법 (빠른 참조)

| 하고 싶은 것     | 명령                               | 비고                                     |
| ---------------- | ---------------------------------- | ---------------------------------------- |
| DB만 띄우기      | `docker compose up -d db`          | pgvector PostgreSQL 17                   |
| WAS 실행         | `./gradlew :workmate-was:bootRun`  | 8081. `.env`의 Gemini·AES 키 필요        |
| WEB 실행         | `./gradlew :workmate-web:bootRun`  | 8080. Vue 빌드까지 자동(06 문서)         |
| 프론트 개발 서버 | `cd workmate-vue && npm run dev`   | 5173, 핫리로드. `/api`는 8080으로 프록시 |
| 프론트 빌드      | `cd workmate-vue && npm run build` | `dist/` 생성                             |
| 프론트 타입체크  | `cd workmate-vue && npm run build` | vue-tsc 포함                             |

> 통합 검증(운영과 동일): DB + WAS + `./gradlew :workmate-web:bootRun` → 브라우저로 **8080** 접속.

---

## 1-2. 서버 내리기 (종료)

올린 순서의 역순(프론트 → WEB → WAS → DB)으로 내리면 깔끔하다.

### ① 먼저 지금 뭐가 떠 있는지 확인

이 프로젝트가 쓰는 포트(5173·8080·8081·5432)에 무엇이 물려 있는지 본다.

- **PowerShell** (포트 + PID + 프로세스명까지):

    ```powershell
    Get-NetTCPConnection -State Listen |
      Where-Object LocalPort -in 5173,8080,8081,5432 |
      Select-Object LocalPort, OwningProcess,
        @{ n='Process'; e={ (Get-Process -Id $_.OwningProcess).ProcessName } }
    ```

- **Git Bash / cmd** (`netstat`로 확인 — 맨 끝 숫자가 PID):

    ```bash
    netstat -ano | findstr "LISTENING" | findstr ":5173 :8080 :8081 :5432"
    ```

    출력 예: `TCP  0.0.0.0:8081  0.0.0.0:0  LISTENING  19932` → 여기서 **19932**가 PID.

### ② 확인한 포트/PID로 종료

위에서 찾은 포트나 PID로 아래처럼 내린다.

| 내릴 대상            | 방법                                                                                         | 비고                                                             |
| -------------------- | -------------------------------------------------------------------------------------------- | ---------------------------------------------------------------- |
| 포그라운드로 띄운 것 | 해당 터미널에서 **`Ctrl + C`**                                                               | `bootRun`·`npm run dev`를 직접 실행한 창이 있으면 이게 제일 쉬움 |
| WAS(8081) 종료       | `Stop-Process -Id (Get-NetTCPConnection -LocalPort 8081 -State Listen).OwningProcess -Force` | PowerShell. 백그라운드/창을 닫아버렸을 때 포트로 종료            |
| WEB(8080) 종료       | 위 명령에서 포트만 `8080` 으로                                                               | WEB 재기동 시 세션이 초기화돼 재로그인 필요                      |
| 프론트(5173) 종료    | 위 명령에서 포트만 `5173` 으로                                                               | vite 개발 서버                                                   |
| DB 종료(컨테이너)    | `docker compose down`                                                                        | 컨테이너·네트워크만 제거, **데이터(볼륨)는 보존**                |
| DB 완전 삭제         | `docker compose down -v`                                                                     | ⚠️ `-v`는 **볼륨까지 삭제** — 사용자·가이드·임베딩 전부 날아감   |

> **포트로 죽이는 이유**: `bootRun`을 백그라운드로 돌렸거나 실행 창을 닫아 PID를 모를 때, "그 포트를 물고 있는 프로세스"를 찾아 종료하는 게 확실하다.
> Git Bash/cmd라면 `netstat -ano | findstr :8081` 로 PID를 찾고 `taskkill /PID <PID> /F` 로 종료해도 된다.
> WSL에서 docker를 쓰는 환경이면 `wsl -e bash -lc "cd <저장소 루트> && docker compose down"` 형태로 감싸 실행한다(WSL 안에서 Windows 경로는 `/mnt/c/...` 형태로 잡힌다).

---

## 2. ⭐ "~하려면 어디를 봐?" (제일 자주 쓰는 표)

### 프론트엔드 (workmate-vue)

| 하고 싶은 것                                  | 파일                                                                       |
| --------------------------------------------- | -------------------------------------------------------------------------- |
| **라우트(화면 경로) 추가·수정**               | `src/modules/{기능}/routes.ts` (모듈별 정의) → `src/router/index.ts`(취합) |
| **로그인 가드·권한 체크**                     | `src/router/guards.ts`                                                     |
| **API 서버 주소·CSRF·401 처리**               | `src/common/api/client.ts` (axios 공통 인스턴스)                           |
| **개발서버 포트·프록시(/api→8080)·`@` 경로**  | `src/../vite.config.ts`                                                    |
| **특정 기능의 API 호출 함수**                 | `src/modules/{기능}/api/{기능}.api.ts`                                     |
| **전역 상태(로그인·채팅 등)**                 | `src/modules/{기능}/stores/{기능}.store.ts` (Pinia)                        |
| **화면(페이지)**                              | `src/modules/{기능}/views/*.vue`                                           |
| **화면 로직(≈ 서비스)**                       | `src/modules/{기능}/composables/use*.ts`                                   |
| **공통 UI 부품(버튼·다이얼로그 등)**          | `src/common/components/ui/` (shadcn-vue)                                   |
| **레이아웃(사이드바·헤더)**                   | `src/common/components/layout/` (AppLayout·AppSidebar·AuthLayout)          |
| **색상·폰트·간격(디자인 토큰)**               | `src/styles/index.css` (Tailwind v4 + CSS 변수)                            |
| **앱 부팅·전역 등록(pinia·router·401핸들러)** | `src/main.ts`                                                              |
| **shadcn 컴포넌트 설치 경로 설정**            | `components.json`                                                          |
| **`@/…` 경로 별칭**                           | `tsconfig.app.json` + `vite.config.ts` (양쪽)                              |
| **들여쓰기·포맷 규칙**                        | `.prettierrc.json`, `.editorconfig`                                        |

### WEB (얇은 BFF, workmate-web)

| 하고 싶은 것                       | 파일                                                                                      |
| ---------------------------------- | ----------------------------------------------------------------------------------------- |
| **세션 인증·CSRF·접근권한 규칙**   | `.../global/config/SecurityConfig.java`                                                   |
| **로그인 성공/실패 JSON 응답**     | `.../global/security/LoginSuccessHandler.java` / `LoginFailureHandler.java`               |
| **WAS로 인증 위임(비번 검증)**     | `.../global/security/WasAuthenticationProvider.java`                                      |
| **세션에 담기는 사용자 정보**      | `.../global/security/LoginUser.java`                                                      |
| **CSRF 쿠키 발급 필터**            | `.../global/security/CsrfCookieFilter.java`                                               |
| **`/api/*` 프록시(화면→WAS 중계)** | `.../{기능}/controller/*Controller.java`                                                  |
| **WAS 주소·HTTP 클라이언트**       | `.../global/config/WasProperties.java`·`RestClientConfig.java`·`WebClientConfig.java`     |
| **SPA 정적 서빙·딥링크 fallback**  | `.../global/web/SpaWebConfig.java`                                                        |
| **공통 응답 포맷·예외 처리**       | `.../global/response/ApiResponse.java`·`.../global/exception/GlobalExceptionHandler.java` |
| **WEB 포트·WAS주소 등 환경값**     | `src/main/resources/application-{local,docker,prod}.yml`                                  |
| **의존성·Vue 빌드 연동**           | `workmate-web/build.gradle`                                                               |

> WEB 자바 패키지 루트: `workmate-web/src/main/java/com/workmate/web/`

### WAS (주인공, workmate-was — 대부분 손대지 않음)

| 하고 싶은 것                                    | 파일                                                              |
| ----------------------------------------------- | ----------------------------------------------------------------- |
| **AI(Gemini) 모델·임베딩 설정**                 | `src/main/resources/application.yml` → `spring.ai.google.genai.*` |
| **개인정보 암호화·채팅 옵션(context·rate·RAG)** | `application.yml` → `app.crypto.*`·`app.chat.*`                   |
| **DB 접속 정보**                                | `application-local.yml`(로컬)·`application-docker.yml`(컨테이너)  |
| **비즈니스 로직**                               | `.../{기능}/{controller,service,dao,vo}`                          |

---

## 3. 환경설정 파일 총정리 (헷갈리기 쉬움)

| 파일                     | 위치         | 역할                                                                                                            |
| ------------------------ | ------------ | --------------------------------------------------------------------------------------------------------------- |
| **`.env`**               | 루트         | 🔑 비밀값. `POSTGRES_*`·`GEMINI_API_KEY`·`AES_SECRET_KEY/IV`. docker와 앱(spring-dotenv)이 읽음. **git 미추적** |
| `.env.example`           | 루트         | `.env` 템플릿(빈 값)                                                                                            |
| `docker-compose.yml`     | 루트         | DB·WAS·WEB 컨테이너. `${VAR}`는 `.env`에서 채움                                                                 |
| `application.yml`        | web·was 각각 | 공통 설정(포트·프로파일 등)                                                                                     |
| `application-local.yml`  | web·was 각각 | 로컬 실행값(DB주소·WAS주소). **git 미추적**(`.example` 복사해 생성)                                             |
| `application-docker.yml` | web·was 각각 | 컨테이너 실행값(서비스명으로 통신)                                                                              |
| `vite.config.ts`         | workmate-vue | 개발서버(5173)·`/api`→8080 프록시·`@` 별칭·Tailwind                                                             |
| `components.json`        | workmate-vue | shadcn-vue 컴포넌트 경로(`common/components/ui`)                                                                |
| `tsconfig*.json`         | workmate-vue | TS 설정·`@/*` 경로                                                                                              |

> **비밀값은 항상 `.env`/`application-local.yml`에만** 두고 git에 올리지 않는다. (둘 다 `.gitignore` 처리됨)

---

## 4. 예시: "로그인" 요청 하나가 지나가는 파일들

화면 클릭부터 DB까지 어떤 파일을 거치는지 따라가 보면 구조가 한눈에 들어온다.

```
① LoginPage.vue            (modules/auth/views)      — 사용자가 이메일·비번 입력, 로그인 클릭
② useAuth.ts               (modules/auth/composables) — login() 로직 호출
③ auth.api.ts              (modules/auth/api)        — POST /api/auth/login (form-urlencoded)
④ client.ts                (common/api)              — axios가 CSRF 헤더 붙여 전송
   └─(Vite 프록시 or WEB 직접)─▶
⑤ SecurityConfig.java      (web/global/config)       — 폼 로그인 필터가 가로챔
⑥ WasAuthenticationProvider(web/global/security)     — WAS에 자격 검증 위임
   └─(REST)─▶ WAS ─▶ DB (비번 BCrypt 대조)
⑦ LoginSuccessHandler.java (web/global/security)     — 성공 → 200 JSON + 사용자정보 + 세션쿠키
⑧ auth.store.ts            (modules/auth/stores)     — 사용자 저장
⑨ router/guards.ts                                    — /chat 으로 이동 허용
```

새로운 기능을 만들 때도 이 순서(**view → composable → api → (WEB 프록시) → WAS**)를 그대로 따라가면 된다.

---

## 5. 새 기능(모듈) 추가 체크리스트

프론트에 기능 하나 추가할 때:

1. `src/modules/{기능}/` 폴더 생성 → `views/` `composables/` `stores/`(필요시) `api/` `types.ts` `routes.ts`
2. `routes.ts`에 라우트 정의 → `src/router/index.ts`에서 `...{기능}Routes` 로 취합
3. API가 새 엔드포인트를 쓰면 WEB에 프록시 컨트롤러 추가(`workmate-web/.../{기능}/controller`)
4. 공통으로 쓸 부품이면 `src/common/`으로, 그 기능 전용이면 모듈 안에 둔다 (모듈 경계 유지)
