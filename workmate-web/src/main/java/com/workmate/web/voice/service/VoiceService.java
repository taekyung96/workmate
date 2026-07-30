package com.workmate.web.voice.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 음성 회의록 도메인 WEB 프록시 서비스 — /api/v1/voice/** 를 WAS 로 중계한다 (F8-1).
 * 사용자 식별(X-User-Seq)은 RestClient 인터셉터가 세션에서 자동 주입한다.
 */
public interface VoiceService {

    ResponseEntity<String> analyze(MultipartFile file, String title) throws IOException;

    /** 저장된 회의록을 사내 가이드로 등록 요청 중계 (F8-1-6). */
    ResponseEntity<String> convertToGuide(Long recordSeq);
}
