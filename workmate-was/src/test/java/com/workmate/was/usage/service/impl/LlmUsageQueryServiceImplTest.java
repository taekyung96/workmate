package com.workmate.was.usage.service.impl;

import com.workmate.was.auth.dao.UserRepository;
import com.workmate.was.usage.config.UsagePricingProperties;
import com.workmate.was.usage.dao.LlmUsageQueryMapper;
import com.workmate.was.usage.dao.row.DailyAggregateRow;
import com.workmate.was.usage.dao.row.FeatureAggregateRow;
import com.workmate.was.usage.dao.row.TotalAggregateRow;
import com.workmate.was.usage.dao.row.UserAggregateRow;
import com.workmate.was.usage.util.UsagePricingCalculator;
import com.workmate.was.usage.vo.LlmFeature;
import com.workmate.was.usage.vo.UsageSummaryVo;
import com.workmate.was.usage.vo.UserUsagePageVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LlmUsageQueryServiceImpl 단위 테스트.
 * PLAN-usage-dashboard.md §8 이 요구한 3가지 — 기간 경계(from/to 포함 여부),
 * NULL 토큰이 0으로 합산되지 않는지, 빈 구간 0 채우기 — 를 고정한다.
 */
@ExtendWith(MockitoExtension.class)
class LlmUsageQueryServiceImplTest {

    @Mock
    private LlmUsageQueryMapper llmUsageQueryMapper;
    @Mock
    private UserRepository userRepository;

    private LlmUsageQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        UsagePricingCalculator calculator =
                new UsagePricingCalculator(new UsagePricingProperties(Map.of(), BigDecimal.valueOf(1400)));
        service = new LlmUsageQueryServiceImpl(llmUsageQueryMapper, userRepository, calculator);
    }

    @Test
    @DisplayName("from·to 미지정 시 오늘 포함 최근 30일 범위로 조회한다 (toExclusive = to+1일)")
    void getSummary_defaults_to_last_30_days() {
        stubEmptyMapper();

        service.getSummary(null, null);

        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> toExclusiveCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(llmUsageQueryMapper).selectTotal(fromCaptor.capture(), toExclusiveCaptor.capture(), any());

        LocalDate today = LocalDate.now();
        assertThat(fromCaptor.getValue()).isEqualTo(today.minusDays(29));
        assertThat(toExclusiveCaptor.getValue()).isEqualTo(today.plusDays(1));
    }

    @Test
    @DisplayName("지정한 to 날짜 전체를 포함하도록 toExclusive=to+1일로 매퍼에 전달한다 (기간 경계)")
    void getSummary_passes_inclusive_to_boundary() {
        stubEmptyMapper();
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        service.getSummary(from, to);

        verify(llmUsageQueryMapper).selectTotal(from, to.plusDays(1), null);
    }

    @Test
    @DisplayName("토큰이 전부 NULL 인 구간도 합계는 0, 미집계 건수는 따로 보존한다")
    void getSummary_does_not_collapse_null_tokens_to_zero_silently() {
        when(llmUsageQueryMapper.selectTotal(any(), any(), any())).thenReturn(totalRow(30, null, null, 30));
        when(llmUsageQueryMapper.selectModelUsageTotal(any(), any(), any())).thenReturn(List.of());
        when(llmUsageQueryMapper.selectByFeature(any(), any(), any())).thenReturn(List.of());
        when(llmUsageQueryMapper.selectDaily(any(), any(), any())).thenReturn(List.of());

        UsageSummaryVo result = service.getSummary(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1));

        assertThat(result.getTotal().getInputTokens()).isZero();
        assertThat(result.getTotal().getOutputTokens()).isZero();
        assertThat(result.getTotal().getUntrackedCallCount()).isEqualTo(30);
        assertThat(result.getTotal().getCallCount()).isEqualTo(30);
    }

    @Test
    @DisplayName("일별 집계는 데이터 없는 날도 0건으로 채워 기간 전체 날짜 수만큼 반환한다")
    void getSummary_fills_empty_days_with_zero() {
        when(llmUsageQueryMapper.selectTotal(any(), any(), any())).thenReturn(totalRow(1, 10L, 5L, 0));
        when(llmUsageQueryMapper.selectModelUsageTotal(any(), any(), any())).thenReturn(List.of());
        when(llmUsageQueryMapper.selectByFeature(any(), any(), any())).thenReturn(List.of());
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 3);
        when(llmUsageQueryMapper.selectDaily(any(), any(), any())).thenReturn(List.of(dailyRow(from, 1, 10L, 5L, 0)));

        UsageSummaryVo result = service.getSummary(from, to);

        assertThat(result.getDaily()).hasSize(3);
        assertThat(result.getDaily().get(0).getCallCount()).isEqualTo(1);
        assertThat(result.getDaily().get(1).getCallCount()).isZero();
        assertThat(result.getDaily().get(2).getCallCount()).isZero();
    }

    @Test
    @DisplayName("기능별 집계는 호출이 없는 기능도 0건으로 채워 전 기능을 반환한다")
    void getSummary_fills_all_features() {
        when(llmUsageQueryMapper.selectTotal(any(), any(), any())).thenReturn(totalRow(2, 100L, 50L, 0));
        when(llmUsageQueryMapper.selectModelUsageTotal(any(), any(), any())).thenReturn(List.of());
        when(llmUsageQueryMapper.selectDaily(any(), any(), any())).thenReturn(List.of());
        when(llmUsageQueryMapper.selectByFeature(any(), any(), any()))
                .thenReturn(List.of(featureRow("CHAT", 2, 100L, 50L, 0)));

        UsageSummaryVo result = service.getSummary(LocalDate.now(), LocalDate.now());

        assertThat(result.getByFeature()).hasSize(LlmFeature.values().length);
        assertThat(result.getByFeature().stream()
                .filter(f -> f.getFeature() == LlmFeature.OCR)
                .findFirst().orElseThrow().getCallCount()).isZero();
    }

    @Test
    @DisplayName("사용량은 있는데 사용자를 찾을 수 없으면 (삭제된 사용자)로 표시한다")
    void getByUser_marks_deleted_user() {
        when(llmUsageQueryMapper.countDistinctUsers(any(), any())).thenReturn(1L);
        when(llmUsageQueryMapper.selectUserPage(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(userRow(99L, 3, 10L, 5L, 0)));
        when(userRepository.findAllById(any())).thenReturn(List.of());
        when(llmUsageQueryMapper.selectModelUsageByUsers(any(), any(), any())).thenReturn(List.of());

        UserUsagePageVo result = service.getByUser(LocalDate.now(), LocalDate.now(), 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUserName()).isEqualTo("(삭제된 사용자)");
        assertThat(result.getContent().get(0).getMaskedEmail()).isEqualTo("-");
    }

    @Test
    @DisplayName("사용량이 0건인 기간은 빈 목록을 반환하고 화면 계산이 깨지지 않는다 (빈 상태)")
    void getByUser_returns_empty_when_no_data() {
        when(llmUsageQueryMapper.countDistinctUsers(any(), any())).thenReturn(0L);

        UserUsagePageVo result = service.getByUser(LocalDate.now(), LocalDate.now(), 0, 20);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
        verify(llmUsageQueryMapper, never()).selectUserPage(any(), any(), anyInt(), anyInt());
    }

    private void stubEmptyMapper() {
        when(llmUsageQueryMapper.selectTotal(any(), any(), any())).thenReturn(totalRow(0, null, null, 0));
        when(llmUsageQueryMapper.selectModelUsageTotal(any(), any(), any())).thenReturn(List.of());
        when(llmUsageQueryMapper.selectByFeature(any(), any(), any())).thenReturn(List.of());
        when(llmUsageQueryMapper.selectDaily(any(), any(), any())).thenReturn(List.of());
    }

    private TotalAggregateRow totalRow(long callCount, Long inputTokens, Long outputTokens, long untracked) {
        TotalAggregateRow row = new TotalAggregateRow();
        ReflectionTestUtils.setField(row, "callCount", callCount);
        ReflectionTestUtils.setField(row, "inputTokens", inputTokens);
        ReflectionTestUtils.setField(row, "outputTokens", outputTokens);
        ReflectionTestUtils.setField(row, "untrackedCallCount", untracked);
        return row;
    }

    private DailyAggregateRow dailyRow(LocalDate date, long callCount, Long inputTokens, Long outputTokens, long untracked) {
        DailyAggregateRow row = new DailyAggregateRow();
        ReflectionTestUtils.setField(row, "usageDate", date);
        ReflectionTestUtils.setField(row, "callCount", callCount);
        ReflectionTestUtils.setField(row, "inputTokens", inputTokens);
        ReflectionTestUtils.setField(row, "outputTokens", outputTokens);
        ReflectionTestUtils.setField(row, "untrackedCallCount", untracked);
        return row;
    }

    private FeatureAggregateRow featureRow(String feature, long callCount, Long inputTokens, Long outputTokens, long untracked) {
        FeatureAggregateRow row = new FeatureAggregateRow();
        ReflectionTestUtils.setField(row, "feature", feature);
        ReflectionTestUtils.setField(row, "callCount", callCount);
        ReflectionTestUtils.setField(row, "inputTokens", inputTokens);
        ReflectionTestUtils.setField(row, "outputTokens", outputTokens);
        ReflectionTestUtils.setField(row, "untrackedCallCount", untracked);
        return row;
    }

    private UserAggregateRow userRow(Long userSeq, long callCount, Long inputTokens, Long outputTokens, long untracked) {
        UserAggregateRow row = new UserAggregateRow();
        ReflectionTestUtils.setField(row, "userSeq", userSeq);
        ReflectionTestUtils.setField(row, "callCount", callCount);
        ReflectionTestUtils.setField(row, "inputTokens", inputTokens);
        ReflectionTestUtils.setField(row, "outputTokens", outputTokens);
        ReflectionTestUtils.setField(row, "untrackedCallCount", untracked);
        return row;
    }

    // ── 본인 사용량 조회 (보안 경계) ──
    // userSeq 가 매퍼까지 전달되지 않으면 전체 집계가 반환되어 남의 사용량이 그대로 노출된다.
    // "필터가 실제로 걸리는가"를 고정하는 것이 이 테스트의 목적이다.

    @Test
    @DisplayName("본인 조회는 userSeq 를 모든 집계 쿼리에 전달한다 (하나라도 빠지면 남의 사용량이 섞인다)")
    void getMySummary_scopes_every_query_to_the_user() {
        stubEmptyMapper();
        Long userSeq = 42L;

        service.getMySummary(userSeq, null, null);

        verify(llmUsageQueryMapper).selectTotal(any(), any(), eq(userSeq));
        verify(llmUsageQueryMapper).selectModelUsageTotal(any(), any(), eq(userSeq));
        verify(llmUsageQueryMapper).selectByFeature(any(), any(), eq(userSeq));
        verify(llmUsageQueryMapper).selectDaily(any(), any(), eq(userSeq));
    }

    @Test
    @DisplayName("관리자 전체 조회는 userSeq 를 null 로 넘겨 필터를 걸지 않는다")
    void getSummary_passes_null_user_for_all() {
        stubEmptyMapper();

        service.getSummary(null, null);

        verify(llmUsageQueryMapper).selectTotal(any(), any(), eq(null));
        verify(llmUsageQueryMapper).selectDaily(any(), any(), eq(null));
    }

    @Test
    @DisplayName("본인 조회에 userSeq 가 없으면 전체 집계로 새지 않고 예외로 막는다")
    void getMySummary_rejects_null_user() {
        assertThatThrownBy(() -> service.getMySummary(null, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        // 쿼리 자체가 나가지 않아야 한다
        verify(llmUsageQueryMapper, never()).selectTotal(any(), any(), any());
    }
}
