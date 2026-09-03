package com.workmate.was.assistant.service;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 화면(라우트) 이름 → 시스템 프롬프트에 넣을 설명 한 줄.
 *
 * <p><b>화이트리스트다.</b> {@code route} 는 클라이언트가 보내는 값이라, 검증 없이 프롬프트에 넣으면
 * 그 자체가 프롬프트 인젝션 통로가 된다. 프론트가 설명 문구까지 보내게 하지 않는 이유도 같다 —
 * 문구를 클라이언트가 정하면 무엇이든 주입할 수 있다.</p>
 *
 * <p>목록에 없으면 <b>예외 대신 빈 문자열</b>을 돌려준다. 라우트를 추가하고 매핑을 깜빡했을 때
 * 도우미가 죽는 것보다 화면 맥락 없이 일반 안내로 답하는 편이 낫다.</p>
 */
@Component
public class AssistantScreenContext {

    private static final Map<String, String> SCREENS = Map.ofEntries(
            Map.entry("chat", "AI 채팅 — 가이드 문서를 근거로 답하는 대화 화면"),
            Map.entry("guide-list", "가이드 목록 — 업무 문서를 찾아보고 새로 쓴다"),
            Map.entry("guide-detail", "가이드 상세 — 문서 본문. 본인 문서는 수정·삭제할 수 있다"),
            Map.entry("guide-new", "가이드 작성 — 새 업무 문서를 쓴다"),
            Map.entry("guide-edit", "가이드 수정 — 기존 문서를 고친다"),
            Map.entry("receipt", "영수증 분석 — 사진을 올리면 금액·날짜·상호를 자동으로 추출한다"),
            Map.entry("receipt-history", "영수증 이력 — 분석한 영수증 목록"),
            Map.entry("voice", "회의록 분석 — 음성 파일을 올리면 텍스트로 바꾸고 요약한다"),
            Map.entry("voice-history", "회의록 이력 — 분석한 회의 목록"),
            Map.entry("voice-record", "회의록 상세 — 전문과 요약"),
            Map.entry("my-usage", "내 사용량 — 본인이 쓴 AI 호출 수·토큰·추정 비용"),
            Map.entry("admin-usage", "사용량 대시보드(관리자) — 전체·기능별·사용자별 집계"),
            Map.entry("admin-users", "사용자 관리(관리자) — 계정 목록·권한·잠금 해제"),
            Map.entry("admin-audit-logs", "감사 로그(관리자) — 관리자 행위 기록"),
            Map.entry("admin-common-codes", "공통 코드(관리자) — 화면에서 쓰는 코드값 관리"));

    /**
     * 시스템 프롬프트에 덧붙일 화면 맥락 문장을 만든다.
     *
     * @param route Vue Router 의 라우트 이름 (클라이언트가 보낸 값)
     * @return 화면 설명 문장. 모르는 화면이면 빈 문자열
     */
    public String describe(String route) {
        if (route == null || route.isBlank()) {
            return "";
        }
        String description = SCREENS.get(route);
        if (description == null) {
            return "";
        }
        return "\n\n[사용자가 지금 보고 있는 화면]\n" + description
                + "\n이 화면과 관련된 질문이면 이 맥락을 우선 고려해 답하라.";
    }
}
