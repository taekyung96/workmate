package com.workmate.web.chat.controller;

import com.workmate.web.chat.service.ChatService;
import com.workmate.web.global.logging.RequestIdFilter;
import com.workmate.web.global.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 채팅 도메인 WEB 프록시 컨트롤러.
 * 화면(fetch)이 호출하는 /api/v1/chat/** 요청을 WAS 로 중계한다 (브라우저는 WEB(8080)만 바라봄).
 * 스트리밍만 WebClient 무버퍼 relay, 나머지는 RestClient 패스스루.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** 내 채팅방 목록 (C1) */
    @GetMapping("/rooms")
    public ResponseEntity<String> getRooms() {
        return jsonPassthrough(chatService.getRooms());
    }

    /** 방 대화 이력 (C2) */
    @GetMapping("/rooms/{roomSeq}/messages")
    public ResponseEntity<String> getMessages(@PathVariable("roomSeq") Long roomSeq) {
        return jsonPassthrough(chatService.getMessages(roomSeq));
    }

    /** 방 논리 삭제 (C4) */
    @PostMapping("/rooms/{roomSeq}/delete")
    public ResponseEntity<String> deleteRoom(@PathVariable("roomSeq") Long roomSeq) {
        return jsonPassthrough(chatService.deleteRoom(roomSeq));
    }

    /**
     * 메시지 전송 + AI 응답 SSE 스트리밍 relay (C3, F2-06).
     * 인증 principal 은 요청(서블릿) 스레드에서 @AuthenticationPrincipal 로 캡처해
     * 리액티브 relay 로 넘긴다 — 리액터 스레드에서 SecurityContext 를 읽지 않기 위함.
     * 요청 추적 ID(F-OBS)도 같은 이유로 여기서 MDC 를 읽어 넘긴다 — MDC 는 ThreadLocal 이라
     * WebClient 교환이 일어나는 리액터 스레드로는 넘어가지 않는다.
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(
            @RequestBody String requestBody,
            @AuthenticationPrincipal LoginUser loginUser) {
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        return chatService.stream(loginUser.getUserSeq(), loginUser.getRole(), requestId, requestBody);
    }

    /** WAS 응답의 상태코드·본문을 유지하며 JSON 컨텐츠 타입으로 화면에 전달한다. */
    private ResponseEntity<String> jsonPassthrough(ResponseEntity<String> wasResponse) {
        return ResponseEntity.status(wasResponse.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(wasResponse.getBody());
    }
}
