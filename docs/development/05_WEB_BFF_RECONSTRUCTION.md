# 05. 얇은 WEB(BFF) 재구성 방향

- **상태**: 백엔드 재구성 **완료**. 남은 것: 빌드 연결(dist 서빙, HANDOVER 4단계) + 통합 검증
- **전제**: v2 `workmate-web`을 재구성 base로 복사 완료. 여기서 **Thymeleaf 서빙 로직을 걷어내고**, SPA 정적서빙 + `/api` 프록시 + SSE 중계 + 세션 인증만 남긴다.

> 원칙: WAS는 무변경(주인공). v3의 변화는 프론트(신규 SPA)와 WEB(얇은 BFF 전환)에 국한.

---

## 1. 재구성 후 WEB의 책임 (남길 4가지)

1. **SPA 정적 서빙** — Vue 빌드 산출물(`workmate-vue/dist`)을 서빙, SPA 라우팅용 fallback(비-`/api` 경로 → `index.html`)
2. **세션 인증** — Spring Security 세션(httpOnly 쿠키, CSRF on). 중복 로그인 차단·즉시 무효화(F1-08) 때문에 JWT 아님
3. **`/api` 프록시** — 브라우저의 `/api/**` 요청을 내부망 WAS(:8081)로 중계. WEB은 DB 직접 접근 금지
4. **SSE 중계** — 채팅 스트리밍(text/event-stream)을 버퍼링 없이 WAS→브라우저로 흘려보냄

---

## 2. 유지 / 제거 목록 (복사된 v2 web 기준)

### ✅ 유지 (그대로 또는 소폭 조정)

| 구분            | 대상                                                                                                                                                   | 비고                                                  |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------- |
| 프록시 REST     | `auth/AuthController`, `chat/ChatController`, `guide/GuideController`, `receipt/ReceiptController`, `admin/AdminController`, `common/CommonController` | WAS 호출 담당 (BFF 핵심). SSE는 ChatController가 중계 |
| 서비스 계층     | 각 기능 `*Service` / `*ServiceImpl`                                                                                                                    | WAS 호출 로직                                         |
| 보안            | `global/security/*` (SecurityConfig·LoginUser·WasAuthenticationProvider·LoginFailureHandler)                                                           | 세션 인증. **SecurityConfig는 조정 필요** (아래 3장)  |
| WAS 연동 config | `global/config/*` (RestClientConfig·WebClientConfig·WasProperties)                                                                                     | WAS 엔드포인트·HTTP 클라이언트                        |
| 공통 응답/예외  | `global/response/ApiResponse`, `global/exception/GlobalExceptionHandler`                                                                               | REST 공통 규약 유지                                   |

### ❌ 제거 (Thymeleaf 서빙 계층)

| 대상                                                                                             | 이유                                                                   |
| ------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------- |
| `*ViewController` (Admin·Chat·Guide·Receipt)                                                     | Thymeleaf 페이지 렌더링 진입점 → SPA가 대체                            |
| `global/web/ErrorViewController`, `global/web/GlobalViewAdvice`                                  | Thymeleaf 뷰/모델 어드바이스 → SPA에선 JSON 에러 + SPA fallback로 대체 |
| `resources/templates/**/*.html` (login·signup·chat·receipt·guide-\*·admin-users·fragments·error) | SPA 화면으로 이전                                                      |
| `resources/static/**` (css·js·common-ui)                                                         | Vue 빌드 산출물이 대체                                                 |
| `build.gradle`의 `spring-boot-starter-thymeleaf` 의존성                                          | 더 이상 불필요                                                         |

---

## 3. SecurityConfig 조정 포인트 (SPA 대응)

v2는 form login이 실패/성공 시 **Thymeleaf 페이지로 redirect**하는 구조다. SPA에선 이걸 **JSON 응답 기반**으로 바꿔야 한다.

- **인증 실패/미인증**: 로그인 페이지 redirect(302) 대신 **401 JSON** 반환 (`authenticationEntryPoint`). SPA가 401을 받으면 라우터가 `/login`으로 이동
- **로그인 성공/실패**: redirect 대신 **JSON(성공 사용자 정보 / 실패 사유)** 반환 (`LoginFailureHandler`는 이미 있으니 successHandler도 JSON화)
- **CSRF**: 켠 상태 유지. SPA는 `XSRF-TOKEN` 쿠키를 읽어 `X-XSRF-TOKEN` 헤더로 전송 (`CookieCsrfTokenRepository.withHttpOnlyFalse()`)
- **정적 리소스 + SPA fallback**: `/`, `/assets/**`, `index.html` 등은 permitAll, 비-`/api` 경로는 `index.html`로 forward (SPA 딥링크 새로고침 대응)
- **`/api/**`\*\*: 인증 필요(로그인·회원가입·health 제외), 통과 시 프록시 컨트롤러로

---

## 3-A. 인증 JSON 계약 (프론트 연동용 — 구현 완료)

SPA `auth.store` / `common/api/client.ts`가 이 계약을 그대로 사용한다. 모든 응답은 공통 `ApiResponse { success, message, result }` 포맷.

| 엔드포인트         | 메서드 | 요청                                                                    | 성공                                                 | 실패                                               |
| ------------------ | ------ | ----------------------------------------------------------------------- | ---------------------------------------------------- | -------------------------------------------------- |
| `/api/auth/login`  | POST   | **form-urlencoded** `email`,`password` (Security 폼 로그인 필터가 처리) | 200 `{success:true, result:{userSeq,userName,role}}` | 401 `{success:false, message:"사유"}`              |
| `/api/auth/signup` | POST   | **JSON** `{email,password,userName,phone}`                              | 200 `{success:true}`                                 | 200 `{success:false, message:"사유"}` (검증은 WAS) |
| `/api/auth/me`     | GET    | —                                                                       | 200 `{success:true, result:{userSeq,userName,role}}` | 401 (미로그인)                                     |
| `/api/auth/logout` | POST   | — (CSRF 토큰 필요)                                                      | 200 `{success:true}`                                 | —                                                  |

**프론트가 지켜야 할 것**

- **로그인만 form-urlencoded**로 보낸다(Spring Security 폼 로그인 필터 사용). 그 외 쓰기 API는 JSON.
- **CSRF**: 서버가 `XSRF-TOKEN` 쿠키(httpOnly=false)를 내려준다. SPA는 이 쿠키 값을 읽어 **모든 상태변경 요청(POST/PUT/DELETE)에 `X-XSRF-TOKEN` 헤더**로 실어 보낸다. (axios면 `xsrfCookieName`/`xsrfHeaderName` 기본값과 일치 — 자동 처리)
- **미인증 401**을 받으면 api client 인터셉터가 auth.store를 비우고 `/login`으로 유도.
- 앱 부팅/새로고침 시 **`GET /api/auth/me`** 로 세션 복원(200이면 로그인 상태, 401이면 로그아웃 상태).

## 4. 다음 세션 작업 순서(제안)

1. `build.gradle`에서 thymeleaf 의존성 제거, SPA 정적 서빙 설정 확인
2. `*ViewController`·`global/web/*`·`templates/**`·`static/**` 제거
3. `SecurityConfig`를 SPA 대응(위 3장)으로 조정
4. SPA fallback 컨트롤러(또는 정적 리소스 핸들러) 추가
5. `workmate-vue` 빌드 산출물을 WEB이 서빙하도록 빌드 연결(HANDOVER 4단계)
6. 로그인 왕복(SPA→WEB 세션→WAS)으로 통합 검증

> 상세 아키텍처: [01_ARCHITECTURE.md](01_ARCHITECTURE.md) · 프론트 계층: [02_FRONTEND_STRUCTURE_GUIDE.md](02_FRONTEND_STRUCTURE_GUIDE.md)
