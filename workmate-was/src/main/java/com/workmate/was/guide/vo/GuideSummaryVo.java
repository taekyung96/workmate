package com.workmate.was.guide.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 가이드 목록(카드) 표시용 경량 VO (G1).
 * 본문 전체 대신 미리보기용 excerpt 만 담아 목록 응답 트래픽을 줄인다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuideSummaryVo {

    /** 가이드 문서 식별자 */
    private Long guideSeq;
    /** 문서 제목 */
    private String title;
    /** 본문 미리보기 (마크다운 기호 제거 후 앞부분만) */
    private String excerpt;
    /** 공개 여부 */
    private Boolean isPublic;
    /** 최종 수정 일시 */
    private LocalDateTime updatedAt;
}
