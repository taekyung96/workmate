package com.workmate.was.usage.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 관리자 사용량 대시보드 — 요약 응답 (기간·합계·기능별·일별을 한 번에 담는다).
 * 화면 한 번 그리는 데 필요한 집계를 한 번의 왕복으로 준다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageSummaryVo {
    private UsagePeriodVo period;
    private UsageTotalVo total;
    private List<FeatureUsageVo> byFeature;
    private List<DailyUsageVo> daily;
}
