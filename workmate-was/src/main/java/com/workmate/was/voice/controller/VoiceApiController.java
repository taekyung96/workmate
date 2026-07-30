package com.workmate.was.voice.controller;

import com.workmate.was.global.response.ApiResponse;
import com.workmate.was.voice.service.VoiceService;
import com.workmate.was.voice.vo.VoiceAnalysisResultVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 음성 회의록 REST API (F8-1). 사용자 식별은 X-User-Seq 헤더로 한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/voice")
@RequiredArgsConstructor
public class VoiceApiController {

    private final VoiceService voiceService;

    /**
     * 업로드된 회의 오디오를 전사·요약해 결과를 반환한다 (F8-1).
     *
     * @param userSeq 인증 세션에서 WEB 인터셉터가 주입한 사용자 식별자
     * @param file    회의 오디오 파일
     * @param title   회의 제목 (선택)
     * @return 전사 원문 + 마크다운 요약
     */
    @PostMapping("/analyze")
    public ApiResponse<VoiceAnalysisResultVo> analyze(
            @RequestHeader("X-User-Seq") Long userSeq,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) {
        log.info("음성 회의록 분석 API 호출 - userSeq: {}, 파일: {}", userSeq, file.getOriginalFilename());
        return ApiResponse.success(voiceService.analyze(userSeq, title, file));
    }

    /**
     * 저장된 회의록 요약을 사내 가이드 문서로 등록한다 (F8-1-6, 지식 공유 → RAG 대상 편입).
     *
     * @param userSeq   인증 세션에서 WEB 인터셉터가 주입한 사용자 식별자
     * @param recordSeq 회의록 식별자
     * @return 새로 생성된 가이드 식별자
     */
    @PostMapping("/{recordSeq}/to-guide")
    public ApiResponse<Long> convertToGuide(
            @RequestHeader("X-User-Seq") Long userSeq,
            @PathVariable Long recordSeq) {
        log.info("회의록 → 가이드 등록 API 호출 - userSeq: {}, recordSeq: {}", userSeq, recordSeq);
        return ApiResponse.success(voiceService.convertToGuide(userSeq, recordSeq));
    }
}
