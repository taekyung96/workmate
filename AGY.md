# Workmate v3 프로젝트 지침 (AGY.md)

> **Antigravity(AGY) 프로젝트 전용 지침 문서**  
> 세부 설계 및 배경은 [docs/project/PROJECT_OVERVIEW.md](docs/project/PROJECT_OVERVIEW.md) 및 [docs/project/HANDOVER.md](docs/project/HANDOVER.md) 참조.

---

## 1. 📌 프로젝트 개요

Spring AI 기반 "업무 비서" 웹 애플리케이션 **v3 (Vue3 단독 SPA)**.  
v2(Thymeleaf + Vue UMD 하이브리드)의 한계를 극복하고 모던 SPA 웹 아키텍처로 전면 재설계된 프로젝트.

> **핵심은 "AI를 어떻게 활용하는가"** — 스트리밍 채팅(SSE)·RAG·Tool Calling 등 Spring AI 활용 설계가 이 프로젝트의 정체성이다. AI 백엔드가 주인공이며, 영수증 자동 인식 등은 부가 기능이다.

---

## 2. 📚 필독 문서 경로

- 📌 [docs/project/PROJECT_OVERVIEW.md](docs/project/PROJECT_OVERVIEW.md) — 프로젝트 종합 개요 및 4대 핵심 기능
- 📌 [docs/project/HANDOVER.md](docs/project/HANDOVER.md) — 셋업 순서 및 개발 진입점
- 📑 [docs/project/adr/*](docs/project/adr/) — 아키텍처 결정 기록 (ADR-0001: SPA 전환, ADR-0002: 프론트 구조)
- 🏗️ [docs/development/01_ARCHITECTURE.md](docs/development/01_ARCHITECTURE.md) — 백엔드/프론트엔드 모듈 및 빌드 명세
- 🎨 [docs/development/02_FRONTEND_STRUCTURE_GUIDE.md](docs/development/02_FRONTEND_STRUCTURE_GUIDE.md) — Vue3 SPA 디렉토리 및 계층 규칙

---

## 3. 🏛️ 아키텍처 핵심 수칙

- **모노레포**: `workmate-was` (:8081, AI 비즈니스 WAS), `workmate-web` (:8080, 얇은 BFF), `workmate-vue` (Vue3 SPA)
- **3-Tier 보안 경계**: 브라우저는 **8080(BFF)**만 직접 바라보며, WAS는 내부망으로 보호. WEB은 DB 직접 접근 불가 (`/api` 프록시 호출)
- **인증 방식**: **Session 기반 인증** (Spring Security, `httpOnly` 쿠키, CSRF 적용)

---

## 4. 🎨 프론트엔드 (Vue3 SPA) 규칙

- **모듈 구조**: 기능별 모듈 (`src/modules/{auth,chat,receipt,guide,admin}/`) + 공통 모듈 (`src/common/`)
- **계층 구조**: `view → composable/store → api` (Controller→Service→DAO 대응, 화면에서 direct API 호출 지양)
- **컴포넌트 작성**: Composition API (`<script setup>`) 전용 작성, Options API 금지
- **스타일링**: **Tailwind v4 + shadcn-vue** (Reka UI 기반). 순수 CSS / 인라인 style 지양
- **상태 관리**: Pinia 사용 (전역/복수 화면 공유용만 store, 단일 화면은 로컬 상태)

---

## 5. 🛠️ 백엔드 & DB 코딩 규칙

- **Java 네이밍**: 클래스 PascalCase (`~Controller`, `~Service`, `~ServiceImpl`, `~Vo`, `~Entity`, `~Repository`), 메서드/변수 camelCase, 상수 SNAKE_CASE
- **로깅**: `System.out.println` 금지. `@Slf4j` + `log` 객체 사용, `{}` 치환자 사용, 예외 시 `log.error(msg, e)`
- **DB 테이블/컬럼**: 소문자 `snake_case`, 테이블명 **단수형** (`user`, `receipt`)
- **DB 제약조건**: `테이블명_컬럼명_제약조건` (PK: `~_pk`, FK: `~_fk`, Unique: `~_uk`, Index: `idx_~`)
- **비밀번호**: `BCryptPasswordEncoder` 단방향 암호화
- **들여쓰기**: **스페이스 4칸** (기존 .prettierrc/.editorconfig 우선)

---

## 🔒 6. DB 및 Git 커밋 지침

- **DB 쓰기/삭제 승인**: DB 스키마 또는 데이터 변경 (`INSERT/UPDATE/DELETE/ALTER/DROP`) 전 사용자 사전 승인
- **개인정보 마스킹**: 데이터 조회 결과 출력 시 PII(이메일, 전화번호 등) 자동 마스킹
- **Git 커밋 지침**: AI/도구 서명 트레일러 (`Co-Authored-By: Claude` 등) 추가 금지, 커밋 작성자는 사용자 계정 온전 반영
