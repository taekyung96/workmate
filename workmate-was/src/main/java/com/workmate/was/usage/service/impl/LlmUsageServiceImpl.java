package com.workmate.was.usage.service.impl;

import com.workmate.was.usage.dao.LlmUsageRepository;
import com.workmate.was.usage.service.LlmUsageService;
import com.workmate.was.usage.vo.LlmFeature;
import com.workmate.was.usage.vo.LlmUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * LLM 사용량 기록 서비스 구현체 (F-OBS).
 *
 * <p>설계 두 가지를 의도적으로 잡았다.
 * <ul>
 *   <li><b>REQUIRES_NEW</b> — 호출한 쪽 트랜잭션이 롤백돼도 기록은 남긴다.
 *       API 호출은 이미 일어나 <b>비용이 발생한 뒤</b>이므로, 비즈니스 실패와 무관하게 남아야
 *       과금·쿼터 근거가 맞는다.</li>
 *   <li><b>예외를 삼킨다</b> — 사용량 기록이 실패했다고 채팅·영수증 분석이 실패하면 안 된다.
 *       기록은 부가 관심사이므로 로그만 남기고 넘어간다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmUsageServiceImpl implements LlmUsageService {

    private final LlmUsageRepository llmUsageRepository;

    @Override
    public void record(Long userSeq, LlmFeature feature, String modelName, Usage usage) {
        Integer input = usage == null ? null : usage.getPromptTokens();
        Integer output = usage == null ? null : usage.getCompletionTokens();
        record(userSeq, feature, modelName, input, output);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userSeq, LlmFeature feature, String modelName,
                       Integer inputTokens, Integer outputTokens) {
        if (userSeq == null || feature == null) {
            log.warn("사용량 기록 건너뜀 — userSeq/feature 누락 (userSeq: {}, feature: {})", userSeq, feature);
            return;
        }
        try {
            llmUsageRepository.save(LlmUsage.builder()
                    .userSeq(userSeq)
                    .feature(feature)
                    .modelName(modelName)
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .build());
            log.debug("LLM 사용량 기록 - userSeq: {}, feature: {}, in: {}, out: {}",
                    userSeq, feature, inputTokens, outputTokens);
        } catch (Exception e) {
            // 기록 실패가 본래 기능을 막으면 안 된다 — 로그만 남기고 넘어간다
            log.error("LLM 사용량 기록 실패 - userSeq: {}, feature: {}", userSeq, feature, e);
        }
    }
}
