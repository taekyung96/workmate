package com.workmate.was.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 제공자 라우팅 검증.
 *
 * <p>여기서 막고 싶은 회귀는 둘이다 — <b>모르는 제공자에 예외를 던지지 않는 것</b>(공통코드에
 * attr1 을 안 채운 모델 하나 때문에 채팅 전체가 죽으면 안 된다)과, <b>멀티모달 경로가 항상
 * Gemini 로 고정되는 것</b>(사용자가 텍스트 전용 모델을 골라 둔 채 영수증을 올리면 조용히 실패한다).</p>
 */
class ChatClientRegistryTest {

    private ChatClientRegistry newRegistry() {
        return new ChatClientRegistry(mock(GoogleGenAiChatModel.class), mock(OpenAiChatModel.class));
    }

    @Test
    @DisplayName("제공자 둘을 모두 등록한다")
    void registers_both_providers() {
        assertThat(newRegistry().providers())
                .containsExactlyInAnyOrder(ChatClientRegistry.GOOGLE_GENAI, ChatClientRegistry.OPENAI);
    }

    @Test
    @DisplayName("제공자별로 서로 다른 클라이언트를 준다")
    void returns_distinct_client_per_provider() {
        ChatClientRegistry registry = newRegistry();

        ChatClient gemini = registry.get(ChatClientRegistry.GOOGLE_GENAI);
        ChatClient groq = registry.get(ChatClientRegistry.OPENAI);

        assertThat(gemini).isNotNull().isNotSameAs(groq);
    }

    @Test
    @DisplayName("모르는 제공자는 예외 대신 기본 제공자로 떨어진다")
    void unknown_provider_falls_back() {
        ChatClientRegistry registry = newRegistry();

        assertThat(registry.get("anthropic")).isSameAs(registry.get(ChatClientRegistry.GOOGLE_GENAI));
    }

    @Test
    @DisplayName("null·빈 제공자도 기본 제공자로 떨어진다 — attr1 미기입 모델을 감당한다")
    void null_or_blank_provider_falls_back() {
        ChatClientRegistry registry = newRegistry();
        ChatClient fallback = registry.get(ChatClientRegistry.GOOGLE_GENAI);

        assertThat(registry.get(null)).isSameAs(fallback);
        assertThat(registry.get("")).isSameAs(fallback);
        assertThat(registry.get("   ")).isSameAs(fallback);
    }

    @Test
    @DisplayName("멀티모달 경로는 항상 Gemini 다 — 제공자를 고를 수 없다")
    void multimodal_is_always_gemini() {
        ChatClientRegistry registry = newRegistry();

        assertThat(registry.multimodal()).isSameAs(registry.get(ChatClientRegistry.GOOGLE_GENAI));
        assertThat(registry.multimodal()).isNotSameAs(registry.get(ChatClientRegistry.OPENAI));
    }
}
