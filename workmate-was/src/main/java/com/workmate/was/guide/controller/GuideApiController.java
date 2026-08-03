package com.workmate.was.guide.controller;

import com.workmate.was.global.response.ApiResponse;
import com.workmate.was.guide.service.GuideService;
import com.workmate.was.guide.vo.GuidePageVo;
import com.workmate.was.guide.vo.GuideResponseVo;
import com.workmate.was.guide.vo.GuideSaveRequestVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 가이드 문서 REST API (G1~G5, F4). 사용자 식별은 X-User-Seq 헤더로 한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/guides")
@RequiredArgsConstructor
public class GuideApiController {

    private final GuideService guideService;

    /** 목록 (본인+공개, 키워드 검색·페이징, G1) */
    @GetMapping
    public ApiResponse<GuidePageVo> list(
            @RequestHeader("X-User-Seq") Long userSeq,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "12") int size) {
        return ApiResponse.success(guideService.searchGuides(userSeq, keyword, page, size));
    }

    /** 상세 (G2) */
    @GetMapping("/{guideSeq}")
    public ApiResponse<GuideResponseVo> detail(
            @RequestHeader("X-User-Seq") Long userSeq,
            @PathVariable("guideSeq") Long guideSeq) {
        return ApiResponse.success(guideService.getGuide(userSeq, guideSeq));
    }

    /** 등록 (G3) */
    @PostMapping
    public ApiResponse<GuideResponseVo> create(
            @RequestHeader("X-User-Seq") Long userSeq,
            @Valid @RequestBody GuideSaveRequestVo request) {
        return ApiResponse.success(guideService.createGuide(userSeq, request));
    }

    /** 수정 (G4) — 본인 문서만, 관리자는 타인 문서도 허용 */
    @PostMapping("/{guideSeq}/update")
    public ApiResponse<GuideResponseVo> update(
            @RequestHeader("X-User-Seq") Long userSeq,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable("guideSeq") Long guideSeq,
            @Valid @RequestBody GuideSaveRequestVo request) {
        return ApiResponse.success(guideService.updateGuide(userSeq, isAdmin(role), guideSeq, request));
    }

    /** 삭제 (G5) — 본인 문서만, 관리자는 타인 문서도 허용 */
    @PostMapping("/{guideSeq}/delete")
    public ApiResponse<Void> delete(
            @RequestHeader("X-User-Seq") Long userSeq,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable("guideSeq") Long guideSeq) {
        guideService.deleteGuide(userSeq, isAdmin(role), guideSeq);
        return ApiResponse.success();
    }

    /** 권한 문자열이 관리자('ADMIN' 또는 'ROLE_ADMIN')인지 판별한다 (프론트 auth 스토어와 동일 기준). */
    private static boolean isAdmin(String role) {
        return "ADMIN".equals(role) || "ROLE_ADMIN".equals(role);
    }
}
