package com.workmate.was.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 관리자 감사 로그 목록 페이지 응답 VO (M4) — 목록 + 페이징 메타.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogPageVo {
    private List<AuditLogVo> content;
    /** 0-based 현재 페이지 */
    private int page;
    private int totalPages;
    private long totalElements;
}
