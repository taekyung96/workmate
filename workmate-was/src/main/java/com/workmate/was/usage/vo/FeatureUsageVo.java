package com.workmate.was.usage.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기능별 사용량 (관리자 사용량 대시보드).
 * 호출이 0건인 기능도 화면에 함께 보여줘야 하므로(빈 상태 처리), {@link LlmFeature} 5종을
 * 서비스 계층에서 항상 채워 반환한다 — DB GROUP BY 결과에는 호출이 있는 기능만 나온다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureUsageVo {
    private LlmFeature feature;
    private long callCount;
    private long inputTokens;
    private long outputTokens;
    /** 토큰 미집계 건수 (해당 기능 내) */
    private long untrackedCallCount;
}
