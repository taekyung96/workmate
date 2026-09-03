package com.workmate.was.assistant.service;

import com.workmate.was.assistant.vo.AssistantStreamRequestVo;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * 페이지 인식 도우미 — 화면 맥락을 아는 일회성 질의응답.
 * 채팅과 달리 대화방·메시지를 저장하지 않는다.
 */
public interface AssistantService {

    /**
     * 도우미 응답을 SSE 로 흘린다.
     *
     * @param userSeq 요청 사용자
     * @param role    요청자 권한 — @Tool 의 권한 검사에 쓰인다
     * @param request 질문·화면·최근 대화
     * @return token* → done 이벤트 Flux
     */
    Flux<ServerSentEvent<String>> stream(Long userSeq, String role, AssistantStreamRequestVo request);
}
