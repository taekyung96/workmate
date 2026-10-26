# 문서

프로젝트 소개와 실행 방법은 [루트 README](../README.md)에 있다. 여기에는 판단 근거와 측정 기록을 모았다.

## 설계 결정 (ADR)

무엇을 골랐는지보다 **무엇을 버렸고 왜 버렸는지**를 남겼다.

| 문서                                                                 | 내용                                                 |
| :------------------------------------------------------------------- | :--------------------------------------------------- |
| [ADR-0001](project/adr/0001-hybrid-ssr-to-vue3-spa.md)               | Vue3 SPA + 얇은 WEB(BFF) + 세션 인증                 |
| [ADR-0002](project/adr/0002-frontend-structure-and-ui.md)            | 프론트 구조(기능별 모듈) · UI(shadcn-vue + Tailwind) |
| [ADR-0003](project/adr/0003-was-modifiable-and-guide-admin-authz.md) | 권한 판정은 WAS 에서 — 소유자 + 관리자               |
| [ADR-0004](project/adr/0004-github-flow-branching.md)                | 브랜치 전략 — GitHub Flow, PR + CI 로 main 보호      |
| [ADR-0005](project/adr/0005-flyway-migration.md)                     | 스키마 관리를 Flyway 마이그레이션 하나로             |
| [ADR-0006](project/adr/0006-redis-distributed-state.md)              | 세션 · 중복 로그인 차단 · 레이트리밋을 Redis 로 공유 |

## 구조와 운영

| 문서                                              | 내용                                                                |
| :------------------------------------------------ | :------------------------------------------------------------------ |
| [아키텍처](development/01_ARCHITECTURE.md)        | 모듈 구성 · 빌드 · 에러 처리 · 테스트                               |
| [API·DB 명세](development/03_API_DB_SPEC.md)      | 엔드포인트와 테이블                                                 |
| [배포 가이드](development/11_DEPLOYMENT_GUIDE.md) | GHCR 이미지 배포 · Cloudflare Tunnel · Flyway 반영 · 자주 막히는 곳 |

## 측정과 분석

| 문서                                                                           | 내용                                              |
| :----------------------------------------------------------------------------- | :------------------------------------------------ |
| [RAG 평가 하네스 설계](features/rag-eval/RAG_EVAL_HARNESS_DESIGN.md)           | 골든셋 · 지표(Hit@K · MRR) · 파라미터 격자        |
| [RAG 평가 리포트 2026-07-29](features/rag-eval/REPORT-2026-07-29.md)           | 첫 평가 — 23문항 · 코퍼스 17건, 전 구간 포화      |
| [RAG 평가 리포트 2026-08-28](features/rag-eval/REPORT-2026-08-28.md)           | 재평가 — 33문항 · 코퍼스 34건, 운영 기본값의 근거 |
| [RAG 평가 리포트 2026-09-04](features/rag-eval/REPORT-2026-09-04.md)           | 오탐 측정 추가 — threshold 0.4 → 0.5 의 근거      |
| [임베딩 쿼터(429) 분석](architecture/RAG_VECTORSTORE_EMBEDDING_QUOTA_GUIDE.md) | 무료 티어 한도에 걸린 원인과 대응                 |
| [부하 테스트 리포트](features/loadtest/REPORT-2026-09-04.md)                   | k6 · 동시 50/200 · 포화 지점과 한계 요인          |
| [Redis 도입 전후 검증](features/redis/REPORT-2026-09-02.md)                    | 인스턴스 2개에서 세션 · 중복 로그인 · 레이트리밋  |

## 그 밖에

- [브랜드 에셋](brand/README.md) — 앱 아이콘
- [CI 워크플로](../.github/workflows/ci.yml) — push · PR 마다 프론트 빌드 · 테스트, WAS/WEB 통합 테스트, E2E
