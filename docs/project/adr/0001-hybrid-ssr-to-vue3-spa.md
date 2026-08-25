# ADR-0001. 하이브리드 SSR → Vue3 SPA + 얇은 WEB(BFF) + 세션 인증

- **상태**: 채택
- **맥락 문서**: [HANDOVER](../HANDOVER.md) · [아키텍처](../../development/01_ARCHITECTURE.md)

## 맥락 (Context)

v2(현행 `taekyung96/workmate`)는 **Thymeleaf SSR + Vue UMD 하이브리드**다. Vue를 페이지에 부분 삽입만 해서
Vue3의 핵심(Router·Pinia·Vite·Composition API)을 거의 활용하지 못한다. 이직 포트폴리오로서 프론트 매력이 약하다.
목표는 **AI 백엔드를 주인공으로 유지**하면서 프론트를 "제대로 된 SPA"로 끌어올리는 것.

## 결정 (Decision)

1. **프론트를 Vue3 단독 SPA로 재설계** (Router·Pinia·Vite·TypeScript·Composition API).
2. **WEB을 얇은 BFF로 전환** — Thymeleaf 페이지 렌더를 버리고, SPA 정적서빙 + 세션 인증 + `/api` 프록시 + SSE 중계만 담당.
3. **세션 인증 유지** (JWT 채택 안 함). WAS는 v2에서 복사해 **무변경**, 내부망에 은닉.

## 고려한 대안 (Alternatives)

| 대안                        | 내용                                | 기각 이유                                                                            |
| --------------------------- | ----------------------------------- | ------------------------------------------------------------------------------------ |
| **A. 얇은 WEB(BFF)** ✅채택 | 브라우저→WEB(세션·프록시)→WAS(내부) | —                                                                                    |
| B. 단일 Spring Boot로 통합  | WEB 제거, WAS가 SPA 서빙+세션       | 주인공 WAS에 세션·서빙을 얹어 건드려야 함. 모던하지만 이 프로젝트엔 이점 대비 리스크 |
| C. SPA가 WAS 직접 호출(JWT) | 프론트 완전 분리 + 토큰             | WAS 노출 + 인증 통째 재설계. 요구사항(즉시무효화·중복로그인)과 상충                  |

## 세션 vs JWT (인증 결정 근거)

- 요구사항 **F1-06(계정잠금)·F1-08(중복로그인 차단)** 은 서버가 세션을 **즉시 무효화**해야 자연스럽다. JWT는 stateless라 만료 전 회수가 어렵다.
- 세션 ID를 **httpOnly 쿠키**에 담아 XSS 토큰 탈취를 방어 (JWT를 localStorage에 두는 흔한 방식보다 안전).
- WAS를 내부망에 숨겨 공격 표면을 8080 하나로 축소.
- 대가: **CSRF 방어 필요** → Spring Security CSRF 활성화로 대응.

## 결과 (Consequences)

- ➕ 3-tier 유지, WAS 보호, 인증 재설계 불필요, 프론트만 신규.
- ➕ 포트폴리오 서사: "v2 하이브리드 → v3 SPA 전환 + 트레이드오프를 설명할 수 있는 판단력".
- ➖ WEB 전환 작업 필요(Thymeleaf 제거). Tailwind/SPA 학습 곡선.
- ➖ 수평 확장 시 세션 공유(Redis) 필요 — 현 단계 범위 밖(YAGNI).
