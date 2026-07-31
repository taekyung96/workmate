package com.workmate.web.voice.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 음성 회의록 도메인 WEB 프록시 서비스 — /api/v1/voice/** 를 WAS 로 중계한다 (F8-1).
 * 사용자 식별(X-User-Seq)은 RestClient 인터셉터가 세션에서 자동 주입한다.
 */
public interface VoiceService {

    ResponseEntity<String> analyze(MultipartFile file, String title) throws IOException;

    /** 내 회의록 이력 조회 중계 */
    ResponseEntity<String> history();

    /** 회의록 상세 조회 중계 */
    ResponseEntity<String> getRecord(Long recordSeq);

    /** 회의록 삭제 중계 */
    ResponseEntity<String> delete(Long recordSeq);

    /**
     * 회의록 오디오를 WAS 에서 받아 브라우저 응답으로 그대로 흘려보낸다.
     *
     * @param recordSeq   회의록 식별자
     * @param rangeHeader 브라우저가 보낸 Range 헤더 (없으면 null)
     * @param response    서블릿 응답 — 상태코드·헤더·본문을 직접 쓴다
     */
    void relayAudio(Long recordSeq, String rangeHeader, HttpServletResponse response);
}
