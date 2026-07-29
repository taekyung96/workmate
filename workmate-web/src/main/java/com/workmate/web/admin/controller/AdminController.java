package com.workmate.web.admin.controller;

import com.workmate.web.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 도메인 WEB 프록시 컨트롤러 — 화면(fetch)의 /api/v1/admin/** 요청을 WAS 로 중계한다.
 * 접근 제어(ROLE_ADMIN)는 SecurityConfig 가 담당한다 (F6-04).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /** 사용자 목록·검색 (M1) */
    @GetMapping("/users")
    public ResponseEntity<String> getUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return jsonPassthrough(adminService.getUsers(keyword, page, size));
    }

    /** 감사 로그 목록 (M4) */
    @GetMapping("/audit-logs")
    public ResponseEntity<String> getAuditLogs(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return jsonPassthrough(adminService.getAuditLogs(page, size));
    }

    /** 계정 잠금 해제 (M2) */
    @PostMapping("/users/{userSeq}/unlock")
    public ResponseEntity<String> unlock(@PathVariable("userSeq") Long userSeq) {
        return jsonPassthrough(adminService.unlock(userSeq));
    }

    /** 비밀번호 초기화 (M3) */
    @PostMapping("/users/{userSeq}/reset-password")
    public ResponseEntity<String> resetPassword(@PathVariable("userSeq") Long userSeq) {
        return jsonPassthrough(adminService.resetPassword(userSeq));
    }

    private ResponseEntity<String> jsonPassthrough(ResponseEntity<String> wasResponse) {
        return ResponseEntity.status(wasResponse.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(wasResponse.getBody());
    }
}
