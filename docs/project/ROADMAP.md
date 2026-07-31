# 로드맵 — 구현 순서

- **원칙**: 리스크 낮은 것부터. 각 단계가 **독립적으로 동작 확인 가능**하도록 끊는다.
- **전제**: WAS는 v2에서 복사해 이미 동작 (마일스톤 4까지 구현됨). 프론트를 화면 단위로 새로 구현한다.

---

## 0단계 — 셋업 (개발 시작)

[HANDOVER §4 체크리스트](HANDOVER.md) 참조. WAS/db/gradle 복사 → 얇은 WEB 재구성 → Vue3 SPA 스캐폴딩 → 빌드 연결.

## 1단계 — 골격 + 로그인/회원가입

- `main.ts`·`App.vue`·`router`·`AuthLayout`·`AppLayout`·`auth.store`·`common/api/client`
- shadcn-vue init + 기본 컴포넌트(button·input·dialog)
- **인증 흐름·라우터 가드가 뼈대** — 여기가 되면 절반 성공.
- 완료 기준: 가입 → 로그인 → AppLayout 진입 → 로그아웃 왕복, 세션 쿠키·401 인터셉터 동작.

## 2단계 — 가이드 목록·상세 (단순 CRUD)

- 가장 단순한 화면으로 **"프록시 → composable/api → 화면" 왕복 패턴 확립**.
- `common`의 `usePagination`·`DataTable`·`useDialog` 최초 사용처.

## 3단계 — 채팅 (SSE 스트리밍)

- 가장 까다로움. `chat.store` + SSE 유틸(`useChatStream`).
- 스트리밍 수신 중 상태 유지, 화면 이동 대응.

## 4단계 — 영수증

- 이미지 업로드(`FileUpload`) + 분석/이력 탭 구조.
- 금액·사업자번호·결제일 자동 인식 결과 표시 (WAS 기존 기능 소비).

## 5단계 — 가이드 RAG 출처표시 + 관리자

- RAG 답변의 출처(citation) 표시.
- 관리자: 사용자 관리(M1~M3) + 감사 로그.
    - 감사 로그 **기록**(F8, 잠금해제·비번초기화 시 `admin_audit_log` 적재)은 사용자 관리에 포함돼 구현됨.
    - 감사 로그 **조회 화면**(M4, `GET /audit-logs`)은 설계 스코프 밖이었으나 관리자 투명성 강화를 위해 추가. 두 관리자 화면은 상단 탭으로 전환.

---

## 참고 — v2 기술 부채 (v3에서 해소 대상)

[FEATURE_SPEC](FEATURE_SPEC.md) 기준, v2에서 미충족으로 남은 항목:

- ③ 비밀번호 5회 실패 → 계정 잠금 (F1-06) 미동작
- ④ 중복 로그인 차단 (F1-08) 미동작

→ v3 1단계(인증) 구현 시 **세션 즉시 무효화 설계와 함께 해소**한다. (세션 채택의 명분이기도 함 — [ADR-0001](adr/0001-hybrid-ssr-to-vue3-spa.md))
