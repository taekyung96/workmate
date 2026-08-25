# 01. 아키텍처 설계 (v3 SPA)

- **상태**: 설계 확정 (구현 대기)
- **상위**: [HANDOVER](../project/HANDOVER.md) · **결정 근거**: [ADR-0001](../project/adr/0001-hybrid-ssr-to-vue3-spa.md)

이 문서는 workmate-v3의 아키텍처를 확정한다. 프론트 디렉토리·계층 상세는 [02_FRONTEND_STRUCTURE_GUIDE](02_FRONTEND_STRUCTURE_GUIDE.md) 참조.

---

## 1. 모듈 구조 & 역할

```
브라우저 (Vue3 SPA)
   │  HTTP(세션 쿠키) + SSE(스트리밍)
workmate-web (:8080) ── 얇은 BFF
   │   • Vue SPA 정적파일 서빙 (index.html 셸 + 번들)
   │   • Spring Security 세션 인증 (httpOnly 쿠키, CSRF on)
   │   • /api/** → WAS 프록시 (JSON 중계)
   │   • /api/**/stream → SSE 중계
   │   • 최소 정적 에러 폴백(500/503) — "SPA가 못 뜰 때"용
   │  REST + 스트리밍 relay (내부망)
workmate-was (:8081) ── 비즈니스 로직 · JPA/MyBatis · Spring AI (Gemini 2.5 Flash)
   │   ★ v2에서 복사(초기), 필요 시 수정 가능. 내부망 — 브라우저에 직접 노출 안 됨
   │  JDBC
PostgreSQL 17 + pgvector
```

**핵심 원칙**

1. **WAS 최소 변경** — 주인공(AI 백엔드) 보호가 우선. 초기엔 v2 그대로 복사하고, 기능상 필요하면 수정 가능. v3 변화의 무게중심은 프론트(신규 SPA) + WEB(얇은 BFF 전환).
2. **WEB은 화면을 안 그림** — Thymeleaf 페이지 로직 제거, BFF(서빙+세션+프록시+SSE)만 담당.
3. **브라우저는 8080만** — WAS 내부망 은닉으로 공격 표면 축소.
4. **3-tier 유지** — 표현(Vue SPA) / 로직(WAS) / 데이터(PostgreSQL). 표현 계층이 서버렌더 → 클라이언트렌더로 이동한 것.

---

## 2. 인증 (세션 기반, JWT 아님)

- Spring Security 세션 인증을 **WEB 레이어**에 유지 (v2 계승).
- 세션 ID는 **httpOnly 쿠키** → JS가 못 읽어 XSS 토큰 탈취 방어.
- **CSRF 방어 필수** (쿠키 기반이므로) — Spring Security CSRF 활성화, SPA는 CSRF 토큰을 헤더로 전송.
- 요구사항 F1-06(계정잠금)·F1-08(중복로그인 차단)은 **서버 세션 즉시 무효화**로 구현 (JWT stateless로는 어려운 부분).
- SPA의 `api/client`는 **401 응답 시 자동 로그아웃 + 로그인 이동** 인터셉터를 둔다.

---

## 3. 빌드 연결 (Vite ↔ 얇은 WEB)

| 환경     | 방식                                                                                                                           |
| -------- | ------------------------------------------------------------------------------------------------------------------------------ |
| **개발** | Vite dev server(:5173) + `server.proxy`로 `/api` → WEB(:8080). HMR(핫리로드) 유지하며 실제 API 호출                            |
| **운영** | `vite build` 산출물을 WEB이 서빙(`static/`). **SPA fallback**: `/api`·정적자원 외 모든 경로 → `index.html` (Vue Router가 처리) |

- Tailwind v4는 `@tailwindcss/vite` 플러그인으로 Vite에 통합.
- Gradle 빌드에 `npm run build` 연동하거나 산출물 커밋 (v2 관례 계승 — [09_COMPATIBILITY_MATRIX](09_COMPATIBILITY_MATRIX.md) 참조).

---

## 4. 에러 전략 (2계층)

| 계층                  | 처리                                                     | 위치                                            |
| --------------------- | -------------------------------------------------------- | ----------------------------------------------- |
| **앱 내부 에러**      | 없는 경로(404), API 실패(4xx/5xx), 401→로그인 리다이렉트 | Vue: `router` 캐치올 + `api/client` 공통 핸들링 |
| **앱이 못 뜨는 에러** | WEB 다운·번들 로딩 실패·Security 차단 (Vue 실행 전)      | WEB의 **정적 500/503 폴백** (Vue 비의존)        |

> 근거: Vue가 못 뜨는 상황에선 Vue로 만든 에러 페이지를 그릴 수 없으므로, 서버 폴백이 안전망.

---

## 5. 테스트 전략

- **프론트**: Vitest(단위) — 핵심 흐름 위주(로그인·채팅·페이징 등). 과설계 지양(YAGNI).
- **백엔드**: v2의 WAS 테스트 그대로 계승 (복사되므로).
- **E2E**: v2처럼 수동 체크리스트 유지 (로그인~채팅 흐름). [FEATURE_SPEC 완료조건](../project/FEATURE_SPEC.md) 기준.

---

## 6. v2 대비 변경 요약

| 항목      | v2 (하이브리드)              | v3 (SPA)                                |
| --------- | ---------------------------- | --------------------------------------- |
| 표현 계층 | Thymeleaf SSR + Vue UMD 조각 | **Vue3 단독 SPA** (Router·Pinia·Vite)   |
| WEB 역할  | HTML 페이지 렌더             | **얇은 BFF** (정적서빙+프록시+세션+SSE) |
| 화면 이동 | 서버 왕복(멀티페이지)        | 클라이언트 라우팅                       |
| 스타일    | 순수 CSS(common.css)         | **Tailwind v4 + shadcn-vue**            |
| WAS       | REST+Spring AI               | **대체로 동일 (필요 시 수정)**          |
| 인증      | 세션                         | **동일 (세션 유지)**                    |
| DB        | PostgreSQL+pgvector          | **동일**                                |
