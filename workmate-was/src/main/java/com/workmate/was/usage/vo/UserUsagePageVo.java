package com.workmate.was.usage.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 사용자별 사용량 페이지 응답 (관리자 사용량 대시보드) — 목록 + 페이징 메타.
 * 기존 admin.vo.UserPageVo 와 같은 페이징 메타 형태를 따른다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUsagePageVo {
    private List<UserUsageVo> content;
    /** 0-based 현재 페이지 */
    private int page;
    private int totalPages;
    private long totalElements;
}
