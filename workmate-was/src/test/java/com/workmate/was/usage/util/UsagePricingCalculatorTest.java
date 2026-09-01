package com.workmate.was.usage.util;

import com.workmate.was.usage.config.UsagePricingProperties;
import com.workmate.was.usage.dao.row.ModelAggregateRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UsagePricingCalculator 단위 테스트.
 * PLAN-usage-dashboard.md §8 이 요구한 "단가 미등록 모델이 0원이 아니라 별도 집계로 빠지는지"를 고정한다.
 */
class UsagePricingCalculatorTest {

    private UsagePricingCalculator calculator;

    @BeforeEach
    void setUp() {
        Map<String, UsagePricingProperties.ModelPricing> pricing = Map.of(
                "gemini-flash-latest",
                new UsagePricingProperties.ModelPricing(BigDecimal.valueOf(0.30), BigDecimal.valueOf(2.50)));
        UsagePricingProperties props = new UsagePricingProperties(pricing, BigDecimal.valueOf(1400));
        calculator = new UsagePricingCalculator(props);
    }

    @Test
    @DisplayName("단가가 등록된 모델은 100만 토큰당 단가 기준으로 USD·KRW 비용을 계산한다")
    void estimate_computes_cost_for_priced_model() {
        List<ModelAggregateRow> rows = List.of(row("gemini-flash-latest", 5, 1_000_000L, 1_000_000L));

        CostEstimate result = calculator.estimate(rows);

        assertThat(result.costUsd()).isEqualByComparingTo("2.80"); // 1M*0.30/1M + 1M*2.50/1M
        assertThat(result.costKrw()).isEqualByComparingTo("3920"); // 2.80 * 1400
        assertThat(result.unpricedCallCount()).isZero();
    }

    @Test
    @DisplayName("단가가 없는 모델은 0원으로 뭉개지 않고 unpricedCallCount 로 별도 집계된다")
    void estimate_separates_unpriced_model_instead_of_zeroing() {
        List<ModelAggregateRow> rows = List.of(row("unknown-model", 3, 1000L, 1000L));

        CostEstimate result = calculator.estimate(rows);

        assertThat(result.costUsd()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.unpricedCallCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("가격 등록 모델과 미등록 모델이 섞여도 각각 맞게 분리 집계한다")
    void estimate_mixes_priced_and_unpriced_models() {
        List<ModelAggregateRow> rows = List.of(
                row("gemini-flash-latest", 1, 1_000_000L, 0L),
                row("unknown-model", 2, 500L, 500L));

        CostEstimate result = calculator.estimate(rows);

        assertThat(result.costUsd()).isEqualByComparingTo("0.30"); // flash 입력분만
        assertThat(result.unpricedCallCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("토큰이 NULL 인 행은 0 토큰으로 취급한다(단가 등록 모델이면 0원)")
    void estimate_treats_null_tokens_as_zero() {
        List<ModelAggregateRow> rows = List.of(row("gemini-flash-latest", 1, null, null));

        CostEstimate result = calculator.estimate(rows);

        assertThat(result.costUsd()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.unpricedCallCount()).isZero();
    }

    private ModelAggregateRow row(String modelName, long callCount, Long inputTokens, Long outputTokens) {
        ModelAggregateRow row = new ModelAggregateRow();
        ReflectionTestUtils.setField(row, "modelName", modelName);
        ReflectionTestUtils.setField(row, "callCount", callCount);
        ReflectionTestUtils.setField(row, "inputTokens", inputTokens);
        ReflectionTestUtils.setField(row, "outputTokens", outputTokens);
        return row;
    }
}
