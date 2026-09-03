package com.workmate.was.global.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * LLM 제공자별 {@link ChatClient} 보관소.
 *
 * <p><b>왜 직접 만드나.</b> Spring AI 의 {@code ChatClientAutoConfiguration} 은
 * {@code chatClientBuilder(..., ChatModel chatModel, ...)} 로 {@link ChatModel} 을 <b>하나만</b> 주입받는다.
 * 제공자 스타터를 둘(google-genai·openai) 다 클래스패스에 두면 {@code ChatModel} 빈이 둘이 되어
 * 자동설정이 실패한다. 그래서 제공자별 구현 타입을 직접 주입받아 클라이언트를 만든다.</p>
 *
 * <p><b>왜 둘 다 살려 두나.</b> {@code spring.ai.model.chat} 으로 하나만 켜면 제공자를 바꿀 때마다
 * 서버를 재시작해야 한다. 그러면 AI_MODEL 공통코드로 모델을 고르게 해 둔 의미가 없어진다.
 * 둘 다 띄워 두고 <b>요청마다</b> 고른다.</p>
 *
 * <p>제공자 이름은 {@code common_code.attr1} 에 적힌 값과 같다 — Spring AI 의
 * {@code spring.ai.model.chat} 값(google-genai·openai)을 그대로 쓴다.</p>
 */
@Component
public class ChatClientRegistry {

    /** Gemini — 멀티모달(이미지·오디오) 경로가 이 제공자에 묶여 있다 */
    public static final String GOOGLE_GENAI = "google-genai";

    /** Groq (OpenAI 호환 엔드포인트) */
    public static final String OPENAI = "openai";

    private final Map<String, ChatClient> clients = new LinkedHashMap<>();
    private final String defaultProvider;

    /**
     * @param googleGenAiChatModel Gemini 자동설정이 만든 모델
     * @param openAiChatModel      OpenAI 호환(Groq) 자동설정이 만든 모델
     */
    public ChatClientRegistry(GoogleGenAiChatModel googleGenAiChatModel, OpenAiChatModel openAiChatModel) {
        this.clients.put(GOOGLE_GENAI, ChatClient.builder(googleGenAiChatModel).build());
        this.clients.put(OPENAI, ChatClient.builder(openAiChatModel).build());
        // 제공자를 알 수 없을 때의 기본값. 멀티모달까지 감당하는 쪽이라 Gemini 로 둔다
        this.defaultProvider = GOOGLE_GENAI;
    }

    /**
     * 제공자에 해당하는 클라이언트를 준다.
     *
     * <p>모르는 제공자면 <b>예외 대신 기본 제공자</b>로 떨어진다. 공통코드에 attr1 을 채우지 않은
     * 모델이 하나 섞였다고 채팅 전체가 죽는 것보다, 기본 제공자로 답하는 편이 낫다.</p>
     *
     * @param provider {@code common_code.attr1} 값 (null 이면 기본 제공자)
     * @return 해당 제공자의 클라이언트
     */
    public ChatClient get(String provider) {
        if (provider == null || provider.isBlank()) {
            return this.clients.get(this.defaultProvider);
        }
        return this.clients.getOrDefault(provider, this.clients.get(this.defaultProvider));
    }

    /**
     * 멀티모달(이미지·오디오) 입력을 다루는 경로가 쓰는 클라이언트.
     *
     * <p>영수증 OCR·음성 전사는 <b>제공자를 고를 수 없다</b> — 사용자가 드롭다운에서 텍스트 전용
     * 모델을 골라 둔 채로 이미지를 올리면 조용히 실패하기 때문이다. Gemini 로 고정한다.</p>
     */
    public ChatClient multimodal() {
        return this.clients.get(GOOGLE_GENAI);
    }

    /** 등록된 제공자 목록 (진단·테스트용) */
    public Set<String> providers() {
        return this.clients.keySet();
    }
}
