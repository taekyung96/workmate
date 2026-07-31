# 📌 HANDOVER — 여기부터 읽으세요 (콜드스타트 진입점)

> 이 문서는 **기록이 없는 새 Claude Code 세션**이 workmate-v3 개발을 이어받을 때
> 가장 먼저 읽는 문서다. 설계 단계(별도 세션)에서 내린 **모든 결정과 그 근거**,
> 현재 상태, 다음 할 일을 담는다.

- **현재 단계**: 설계 완료 → **개발 시작 직전** (코드는 아직 없음, 문서만 존재)
- **관련 문서**: [ADR](adr/) · [아키텍처](../development/01_ARCHITECTURE.md) · [프론트 구조 가이드](../development/02_FRONTEND_STRUCTURE_GUIDE.md) · [로드맵](ROADMAP.md)

---

## 1. 이 프로젝트가 뭔가

**Workmate = Spring AI 기반 업무 자동화 비서 웹 애플리케이션.**

- **핵심은 "AI를 어떻게 활용하는가"**: Spring AI 기반의 스트리밍 응답(SSE)·RAG(검색 증강 생성)·Tool Calling(자연어→DB 조회)·멀티모델 스위칭 설계가 이 프로젝트의 정체성이다.
- 주요 기능: ① 스트리밍 채팅(SSE) ② 문서 RAG ③ Tool Calling(자연어→DB 조회) ④ 영수증 자동 인식 · 관리자 (영수증은 부가 기능)
- **v3 재설계 배경**: 기존 Thymeleaf SSR + Vue UMD 하이브리드 구조를 **Vue3 단독 SPA**로 전면 재설계(유지보수성·UX 향상). 단, **주인공은 어디까지나 AI 백엔드**이고 프론트는 "제대로 된 SPA"면 충분하다.

> 배경 상세: [PROJECT_BACKGROUND_V2.md](PROJECT_BACKGROUND_V2.md) · 기능 요구사항: [FEATURE_SPEC.md](FEATURE_SPEC.md)

---

## 2. 아키텍처 한 장 요약 (설계 결과)

```
브라우저 (Vue3 SPA)
   │  HTTP(세션 쿠키) + SSE
workmate-web (:8080) — 얇은 BFF: SPA 정적파일 서빙 + 세션 인증(Spring Security) + /api 프록시 + SSE 중계
   │  REST + 스트리밍 relay (내부망)
workmate-was (:8081) — 비즈니스 로직 · JPA/MyBatis · Spring AI (Gemini 2.5 Flash)   ← v2에서 복사, 무변경
   │  JDBC
PostgreSQL 17 + pgvector
```

- **3-tier 유지**: 표현(Vue SPA) / 로직(WAS) / 데이터(PostgreSQL)
- **브라우저는 8080만 바라봄**, WAS는 내부망에 숨김 (보안 경계)
- **모노레포** 단일 저장소 (`workmate-was` / `workmate-web` / `workmate-vue`)

---

## 3. 핵심 결정과 근거 (면접에서 물어보면 이대로 답한다)

| 결정                                         | 왜                                                                                                        | 상세                                              |
| -------------------------------------------- | --------------------------------------------------------------------------------------------------------- | ------------------------------------------------- |
| **하이브리드 SSR → Vue3 SPA**                | v2는 Vue를 부분 삽입만 해 Vue3 강점(Router·Pinia·Vite·Composition)을 못 씀                                | [ADR-0001](adr/0001-hybrid-ssr-to-vue3-spa.md)    |
| **얇은 WEB(BFF) 유지** (단일 부트 합치기 ❌) | WAS(주인공)를 안 건드리고 내부망에 보호, 세션·SSE 중계 담당                                               | [ADR-0001](adr/0001-hybrid-ssr-to-vue3-spa.md)    |
| **세션 인증** (JWT ❌)                       | 요구사항에 중복 로그인 차단·즉시 무효화(F1-08)가 있어 stateless JWT와 상충. httpOnly 쿠키로 XSS 방어 우위 | [ADR-0001](adr/0001-hybrid-ssr-to-vue3-spa.md)    |
| **모노레포** (저장소 분리 ❌)                | v1의 3-저장소 분리 불편을 v2에서 이미 교정. 1인 프로젝트엔 단일 저장소가 유리                             | —                                                 |
| **기능별 모듈 + 공통 모듈** 구조             | 유지보수·소스 분석 용이, 모듈 경계 습관 확립                                                              | [ADR-0002](adr/0002-frontend-structure-and-ui.md) |
| **shadcn-vue + Tailwind v4**                 | 적은 노력으로 전문가급 UI(프론트는 비주인공) + 컴포넌트 소유 → 공통 모듈과 시너지                         | [ADR-0002](adr/0002-frontend-structure-and-ui.md) |

> ⚠️ **하지 말 것**: WAS의 AI 로직을 재설계하지 마라. v3의 변화는 프론트(신규 SPA)와 WEB(얇은 BFF로 전환)에 국한된다.

---

## 4. 개발 단계 셋업 체크리스트 (이 순서로 시작)

현재 이 폴더엔 **문서만** 있다. 코드는 아래 순서로 세팅한다:

- [ ] **1. WAS 복사** — **원본 위치: `C:\ClaudeCode\workmate-ws\workmate`** (v2 저장소). 여기서 아래를 이 저장소(`workmate-v3`)로 복사
    - `workmate-was/` (AI 로직 무변경)
    - `db/init/*.sql` (스키마 — `ddl-auto: validate`)
    - `docker-compose.yml`, gradle 루트(`settings.gradle`·`build.gradle`·`gradlew*`·`gradle/`), `gradle.properties`
    - `.env.example` (실제 `.env`는 새로 작성, git 미추적)
    - > ⚠️ **`.git` 딸려오지 않게 주의**: v2의 `.git`은 저장소 **루트**(`workmate/.git`)에 있다. **하위 폴더(`workmate-was/` 등)만 골라 복사**하면 `.git`은 안 따라온다. 루트 전체를 통째로 복사하지 말 것.
    - > 복사 후 이 저장소에서 **새로 `git init`** 하여 독립된 이력으로 시작한다 (v2 이력과 분리). 혹시 실수로 `.git`이 딸려왔으면 그 `.git` 폴더를 지우고 새로 init.
- [ ] **2. 얇은 WEB 재구성** — v2 `workmate-web`에서 Thymeleaf 페이지 로직 제거, SPA 정적서빙 + `/api` 프록시 + SSE 중계 + Spring Security 세션만 남김
- [ ] **3. Vue3 SPA 스캐폴딩** — `workmate-vue`를 SPA로 승격 (아래 명령)
- [ ] **4. 빌드 연결** — Vite dev proxy(5173→8080) / 운영 빌드 산출물 → WEB 서빙 ([아키텍처 §빌드](../development/01_ARCHITECTURE.md))
- [ ] **5. 구현** — [ROADMAP.md](ROADMAP.md) 순서대로

```bash
# Vue3 SPA + shadcn-vue 초기 세팅 (workmate-vue 안에서)
npm create vue@latest .            # Router·Pinia·TS 선택
npm install
npx shadcn-vue@latest init         # components.json 생성 (경로를 common/ 모듈로 설정)
npx shadcn-vue@latest add button dialog ...   # 필요한 컴포넌트만
```

---

## 5. 구현 순서 (리스크 낮은 것부터)

1. **골격 + 로그인** — router·layout·authStore + 로그인/회원가입 (인증·가드가 뼈대)
2. **가이드 목록·상세** — 가장 단순한 CRUD로 "프록시→화면" 왕복 패턴 확립
3. **채팅(SSE)** — 가장 까다로움, 패턴 확립 후
4. **영수증** — 이미지 업로드 + 분석/이력 탭
5. **가이드 RAG 출처표시 + 관리자** (사용자·감사로그)

> 상세: [ROADMAP.md](ROADMAP.md)

---

## 6. 지켜야 할 규칙 (요약 — 상세는 CLAUDE.md)

- 프론트 계층: **view → composable/store → api** (화면은 얇게, api 직접호출 지양)
- 공통 부품(안내창·페이징·버튼 등)은 **`common/` 모듈**로, 기능 코드는 **`modules/{기능}/`** 로 콜로케이션
- 모듈 간 내부 import 금지 (경계 존중)
- WEB은 DB 직접 접근 금지 — `/api` 프록시로만 WAS 호출
- 스타일링은 **Tailwind + shadcn-vue** (순수 CSS·인라인 style 지양)
