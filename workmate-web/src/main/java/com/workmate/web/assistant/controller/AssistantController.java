package com.workmate.web.assistant.controller;

import com.workmate.web.assistant.service.AssistantService;
import com.workmate.web.global.logging.RequestIdFilter;
import com.workmate.web.global.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 도우미 SSE 중계 컨트롤러.
 *
 * <p>본문을 파싱하지 않고 문자열 그대로 넘긴다 — WEB 은 얇은 BFF 이고 요청 형식은 WAS 가 정한다.
 * 인증 principal 과 요청 추적 ID 를 <b>여기서</b> 읽는 이유는, 둘 다 ThreadLocal 기반이라
 * WebClient 교환이 일어나는 리액터 스레드로 넘어가지 않기 때문이다(채팅과 같은 이유).</p>
 */
@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    /**
     * 도우미 응답 스트리밍.
     *
     * @param requestBody 프론트가 보낸 JSON 원문
     * @param loginUser   세션의 로그인 사용자 — 클라이언트가 조작할 수 없는 신원의 출처다
     * @return token* → done SSE
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(
            @RequestBody String requestBody,
            @AuthenticationPrincipal LoginUser loginUser) {
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        return assistantService.stream(loginUser.getUserSeq(), loginUser.getRole(), requestId, requestBody);
    }
}
