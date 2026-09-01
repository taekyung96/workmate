package com.workmate.was.usage.util;

import java.math.BigDecimal;

/**
 * {@code UsagePricingCalculator} 계산 결과 — 추정 비용(USD·KRW)과 단가 미등록 건수.
 *
 * @param costUsd            추정 비용(USD). 단가 미등록 모델의 호출은 제외된 값
 * @param costKrw            추정 비용(KRW, 고정 환율 환산)
 * @param unpricedCallCount  단가가 등록되지 않은 모델이라 비용 계산에서 제외된 호출 건수
 */
public record CostEstimate(BigDecimal costUsd, BigDecimal costKrw, long unpricedCallCount) {
}
