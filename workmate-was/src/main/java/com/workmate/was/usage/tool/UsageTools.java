package com.workmate.was.usage.tool;

import com.workmate.was.usage.service.LlmUsageQueryService;
import com.workmate.was.usage.vo.UsageSummaryVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

/**
 * 도우미 AI 가 호출하는 사용량 조회 도구 (F-OBS).
 *
 * <p><b>권한은 도구 안에서 검사한다.</b> 시스템 프롬프트에 "관리자가 아니면 전체 사용량을 부르지 마라"고
 * 적는 것은 지시일 뿐 방어가 아니다. LLM 이 잘못 불러도 여기서 막혀야 한다.</p>
 *
 * <p>거부는 <b>예외가 아니라 문자열 반환</b>이다. 예외를 던지면 스트림 전체가 죽지만,
 * 문자열이면 LLM 이 "권한이 없습니다"로 자연스럽게 답한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsageTools {

    private final LlmUsageQueryService llmUsageQueryService;

    @Tool(description = "로그인한 본인의 AI 사용량(호출 수·토큰·추정 비용)을 조회한다. "
            + "'내가 얼마나 썼어?', '이번 달 내 비용', '내가 제일 많이 쓴 기능' 등 본인 사용량을 물을 때 사용한다.")
    public String getMyUsage(
            @ToolParam(description = "조회 시작일 (yyyy-MM-dd). 지정하지 않으면 최근 30일", required = false) String from,
            @ToolParam(description = "조회 종료일 (yyyy-MM-dd). 지정하지 않으면 오늘", required = false) String to,
            ToolContext toolContext) {

        Long userSeq = (Long) toolContext.getContext().get("userSeq");
        UsageSummaryVo summary = llmUsageQueryService.getMySummary(userSeq, parseDate(from), parseDate(to));

        log.info("[Tool] getMyUsage - userSeq: {}, from: {}, to: {}", userSeq, from, to);
        return "본인 사용량 " + describe(summary);
    }

    @Tool(description = "전체 사용자의 AI 사용량 합계를 조회한다. 관리자만 사용할 수 있다. "
            + "'전체 사용량', '이번 달 총 비용', '어떤 기능이 제일 많이 쓰이나' 등을 물을 때 사용한다.")
    public String getAllUsage(
            @ToolParam(description = "조회 시작일 (yyyy-MM-dd). 지정하지 않으면 최근 30일", required = false) String from,
            @ToolParam(description = "조회 종료일 (yyyy-MM-dd). 지정하지 않으면 오늘", required = false) String to,
            ToolContext toolContext) {

        String role = (String) toolContext.getContext().get("role");
        if (!"ROLE_ADMIN".equals(role)) {
            log.info("[Tool] getAllUsage 거부 - role: {}", role);
            return "전체 사용량은 관리자만 조회할 수 있습니다. 본인 사용량은 확인할 수 있습니다.";
        }

        UsageSummaryVo summary = llmUsageQueryService.getSummary(parseDate(from), parseDate(to));
        log.info("[Tool] getAllUsage - from: {}, to: {}", from, to);
        return "전체 사용량 " + describe(summary);
    }

    /**
     * 집계를 LLM 프롬프트에 넣을 한 줄 요약으로 만든다.
     * 표 전체를 넣지 않는 이유는 반환값이 곧 입력 토큰이기 때문이다.
     *
     * @param s 서버가 준 기간 집계
     * @return 사람이 읽는 한 줄 요약
     */
    private String describe(UsageSummaryVo s) {
        String features = s.getByFeature().stream()
                .filter(f -> f.getCallCount() > 0)
                .map(f -> f.getFeature().name() + " " + f.getCallCount() + "건")
                .collect(Collectors.joining(", "));

        StringBuilder sb = new StringBuilder()
                .append("(").append(s.getPeriod().getFrom()).append(" ~ ").append(s.getPeriod().getTo()).append(") ")
                .append("총 ").append(String.format("%,d", s.getTotal().getCallCount())).append("건, ")
                .append("입력 ").append(String.format("%,d", s.getTotal().getInputTokens())).append("토큰, ")
                .append("출력 ").append(String.format("%,d", s.getTotal().getOutputTokens())).append("토큰, ")
                .append("추정 비용 ").append(formatKrw(s.getTotal().getEstimatedCostKrw())).append("원");

        if (!features.isEmpty()) {
            sb.append(". 기능별: ").append(features);
        }
        // 집계에서 빠진 건수를 반드시 드러낸다 — 화면과 같은 원칙이다. 숨기면 합계가 거짓말을 한다
        if (s.getTotal().getUnpricedCallCount() > 0) {
            sb.append(". 단가 미등록으로 비용 계산에서 빠진 호출이 ")
                    .append(s.getTotal().getUnpricedCallCount()).append("건 있다");
        }
        if (s.getTotal().getUntrackedCallCount() > 0) {
            sb.append(". 토큰이 집계되지 않은 호출이 ")
                    .append(s.getTotal().getUntrackedCallCount()).append("건 있다");
        }
        return sb.toString();
    }

    /** 원화 추정치를 천 단위 구분 기호가 붙은 정수 문자열로. null 은 0 으로 본다 */
    private String formatKrw(BigDecimal krw) {
        BigDecimal value = krw == null ? BigDecimal.ZERO : krw;
        return String.format("%,d", value.setScale(0, java.math.RoundingMode.HALF_UP).longValue());
    }

    /** 'yyyy-MM-dd' → LocalDate. 비었거나 형식이 틀리면 null 을 반환해 서버 기본 기간을 쓰게 한다 */
    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            log.warn("[Tool] 날짜 파싱 실패, 기본 기간을 쓴다 - value: {}", value);
            return null;
        }
    }
}
