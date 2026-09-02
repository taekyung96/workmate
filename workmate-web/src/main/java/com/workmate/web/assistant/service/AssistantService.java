package com.workmate.web.assistant.service;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/** 도우미 스트리밍을 WAS 로 중계한다 (버퍼링 없이). */
public interface AssistantService {

    /**
     * 도우미 SSE 를 WAS 에서 받아 그대로 흘린다.
     *
     * @param userSeq     요청자 (세션에서 온 값)
     * @param role        요청자 권한 (세션에서 온 값)
     * @param requestId   요청 추적 ID (F-OBS). null 이면 붙이지 않는다
     * @param requestBody 프론트가 보낸 JSON 원문 — WEB 은 파싱하지 않고 그대로 넘긴다
     * @return token* → done SSE
     */
    Flux<ServerSentEvent<String>> stream(Long userSeq, String role, String requestId, String requestBody);
}
