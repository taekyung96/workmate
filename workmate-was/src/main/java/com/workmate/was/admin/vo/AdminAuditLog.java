package com.workmate.was.admin.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 관리자 감사 로그 Entity (admin_audit_log, 04 §4.6) — append-only.
 * 잠금 해제·비밀번호 초기화 등 관리자 조치를 수행 관리자·대상 사용자와 함께 기록한다 (F8).
 */
@Entity
@Table(name = "admin_audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditLog {

    /** 조치 유형 — 잠금 해제 */
    public static final String ACTION_UNLOCK = "UNLOCK";
    /** 조치 유형 — 비밀번호 초기화 */
    public static final String ACTION_RESET_PASSWORD = "RESET_PASSWORD";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_seq")
    private Long auditSeq;

    /** 조치를 수행한 관리자 (app_user.user_seq) */
    @Column(name = "admin_user_seq", nullable = false)
    private Long adminUserSeq;

    /** 조치 대상 사용자 (app_user.user_seq) */
    @Column(name = "target_user_seq", nullable = false)
    private Long targetUserSeq;

    /** 'UNLOCK' | 'RESET_PASSWORD' */
    @Column(name = "action", nullable = false, length = 30)
    private String action;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public AdminAuditLog(Long adminUserSeq, Long targetUserSeq, String action) {
        this.adminUserSeq = adminUserSeq;
        this.targetUserSeq = targetUserSeq;
        this.action = action;
        this.createdAt = LocalDateTime.now();
    }
}
