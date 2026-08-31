package com.workmate.was.usage.service.impl;

import com.workmate.was.usage.dao.LlmUsageRepository;
import com.workmate.was.usage.vo.LlmFeature;
import com.workmate.was.usage.vo.LlmUsage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.metadata.Usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LLM 사용량 기록 서비스 단위 테스트 (F-OBS).
 *
 * <p>이 서비스의 계약은 "기록은 최선을 다하되 <b>본래 기능을 절대 막지 않는다</b>"이다.
 * 채팅·영수증 분석이 사용량 기록 실패로 무너지면 안 되므로 그 성질을 여기서 고정한다.
 */
class LlmUsageServiceImplTest {

    private final LlmUsageRepository repository = mock(LlmUsageRepository.class);
    private final LlmUsageServiceImpl service = new LlmUsageServiceImpl(repository);

    /** Spring AI Usage 스텁 — 프롬프트/생성 토큰만 쓰므로 나머지는 기본값 */
    private Usage usage(Integer prompt, Integer completion) {
        Usage usage = mock(Usage.class);
        when(usage.getPromptTokens()).thenReturn(prompt);
        when(usage.getCompletionTokens()).thenReturn(completion);
        return usage;
    }

    @Test
    @DisplayName("Spring AI usage 의 토큰 수를 그대로 기록한다")
    void records_tokens_from_usage() {
        service.record(12L, LlmFeature.CHAT, "gemini-3.7-flash", usage(280, 35));

        ArgumentCaptor<LlmUsage> captor = ArgumentCaptor.forClass(LlmUsage.class);
        verify(repository).save(captor.capture());

        LlmUsage saved = captor.getValue();
        assertThat(saved.getUserSeq()).isEqualTo(12L);
        assertThat(saved.getFeature()).isEqualTo(LlmFeature.CHAT);
        assertThat(saved.getModelName()).isEqualTo("gemini-3.7-flash");
        assertThat(saved.getInputTokens()).isEqualTo(280);
        assertThat(saved.getOutputTokens()).isEqualTo(35);
    }

    @Test
    @DisplayName("usage 가 없어도 호출 사실은 남긴다 — 토큰만 null (임베딩 경로)")
    void records_call_without_usage() {
        service.record(7L, LlmFeature.EMBEDDING, null, (Usage) null);

        ArgumentCaptor<LlmUsage> captor = ArgumentCaptor.forClass(LlmUsage.class);
        verify(repository).save(captor.capture());

        LlmUsage saved = captor.getValue();
        assertThat(saved.getFeature()).isEqualTo(LlmFeature.EMBEDDING);
        assertThat(saved.getInputTokens()).isNull();
        assertThat(saved.getOutputTokens()).isNull();
    }

    @Test
    @DisplayName("userSeq 가 없으면 저장하지 않는다 — 귀속 대상 없는 기록은 집계를 오염시킨다")
    void skips_when_user_missing() {
        service.record(null, LlmFeature.CHAT, "m", 10, 20);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("저장이 실패해도 예외를 던지지 않는다 — 기록 실패가 본래 기능을 막으면 안 된다")
    void never_propagates_failure() {
        when(repository.save(any())).thenThrow(new RuntimeException("DB 장애"));

        assertThatCode(() -> service.record(12L, LlmFeature.OCR, "m", 10, 20))
                .doesNotThrowAnyException();
    }
}
