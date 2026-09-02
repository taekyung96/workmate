package com.workmate.was.usage.tool;

import com.workmate.was.usage.service.LlmUsageQueryService;
import com.workmate.was.usage.vo.FeatureUsageVo;
import com.workmate.was.usage.vo.LlmFeature;
import com.workmate.was.usage.vo.UsagePeriodVo;
import com.workmate.was.usage.vo.UsageSummaryVo;
import com.workmate.was.usage.vo.UsageTotalVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UsageTools 단위 테스트.
 *
 * <p>여기서 지키려는 것은 하나다 — <b>LLM 이 도구를 통해 권한을 우회하지 못한다.</b>
 * 시스템 프롬프트에 "관리자만 불러라"고 적는 것은 지시일 뿐 방어가 아니므로 도구 안에서 막는다.</p>
 */
@ExtendWith(MockitoExtension.class)
class UsageToolsTest {

    @Mock
    private LlmUsageQueryService llmUsageQueryService;

    @InjectMocks
    private UsageTools usageTools;

    /** userSeq·role 을 담은 ToolContext — Spring AI 가 런타임에 넣어주는 것과 같은 모양 */
    private ToolContext context(Long userSeq, String role) {
        return new ToolContext(Map.of("userSeq", userSeq, "role", role));
    }

    /** 합계만 채운 최소 요약 — 도구는 이 값을 문자열로 요약하기만 한다 */
    private UsageSummaryVo summary(long callCount, long inputTokens, long krw) {
        return UsageSummaryVo.builder()
                .period(new UsagePeriodVo(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 9, 2)))
                .total(UsageTotalVo.builder()
                        .callCount(callCount)
                        .inputTokens(inputTokens)
                        .outputTokens(0)
                        .untrackedCallCount(0)
                        .unpricedCallCount(0)
                        .estimatedCostUsd(BigDecimal.ZERO)
                        .estimatedCostKrw(BigDecimal.valueOf(krw))
                        .build())
                .byFeature(List.of(FeatureUsageVo.builder()
                        .feature(LlmFeature.CHAT)
                        .callCount(callCount)
                        .inputTokens(inputTokens)
                        .outputTokens(0)
                        .untrackedCallCount(0)
                        .build()))
                .daily(List.of())
                .build();
    }

    @Test
    @DisplayName("본인 사용량은 일반 사용자도 조회할 수 있다")
    void my_usage_allowed_for_normal_user() {
        when(llmUsageQueryService.getMySummary(eq(12L), any(), any())).thenReturn(summary(44, 1770, 1067));

        String result = usageTools.getMyUsage(null, null, context(12L, "ROLE_USER"));

        assertThat(result).contains("44").contains("1,770");
        verify(llmUsageQueryService).getMySummary(eq(12L), any(), any());
    }

    @Test
    @DisplayName("전체 사용량은 관리자만 조회할 수 있다")
    void all_usage_allowed_for_admin() {
        when(llmUsageQueryService.getSummary(any(), any())).thenReturn(summary(120, 9000, 3200));

        String result = usageTools.getAllUsage(null, null, context(12L, "ROLE_ADMIN"));

        assertThat(result).contains("120");
    }

    @Test
    @DisplayName("일반 사용자가 전체 사용량을 부르면 거부한다 — 조회 자체를 하지 않는다")
    void all_usage_denied_for_normal_user() {
        String result = usageTools.getAllUsage(null, null, context(12L, "ROLE_USER"));

        assertThat(result).contains("관리자");
        // 거부는 문자열로만 하고 서비스는 부르지 않는다 — 부르면 그 순간 정보가 새어나간다
        verify(llmUsageQueryService, never()).getSummary(any(), any());
    }

    @Test
    @DisplayName("role 이 비어 있으면 전체 사용량을 거부한다")
    void all_usage_denied_when_role_missing() {
        String result = usageTools.getAllUsage(null, null, context(12L, ""));

        assertThat(result).contains("관리자");
        verify(llmUsageQueryService, never()).getSummary(any(), any());
    }

    @Test
    @DisplayName("날짜 문자열을 파싱해 그대로 넘긴다")
    void parses_date_range() {
        when(llmUsageQueryService.getMySummary(any(), any(), any())).thenReturn(summary(1, 10, 1));

        usageTools.getMyUsage("2026-08-01", "2026-08-31", context(12L, "ROLE_USER"));

        verify(llmUsageQueryService).getMySummary(
                eq(12L), eq(LocalDate.of(2026, 8, 1)), eq(LocalDate.of(2026, 8, 31)));
    }

    @Test
    @DisplayName("날짜 형식이 틀리면 null 로 넘겨 서버 기본 기간을 쓰게 한다")
    void invalid_date_falls_back_to_null() {
        when(llmUsageQueryService.getMySummary(any(), any(), any())).thenReturn(summary(1, 10, 1));

        usageTools.getMyUsage("어제", "오늘", context(12L, "ROLE_USER"));

        verify(llmUsageQueryService).getMySummary(eq(12L), eq(null), eq(null));
    }

    @Test
    @DisplayName("집계에서 빠진 건수를 반드시 드러낸다 — 숨기면 합계가 거짓말을 한다")
    void reveals_excluded_call_counts() {
        UsageSummaryVo s = UsageSummaryVo.builder()
                .period(new UsagePeriodVo(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 9, 2)))
                .total(UsageTotalVo.builder()
                        .callCount(44)
                        .inputTokens(1770)
                        .outputTokens(92)
                        .untrackedCallCount(2)
                        .unpricedCallCount(42)
                        .estimatedCostUsd(BigDecimal.ZERO)
                        .estimatedCostKrw(BigDecimal.ONE)
                        .build())
                .byFeature(List.of())
                .daily(List.of())
                .build();
        when(llmUsageQueryService.getMySummary(any(), any(), any())).thenReturn(s);

        String result = usageTools.getMyUsage(null, null, context(12L, "ROLE_USER"));

        assertThat(result).contains("42").contains("2");
    }
}
