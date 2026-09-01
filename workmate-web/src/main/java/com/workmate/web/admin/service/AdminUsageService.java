package com.workmate.web.admin.service;

import org.springframework.http.ResponseEntity;

/**
 * 관리자 사용량 대시보드 WEB 프록시 서비스 — /api/v1/admin/usage/** 를 WAS 로 중계한다.
 */
public interface AdminUsageService {

    /** 기간 요약 중계 */
    ResponseEntity<String> getSummary(String from, String to);

    /** 사용자별 집계 중계 */
    ResponseEntity<String> getByUser(String from, String to, int page, int size);
}
