# 📌 HANDOVER — 여기부터 읽으세요 (콜드스타트 진입점)

> 이 문서는 **기록이 없는 새 Claude Code 세션**이 workmate 개발을 이어받을 때
> 가장 먼저 읽는 문서다. 설계 단계(별도 세션)에서 내린 **모든 결정과 그 근거**,
> 현재 상태, 다음 할 일을 담는다.

- **현재 상태**: 기능 구현 완료, 배포 경로 검증 완료. **공개 데모는 도메인 확보 전까지 내려 둔 상태** — 아래 §0 참고.
- **관련 문서**: [ADR](adr/) · [아키텍처](../development/01_ARCHITECTURE.md) · [프론트 구조 가이드](../development/02_FRONTEND_STRUCTURE_GUIDE.md) · [로드맵](ROADMAP.md)

---

## 0. 지금 어디까지 왔나 (2026-08-31)

기능 개발이 끝났고, **데스크탑에 배포해서 실제로 띄우는 것까지 검증했다.**
다만 공개 데모는 **임시 터널(Quick Tunnel) 주소가 재기동마다 바뀌는 문제** 때문에 상시 운영하지 않는다.
도메인을 확보하면 고정 주소로 전환한다. 새 세션은 여기부터 보면 된다.

- 운영 방법(기동·복구·주소 갱신·문제 대응)은 → **[12. 운영 가이드](../development/12_OPERATIONS.md)**
- 처음 배포하는 절차는 → [11. 배포 가이드](../development/11_DEPLOYMENT_GUIDE.md)

### 갖춰진 것

| 영역     | 상태                                                                                                                   |
| -------- | ---------------------------------------------------------------------------------------------------------------------- |
| 기능     | 채팅(SSE·RAG)·영수증·가이드·회의록·관리자·소셜 로그인                                                                  |
| 테스트   | WAS 122 · WEB 9 · Vue 11 (`./gradlew :workmate-was:test` · `npm run test:unit`)                                        |
| CI       | push·PR 마다 테스트, `main` 머지 시 GHCR 이미지 push                                                                   |
| 브랜치   | **GitHub Flow** — `main` 보호(PR 필수 + CI 통과 필수), 머지 시 브랜치 자동 삭제. [ADR-0004](adr/0004-github-flow-branching.md) |
| 컨테이너 | 개발 `docker-compose.yml` / 배포 `docker-compose.deploy.yml`(GHCR pull)                                                |
| **배포** | **WSL2 에서 가동 중.** Cloudflare Quick Tunnel 로 공개(임시 주소) — [12. 운영 가이드](../development/12_OPERATIONS.md) |
| 관측     | Actuator+Prometheus 지표, 요청 추적 ID(MDC), 구조화 로깅(JSON), Grafana                                                |
| 사용량   | 토큰을 `llm_usage` 에 기록 + 관리자 대시보드(`/admin/usage`) + **본인 사용량**(`/usage`, 사이드바 계정 메뉴)          |
| RAG 품질 | 골든셋 33문항 평가 하네스 + 리포트 2회                                                                                 |

### 다음에 할 일

**끝난 것**: Flyway 도입(→ [ADR-0005](adr/0005-flyway-migration.md)) · 관리자 사용량 대시보드 · 본인 사용량 화면.
남은 과제:

1. **Redis 도입** — 세션·SessionRegistry·레이트리미터가 인메모리라 인스턴스를 못 늘린다(아래 한계 참고).
   **부하 테스트보다 먼저 한다** — 그래야 도입 전/후를 숫자로 비교할 수 있다
2. **페이지 인식 도우미 챗봇** — 기능 확장

그 밖에 계속 남아 있는 것:

- **도메인 연결** — 확보하면 [12. 운영 가이드 §8](../development/12_OPERATIONS.md) 대로 고정 터널로 전환.
  소셜 로그인 콜백도 이때 등록할 수 있다
- **프론트 테스트 보강** — 현재 11건(사용량 작업에서 3→11). **아직 composable 뿐이고 store 테스트가 없다** —
  ADR-0002 의 "로직은 composable/store" 중 store 쪽이 미증명 상태다
- **부하 테스트** — k6 로 SSE 동시접속 한계 측정. **부하 생성기와 대상 서버를 분리**해야 숫자가 유효하다.
  순서상 **Redis 뒤**에 둔다(위 1번 참고)
- **RAG 권한 필터** — 아래 한계의 2번. 평가 하네스가 있어 개선을 수치로 증명할 수 있다

### 사용량 화면 설계 메모

- **표시부는 공통**이다 — `common/components/usage/UsageOverview.vue` 를 관리자 화면과 본인 화면이
  함께 쓴다. 모듈끼리 서로의 내부를 import 하지 않는 규칙이라 공유물은 `common/` 에 둔다
- **본인 조회 대상은 `X-User-Seq` 헤더로만 정한다.** WEB 의 RestClient 인터셉터가 세션 값으로
  덮어쓰므로 브라우저가 조작할 수 없다. **사용자 번호를 파라미터로 받지 않는 것 자체가 방어 장치**다
- **차트는 기간이 8일을 넘으면 주 단위로 묶는다.** 30일을 일별 막대 30개로 그리면 좁아서 못 읽는다.
  집계는 서버가 계속 일별로 주고, 묶는 것은 표현 사정이라 화면에서 한다
- **차트 라이브러리를 쓰지 않는다.** 기능 5종 + 막대 7개 이하 규모라 CSS 로 충분하다

### 알려진 한계 (숨기지 말고 설명할 것)

- **RAG 접근 필터가 topK 뒤에 있다.** 비공개 타인 문서가 상위를 차지하면 결과 건수가 줄어든다.
  `filterExpression` 으로 DB 단계에서 거는 것이 개선 방향
- **임베딩 사용량은 토큰이 NULL** 이다. `VectorStore.add()` 가 usage 를 감춘다.
  사용량 대시보드는 이 건수를 합계에 넣지 않고 "토큰 미집계 N건" 으로 분리 표시한다
- **비용은 추정치다.** `app.usage.pricing` 의 단가로 계산하며 실제 청구액이 아니다.
  `model_name` 에는 별칭(`gemini-flash-latest`)이 아니라 **제공자가 해석한 실제 이름**(`gemini-3.7-flash`)이
  저장되므로, 별칭이 새 버전을 가리키면 단가를 추가해야 한다 — 그전까지는 "단가 미등록 N건" 으로 드러난다
- **세션·SessionRegistry·레이트리미터가 인메모리**다. 인스턴스를 늘리면 조용히 깨진다 → Redis 필요
- **WEB→WAS 신원 전달이 서명 없는 평문 헤더(`X-User-Seq`)다.** WEB 의 `RestClientConfig` 가
  세션의 로그인 사용자로 `set()` 해 덮어쓰므로 **브라우저는 위조할 수 없다**. 다만 **WAS 는 이 헤더를
  검증 없이 믿으므로**, 8081 에 직접 닿을 수 있으면 아무나 관리자가 된다. 지금 이를 막는 것은
  **네트워크 격리뿐**이다 — `docker-compose.deploy.yml` 은 8081 을 호스트에 열지 않는다
  (개발용 `docker-compose.yml` 은 편의상 연다). 서비스가 늘거나 WAS 가 여러 노드가 되면
  **짧은 수명의 서명 토큰**으로 바꿔야 한다. [ADR-0001](adr/0001-hybrid-ssr-to-vue3-spa.md) 은
  *브라우저* 인증만 다루고 이 구간은 다루지 않는다
- **데모 계정은 Flyway 마이그레이션에 없다.** 이메일이 배포마다 다른 AES 키로 암호화돼야 해서
  스키마 마이그레이션에 고정 암호문을 넣을 수 없기 때문이다(의도적 — D4). 대신
  `scripts/bootstrap-demo-data.sh` 가 실제 AES 키를 쥔 채로 계정·콘텐츠를 직접 만든다(멱등,
  로컬·데모 전용). 배포 후 이 스크립트를 1회 실행해야 데모 로그인이 된다
- **`/api/auth/signup` 이 `ROLE_ADMIN` 전용**이다. 신규 배포에는 관리자가 없으므로
  계정을 새로 만들 수 없다. 위 부트스트랩이 유일한 진입 경로다

### 최근 변경 (2026-08-31)

로컬 배포 리허설을 돌리면서 **실제 배포를 막는 버그들**을 찾아 고쳤다. 전부 CI·개발 compose 에서는
드러나지 않고 **리눅스에 클론한 실배포 환경에서만** 재현되는 것들이었다.

| PR                                                  | 내용                                                                                                    |
| --------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| [#4](https://github.com/taekyung96/workmate/pull/4) | README 이미지 경로 정정 · `.gitattributes` 로 `*.sh`·`gradlew` LF 고정 (CRLF 면 컨테이너에서 실행 불가) |
| [#5](https://github.com/taekyung96/workmate/pull/5) | 데모 계정을 `ROLE_USER` 로 시드. 관리자 승격은 `DEMO_ADMIN_ENABLED` 로 로컬 전용 분리                   |
| [#6](https://github.com/taekyung96/workmate/pull/6) | init `.sh` 의 조기 `exit` 가 DB 초기화를 끊던 문제 · 소셜 자격증명 빈 값이면 WEB 이 기동 실패하던 문제  |
| [#7](https://github.com/taekyung96/workmate/pull/7) | `scripts/bootstrap-demo-login.sh` — 배포 환경 AES 키로 데모 계정 로그인을 살린다                        |
| [#8](https://github.com/taekyung96/workmate/pull/8) | `scripts/update-demo-url.sh` — 임시 터널 주소를 README 에 반영                                          |

각 PR 본문에 증상·원인·재현·검증 로그가 들어 있다.

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
workmate-was (:8081) — 비즈니스 로직 · JPA/MyBatis · Spring AI (Gemini 2.5 Flash)   ← v2에서 복사(초기), 필요 시 수정 가능
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

## 4. 초기 셋업 기록 (v2 → v3 이관)

v3는 빈 저장소에서 시작해, v2에서 쓸 만한 것만 골라 옮겨온 뒤 프론트를 새로 만들었다.
아래는 그때 무엇을 왜 가져왔는지 남겨두는 기록이다. 전부 끝난 단계다.

1. **WAS 이관** — v2 저장소에서 `workmate-was/`(AI 로직), `db/init/*.sql`(스키마, `ddl-auto: validate`),
   `docker-compose.yml`, Gradle 루트 파일(`settings.gradle`·`build.gradle`·`gradlew*`·`gradle/`·`gradle.properties`),
   `.env.example`을 복사했다. 실제 `.env`는 비밀값이라 새로 작성했고 git에 올리지 않는다.

    v2의 `.git`은 저장소 루트에 있어서, 하위 폴더만 골라 복사하면 이력이 딸려오지 않는다.
    v3는 `git init`으로 이력을 새로 시작해 v2와 완전히 분리했다.

2. **얇은 WEB 재구성** — v2 `workmate-web`에서 Thymeleaf 페이지 로직을 걷어내고,
   SPA 정적 서빙 + `/api` 프록시 + SSE 중계 + Spring Security 세션만 남겼다.

3. **Vue3 SPA 스캐폴딩** — `workmate-vue`를 단독 SPA로 세웠다. 당시 쓴 명령은 아래와 같다.

    ```bash
    npm create vue@latest .                       # Router·Pinia·TS 선택
    npm install
    npx shadcn-vue@latest init                    # components.json 생성 (경로를 common/ 모듈로 설정)
    npx shadcn-vue@latest add button dialog ...   # 필요한 컴포넌트만
    ```

4. **빌드 연결** — 개발은 Vite dev proxy(5173→8080)로, 운영은 Vue 빌드 산출물을 WEB이 서빙하도록 붙였다.
   ([아키텍처 §빌드](../development/01_ARCHITECTURE.md))

5. **기능 구현** — [ROADMAP.md](ROADMAP.md) 순서대로 진행했다.

---

## 5. 구현 순서 (초기 계획 — **전부 완료됨**, 기록용)

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
