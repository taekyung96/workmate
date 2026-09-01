package com.workmate.was.usage.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 일별 사용량 (관리자 사용량 대시보드).
 * 조회 기간 내 데이터가 없는 날짜도 서버가 0 으로 채워 반환한다 — 날짜 채우기의 단일 출처는
 * 서버이며, 프론트에서 다시 채우지 않는다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyUsageVo {
    private LocalDate date;
    private long callCount;
    private long inputTokens;
    private long outputTokens;
    /** 토큰 미집계 건수 (해당 날짜 내) */
    private long untrackedCallCount;
}
