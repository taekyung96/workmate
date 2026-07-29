package com.workmate.web.admin.service;

import org.springframework.http.ResponseEntity;

/**
 * 관리자 도메인 WEB 프록시 서비스 — /api/v1/admin/** 를 WAS 로 중계한다.
 * 수행 관리자 식별(X-User-Seq)은 RestClient 인터셉터가 세션에서 자동 주입한다.
 */
public interface AdminService {

    /** 사용자 목록·검색 중계 (M1) */
    ResponseEntity<String> getUsers(String keyword, int page, int size);

    /** 감사 로그 목록 중계 (M4) */
    ResponseEntity<String> getAuditLogs(int page, int size);

    /** 잠금 해제 중계 (M2) */
    ResponseEntity<String> unlock(Long userSeq);

    /** 비밀번호 초기화 중계 (M3) */
    ResponseEntity<String> resetPassword(Long userSeq);
}
