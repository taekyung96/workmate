# 📚 Workmate v3 문서 색인 (Docs Index)

Workmate v3의 모든 문서를 범주별로 모은 목차입니다. **문서를 찾을 땐 여기서 시작하세요.**
(파일명은 안정성·이식성을 위해 영문을 유지하고, 아래 링크 텍스트는 한글 제목으로 안내합니다.)

> 처음 오셨다면 → [프로젝트 종합 개요](project/PROJECT_OVERVIEW.md) → [개발 진입점(HANDOVER)](project/HANDOVER.md) 순서로 읽으세요.

---

## 🧭 프로젝트 (project) — 개요·의사결정·로드맵

| 문서                                                     | 설명                                              |
| :------------------------------------------------------- | :------------------------------------------------ |
| [프로젝트 종합 개요](project/PROJECT_OVERVIEW.md)        | v3 전체 개요와 4대 핵심 기능                      |
| [개발 진입점 · 인수인계 (HANDOVER)](project/HANDOVER.md) | 콜드스타트 진입점 — 설계 결정·근거·초기 셋업 기록 |
| [기능 명세서](project/FEATURE_SPEC.md)                   | F1~F7 기능별 요구사항·예외·완료 조건              |
| [v2 진화 배경](project/PROJECT_BACKGROUND_V2.md)         | 왜 v2에서 v3로 재설계했는가                       |
| [로드맵](project/ROADMAP.md)                             | 구현 순서                                         |

### 아키텍처 결정 기록 (ADR)

| 문서                                                                                                     | 설명                                                |
| :------------------------------------------------------------------------------------------------------- | :-------------------------------------------------- |
| [ADR-0001. 하이브리드 SSR → Vue3 SPA](project/adr/0001-hybrid-ssr-to-vue3-spa.md)                        | SPA 전환 + 얇은 WEB(BFF) + 세션 인증 결정           |
| [ADR-0002. 프론트 구조 & UI](project/adr/0002-frontend-structure-and-ui.md)                              | 기능별 모듈 + 공통 모듈, shadcn-vue + Tailwind 채택 |
| [ADR-0003. WAS 수정 허용 & 가이드 관리자 권한](project/adr/0003-was-modifiable-and-guide-admin-authz.md) | WAS 무변경 원칙 완화, 가이드 수정·삭제 관리자 허용  |
| [ADR-0004. 브랜치 전략 — GitHub Flow](project/adr/0004-github-flow-branching.md)                         | Git Flow·develop 기각, PR+CI 로 main 보호           |
| [ADR-0005. Flyway 마이그레이션 전환](project/adr/0005-flyway-migration.md)                                | db/init 3갈래 적용 경로를 Flyway 하나로 통합        |

---

## 🏗️ 아키텍처 (architecture) — 시스템·인프라·환경설정

| 문서                                                                                               | 설명                                     |
| :------------------------------------------------------------------------------------------------- | :--------------------------------------- |
| [RAG 임베딩 쿼터 & 로컬 임베딩 전환 가이드](architecture/RAG_VECTORSTORE_EMBEDDING_QUOTA_GUIDE.md) | VectorStore 임베딩 쿼터 이슈 분석과 대응 |

---

## 🛠️ 개발 가이드 (development) — 아키텍처·프론트·백엔드·셋업

| 문서                                                                                     | 설명                                          |
| :--------------------------------------------------------------------------------------- | :-------------------------------------------- |
| [01. 아키텍처 설계 (v3 SPA)](development/01_ARCHITECTURE.md)                             | 백엔드/프론트 모듈·빌드·에러·테스트 상세 spec |
| [02. 프론트엔드 구조 가이드](development/02_FRONTEND_STRUCTURE_GUIDE.md)                 | Vue3 SPA 디렉토리·계층·모듈 규칙              |
| [03. API·DB 상세 설계서](development/03_API_DB_SPEC.md)                                  | 엔드포인트·테이블 스키마                      |
| [04. 백엔드 개발 표준 가이드](development/04_BACKEND_GUIDE.md)                           | 백엔드 3-tier·네이밍·로깅·예외                |
| [05. 공통 요구사항 명세](development/05_COMMON_REQUIREMENTS.md)                          | F8 로깅·F9 서버 측 입력 검증                  |
| [06. 얇은 WEB(BFF) 재구성 방향](development/06_WEB_BFF_RECONSTRUCTION.md)                | BFF 계층 설계                                 |
| [07. 빌드 연결 (Vue 산출물 → WEB)](development/07_BUILD_WIRING.md)                       | SPA 빌드 산출물을 WEB에 붙이는 방법           |
| [08. WSL2 · Docker PostgreSQL(pgvector) 구축](development/08_DOCKER_WSL2_SETUP_GUIDE.md) | 로컬 DB 환경 구축                             |
| [09. 버전 호환성 매트릭스](development/09_COMPATIBILITY_MATRIX.md)                       | 스택 버전 호환 정보                           |
| [10. 프로젝트 지도](development/10_PROJECT_MAP.md)                                       | "뭘 하려면 어디를 봐야 하나" 탐색 지도        |
| [11. 배포 가이드](development/11_DEPLOYMENT_GUIDE.md)                                    | GHCR pull·터널·스키마 반영·자주 막히는 곳     |
| [12. 운영 가이드](development/12_OPERATIONS.md)                                          | 돌아가는 배포를 다루는 법 — 기동·복구·주소 갱신·전환 |

---

## 🎨 디자인 (design) — 디자인 시스템·화면 설계

| 문서                                                                | 설명                                                      |
| :------------------------------------------------------------------ | :-------------------------------------------------------- |
| [디자인 시스템 (shadcn-vue + Tailwind)](design/00_DESIGN_SYSTEM.md) | 스타일링 방식 + 컬러·타이포·간격·컴포넌트 톤 (v2 값 흡수) |
| [화면 설계서](design/01_SCREEN_DESIGN.md)                           | 화면 와이어프레임·이동 흐름                               |

---

## 🧩 기능별 문서 (features) — 스펙 · 구현 계획 · 평가 리포트

기능 단위로 명세(Spec)·작업 계획(Plan)·평가 리포트(Report)를 한곳에 모았습니다.

### 🎙️ 음성 회의록 (voice)

| 문서                                                                                             | 설명                                |
| :----------------------------------------------------------------------------------------------- | :---------------------------------- |
| [회의 녹음 & AI 회의록 자동 요약 명세 (F8-1)](features/voice/F8-1_VOICE_MEETING_SUMMARY_SPEC.md) | 음성 회의록 요약 기능 명세          |
| [회의록 이력 · 오디오 보관 설계서 (F8-1 확장)](features/voice/F8-1_VOICE_HISTORY_SPEC.md)        | 이력·오디오 저장/스트리밍/삭제 설계 |
| [회의록 이력 · 오디오 보관 구현 계획](features/voice/VOICE_HISTORY_PLAN.md)                      | 위 스펙의 작업 계획서               |

### 🔐 소셜 로그인 (social-login)

| 문서                                                                         | 설명                                           |
| :--------------------------------------------------------------------------- | :--------------------------------------------- |
| [소셜 로그인 설계서 (F1-1)](features/social-login/F1-1_SOCIAL_LOGIN_SPEC.md) | 구글·네이버 OAuth 도입 — 제공자 조사·결정·설계 |

### 📋 코드 복사 (code-copy)

| 문서                                                                                          | 설명                |
| :-------------------------------------------------------------------------------------------- | :------------------ |
| [AI 응답 코드 블록 원클릭 복사 & 툴바 명세 (F2-1)](features/code-copy/F2-1_CODE_COPY_SPEC.md) | 코드 복사 기능 명세 |

### 🔎 RAG 검색 품질 평가 (rag-eval)

| 문서                                                                                  | 설명                      |
| :------------------------------------------------------------------------------------ | :------------------------ |
| [RAG 검색 품질 평가 하네스 — 설계 문서](features/rag-eval/RAG_EVAL_HARNESS_DESIGN.md) | 평가 하네스 설계          |
| [RAG 검색 품질 평가 하네스 — 구현 계획](features/rag-eval/RAG_EVAL_HARNESS_PLAN.md)   | 위 설계의 작업 계획서     |
| [RAG 검색 품질 평가 리포트 (2026-07-29)](features/rag-eval/REPORT-2026-07-29.md)      | 첫 평가 — 골든셋 23문항·코퍼스 17건 (전 구간 포화) |
| [RAG 검색 품질 평가 리포트 (2026-08-28)](features/rag-eval/REPORT-2026-08-28.md)      | 재평가 — 골든셋 33문항·코퍼스 34건, 운영 기본값 근거 |

---

## 🗂️ 루트 문서

| 문서                                                | 설명                                                         |
| :-------------------------------------------------- | :----------------------------------------------------------- |
| [../README.md](../README.md)                        | 프로젝트 소개 · 빠른 시작                                    |
| [CI 워크플로](../.github/workflows/ci.yml)          | push·PR 마다 SPA 빌드·타입체크·테스트 + WAS/WEB 통합 테스트  |
| [../CLAUDE.md](../CLAUDE.md) | **공통 규칙 단일 기준(SSOT)** — 코딩·네이밍·DB·Git 커밋 규칙 |
