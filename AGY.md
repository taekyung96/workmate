# Workmate v3 프로젝트 지침 (AGY.md)

> **Antigravity(AGY) 전용 진입 문서.**
> **공통 규칙(코딩·네이밍·DB·Git 커밋)은 [CLAUDE.md](CLAUDE.md) 가 단일 기준(SSOT)이다.** 이 문서는 중복 서술하지 않고 Antigravity 관점의 진입점·핵심 요약만 둔다.
> 세부 설계·배경은 [docs/project/PROJECT_OVERVIEW.md](docs/project/PROJECT_OVERVIEW.md) 및 [docs/project/HANDOVER.md](docs/project/HANDOVER.md) 참조.

---

## 1. 📌 프로젝트 개요

Spring AI 기반 "업무 비서" 웹 애플리케이션 **v3 (Vue3 단독 SPA)**.
v2(Thymeleaf + Vue UMD 하이브리드)의 한계를 극복하고 모던 SPA 웹 아키텍처로 전면 재설계된 프로젝트.

> **핵심은 "AI를 어떻게 활용하는가"** — 스트리밍 채팅(SSE)·RAG·Tool Calling 등 Spring AI 활용 설계가 이 프로젝트의 정체성이다. AI 백엔드가 주인공이며, 영수증 자동 인식 등은 부가 기능이다.

---

## 2. 📚 필독 문서

**전체 문서 색인은 [docs/README.md](docs/README.md) 참조.** 우선 읽을 것:

- 📌 [docs/project/PROJECT_OVERVIEW.md](docs/project/PROJECT_OVERVIEW.md) — 프로젝트 종합 개요 및 4대 핵심 기능
- 📌 [docs/project/HANDOVER.md](docs/project/HANDOVER.md) — 셋업 순서 및 개발 진입점
- 📑 [docs/project/adr/*](docs/project/adr/) — 아키텍처 결정 기록 (ADR-0001: SPA 전환, ADR-0002: 프론트 구조)
- 🏗️ [docs/development/01_ARCHITECTURE.md](docs/development/01_ARCHITECTURE.md) — 백엔드/프론트엔드 모듈 및 빌드 명세
- 🎨 [docs/development/02_FRONTEND_STRUCTURE_GUIDE.md](docs/development/02_FRONTEND_STRUCTURE_GUIDE.md) — Vue3 SPA 디렉토리 및 계층 규칙

---

## 3. 🏛️ 아키텍처 핵심 (요약)

- **모노레포**: `workmate-was` (:8081, AI 비즈니스 WAS), `workmate-web` (:8080, 얇은 BFF), `workmate-vue` (Vue3 SPA)
- **3-Tier 보안 경계**: 브라우저는 **8080(BFF)**만 직접 바라보며, WAS는 내부망으로 보호. WEB은 DB 직접 접근 불가 (`/api` 프록시 호출)
- **인증 방식**: **Session 기반 인증** (Spring Security, `httpOnly` 쿠키, CSRF 적용)

> 프론트엔드 규칙·백엔드/DB 코딩 규칙·Git 커밋 규칙·PII 마스킹 등 **공통 지침은 모두 [CLAUDE.md](CLAUDE.md) 에 있다.** Antigravity로 작업할 때도 CLAUDE.md 를 함께 로드해 그 규칙을 그대로 따른다.

---

## 4. 🤖 Antigravity 특화 메모

- 이 문서(AGY.md)는 Antigravity 환경에서 프로젝트 컨텍스트를 빠르게 잡기 위한 진입점이다.
- 규칙이 CLAUDE.md 와 어긋나 보이면 **CLAUDE.md 가 우선**한다. 새 규칙은 CLAUDE.md 에 추가하고 이 문서는 중복하지 않는다.
