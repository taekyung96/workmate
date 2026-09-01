package com.workmate.was.usage.controller;

import com.workmate.was.global.response.ApiResponse;
import com.workmate.was.usage.service.LlmUsageQueryService;
import com.workmate.was.usage.vo.UsageSummaryVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 본인 사용량 조회 REST API.
 *
 * <p>관리자 대시보드({@code /api/v1/admin/usage})와 달리 <b>로그인한 모든 사용자</b>가 쓴다.
 * 그래서 관리자 전용 경로 밖에 두고, 집계 대상은 언제나 요청자 본인으로 고정한다.</p>
 *
 * <p><b>조회 대상은 {@code X-User-Seq} 헤더로만 정한다.</b> 이 헤더는 WEB 의 RestClient 인터셉터가
 * 세션의 로그인 사용자로 <i>덮어써서</i> 넣는 값이라 브라우저가 조작할 수 없다.
 * 사용자 번호를 쿼리 파라미터로 받으면 그 순간 남의 사용량을 조회할 수 있게 되므로 받지 않는다.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/usage")
@RequiredArgsConstructor
public class MyUsageApiController {

    private final LlmUsageQueryService llmUsageQueryService;

    /**
     * 본인 기간 요약 — 합계·기능별·일별 (from·to 미지정 시 최근 30일).
     *
     * @param userSeq 요청자 본인 (WEB 이 세션에서 주입, 클라이언트가 지정할 수 없다)
     * @param from    조회 시작일
     * @param to      조회 종료일
     * @return 본인 사용량 집계
     */
    @GetMapping("/me")
    public ApiResponse<UsageSummaryVo> getMySummary(
            @RequestHeader("X-User-Seq") Long userSeq,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.info("본인 사용량 조회 - userSeq: {}, from: {}, to: {}", userSeq, from, to);
        return ApiResponse.success(llmUsageQueryService.getMySummary(userSeq, from, to));
    }
}
