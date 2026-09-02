package com.workmate.web.assistant.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workmate.web.assistant.service.AssistantService;
import com.workmate.web.global.logging.RequestIdFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 도우미 SSE 중계 — 채팅과 같은 무버퍼 relay 방식이다.
 *
 * <p>인증 헤더(X-User-Seq·X-User-Role)를 리액터 스레드가 아닌 <b>조립 시점</b>에 붙이는 것도 같다.
 * 요청 본문은 파싱하지 않고 문자열 그대로 넘긴다 — WEB 은 얇은 BFF 이고 요청 형식은 WAS 가 정한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantServiceImpl implements AssistantService {

    private final WebClient wasWebClient;
    private final ObjectMapper objectMapper;

    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    /** {@inheritDoc} */
    @Override
    public Flux<ServerSentEvent<String>> stream(Long userSeq, String role, String requestId, String requestBody) {
        log.info("도우미 스트리밍 relay - userSeq: {}", userSeq);
        WebClient.RequestBodySpec req = wasWebClient.post()
                .uri("/api/v1/assistant/stream")
                .header("X-User-Seq", String.valueOf(userSeq))
                .header("X-User-Role", role);
        // 요청 추적 ID 전파 (F-OBS) — .header(...) 는 조립 시점(서블릿 스레드)에 실행되므로 안전하다.
        // null 이면 WebClient 가 예외를 던지므로 값이 있을 때만 붙인다.
        if (requestId != null) {
            req = req.header(RequestIdFilter.HEADER, requestId);
        }
        return req.contentType(MediaType.APPLICATION_JSON)
                // 성공은 SSE 로 받되, 스트림 시작 전 오류(429·400)는 WAS 가 JSON(ApiResponse)으로 응답한다.
                // Accept 에 JSON 을 함께 넣지 않으면 WAS 의 오류 응답이 콘텐츠 협상(406)에 걸려 본문이 사라진다.
                .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                // 스트림 시작 전 WAS 오류는 event-stream 이 아니므로 상태코드를 검사해 error 이벤트 하나로 바꾼다.
                // 상태코드를 함께 실어야 화면이 429(요청제한)와 그 외를 구분할 수 있다.
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
     * WAS 오류 응답(ApiResponse JSON)에서 사용자 메시지를 뽑아 상태코드와 함께 화면용 error 데이터로 만든다.
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
            log.warn("도우미 스트림 오류 응답 변환 실패 - status: {}", status, e);
            return "{\"status\":" + status + ",\"message\":\"요청 처리에 실패했습니다.\"}";
        }
    }
}
