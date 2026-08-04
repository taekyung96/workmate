package com.workmate.was.voice.controller;

import com.workmate.was.global.response.ApiResponse;
import com.workmate.was.voice.service.VoiceService;
import com.workmate.was.voice.vo.VoiceAnalysisResultVo;
import com.workmate.was.voice.vo.VoiceAudioVo;
import com.workmate.was.voice.vo.VoiceRecordPageVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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
     * 내 회의록 이력을 최신순으로 조회한다.
     * page·size 가 오면 그 값으로 페이징하고, 없으면 전체를 한 페이지로 반환한다.
     *
     * @param userSeq 인증 세션에서 WEB 인터셉터가 주입한 사용자 식별자
     * @param page    0-based 페이지 (선택)
     * @param size    페이지 크기 (선택)
     * @return 목록 요약 페이지 (전사문·요약 본문 제외)
     */
    @GetMapping
    public ApiResponse<VoiceRecordPageVo> history(
            @RequestHeader("X-User-Seq") Long userSeq,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        log.info("회의록 이력 조회 API 호출 - userSeq: {}, page: {}, size: {}", userSeq, page, size);
        return ApiResponse.success(voiceService.getHistoryPage(userSeq, page, size));
    }

    /**
     * 회의록 상세를 조회한다 (본인 소유만).
     *
     * @param userSeq   인증 세션에서 WEB 인터셉터가 주입한 사용자 식별자
     * @param recordSeq 회의록 식별자
     * @return 전사문·요약 전문
     */
    @GetMapping("/{recordSeq}")
    public ApiResponse<VoiceAnalysisResultVo> record(
            @RequestHeader("X-User-Seq") Long userSeq,
            @PathVariable Long recordSeq) {
        log.info("회의록 상세 조회 API 호출 - userSeq: {}, recordSeq: {}", userSeq, recordSeq);
        return ApiResponse.success(voiceService.getRecord(userSeq, recordSeq));
    }

    /**
     * 회의록 오디오를 스트리밍한다 (본인 소유만).
     * Range 요청이 오면 206 Partial Content 로 해당 구간만 내려 재생 중 구간 이동을 지원한다.
     *
     * @param userSeq   인증 세션에서 WEB 인터셉터가 주입한 사용자 식별자
     * @param recordSeq 회의록 식별자
     * @param headers   요청 헤더 (Range 확인용)
     * @return 전체(200) 또는 요청 구간(206) 오디오
     * @throws IOException 리소스 길이 조회 실패 시
     */
    @GetMapping("/{recordSeq}/audio")
    public ResponseEntity<ResourceRegion> audio(
            @RequestHeader("X-User-Seq") Long userSeq,
            @PathVariable Long recordSeq,
            @RequestHeader HttpHeaders headers) throws IOException {
        VoiceAudioVo audio = voiceService.getAudio(userSeq, recordSeq);
        Resource resource = audio.getResource();
        long total = resource.contentLength();

        List<HttpRange> ranges = headers.getRange();
        boolean partial = !ranges.isEmpty();
        ResourceRegion region = partial
                ? ranges.get(0).toResourceRegion(resource)
                : new ResourceRegion(resource, 0, total);

        return ResponseEntity.status(partial ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK)
                .contentType(MediaType.parseMediaType(audio.getContentType()))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(region);
    }

    /**
     * 회의록을 삭제한다 (본인 소유만). DB 행과 오디오 파일을 함께 지운다.
     *
     * @param userSeq   인증 세션에서 WEB 인터셉터가 주입한 사용자 식별자
     * @param recordSeq 회의록 식별자
     * @return 성공 응답
     */
    @PostMapping("/{recordSeq}/delete")
    public ApiResponse<Void> delete(
            @RequestHeader("X-User-Seq") Long userSeq,
            @PathVariable Long recordSeq) {
        log.info("회의록 삭제 API 호출 - userSeq: {}, recordSeq: {}", userSeq, recordSeq);
        voiceService.deleteRecord(userSeq, recordSeq);
        return ApiResponse.success();
    }
}
