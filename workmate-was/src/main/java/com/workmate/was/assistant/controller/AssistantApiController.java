package com.workmate.was.assistant.controller;

import com.workmate.was.assistant.service.AssistantService;
import com.workmate.was.assistant.vo.AssistantStreamRequestVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 페이지 인식 도우미 REST API.
 *
 * <p>채팅과 달리 대화방을 만들지 않으므로 {@code roomSeq} 를 받지 않는다.
 * 요청자·권한은 언제나 헤더로만 정한다 — WEB 의 RestClient·WebClient 가 세션 값으로 덮어써서
 * 넣기 때문에 브라우저가 조작할 수 없다.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
public class AssistantApiController {

    private final AssistantService assistantService;

    /**
     * 도우미 응답 스트리밍.
     *
     * @param userSeq 요청자 (WEB 이 세션에서 주입)
     * @param role    요청자 권한 (WEB 이 세션에서 주입). 없으면 권한 없는 것으로 본다
     * @param request 질문·화면·최근 대화
     * @return token* → done SSE
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(
            @RequestHeader("X-User-Seq") Long userSeq,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody AssistantStreamRequestVo request) {
        log.info("도우미 스트리밍 요청 - userSeq: {}, route: {}", userSeq, request.getRoute());
        return assistantService.stream(userSeq, role, request);
    }
}
