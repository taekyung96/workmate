package com.workmate.was.assistant.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 도우미 스트리밍 요청.
 *
 * <p>채팅과 달리 {@code roomSeq}·이미지·모델 선택이 없다. 대화 맥락은 서버가 들지 않고
 * 프론트가 최근 몇 개를 {@code history} 로 함께 보낸다 — 도우미 대화는 저장하지 않기 때문이다.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class AssistantStreamRequestVo {

    /** 사용자 질문 */
    private String message;

    /** 사용자가 보고 있는 화면(Vue Router 라우트 이름). 화이트리스트로 검증한다 */
    private String route;

    /** 최근 대화 (시간순). 프론트가 최대 6개까지 보낸다 */
    private List<Turn> history;

    /** 대화 한 턴 — role 은 "user" 또는 "assistant" */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Turn {
        private String role;
        private String content;
    }
}
