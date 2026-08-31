package com.workmate.was.usage.service;

import com.workmate.was.usage.vo.LlmFeature;
import org.springframework.ai.chat.metadata.Usage;

/**
 * LLM 사용량 기록 서비스 (F-OBS) — LLM 을 호출하는 모든 지점이 공통으로 쓴다.
 *
 * <p>기록 실패가 본래 기능을 막으면 안 되므로 구현체는 예외를 삼키고 로그만 남긴다.
 */
public interface LlmUsageService {

    /**
     * Spring AI 응답의 usage 를 그대로 기록한다.
     *
     * @param userSeq   사용한 사용자
     * @param feature   호출 기능
     * @param modelName 실제 호출된 모델명 (없으면 null)
     * @param usage     Spring AI 응답 메타데이터의 usage (null 이면 토큰 없이 호출 사실만 남긴다)
     */
    void record(Long userSeq, LlmFeature feature, String modelName, Usage usage);

    /**
     * 토큰 수를 직접 넘겨 기록한다. 제공자가 usage 를 주지 않는 경로에서 쓴다.
     *
     * @param userSeq      사용한 사용자
     * @param feature      호출 기능
     * @param modelName    모델명 (없으면 null)
     * @param inputTokens  입력 토큰 (모르면 null)
     * @param outputTokens 출력 토큰 (모르면 null)
     */
    void record(Long userSeq, LlmFeature feature, String modelName,
                Integer inputTokens, Integer outputTokens);
}
