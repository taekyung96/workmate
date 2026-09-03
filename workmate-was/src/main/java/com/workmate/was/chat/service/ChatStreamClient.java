package com.workmate.was.chat.service;

import com.workmate.was.usage.vo.LlmFeature;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * LLM 스트리밍 호출 seam.
 * Spring AI ChatClient 의 fluent 체인을 이 인터페이스 뒤로 숨겨, 스트리밍 조립 로직을
 * 실제 Gemini 호출 없이 단위 테스트할 수 있게 한다.
 */
public interface ChatStreamClient {

    /**
     * 시스템 지시 + 대화 히스토리 + 현재 질문(+선택 이미지)으로 응답을 토큰 단위 스트리밍한다.
     * userSeq 는 Tool 실행 컨텍스트로 전달돼, 도구가 본인 데이터만 조회하도록 한다 (F5-03).
     *
     * @param userSeq       로그인 사용자 (Tool 컨텍스트)
     * @param role          요청자 권한 (ROLE_USER·ROLE_ADMIN) — @Tool 이 ToolContext 로 읽어 권한을 검사한다
     * @param feature       사용량 기록 분류 (CHAT · ASSISTANT)
     * @param provider      LLM 제공자 (common_code.attr1). null 이면 기본 제공자
     * @param model         사용할 모델명 (AI_MODEL 화이트리스트 통과값, F5-05)
     * @param systemPrompt  시스템 지시문
     * @param history       이전 대화 맥락 (시간순)
     * @param userMessage   현재 사용자 질문
     * @param imageData     첨부 이미지 바이트 (없으면 null)
     * @param imageMimeType 첨부 이미지 MIME 타입 (없으면 null)
     * @return 응답 토큰 Flux
     */
    Flux<String> stream(Long userSeq, String role, LlmFeature feature, String provider, String model,
                        String systemPrompt, List<Message> history, String userMessage,
                        byte[] imageData, String imageMimeType);
}
