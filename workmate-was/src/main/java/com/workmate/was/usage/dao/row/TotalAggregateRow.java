package com.workmate.was.usage.dao.row;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@code LlmUsageQueryMapper.selectTotal} 결과 행 — 기간 전체 합계 1건.
 * SUM 대상 행이 하나도 없어도(호출 0건) COUNT 는 0으로 나오므로 항상 행이 반환된다.
 */
@Getter
@NoArgsConstructor
public class TotalAggregateRow {
    private long callCount;
    private Long inputTokens;
    private Long outputTokens;
    private long untrackedCallCount;
}
