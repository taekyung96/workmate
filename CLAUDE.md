# Workmate v3 프로젝트 지침 (CLAUDE.md)

> **먼저 [docs/project/HANDOVER.md](docs/project/HANDOVER.md) 를 읽어라.** 설계 결정·근거·셋업 순서가 거기 있다.

## 프로젝트 한 줄 요약

Spring AI 기반 "업무 비서" 웹앱의 **v3 (Vue3 단독 SPA)**. v2의 Thymeleaf+Vue UMD 하이브리드를 SPA로 재설계.
**AI 백엔드가 주인공, 프론트는 제대로 된 SPA면 충분.**

## 필독 문서

- `docs/README.md` — **전체 문서 색인** (범주별 목차·한 줄 설명, 문서 탐색은 여기서 시작)
- `docs/project/HANDOVER.md` — 콜드스타트 진입점 (결정·근거·셋업 체크리스트)
- `docs/project/adr/*` — 아키텍처 결정 기록 (왜 SPA·왜 세션·왜 기능별 모듈·왜 shadcn-vue)
- `docs/development/01_ARCHITECTURE.md` — 아키텍처 상세 spec (모듈 구조·빌드·에러·테스트)
- `docs/development/02_FRONTEND_STRUCTURE_GUIDE.md` — Vue3 SPA 디렉토리·계층·모듈 규칙 (백엔드 계층 매핑 포함)
- `docs/development/04_BACKEND_GUIDE.md` — 백엔드 3-tier·네이밍·로깅·예외 (v2 계승)
- `docs/development/03_API_DB_SPEC.md` — 엔드포인트·테이블 (v2 계승)
- `docs/design/*` — 화면 설계·디자인 시스템(shadcn-vue 테마)

## 아키텍처 요약

- 모노레포: `workmate-was`(:8081, REST+JPA+MyBatis+Spring AI, **내부망·v2에서 복사 무변경**) / `workmate-web`(:8080, **얇은 BFF** — SPA 서빙+세션+프록시+SSE 중계) / `workmate-vue`(Vue3 SPA)
- 브라우저는 **8080만** 바라봄. WEB은 DB 직접 접근 금지 — `/api` 프록시로만 WAS 호출
- 인증: **세션(Spring Security, httpOnly 쿠키, CSRF on)** — JWT 아님
- 3-tier 유지: 표현(Vue SPA) / 로직(WAS) / 데이터(PostgreSQL 17 + pgvector)
- 스키마는 `db/init/*.sql` 로만 관리(`ddl-auto: validate`), 비밀값은 `.env`(git 미추적)

## 프론트엔드 규칙 (핵심)

- **구조**: 기능별 모듈(`src/modules/{auth,chat,receipt,guide,admin}/`) + 공통 모듈(`src/common/`)
- **계층**: `view → composable/store → api` (Controller→Service→DAO 대응). 화면은 얇게, `api` 직접 호출 지양
- **모듈 경계**: 모듈 간 내부 import 금지. 공통은 `common/`으로만 공유
- **공통 부품**: 안내창·페이징·버튼·모달 등은 `common/components/`, 로직은 `common/composables/`(useDialog·usePagination)
- **스타일링**: **Tailwind v4 + shadcn-vue** (Reka UI 기반). shadcn 컴포넌트는 `common/components/ui/`에 배치(components.json 경로 설정). 순수 CSS·인라인 style 지양
- **컴포넌트 작성**: `<script setup>` Composition API. Options API 금지
- **상태관리**: Pinia. 여러 화면 공유하는 것만 store, 한 화면용은 로컬 상태

## 코딩 규칙 (범용)

- **Java 패키지**: 전체 소문자 / **클래스**: PascalCase(`~ApiController`·`~Service`·`~ServiceImpl`·`~Vo`·`~Entity`·`~Repository`) / **메서드·변수**: camelCase / **상수**: SNAKE_CASE
- **DB 테이블·컬럼**: 소문자 snake_case, 테이블명 단수형. 제약조건 `테이블_컬럼_제약`(PK `~_pk`·FK `~_fk`·UK `~_uk`·Index `idx_~`)
- **로깅**: `System.out.println` 금지. `@Slf4j`+`log`, `{}` 치환자, 예외 시 `log.error(msg, e)`
- **비밀번호**: `BCryptPasswordEncoder` 단방향
- **WAS API 응답**: `global/response/ApiResponse` 공통, 예외는 `global/exception/GlobalExceptionHandler` (컨트롤러 개별 try-catch 금지)
- **Vue 컴포넌트 파일**: PascalCase(`.vue`) / **composable**: camelCase + `use` 접두사 / **TS 인터페이스**: PascalCase, `I` 접두사 금지
- **들여쓰기**: 스페이스 4칸 (프로젝트에 .prettierrc/.editorconfig 있으면 그 설정 우선)

## 언어

질문·작업·에러 원인·결과 설명은 **한국어**로. 처음 나오는 전문 용어는 한 줄 풀이.

## Git 커밋 규칙 (중요)

- 커밋 메시지에는 **변경 내용 요약만** 적는다.
- **`Co-Authored-By: Claude` / `Generated with Claude Code` 등 AI·도구 서명·트레일러를 넣지 않는다.**
- 커밋 작성자(author)는 저장소의 git 설정(사용자 본인 계정)으로 남긴다.
- 이유: 저장소의 커밋 이력을 명확하고 온전하게 관리하기 위해.
- 예: `feat(auth): 로그인/회원가입 화면 + 세션 인증 연동`, `docs: 아키텍처 설계 문서 추가`
