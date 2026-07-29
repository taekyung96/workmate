package com.workmate.was.admin.controller;

import com.workmate.was.admin.service.AdminService;
import com.workmate.was.admin.vo.AuditLogPageVo;
import com.workmate.was.admin.vo.ResetPasswordResultVo;
import com.workmate.was.admin.vo.UserPageVo;
import com.workmate.was.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 사용자 관리 REST API (M1~M3, 04 §3.5).
 * 접근 제어(ROLE_ADMIN)는 WEB Security 가 담당하고, 여기서는 X-User-Seq(수행 관리자)로 감사 주체를 식별한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final AdminService adminService;

    /** 사용자 목록·검색 (M1) */
    @GetMapping("/users")
    public ApiResponse<UserPageVo> getUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        log.info("관리자 사용자 목록 조회 - keyword: {}, page: {}", keyword, page);
        return ApiResponse.success(adminService.getUsers(keyword, page, size));
    }

    /** 감사 로그 목록 (M4) — 최신순 페이징, 행위자·대상 이름 포함 */
    @GetMapping("/audit-logs")
    public ApiResponse<AuditLogPageVo> getAuditLogs(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        log.info("관리자 감사 로그 조회 - page: {}, size: {}", page, size);
        return ApiResponse.success(adminService.getAuditLogs(page, size));
    }

    /** 계정 잠금 해제 (M2) */
    @PostMapping("/users/{userSeq}/unlock")
    public ApiResponse<Void> unlock(
            @RequestHeader("X-User-Seq") Long adminUserSeq,
            @PathVariable("userSeq") Long targetUserSeq) {
        adminService.unlock(adminUserSeq, targetUserSeq);
        return ApiResponse.success();
    }

    /** 비밀번호 초기화 (M3) — 임시 비밀번호 평문을 1회 반환 */
    @PostMapping("/users/{userSeq}/reset-password")
    public ApiResponse<ResetPasswordResultVo> resetPassword(
            @RequestHeader("X-User-Seq") Long adminUserSeq,
            @PathVariable("userSeq") Long targetUserSeq) {
        return ApiResponse.success(adminService.resetPassword(adminUserSeq, targetUserSeq));
    }
}
