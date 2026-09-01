package com.workmate.was.usage.dao.row;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** {@code LlmUsageQueryMapper.selectDaily} 결과 행 — 날짜(created_at 의 date)별 집계. */
@Getter
@NoArgsConstructor
public class DailyAggregateRow {
    private LocalDate usageDate;
    private long callCount;
    private Long inputTokens;
    private Long outputTokens;
    private long untrackedCallCount;
}
