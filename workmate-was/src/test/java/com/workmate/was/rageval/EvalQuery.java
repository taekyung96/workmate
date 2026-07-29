package com.workmate.was.rageval;

import java.util.List;

/**
 * 평가 골든셋의 한 문항.
 *
 * @param question       사용자 질문
 * @param expectedTitles 정답으로 인정하는 가이드 제목들(하나라도 검색되면 hit)
 */
public record EvalQuery(String question, List<String> expectedTitles) {
}
