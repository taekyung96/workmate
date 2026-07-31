package com.workmate.web.voice.controller;

import com.workmate.web.voice.service.VoiceService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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

/**
 * 음성 회의록 도메인 WEB 프록시 컨트롤러 (F8-1).
 * 화면(fetch)의 /api/v1/voice/** 를 WAS 로 중계한다. (3-tier: 브라우저는 WEB만 바라봄)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/voice")
@RequiredArgsConstructor
public class VoiceController {

    private final VoiceService voiceService;

    /**
     * 회의 오디오 분석 요청 중계.
     *
     * @param file  업로드된 오디오 파일
     * @param title 회의 제목 (선택)
     * @return WAS 분석 응답 JSON
     * @throws IOException 파일 스트림 처리 실패 시
     */
    @PostMapping("/analyze")
    public ResponseEntity<String> analyze(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) throws IOException {
        return jsonPassthrough(voiceService.analyze(file, title));
    }

    /**
     * 내 회의록 이력 조회 중계.
     *
     * @return WAS 목록 응답 JSON
     */
    @GetMapping
    public ResponseEntity<String> history() {
        return jsonPassthrough(voiceService.history());
    }

    /**
     * 회의록 상세 조회 중계.
     *
     * @param recordSeq 회의록 식별자
     * @return WAS 상세 응답 JSON
     */
    @GetMapping("/{recordSeq}")
    public ResponseEntity<String> record(@PathVariable Long recordSeq) {
        return jsonPassthrough(voiceService.getRecord(recordSeq));
    }

    /**
     * 회의록 삭제 중계.
     *
     * @param recordSeq 회의록 식별자
     * @return WAS 응답 JSON
     */
    @PostMapping("/{recordSeq}/delete")
    public ResponseEntity<String> delete(@PathVariable Long recordSeq) {
        return jsonPassthrough(voiceService.delete(recordSeq));
    }

    /**
     * 회의록 오디오 스트리밍 중계. Range 헤더를 그대로 전달해 구간 재생을 지원한다.
     *
     * @param recordSeq   회의록 식별자
     * @param rangeHeader 브라우저 Range 헤더 (선택)
     * @param response    서블릿 응답 — 서비스가 직접 기록한다
     */
    @GetMapping("/{recordSeq}/audio")
    public void audio(
            @PathVariable Long recordSeq,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader,
            HttpServletResponse response) {
        voiceService.relayAudio(recordSeq, rangeHeader, response);
    }

    /** WAS 응답의 상태코드·본문을 유지하며 JSON 컨텐츠 타입으로 화면에 전달한다. */
    private ResponseEntity<String> jsonPassthrough(ResponseEntity<String> wasResponse) {
        return ResponseEntity.status(wasResponse.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(wasResponse.getBody());
    }
}
