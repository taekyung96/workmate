package com.workmate.was.assistant.service.impl;

import com.workmate.was.assistant.service.AssistantScreenContext;
import com.workmate.was.assistant.vo.AssistantStreamRequestVo;
import com.workmate.was.chat.service.ChatRateLimiter;
import com.workmate.was.chat.service.ChatStreamClient;
import com.workmate.was.guide.service.GuideRetriever;
import com.workmate.was.chat.service.RagPromptBuilder;
import com.workmate.was.usage.vo.LlmFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 도우미 서비스 단위 테스트.
 *
 * <p>핵심 계약 둘을 고정한다 — <b>사용량이 ASSISTANT 로 기록된다</b>(비용을 채팅과 섞지 않는다)와
 * <b>대화방·메시지를 만들지 않는다</b>(채팅 이력 오염 금지). 후자는 이 서비스가 아예
 * {@code ChatMessagePersister} 를 의존하지 않는 것으로 구조적으로 보장된다.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssistantServiceImplTest {

    @Mock private ChatRateLimiter rateLimiter;
    @Mock private GuideRetriever guideRetriever;
    @Mock private RagPromptBuilder ragPromptBuilder;
    @Mock private ChatStreamClient chatStreamClient;
    @Mock private AssistantScreenContext screenContext;
    @Mock private com.workmate.was.common.service.CommonCodeService commonCodeService;

    private AssistantServiceImpl newService() {
        return newService("gemini-flash-latest");
    }

    /**
     * 기본 모델을 지정해 서비스를 만든다.
     *
     * @param defaultModel LLM_CHAT_MODEL 에 해당하는 기본 모델 코드
     * @return 도우미 서비스
     */
    private AssistantServiceImpl newService(String defaultModel) {
        com.workmate.was.chat.service.ChatModelResolver modelResolver =
                new com.workmate.was.chat.service.ChatModelResolver(commonCodeService);
        ReflectionTestUtils.setField(modelResolver, "defaultModel", defaultModel);
        return new AssistantServiceImpl(
                rateLimiter, guideRetriever, ragPromptBuilder, chatStreamClient, screenContext,
                modelResolver);
    }

    private AssistantStreamRequestVo request(String message, String route) {
        AssistantStreamRequestVo vo = new AssistantStreamRequestVo();
        vo.setMessage(message);
        vo.setRoute(route);
        return vo;
    }

    /** 도구·RAG 를 비워 두고 토큰만 흘리는 기본 스텁 */
    private void stubHappyPath(String... tokens) {
        when(guideRetriever.retrieve(anyLong(), anyString())).thenReturn(List.of());
        when(ragPromptBuilder.build(any())).thenReturn("");
        when(screenContext.describe(any())).thenReturn("[화면] 내 사용량");
        when(chatStreamClient.stream(anyLong(), any(), any(), any(), anyString(), anyString(),
                any(), anyString(), any(), any())).thenReturn(Flux.just(tokens));
    }

    @Test
    @DisplayName("사용량은 ASSISTANT 로 기록된다 — 채팅과 비용을 섞지 않는다")
    void records_usage_as_assistant_feature() {
        stubHappyPath("안녕");

        newService().stream(12L, "ROLE_USER", request("이 화면 뭔가요", "my-usage")).blockLast();

        verify(chatStreamClient).stream(eq(12L), eq("ROLE_USER"), eq(LlmFeature.ASSISTANT),
                any(), anyString(), anyString(), any(), anyString(), any(), any());
    }

    @Test
    @DisplayName("도우미도 기본 모델의 제공자를 함께 보낸다 — 제공자를 비우면 Groq 모델이 Gemini 로 나간다")
    void sends_provider_of_the_default_model() {
        stubHappyPath("안녕");
        // 기본 모델이 Groq 계열인 상황. 예전처럼 제공자를 null 로 고정하면 Groq 모델명이
        // Gemini 클라이언트로 나가 예외 없이 빈 응답이 된다
        when(commonCodeService.findAttr1("AI_MODEL", "qwen/qwen3.8-27b"))
                .thenReturn(java.util.Optional.of("openai"));

        newService("qwen/qwen3.8-27b")
                .stream(12L, "ROLE_USER", request("이 화면 뭔가요", "my-usage")).blockLast();

        verify(chatStreamClient).stream(anyLong(), any(), eq(LlmFeature.ASSISTANT), eq("openai"),
                eq("qwen/qwen3.8-27b"), anyString(), any(), anyString(), any(), any());
    }

    @Test
    @DisplayName("화면 맥락이 시스템 프롬프트에 들어간다")
    void screen_context_is_appended_to_system_prompt() {
        stubHappyPath("안녕");

        newService().stream(12L, "ROLE_USER", request("이 화면 뭔가요", "my-usage")).blockLast();

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(chatStreamClient).stream(anyLong(), any(), any(), any(), anyString(), prompt.capture(),
                any(), anyString(), any(), any());
        assertThat(prompt.getValue()).contains("[화면] 내 사용량");
    }

    @Test
    @DisplayName("토큰 뒤에 done 이벤트가 붙는다")
    void emits_token_then_done() {
        stubHappyPath("안", "녕");

        StepVerifier.create(newService().stream(12L, "ROLE_USER", request("안녕", "chat")))
                .expectNextMatches(e -> "token".equals(e.event()))
                .expectNextMatches(e -> "token".equals(e.event()))
                .expectNextMatches(e -> "done".equals(e.event()))
                .verifyComplete();
    }

    @Test
    @DisplayName("빈 메시지는 레이트리밋을 쓰지 않고 바로 거절한다")
    void rejects_blank_message_without_consuming_rate_limit() {
        StepVerifier.create(newService().stream(12L, "ROLE_USER", request("   ", "chat")))
                .expectNextMatches(e -> "error".equals(e.event()))
                .verifyComplete();

        // 빈 요청으로 한도를 갉아먹으면 안 된다
        verify(rateLimiter, never()).check(anyLong());
    }

    @Test
    @DisplayName("RAG 는 항상 켠다 — 사용법 답변의 근거가 가이드 문서다")
    void rag_is_always_on() {
        stubHappyPath("안녕");

        newService().stream(12L, "ROLE_USER", request("영수증 어떻게 올려요", "receipt")).blockLast();

        verify(guideRetriever).retrieve(12L, "영수증 어떻게 올려요");
    }

    @Test
    @DisplayName("프론트가 상한을 넘겨 보내도 최근 6개만 쓴다 — 클라이언트를 믿지 않는다")
    void history_is_capped_by_server() {
        stubHappyPath("안녕");
        AssistantStreamRequestVo vo = request("질문", "chat");
        vo.setHistory(turns(10));

        newService().stream(12L, "ROLE_USER", vo).blockLast();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<org.springframework.ai.chat.messages.Message>> history =
                ArgumentCaptor.forClass(List.class);
        verify(chatStreamClient).stream(anyLong(), any(), any(), any(), anyString(), anyString(),
                history.capture(), anyString(), any(), any());
        assertThat(history.getValue()).hasSize(6);
    }

    /** user/assistant 가 번갈아 오는 대화 n턴 */
    private List<AssistantStreamRequestVo.Turn> turns(int n) {
        return java.util.stream.IntStream.range(0, n).mapToObj(i -> {
            AssistantStreamRequestVo.Turn t = new AssistantStreamRequestVo.Turn();
            t.setRole(i % 2 == 0 ? "user" : "assistant");
            t.setContent("내용" + i);
            return t;
        }).toList();
    }
}
