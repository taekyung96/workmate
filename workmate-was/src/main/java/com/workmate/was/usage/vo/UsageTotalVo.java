package com.workmate.was.usage.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 관리자 사용량 대시보드 — 기간 합계 (관리자 사용량 대시보드).
 *
 * <p>토큰이 NULL 인 호출(제공자가 usage 를 안 주는 경로, 주로 EMBEDDING)은 합계에서 0 으로
 * 뭉개지 않고 {@link #untrackedCallCount} 로 따로 센다. 마찬가지로 단가가 등록되지 않은
 * 모델의 호출은 비용을 0 으로 두지 않고 {@link #unpricedCallCount} 로 분리한다.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageTotalVo {
    /** 기간 내 전체 호출 건수 */
    private long callCount;
    /** 입력 토큰 합계 (토큰 미집계 건은 자연히 제외된 합) */
    private long inputTokens;
    /** 출력 토큰 합계 (토큰 미집계 건은 자연히 제외된 합) */
    private long outputTokens;
    /** 토큰 미집계 건수 — 입력·출력 토큰 중 하나라도 NULL 인 호출 (주로 EMBEDDING) */
    private long untrackedCallCount;
    /** 단가 미등록 모델이라 비용 계산에서 제외된 건수 */
    private long unpricedCallCount;
    /** 추정 비용(USD) — 설정된 단가 기준 추정치이며 실제 청구액이 아니다 */
    private BigDecimal estimatedCostUsd;
    /** 추정 비용(KRW) — 고정 환율로 환산한 추정치이며 실제 청구액이 아니다 */
    private BigDecimal estimatedCostKrw;
}
