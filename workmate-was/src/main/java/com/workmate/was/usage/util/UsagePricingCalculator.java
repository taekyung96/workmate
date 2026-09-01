package com.workmate.was.usage.util;

import com.workmate.was.usage.config.UsagePricingProperties;
import com.workmate.was.usage.dao.row.ModelTokenUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 모델별 토큰 사용량을 설정된 단가(application.yml 의 app.usage.pricing)로 추정 비용 환산한다.
 *
 * <p>기간 전체 합계({@code ModelAggregateRow})와 사용자별 합계({@code UserModelAggregateRow})가
 * 같은 계산을 공유하도록 {@link ModelTokenUsage} 로 추상화했다(중복 배제 — CLAUDE.md 코딩 규칙).
 * 단가가 등록되지 않은 모델은 비용을 0으로 두지 않고 {@link CostEstimate#unpricedCallCount()} 로
 * 따로 센다(계획서 §4 — "단가 미등록"으로 분리 집계).</p>
 */
@Component
@RequiredArgsConstructor
public class UsagePricingCalculator {

    private static final BigDecimal PER_MILLION = BigDecimal.valueOf(1_000_000);

    private final UsagePricingProperties usagePricingProperties;

    /**
     * @param rows 모델별(또는 사용자·모델별) 토큰 사용량 집계 행
     * @return 추정 비용(USD·KRW)과 단가 미등록 건수
     */
    public CostEstimate estimate(List<? extends ModelTokenUsage> rows) {
        BigDecimal costUsd = BigDecimal.ZERO;
        long unpricedCallCount = 0;

        for (ModelTokenUsage row : rows) {
            UsagePricingProperties.ModelPricing pricing = row.getModelName() == null
                    ? null
                    : usagePricingProperties.getPricing().get(row.getModelName());
            if (pricing == null) {
                // 단가 미등록 — 0원으로 뭉개지 않고 건수만 따로 누적한다
                unpricedCallCount += row.getCallCount();
                continue;
            }
            long inputTokens = row.getInputTokens() == null ? 0 : row.getInputTokens();
            long outputTokens = row.getOutputTokens() == null ? 0 : row.getOutputTokens();
            costUsd = costUsd.add(costOf(pricing, inputTokens, outputTokens));
        }

        BigDecimal costKrw = costUsd.multiply(usagePricingProperties.getCurrencyRate())
                .setScale(0, RoundingMode.HALF_UP);
        return new CostEstimate(costUsd.setScale(6, RoundingMode.HALF_UP), costKrw, unpricedCallCount);
    }

    /** 100만 토큰당 단가 기준 비용 = 입력분 + 출력분 */
    private BigDecimal costOf(UsagePricingProperties.ModelPricing pricing, long inputTokens, long outputTokens) {
        BigDecimal inputCost = BigDecimal.valueOf(inputTokens)
                .multiply(pricing.getInput())
                .divide(PER_MILLION, 10, RoundingMode.HALF_UP);
        BigDecimal outputCost = BigDecimal.valueOf(outputTokens)
                .multiply(pricing.getOutput())
                .divide(PER_MILLION, 10, RoundingMode.HALF_UP);
        return inputCost.add(outputCost);
    }
}
