package com.workmate.was.chat.vo;

/**
 * 답변의 RAG 출처 한 건 (F4-07).
 *
 * <p>검색 단계의 {@code GuideSourceChunk} 는 본문 청크까지 들고 있지만, 화면에 필요한 건
 * 가이드 식별자와 제목뿐이다. 저장·응답에는 이 축약형만 쓴다.</p>
 *
 * @param guideSeq 가이드 문서 식별자 (화면에서 상세로 이동하는 링크에 쓴다)
 * @param title    가이드 제목
 */
public record ChatSourceVo(Long guideSeq, String title) {
}
