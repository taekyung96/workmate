package com.workmate.was.admin.controller;

import com.workmate.was.usage.service.LlmUsageQueryService;
import com.workmate.was.usage.vo.UsageSummaryVo;
import com.workmate.was.usage.vo.UserUsagePageVo;
import com.workmate.was.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 관리자 사용량 대시보드 REST API (PLAN-usage-dashboard.md).
 * 기존 AdminApiController(사용자·감사로그)를 비대하게 만들지 않으려고 별도 컨트롤러로 둔다(§6-3).
 * 접근 제어(ROLE_ADMIN)는 WEB Security 가 담당한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/usage")
@RequiredArgsConstructor
public class AdminUsageApiController {

    private final LlmUsageQueryService llmUsageQueryService;

    /** 기간 요약 — 합계·기능별·일별 (from·to 미지정 시 최근 30일) */
    @GetMapping("/summary")
    public ApiResponse<UsageSummaryVo> getSummary(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.info("관리자 사용량 요약 조회 - from: {}, to: {}", from, to);
        return ApiResponse.success(llmUsageQueryService.getSummary(from, to));
    }

    /** 사용자별 집계 — 토큰 합계 내림차순 페이징 (from·to 미지정 시 최근 30일) */
    @GetMapping("/by-user")
    public ApiResponse<UserUsagePageVo> getByUser(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        log.info("관리자 사용자별 사용량 조회 - from: {}, to: {}, page: {}", from, to, page);
        return ApiResponse.success(llmUsageQueryService.getByUser(from, to, page, size));
    }
}
