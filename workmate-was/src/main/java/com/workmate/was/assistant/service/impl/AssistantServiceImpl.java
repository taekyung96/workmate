package com.workmate.was.assistant.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workmate.was.assistant.service.AssistantScreenContext;
import com.workmate.was.assistant.service.AssistantService;
import com.workmate.was.assistant.vo.AssistantStreamRequestVo;
import com.workmate.was.chat.service.ChatRateLimiter;
import com.workmate.was.chat.service.ChatStreamClient;
import com.workmate.was.guide.service.GuideRetriever;
import com.workmate.was.chat.service.RagPromptBuilder;
import com.workmate.was.guide.vo.GuideSourceChunk;
import com.workmate.was.usage.vo.LlmFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 페이지 인식 도우미.
 *
 * <p>채팅({@code ChatServiceImpl})에서 <b>대화방·메시지 저장을 뺀</b> 얇은 버전이다.
 * 저장하지 않는 이유는 도우미 질문이 일회성이고, 저장하면 스키마·조회 API·삭제 기능이
 * 줄줄이 따라오기 때문이다. {@code ChatMessagePersister} 를 의존하지 않는 것 자체가
 * "채팅 이력을 오염시키지 않는다"는 보장이다.</p>
 *
 * <p>레이트리밋은 <b>채팅과 카운터를 공유한다</b> — 나누면 도우미로 우회해 한도를 두 배로 쓸 수 있다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantServiceImpl implements AssistantService {

    /** 서버가 강제하는 대화 맥락 상한 — 클라이언트가 얼마를 보내든 여기서 자른다 */
    private static final int MAX_HISTORY = 6;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
            "당신은 Workmate 웹앱의 도우미입니다. 사용자가 지금 보고 있는 화면의 사용법과, "
            + "본인의 AI 사용량에 대한 질문에 한국어로 짧고 명확하게 답하세요. "
            + "화면 사용법은 제공된 가이드 문서를 근거로 답하고, 근거가 없으면 모른다고 말하세요. "
            + "사용량 수치가 필요하면 제공된 도구를 사용하세요. 추측으로 숫자를 지어내지 마세요.";

    private final ChatRateLimiter rateLimiter;
    private final GuideRetriever guideRetriever;
    private final RagPromptBuilder ragPromptBuilder;
    private final ChatStreamClient chatStreamClient;
    private final AssistantScreenContext screenContext;

    /**
     * 도우미는 모델을 고르지 않는다 — 기본 모델로 고정한다.
     * 제공자별 경로가 아니라 제공자 중립 환경변수를 읽는다 (채팅과 같은 이유).
     */
    @Value("${LLM_CHAT_MODEL:gemini-flash-latest}")
    private String modelName;

    /** {@inheritDoc} */
    @Override
    public Flux<ServerSentEvent<String>> stream(Long userSeq, String role, AssistantStreamRequestVo request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            // 빈 요청으로 레이트리밋 한도를 갉아먹지 않도록 검사 전에 막는다
            return Flux.just(sse("error", Map.of("message", "질문을 입력해주세요.")));
        }
        // 채팅과 같은 카운터를 쓴다 (F2-11) — 나누면 도우미로 우회할 수 있다
        rateLimiter.check(userSeq);

        // 도우미는 RAG 를 항상 켠다 — 사용법 답변의 근거가 가이드 문서이기 때문이다
        List<GuideSourceChunk> chunks = guideRetriever.retrieve(userSeq, request.getMessage());

        String systemPrompt = SYSTEM_PROMPT
                + screenContext.describe(request.getRoute())
                + ragPromptBuilder.build(chunks);

        Flux<ServerSentEvent<String>> tokens = chatStreamClient
                .stream(userSeq, role, LlmFeature.ASSISTANT, modelName, systemPrompt,
                        toMessages(request.getHistory()), request.getMessage(), null, null)
                .map(token -> sse("token", Map.of("delta", token)));

        return tokens.concatWith(Flux.just(sse("done", Map.of())))
                .onErrorResume(e -> {
                    log.error("도우미 스트리밍 실패 - userSeq: {}, route: {}", userSeq, request.getRoute(), e);
                    return Flux.just(sse("error", Map.of("message", "응답 생성에 실패했습니다.")));
                });
    }

    /**
     * 프론트가 보낸 대화 턴을 Spring AI Message 로 바꾼다.
     *
     * <p>최근 {@value #MAX_HISTORY} 개만 쓴다 — 클라이언트가 얼마를 보내든 서버가 상한을 강제한다.
     * 맥락이 길수록 토큰이 늘고, 토큰이 곧 비용이다.</p>
     *
     * @param history 프론트가 보낸 대화 (없으면 null)
     * @return Spring AI 메시지 목록 (없으면 빈 목록)
     */
    private List<Message> toMessages(List<AssistantStreamRequestVo.Turn> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        List<AssistantStreamRequestVo.Turn> recent = history.size() > MAX_HISTORY
                ? history.subList(history.size() - MAX_HISTORY, history.size())
                : history;
        List<Message> messages = new ArrayList<>();
        for (AssistantStreamRequestVo.Turn turn : recent) {
            if (turn.getContent() == null || turn.getContent().isBlank()) {
                continue;
            }
            messages.add("assistant".equals(turn.getRole())
                    ? new AssistantMessage(turn.getContent())
                    : new UserMessage(turn.getContent()));
        }
        return messages;
    }

    /** SSE 이벤트 하나를 만든다 (채팅과 같은 형식) */
    private ServerSentEvent<String> sse(String event, Map<String, ?> data) {
        try {
            return ServerSentEvent.<String>builder()
                    .event(event)
                    .data(MAPPER.writeValueAsString(data))
                    .build();
        } catch (JsonProcessingException e) {
            // 직렬화 대상이 Map<String,?> 뿐이라 실제로는 일어나지 않는다
            throw new IllegalStateException("SSE 데이터 직렬화 실패", e);
        }
    }
}
