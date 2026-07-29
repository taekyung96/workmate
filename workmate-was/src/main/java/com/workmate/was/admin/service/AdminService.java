package com.workmate.was.admin.service;

import com.workmate.was.admin.vo.AuditLogPageVo;
import com.workmate.was.admin.vo.ResetPasswordResultVo;
import com.workmate.was.admin.vo.UserPageVo;

/**
 * 관리자 사용자 관리 비즈니스 로직 (F6). 개인정보 마스킹·잠금 해제·비밀번호 초기화·감사 기록을 담당한다.
 */
public interface AdminService {

    /** 사용자 목록·검색 (M1, F6-01) — 이메일·전화 마스킹 포함 */
    UserPageVo getUsers(String keyword, int page, int size);

    /** 감사 로그 목록 조회 (M4, F8 — 최신순 페이징, 행위자·대상 이름 포함) */
    AuditLogPageVo getAuditLogs(int page, int size);

    /** 계정 잠금 해제 + 감사 기록 (M2, F6-02) */
    void unlock(Long adminUserSeq, Long targetUserSeq);

    /** 비밀번호 초기화 (임시 비번 발급) + 감사 기록 (M3, F6-03) */
    ResetPasswordResultVo resetPassword(Long adminUserSeq, Long targetUserSeq);
}
