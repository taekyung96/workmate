package com.workmate.was.admin.vo;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자 감사 로그 조회 항목 VO (M4) — 행위자·대상 사용자 이름을 조인해 함께 내려준다.
 * admin_audit_log 는 seq 만 저장하므로, 사람이 읽을 수 있도록 이름을 채워 응답한다.
 */
@Getter
@Builder
public class AuditLogVo {

    private Long auditSeq;
    /** 조치를 수행한 관리자 식별자 */
    private Long adminUserSeq;
    /** 행위자 이름 (삭제된 사용자면 대체 문구) */
    private String adminUserName;
    /** 조치 대상 사용자 식별자 */
    private Long targetUserSeq;
    /** 대상 사용자 이름 (삭제된 사용자면 대체 문구) */
    private String targetUserName;
    /** 'UNLOCK' | 'RESET_PASSWORD' */
    private String action;
    private LocalDateTime createdAt;
}
