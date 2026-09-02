package com.workmate.was.usage.service.impl;

import com.workmate.was.admin.util.PiiMasker;
import com.workmate.was.auth.dao.UserRepository;
import com.workmate.was.auth.vo.User;
import com.workmate.was.usage.dao.LlmUsageQueryMapper;
import com.workmate.was.usage.dao.row.DailyAggregateRow;
import com.workmate.was.usage.dao.row.FeatureAggregateRow;
import com.workmate.was.usage.dao.row.ModelAggregateRow;
import com.workmate.was.usage.dao.row.TotalAggregateRow;
import com.workmate.was.usage.dao.row.UserAggregateRow;
import com.workmate.was.usage.dao.row.UserModelAggregateRow;
import com.workmate.was.usage.service.LlmUsageQueryService;
import com.workmate.was.usage.util.CostEstimate;
import com.workmate.was.usage.util.UsagePricingCalculator;
import com.workmate.was.usage.vo.DailyUsageVo;
import com.workmate.was.usage.vo.FeatureUsageVo;
import com.workmate.was.usage.vo.LlmFeature;
import com.workmate.was.usage.vo.UsagePeriodVo;
import com.workmate.was.usage.vo.UsageSummaryVo;
import com.workmate.was.usage.vo.UsageTotalVo;
import com.workmate.was.usage.vo.UserUsagePageVo;
import com.workmate.was.usage.vo.UserUsageVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LlmUsageQueryService 구현체 (관리자 사용량 대시보드).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmUsageQueryServiceImpl implements LlmUsageQueryService {

    /** from·to 미지정 시 적용하는 기본 조회 범위(오늘 포함 최근 30일) */
    private static final int DEFAULT_RANGE_DAYS = 30;

    /** 사용량은 남아 있는데 사용자를 더 이상 찾을 수 없을 때(탈퇴 등)의 대체 표기 */
    private static final String DELETED_USER = "(삭제된 사용자)";

    private final LlmUsageQueryMapper llmUsageQueryMapper;
    private final UserRepository userRepository;
    private final UsagePricingCalculator usagePricingCalculator;

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public UsageSummaryVo getSummary(LocalDate from, LocalDate to) {
        return summarize(null, from, to);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public UsageSummaryVo getMySummary(Long userSeq, LocalDate from, LocalDate to) {
        // null 이면 전체 집계로 흘러가 남의 사용량이 노출된다 — 방어적으로 막는다
        if (userSeq == null) {
            throw new IllegalArgumentException("본인 사용량 조회에는 userSeq 가 필요하다");
        }
        return summarize(userSeq, from, to);
    }

    /**
     * 기간 요약 집계 공통 로직.
     *
     * @param userSeq null 이면 전체(관리자), 값이 있으면 그 사용자만
     */
    private UsageSummaryVo summarize(Long userSeq, LocalDate from, LocalDate to) {
        LocalDate resolvedTo = resolveTo(to);
        LocalDate resolvedFrom = resolveFrom(from, resolvedTo);
        LocalDate toExclusive = resolvedTo.plusDays(1);

        TotalAggregateRow totalRow = llmUsageQueryMapper.selectTotal(resolvedFrom, toExclusive, userSeq);
        List<ModelAggregateRow> modelRows = llmUsageQueryMapper.selectModelUsageTotal(resolvedFrom, toExclusive, userSeq);
        CostEstimate cost = usagePricingCalculator.estimate(modelRows);

        UsageTotalVo total = UsageTotalVo.builder()
                .callCount(totalRow.getCallCount())
                .inputTokens(nz(totalRow.getInputTokens()))
                .outputTokens(nz(totalRow.getOutputTokens()))
                .untrackedCallCount(totalRow.getUntrackedCallCount())
                .unpricedCallCount(cost.unpricedCallCount())
                .estimatedCostUsd(cost.costUsd())
                .estimatedCostKrw(cost.costKrw())
                .build();

        List<FeatureAggregateRow> featureRows = llmUsageQueryMapper.selectByFeature(resolvedFrom, toExclusive, userSeq);
        List<DailyAggregateRow> dailyRows = llmUsageQueryMapper.selectDaily(resolvedFrom, toExclusive, userSeq);

        log.info("사용량 요약 집계 - userSeq: {}, from: {}, to: {}, callCount: {}",
                userSeq == null ? "전체" : userSeq, resolvedFrom, resolvedTo, totalRow.getCallCount());

        return UsageSummaryVo.builder()
                .period(new UsagePeriodVo(resolvedFrom, resolvedTo))
                .total(total)
                .byFeature(fillFeatures(featureRows))
                .daily(fillDailyRange(resolvedFrom, resolvedTo, dailyRows))
                .build();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public UserUsagePageVo getByUser(LocalDate from, LocalDate to, int page, int size) {
        LocalDate resolvedTo = resolveTo(to);
        LocalDate resolvedFrom = resolveFrom(from, resolvedTo);
        LocalDate toExclusive = resolvedTo.plusDays(1);

        long totalElements = llmUsageQueryMapper.countDistinctUsers(resolvedFrom, toExclusive);
        int totalPages = (int) Math.ceil(totalElements / (double) size);

        List<UserAggregateRow> rows = totalElements == 0
                ? List.of()
                : llmUsageQueryMapper.selectUserPage(resolvedFrom, toExclusive, page * size, size);

        List<Long> userSeqs = rows.stream().map(UserAggregateRow::getUserSeq).toList();

        // 이메일은 AES 암호화 컬럼이라 SQL LEFT JOIN 으로는 마스킹할 평문을 얻을 수 없다(복호화는
        // JPA 컨버터가 담당). getAuditLogs 와 같은 패턴으로 이 페이지의 userSeq만 모아 한 번에
        // 조회한다(N+1 방지) — 결과에 없는 userSeq는 탈퇴 등으로 삭제된 사용자로 본다.
        Map<Long, User> userMap = userSeqs.isEmpty()
                ? Map.of()
                : userRepository.findAllById(userSeqs).stream()
                        .collect(Collectors.toMap(User::getUserSeq, u -> u));

        Map<Long, List<UserModelAggregateRow>> modelRowsByUser = userSeqs.isEmpty()
                ? Map.of()
                : llmUsageQueryMapper.selectModelUsageByUsers(userSeqs, resolvedFrom, toExclusive).stream()
                        .collect(Collectors.groupingBy(UserModelAggregateRow::getUserSeq));

        List<UserUsageVo> content = rows.stream()
                .map(row -> toUserUsageVo(row, userMap.get(row.getUserSeq()),
                        modelRowsByUser.getOrDefault(row.getUserSeq(), List.of())))
                .toList();

        return UserUsagePageVo.builder()
                .content(content)
                .page(page)
                .totalPages(totalPages)
                .totalElements(totalElements)
                .build();
    }

    private UserUsageVo toUserUsageVo(UserAggregateRow row, User user, List<UserModelAggregateRow> modelRows) {
        CostEstimate cost = usagePricingCalculator.estimate(modelRows);
        return UserUsageVo.builder()
                .userSeq(row.getUserSeq())
                .userName(user != null ? user.getUserName() : DELETED_USER)
                .maskedEmail(user != null ? PiiMasker.maskEmail(user.getEmail()) : "-")
                .callCount(row.getCallCount())
                .inputTokens(nz(row.getInputTokens()))
                .outputTokens(nz(row.getOutputTokens()))
                .untrackedCallCount(row.getUntrackedCallCount())
                .estimatedCostUsd(cost.costUsd())
                .estimatedCostKrw(cost.costKrw())
                .build();
    }

    /** to 미지정 시 오늘 */
    private LocalDate resolveTo(LocalDate to) {
        return to != null ? to : LocalDate.now();
    }

    /** from 미지정 시 resolvedTo 기준 최근 30일(오늘 포함)의 시작일 */
    private LocalDate resolveFrom(LocalDate from, LocalDate resolvedTo) {
        return from != null ? from : resolvedTo.minusDays(DEFAULT_RANGE_DAYS - 1L);
    }

    /** LlmFeature 전 기능을 항상 채운다 — DB 에는 호출이 있는 기능만 나오므로 0건 기능은 빈 상태를 위해 서비스가 채운다 */
    private List<FeatureUsageVo> fillFeatures(List<FeatureAggregateRow> rows) {
        Map<String, FeatureAggregateRow> byFeature = rows.stream()
                .collect(Collectors.toMap(FeatureAggregateRow::getFeature, r -> r));
        List<FeatureUsageVo> result = new ArrayList<>();
        for (LlmFeature feature : LlmFeature.values()) {
            FeatureAggregateRow row = byFeature.get(feature.name());
            result.add(FeatureUsageVo.builder()
                    .feature(feature)
                    .callCount(row != null ? row.getCallCount() : 0)
                    .inputTokens(row != null ? nz(row.getInputTokens()) : 0)
                    .outputTokens(row != null ? nz(row.getOutputTokens()) : 0)
                    .untrackedCallCount(row != null ? row.getUntrackedCallCount() : 0)
                    .build());
        }
        return result;
    }

    /** from~to 모든 날짜를 채운다 — 데이터 없는 날은 0. 날짜 채우기의 단일 출처는 서버(프론트 재계산 금지) */
    private List<DailyUsageVo> fillDailyRange(LocalDate from, LocalDate to, List<DailyAggregateRow> rows) {
        Map<LocalDate, DailyAggregateRow> byDate = rows.stream()
                .collect(Collectors.toMap(DailyAggregateRow::getUsageDate, r -> r));
        List<DailyUsageVo> result = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            DailyAggregateRow row = byDate.get(date);
            result.add(DailyUsageVo.builder()
                    .date(date)
                    .callCount(row != null ? row.getCallCount() : 0)
                    .inputTokens(row != null ? nz(row.getInputTokens()) : 0)
                    .outputTokens(row != null ? nz(row.getOutputTokens()) : 0)
                    .untrackedCallCount(row != null ? row.getUntrackedCallCount() : 0)
                    .build());
        }
        return result;
    }

    /** SUM 결과의 NULL(전부 미집계)을 화면 표시용 0으로 — 미집계 사실 자체는 untrackedCallCount 가 별도로 담는다 */
    private long nz(Long value) {
        return value == null ? 0 : value;
    }
}
