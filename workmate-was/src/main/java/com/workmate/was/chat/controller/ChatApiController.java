package com.workmate.was.chat.controller;

import com.workmate.was.chat.service.ChatService;
import com.workmate.was.chat.vo.ChatMessageVo;
import com.workmate.was.chat.vo.ChatRoomVo;
import com.workmate.was.chat.vo.ChatStreamRequestVo;
import com.workmate.was.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 채팅 REST API 컨트롤러 (C1~C4, 04 §3.2).
 * 사용자 식별은 WEB 프록시가 주입하는 X-User-Seq 헤더로 한다.
 * 스트리밍(/stream)만 text/event-stream 이고 나머지는 ApiResponse 공통 포맷이다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatApiController {

    private final ChatService chatService;

    /** 내 채팅방 목록 (C1) */
    @GetMapping("/rooms")
    public ApiResponse<List<ChatRoomVo>> getRooms(@RequestHeader("X-User-Seq") Long userSeq) {
        log.info("채팅방 목록 조회 - userSeq: {}", userSeq);
        return ApiResponse.success(chatService.getRooms(userSeq));
    }

    /** 방 대화 이력 (C2) */
    @GetMapping("/rooms/{roomSeq}/messages")
    public ApiResponse<List<ChatMessageVo>> getMessages(
            @RequestHeader("X-User-Seq") Long userSeq,
            @PathVariable("roomSeq") Long roomSeq) {
        log.info("채팅 이력 조회 - userSeq: {}, roomSeq: {}", userSeq, roomSeq);
        return ApiResponse.success(chatService.getMessages(userSeq, roomSeq));
    }

    /** 방 논리 삭제 (C4) */
    @PostMapping("/rooms/{roomSeq}/delete")
    public ApiResponse<Void> deleteRoom(
            @RequestHeader("X-User-Seq") Long userSeq,
            @PathVariable("roomSeq") Long roomSeq) {
        log.info("채팅방 삭제 - userSeq: {}, roomSeq: {}", userSeq, roomSeq);
        chatService.deleteRoom(userSeq, roomSeq);
        return ApiResponse.success();
    }

    /**
     * 메시지 전송 + AI 응답 SSE 스트리밍 (C3, F2-06).
     * 반환 타입이 Flux&lt;ServerSentEvent&gt; 라 Spring MVC 가 비동기 서블릿으로 브리지해
     * 버퍼링 없이 토큰을 흘린다.
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(
            @RequestHeader("X-User-Seq") Long userSeq,
            // WEB 의 WebClient 가 세션 권한으로 넣어주는 값이다. 없으면 권한 없는 것으로 본다
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody ChatStreamRequestVo request) {
        log.info("채팅 스트리밍 요청 - userSeq: {}, roomSeq: {}", userSeq, request.getRoomSeq());
        return chatService.streamChat(userSeq, role, request);
    }
}
