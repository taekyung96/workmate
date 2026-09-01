package com.workmate.was.usage.dao.row;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@code LlmUsageQueryMapper.selectUserPage} 결과 행 — 사용자(user_seq)별 집계 한 페이지분.
 * 토큰 합계 내림차순으로 정렬돼 온다.
 */
@Getter
@NoArgsConstructor
public class UserAggregateRow {
    private Long userSeq;
    private long callCount;
    private Long inputTokens;
    private Long outputTokens;
    private long untrackedCallCount;
}
