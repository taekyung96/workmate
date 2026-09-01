package com.workmate.was.usage.dao.row;

import lombok.Getter;
import lombok.NoArgsConstructor;

/** {@code LlmUsageQueryMapper.selectByFeature} 결과 행 — 기능(feature)별 집계. */
@Getter
@NoArgsConstructor
public class FeatureAggregateRow {
    private String feature;
    private long callCount;
    private Long inputTokens;
    private Long outputTokens;
    private long untrackedCallCount;
}
