package com.workmate.was.chat.service.impl;

import com.workmate.was.chat.service.ChatStreamClient;
import com.workmate.was.guide.tool.GuideTools;
import com.workmate.was.usage.service.LlmUsageService;
import com.workmate.was.usage.vo.LlmFeature;
import com.workmate.was.receipt.tool.ReceiptTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Spring AI(Gemini) 기반 스트리밍 호출 구현체.
 * ChatClient.stream().content() 로 응답을 토큰 Flux 로 흘린다 (F2-05·06).
 * 영수증·가이드 조회 @Tool 을 등록하고, userSeq 를 ToolContext 로 넘겨 본인 데이터만 조회하게 한다 (F5).
 */
@Slf4j
@Component
public class ChatStreamClientImpl implements ChatStreamClient {

    private final ChatClient chatClient;
    private final ReceiptTools receiptTools;
    private final GuideTools guideTools;
    private final LlmUsageService llmUsageService;

    public ChatStreamClientImpl(ChatClient.Builder chatClientBuilder,
                                ReceiptTools receiptTools, GuideTools guideTools,
                                LlmUsageService llmUsageService) {
        this.chatClient = chatClientBuilder.build();
        this.receiptTools = receiptTools;
        this.guideTools = guideTools;
        this.llmUsageService = llmUsageService;
    }

    @Override
    public Flux<String> stream(Long userSeq, String model, String systemPrompt, List<Message> history,
                               String userMessage, byte[] imageData, String imageMimeType) {
        // 첫 토큰 전 실패(타임아웃·쿼터)만 1회 재시도한다 (F2.3).
        // 이미 토큰을 흘린 뒤 재시도하면 클라이언트에 중복 응답이 쌓이므로, 재시도 조건에서 제외한다.
        AtomicBoolean tokenEmitted = new AtomicBoolean(false);
        // 사용량(F-OBS) — 스트리밍에서 usage 는 보통 마지막 청크에 실려온다. 마지막 값을 들고 있다가
        // 스트림이 끝날 때 한 번 기록한다. 실제 응답 모델은 별칭(gemini-flash-latest)과 다를 수 있어 함께 잡는다
        AtomicReference<Usage> lastUsage = new AtomicReference<>();
        AtomicReference<String> responseModel = new AtomicReference<>(model);
        boolean hasImage = imageData != null && imageData.length > 0;
        // 요청 모델 적용 (F5-05) — 값이 있으면 이 요청에 한해 모델을 교체한다
        org.springframework.ai.chat.prompt.ChatOptions options =
                org.springframework.ai.chat.prompt.ChatOptions.builder().model(model).build();
        // Flux.defer 로 감싸 재시도 시마다 prompt·stream 을 새로 만든다 — Spring AI 의
        // stream().content() Flux 는 재구독이 불가("No StreamAdvisors available")해서다.
        return Flux.defer(() -> chatClient.prompt()
                        .options(options)
                        .system(systemPrompt)
                        .messages(history)          // 이전 맥락 (F2-10)
                        .user(userSpec -> {
                            userSpec.text(userMessage);
                            // 첨부 이미지가 있으면 멀티모달 입력으로 추가 (OcrServiceImpl 과 동일한 media 방식)
                            if (hasImage) {
                                userSpec.media(parseMimeType(imageMimeType), new ByteArrayResource(imageData));
                            }
                        })
                        // @Tool 등록 + userSeq 컨텍스트 (F5-01·02·03). AI 가 필요 시에만 호출한다.
                        .tools(receiptTools, guideTools)
                        .toolContext(Map.of("userSeq", userSeq))
                        .stream()
                        // content() 대신 chatResponse() 를 쓰는 이유는 usage 메타데이터 때문이다.
                        // 바깥으로 내보내는 타입(Flux<String>)은 그대로라 호출부는 영향이 없다
                        .chatResponse())
                .doOnNext(response -> captureUsage(response, lastUsage, responseModel))
                .map(this::textOf)
                .filter(token -> !token.isEmpty())
                .doOnNext(token -> tokenEmitted.set(true))
                .retryWhen(Retry.max(1).filter(ex -> !tokenEmitted.get()))
                // 정상 종료뿐 아니라 중도 취소(사용자 이탈)에서도 남긴다 — 호출은 이미 비용이 발생했다.
                // 취소 시엔 usage 가 없어 토큰이 null 로 기록되지만 "누가 언제 썼는지"는 남는다
                .doFinally(signal -> {
                    if (lastUsage.get() != null || tokenEmitted.get()) {
                        llmUsageService.record(userSeq, LlmFeature.CHAT, responseModel.get(), lastUsage.get());
                    }
                });
    }

    /** 응답 청크에서 usage·실제 모델명을 뽑아 최신값으로 갱신한다 (마지막 청크에 실려오는 경우가 많다) */
    private void captureUsage(ChatResponse response, AtomicReference<Usage> lastUsage,
                              AtomicReference<String> responseModel) {
        if (response == null || response.getMetadata() == null) {
            return;
        }
        if (response.getMetadata().getUsage() != null) {
            lastUsage.set(response.getMetadata().getUsage());
        }
        String model = response.getMetadata().getModel();
        if (model != null && !model.isBlank()) {
            responseModel.set(model);
        }
    }

    /** 응답 청크의 본문 텍스트 — usage 만 실린 청크는 본문이 없어 빈 문자열이 된다 */
    private String textOf(ChatResponse response) {
        if (response == null || response.getResult() == null
                || response.getResult().getOutput() == null) {
            return "";
        }
        String text = response.getResult().getOutput().getText();
        return text == null ? "" : text;
    }

    private MimeType parseMimeType(String mimeType) {
        try {
            return MimeType.valueOf(mimeType);
        } catch (Exception e) {
            return MimeTypeUtils.IMAGE_JPEG;
        }
    }
}
