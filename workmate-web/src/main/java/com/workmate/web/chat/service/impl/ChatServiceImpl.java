package com.workmate.web.chat.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workmate.web.chat.service.ChatService;
import com.workmate.web.global.logging.RequestIdFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 채팅 도메인 WEB 프록시 서비스 구현체.
 * 조회·삭제는 RestClient(블로킹, 인터셉터가 X-User-Seq 자동 주입), 스트리밍은 WebClient(무버퍼 relay).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final RestClient wasRestClient;
    private final WebClient wasWebClient;
    private final ObjectMapper objectMapper;

    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE =
            new ParameterizedTypeReference<>() {};

    @Override
    public ResponseEntity<String> getRooms() {
        return wasRestClient.get()
                .uri("/api/v1/chat/rooms")
                .retrieve()
                .toEntity(String.class);
    }

    @Override
    public ResponseEntity<String> getMessages(Long roomSeq) {
        return wasRestClient.get()
                .uri("/api/v1/chat/rooms/{roomSeq}/messages", roomSeq)
                .retrieve()
                .toEntity(String.class);
    }

    @Override
    public ResponseEntity<String> deleteRoom(Long roomSeq) {
        return wasRestClient.post()
                .uri("/api/v1/chat/rooms/{roomSeq}/delete", roomSeq)
                .retrieve()
                .toEntity(String.class);
    }

    @Override
    public Flux<ServerSentEvent<String>> stream(Long userSeq, String role, String requestId, String requestBody) {
        log.info("채팅 스트리밍 relay - userSeq: {}", userSeq);
        WebClient.RequestBodySpec req = wasWebClient.post()
                .uri("/api/v1/chat/stream")
                .header("X-User-Seq", String.valueOf(userSeq))
                .header("X-User-Role", role);
        // 요청 추적 ID 전파 (F-OBS) — .header(...) 는 조립 시점(서블릿 스레드)에 실행되므로 안전하다.
        // null 이면 WebClient 가 예외를 던지므로 값이 있을 때만 붙인다.
        if (requestId != null) {
            req = req.header(RequestIdFilter.HEADER, requestId);
        }
        return req.contentType(MediaType.APPLICATION_JSON)
                // 성공은 SSE로 받되, 스트림 시작 전 오류(429·400)는 WAS가 JSON(ApiResponse)으로 응답한다.
                // Accept에 JSON을 함께 넣지 않으면 WAS의 오류 응답이 콘텐츠 협상(406)에 걸려 본문이 사라진다.
                .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                // 스트림 시작 전 WAS 오류(빈 메시지 400·요청제한 429 등)는 event-stream 이 아니므로
                // 상태코드를 검사해 error 이벤트 하나로 변환한다. 정상(2xx)은 이벤트를 그대로 통과.
                // 이때 HTTP 상태코드를 error 데이터에 실어, 화면이 429(요청제한)와 그 외를 구분하고
                // 재시도 가능 여부(스트림 시작 전 실패라 저장된 게 없음)를 판단할 수 있게 한다.
                .exchangeToFlux(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToFlux(SSE_TYPE);
                    }
                    int status = response.statusCode().value();
                    return response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMapMany(body -> Flux.just(ServerSentEvent.<String>builder()
                                    .event("error")
                                    .data(buildErrorData(status, body))
                                    .build()));
                });
    }

    /**
     * WAS 오류 응답(ApiResponse JSON)에서 사용자 메시지를 추출해 상태코드와 함께 화면용 error 데이터로 만든다.
     *
     * @param status  WAS HTTP 상태코드 (429·400 등)
     * @param wasBody WAS 오류 본문 (비어 있을 수 있음)
     * @return {@code {"status":<코드>,"message":"<메시지>"}} 형태의 JSON 문자열
     */
    private String buildErrorData(int status, String wasBody) {
        String message = "요청 처리에 실패했습니다.";
        try {
            if (wasBody != null && !wasBody.isBlank()) {
                JsonNode msg = objectMapper.readTree(wasBody).get("message");
                if (msg != null && !msg.isNull() && !msg.asText().isBlank()) {
                    message = msg.asText();
                }
            }
            return objectMapper.writeValueAsString(Map.of("status", status, "message", message));
        } catch (JsonProcessingException e) {
            log.warn("스트림 오류 응답 변환 실패 - status: {}", status, e);
            return "{\"status\":" + status + ",\"message\":\"요청 처리에 실패했습니다.\"}";
        }
    }
}
