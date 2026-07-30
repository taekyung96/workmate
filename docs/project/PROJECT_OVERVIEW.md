# 📌 Workmate v3 프로젝트 종합 개요 (Project Overview)

> **Workmate v3**는 Spring AI 및 Vue3 SPA 기반의 **실무형 AI 업무 비서(AI Work Assistant) 웹 애플리케이션**입니다.

---

## 1. 🎯 프로젝트 정체성 및 비즈니스 목표

- **프로젝트명**: Workmate v3 (Spring AI 기반 업무 비서 웹앱)
- **개발 목적**: 사내 주요 반복 업무(영수증 처리, 사내 문서 검색, AI 질의응답)의 자동화 및 생산성 향상
- **아키텍처 진화**: **v2 (Thymeleaf SSR + Vue UMD 하이브리드)** ➔ **v3 (Vue3 단독 SPA + 얇은 BFF + AI WAS)** 구조 재설계를 통한 유지보수성 및 고성능 SPA 반응성 확보

---

## 2. 🚀 4대 핵심 기능 (Key Features)

| 기능 | 주요 기술 및 특징 | 비고 |
| :--- | :--- | :--- |
| 💬 **실시간 스트리밍 AI 채팅** | • Spring AI (`Gemini 2.5 Flash`) + SSE(Server-Sent Events) 스트리밍<br>• 세션별 채팅방 및 대화 맥락(Context) 유지 | 실시간 반응형 UX |
| 🧾 **영수증 자동 인식 (대표 기능 ⭐)** | • Vision AI 기반 결제 건 추출 (금액·사업자번호·결제일)<br>• 한국 사업자등록번호 알고리즘 체크섬 검증 및 이력 자동 저장 | ERP 수기 입력 보조 |
| 📚 **사내 문서 RAG 검색** | • Markdown 문서 청크 분할 및 Gemini 임베딩 (`text-embedding-004`)<br>• PostgreSQL `pgvector` 코사인 유사도 검색 & 답변 출처(Citation) 표기 | 환각 방지 및 근거 제시 |
| 🛠 **Tool Calling & 멀티모델** | • "지난달 영수증 총액은?" 자연어 질문 시 DB 자동 조회 (`@Tool` Function Calling)<br>• 설정 기반 AI 모델 동적 스위칭 지원 | AI 에이전트 확장 |

---

## 3. 🏛️ 시스템 아키텍처

```
[ 브라우저 (Vue3 SPA) ]
       │ HTTP (Session Cookie) + SSE
       ▼
[ workmate-web (:8080) ] ─── 얇은 BFF (SPA 정적파일 서빙, 세션 인증, /api 프록시, SSE 중계)
       │ REST + SSE Relay (내부망 보호)
       ▼
[ workmate-was (:8081) ] ─── AI 비즈니스 WAS (Spring AI, JPA/MyBatis, Gemini 연동)
       │
       ▼
[ PostgreSQL 17 + pgvector ]
```

### 아키텍처 핵심 가치
1. **3-Tier 보안 경계**: 브라우저는 `:8080`(BFF)만 직접 바라보고, AI 비즈니스 로직을 담은 `:8081`(WAS)는 내부망으로 보호.
2. **세션 기반 인증**: 중복 로그인 제어 및 계정 잠금 기능 구현, `httpOnly` 쿠키를 통한 XSS 방어 강화.
3. **도메인 모듈화 체계**: Vue3 프론트엔드의 `modules/{auth,chat,receipt,guide,admin}` 기능별 캡슐화 및 `common/` 공유 계층 구조 준수.

---

## 4. 🔗 관련 문서

- 📌 [HANDOVER.md](HANDOVER.md) — 콜드스타트 및 설계 결정/근거
- 📋 [FEATURE_SPEC.md](FEATURE_SPEC.md) — 상세 기능 명세서
- 🗺️ [ROADMAP.md](ROADMAP.md) — 단계별 구현 로드맵
- 📑 [ADR 문서 모음](adr/) — 아키텍처 의사결정 기록 (ADR-0001, ADR-0002)
