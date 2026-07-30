package com.workmate.was.guide.tool;

import com.workmate.was.guide.service.GuideService;
import com.workmate.was.guide.vo.GuideSummaryVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 채팅 AI 가 호출하는 가이드 문서 검색 도구 (F5-02).
 * 본인 + 공개 문서 중 제목에 키워드가 포함된 문서를 반환한다 (F5-03 본인 접근 범위).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuideTools {

    private final GuideService guideService;

    /** 도구가 한 번에 훑을 최대 문서 수 — 결과는 LLM 프롬프트에 요약으로 들어가므로 과하지 않게 제한 */
    private static final int TOOL_SEARCH_SIZE = 20;

    @Tool(description = "제목이나 본문에 특정 키워드가 포함된 가이드 문서를 검색한다. "
            + "'~에 대한 가이드 있어?', '문서 찾아줘' 등 문서 존재 여부를 물을 때 사용한다.")
    public String searchGuidesByTitle(
            @ToolParam(description = "검색 키워드") String keyword,
            ToolContext toolContext) {

        Long userSeq = (Long) toolContext.getContext().get("userSeq");

        // DB 에서 접근 가능(본인+공개) 문서 중 제목·본문 키워드 매칭분을 바로 조회 (검색 API 와 동일 경로)
        List<GuideSummaryVo> matched =
                guideService.searchGuides(userSeq, keyword, 0, TOOL_SEARCH_SIZE).getContent();

        log.info("[Tool] searchGuidesByTitle - userSeq: {}, keyword: {}, matched: {}",
                userSeq, keyword, matched.size());

        if (matched.isEmpty()) {
            return "'" + keyword + "' 관련 가이드 문서를 찾지 못했습니다.";
        }
        return "관련 가이드 문서: " + matched.stream()
                .map(g -> g.getTitle() + "(id=" + g.getGuideSeq() + ")")
                .collect(Collectors.joining(", "));
    }
}
