package com.workmate.was.guide.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 가이드 목록 페이지 응답 VO (G1) — 목록 + 페이징 메타. (Admin 의 AuditLogPageVo 와 동일 구조)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuidePageVo {
    private List<GuideSummaryVo> content;
    /** 0-based 현재 페이지 */
    private int page;
    private int totalPages;
    private long totalElements;
}
