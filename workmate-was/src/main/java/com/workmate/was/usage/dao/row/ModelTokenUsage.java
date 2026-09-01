package com.workmate.was.usage.dao.row;

/**
 * 모델별 토큰 사용량 최소 형태 — 비용 환산 계산({@code UsagePricingCalculator})이
 * 전체 합계용 행({@link ModelAggregateRow})과 사용자별 행({@link UserModelAggregateRow})을
 * 같은 로직으로 처리할 수 있도록 공통 인터페이스로 뽑았다(중복 계산 로직 방지).
 */
public interface ModelTokenUsage {
    /** 실제 호출된 모델명 (단가 조회 키) */
    String getModelName();
    /** 이 모델로 호출된 건수 — 단가 미등록 시 미등록 건수 집계에 쓴다 */
    long getCallCount();
    /** 입력 토큰 합 — 전부 NULL 이면 null */
    Long getInputTokens();
    /** 출력 토큰 합 — 전부 NULL 이면 null */
    Long getOutputTokens();
}
