package com.workmate.was.chat.service;

import com.workmate.was.chat.service.ChatModelResolver.ModelChoice;
import com.workmate.was.common.service.CommonCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 모델·제공자 해석 검증.
 *
 * <p>여기서 막고 싶은 회귀는 <b>모델과 제공자가 따로 정해지는 것</b>이다. 기본 모델이 Gemini 일
 * 때는 제공자를 비워 둬도 우연히 맞았지만, 기본 모델이 Groq 계열로 바뀌면 Groq 모델명이
 * Gemini 클라이언트로 나가 조용히 실패한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class ChatModelResolverTest {

    private static final String GROUP = "AI_MODEL";

    @Mock private CommonCodeService commonCodeService;

    private ChatModelResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ChatModelResolver(commonCodeService);
        ReflectionTestUtils.setField(resolver, "defaultModel", "qwen/qwen3.8-27b");
    }

    @Test
    @DisplayName("모델을 안 고르면 기본 모델과 그 제공자를 함께 준다")
    void uses_default_model_with_its_provider() {
        when(commonCodeService.findAttr1(GROUP, "qwen/qwen3.8-27b")).thenReturn(Optional.of("openai"));

        ModelChoice choice = resolver.resolve(null);

        assertThat(choice.model()).isEqualTo("qwen/qwen3.8-27b");
        // 기본 모델이 Groq 계열이면 제공자도 반드시 따라와야 한다 — 여기서 null 이면 Gemini 로 나간다
        assertThat(choice.provider()).isEqualTo("openai");
    }

    @Test
    @DisplayName("빈 문자열도 기본 모델로 본다")
    void blank_model_uses_default() {
        when(commonCodeService.findAttr1(GROUP, "qwen/qwen3.8-27b")).thenReturn(Optional.of("openai"));

        assertThat(resolver.resolve("   ").model()).isEqualTo("qwen/qwen3.8-27b");
    }

    @Test
    @DisplayName("고른 모델은 화이트리스트를 통과해야 하고 제공자는 attr1 에서 온다")
    void requested_model_resolves_its_own_provider() {
        when(commonCodeService.isValidCode(GROUP, "gemini-flash-latest")).thenReturn(true);
        when(commonCodeService.findAttr1(GROUP, "gemini-flash-latest")).thenReturn(Optional.of("google-genai"));

        ModelChoice choice = resolver.resolve("gemini-flash-latest");

        assertThat(choice.model()).isEqualTo("gemini-flash-latest");
        assertThat(choice.provider()).isEqualTo("google-genai");
    }

    @Test
    @DisplayName("허용 목록 밖의 모델은 거부한다")
    void rejects_model_outside_whitelist() {
        when(commonCodeService.isValidCode(GROUP, "gpt-4o")).thenReturn(false);

        assertThatThrownBy(() -> resolver.resolve("gpt-4o"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("허용되지 않은 모델");
    }

    @Test
    @DisplayName("attr1 이 비어 있어도 예외를 던지지 않는다 — registry 가 기본 제공자로 떨어뜨린다")
    void missing_attr1_yields_null_provider() {
        when(commonCodeService.isValidCode(GROUP, "some-model")).thenReturn(true);
        when(commonCodeService.findAttr1(GROUP, "some-model")).thenReturn(Optional.empty());

        assertThat(resolver.resolve("some-model").provider()).isNull();
    }
}
