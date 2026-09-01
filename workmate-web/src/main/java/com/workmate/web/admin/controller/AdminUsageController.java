package com.workmate.web.admin.controller;

import com.workmate.web.admin.service.AdminUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 사용량 대시보드 WEB 프록시 컨트롤러 — 화면(fetch)의 /api/v1/admin/usage/** 요청을
 * WAS 로 중계한다. 기존 AdminController(사용자·감사로그)와 같은 이유로 별도 컨트롤러로 둔다
 * (WAS 쪽 AdminUsageApiController 와 대칭). 접근 제어(ROLE_ADMIN)는 SecurityConfig 가 담당한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/usage")
@RequiredArgsConstructor
public class AdminUsageController {

    private final AdminUsageService adminUsageService;

    /** 기간 요약 중계 */
    @GetMapping("/summary")
    public ResponseEntity<String> getSummary(
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to) {
        return jsonPassthrough(adminUsageService.getSummary(from, to));
    }

    /** 사용자별 집계 중계 */
    @GetMapping("/by-user")
    public ResponseEntity<String> getByUser(
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return jsonPassthrough(adminUsageService.getByUser(from, to, page, size));
    }

    private ResponseEntity<String> jsonPassthrough(ResponseEntity<String> wasResponse) {
        return ResponseEntity.status(wasResponse.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(wasResponse.getBody());
    }
}
