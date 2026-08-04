package com.workmate.was.receipt.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 영수증 이력 페이지 응답 VO — 목록 + 페이징 메타. (Guide/Admin 의 PageVo 와 동일 구조)
 * page·size 파라미터가 없으면 전체를 한 페이지(totalPages=1)로 담아 반환한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptPageVo {
    private List<Receipt> content;
    /** 0-based 현재 페이지 */
    private int page;
    private int totalPages;
    private long totalElements;
}
