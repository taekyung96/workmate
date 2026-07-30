# Workmate

> Spring AI 기반 **업무 비서 웹앱**
> **v2(Thymeleaf + Vue UMD 하이브리드) → v3(Vue3 단독 SPA)** 로 재설계한 버전

## 핵심 기능

- 💬 **스트리밍 채팅** — Spring AI(Gemini 2.5 Flash) + SSE 실시간 스트리밍
- 🧾 **영수증 자동 인식** — 이미지에서 금액·사업자번호·결제일 추출
- 📚 **문서 RAG** — 사내 가이드 기반 검색·인용 답변 (pgvector)
- 🛠 **관리자** — 사용자 관리 · 감사 로그

## 아키텍처

```
브라우저 (Vue3 SPA)
   │  HTTP(세션) + SSE
workmate-web (:8080)  얇은 BFF — SPA 서빙 · 세션 인증 · /api 프록시 · SSE 중계
   │  (내부망)
workmate-was (:8081)  비즈니스 로직 · JPA/MyBatis · Spring AI
   │
PostgreSQL 17 + pgvector
```

- **모노레포**: `workmate-was` / `workmate-web` / `workmate-vue`
- **프론트**: Vue3 · Vite · TypeScript · Vue Router · Pinia · **shadcn-vue + Tailwind v4**
- **백엔드**: Spring Boot · Spring AI · JPA/MyBatis · Spring Security(세션)
- **3-tier** 유지, 브라우저는 8080만 바라봄

## 문서

- 📌 **[docs/project/PROJECT_OVERVIEW.md](docs/project/PROJECT_OVERVIEW.md)** — 프로젝트 종합 개요
- 📌 **[docs/project/HANDOVER.md](docs/project/HANDOVER.md)** — 개발 진입점 (결정·근거·셋업)
- 🐳 **[docs/development/05_DOCKER_WSL2_SETUP_GUIDE.md](docs/development/05_DOCKER_WSL2_SETUP_GUIDE.md)** — WSL2 Docker DB 구축 가이드
- [아키텍처](docs/development/01_ARCHITECTURE.md) · [프론트 구조](docs/development/02_FRONTEND_STRUCTURE_GUIDE.md) · [ADR](docs/project/adr/) · [로드맵](docs/project/ROADMAP.md)

## 왜 v3인가 (아키텍처 진화 배경)

v2는 엔터프라이즈 하이브리드(SSR+Vue 조각) 구조였다. Vue3의 강점(SPA·Router·Pinia)을 제대로 살리고,
기존 아키텍처의 한계를 극복하여 모던 SPA 웹앱으로 전환하기 위해 재설계했다.
아키텍처 결정 트레이드오프는 [ADR](docs/project/adr/)에 기록.
