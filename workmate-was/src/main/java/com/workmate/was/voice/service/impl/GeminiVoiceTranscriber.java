package com.workmate.was.voice.service.impl;

import com.workmate.was.usage.service.LlmUsageService;
import com.workmate.was.global.config.ChatClientRegistry;
import com.workmate.was.usage.vo.LlmFeature;
import com.workmate.was.voice.service.VoiceTranscriber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Gemini 멀티모달 기반 음성 전사 구현체 (F8-1).
 * 오디오를 media 로 첨부해 Gemini 에 직접 전사시킨다(별도 STT 벤더 불필요).
 * 품질이 병목이면 이 구현체만 Clova/Whisper 등으로 교체하면 된다.
 */
@Slf4j
@Service
public class GeminiVoiceTranscriber implements VoiceTranscriber {

    private final ChatClient chatClient;
    private final LlmUsageService llmUsageService;

    /** @param registry 오디오 입력이라 제공자를 고를 수 없다 — 멀티모달 고정 클라이언트를 받는다 */
    public GeminiVoiceTranscriber(ChatClientRegistry registry, LlmUsageService llmUsageService) {
        this.chatClient = registry.multimodal();
        this.llmUsageService = llmUsageService;
    }

    private static final String SYSTEM_PROMPT =
            "당신은 한국어 회의 음성을 정확하게 받아쓰는 전사(STT) 전문가입니다. "
            + "들리는 내용을 자연스러운 한국어 문장으로 그대로 옮기되, 없는 내용을 지어내지 마세요. "
            + "화자가 여럿이면 문단으로 구분하고, 잡음·군말은 정리해도 됩니다.";

    /** {@inheritDoc} */
    @Override
    public String transcribe(Long userSeq, Resource audio, String mimeType) {
        MimeType mediaType = parseMimeType(mimeType);
        log.info("음성 전사 요청 시작 (MimeType: {})", mediaType);

        // content() 대신 chatResponse() — 사용량(usage) 을 함께 받기 위함 (F-OBS)
        ChatResponse response = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userSpec -> userSpec
                        .text("이 회의 오디오의 내용을 한국어 텍스트로 받아써 주세요. 다른 설명 없이 전사문만 출력하세요.")
                        .media(mediaType, audio))
                .call()
                .chatResponse();

        if (response != null && response.getMetadata() != null) {
            llmUsageService.record(userSeq, LlmFeature.STT,
                    response.getMetadata().getModel(), response.getMetadata().getUsage());
        }
        String text = (response == null || response.getResult() == null
                || response.getResult().getOutput() == null)
                ? null : response.getResult().getOutput().getText();

        log.info("음성 전사 완료 (길이: {}자)", text != null ? text.length() : 0);
        return text != null ? text : "";
    }

    /** MIME 문자열을 파싱하고, 실패하면 일반 오디오 기본값으로 대체한다 */
    private MimeType parseMimeType(String mimeType) {
        try {
            return MimeType.valueOf(mimeType);
        } catch (Exception e) {
            return MimeTypeUtils.parseMimeType("audio/mpeg");
        }
    }
}
