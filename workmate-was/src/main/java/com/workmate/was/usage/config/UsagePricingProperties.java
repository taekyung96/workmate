package com.workmate.was.usage.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;
import java.util.Map;

/**
 * LLM 사용량 추정 비용 환산 프로퍼티 바인딩 (application.yml 의 app.usage.*).
 *
 * <p>단가를 {@code common_code} 테이블(관리자가 화면에서 수정)에 두는 안도 있었으나 기각했다
 * — 단가 오타 하나로 비용 통계 전체가 조용히 틀어질 수 있고, 설정 파일은 git 이력으로
 * "언제 누가 바꿨는지" 추적된다(PLAN-usage-dashboard.md §4). 단가를 모르는 모델은 여기 없다는
 * 뜻이고, 계산 쪽({@code UsagePricingCalculator})이 0원이 아니라 "단가 미등록"으로 분리한다.</p>
 */
@Getter
@ConfigurationProperties(prefix = "app.usage")
public class UsagePricingProperties {

    /** 모델명 → 100만 토큰당 단가(USD). 키는 llm_usage.model_name 과 정확히 일치해야 한다 */
    private final Map<String, ModelPricing> pricing;

    /** USD → KRW 환산에 쓰는 고정 환율 (화면에는 반드시 "추정"으로 표시한다) */
    private final BigDecimal currencyRate;

    /**
     * @param pricing      모델별 단가 맵 (미설정 시 빈 맵 — 모든 모델이 "단가 미등록")
     * @param currencyRate USD→KRW 환율 (미설정 시 1400)
     */
    public UsagePricingProperties(
            @DefaultValue Map<String, ModelPricing> pricing,
            @DefaultValue("1400") BigDecimal currencyRate) {
        this.pricing = pricing == null ? Map.of() : pricing;
        this.currencyRate = currencyRate;
    }

    /** 모델 하나의 입력·출력 단가(100만 토큰당 USD) */
    @Getter
    public static class ModelPricing {
        private final BigDecimal input;
        private final BigDecimal output;

        public ModelPricing(
                @DefaultValue("0") BigDecimal input,
                @DefaultValue("0") BigDecimal output) {
            this.input = input;
            this.output = output;
        }
    }
}
