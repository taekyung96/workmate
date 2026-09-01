package com.workmate.was.usage.dao.row;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@code LlmUsageQueryMapper.selectModelUsageTotal} 결과 행 — 모델명별 집계(기간 전체 대상).
 * 기간 합계의 추정 비용·단가 미등록 건수를 계산하는 데 쓴다.
 */
@Getter
@NoArgsConstructor
public class ModelAggregateRow implements ModelTokenUsage {
    private String modelName;
    private long callCount;
    private Long inputTokens;
    private Long outputTokens;
}
