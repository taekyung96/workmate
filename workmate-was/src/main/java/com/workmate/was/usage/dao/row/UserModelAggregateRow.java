package com.workmate.was.usage.dao.row;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@code LlmUsageQueryMapper.selectModelUsageByUsers} 결과 행 — (user_seq, model_name)별 집계.
 * by-user 페이지에 표시된 사용자들의 사용자별 추정 비용을 모델 단가에 맞게 정확히 계산하려고
 * 별도로 조회한다(사용자 한 명이 여러 모델을 섞어 썼을 수 있다).
 */
@Getter
@NoArgsConstructor
public class UserModelAggregateRow implements ModelTokenUsage {
    private Long userSeq;
    private String modelName;
    private long callCount;
    private Long inputTokens;
    private Long outputTokens;
}
