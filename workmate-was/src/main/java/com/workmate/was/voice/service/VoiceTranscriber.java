package com.workmate.was.voice.service;

import org.springframework.core.io.Resource;

/**
 * 음성 전사(STT) 추상화 (F8-1).
 * 전사 엔진을 요약 로직과 분리해, 품질이 병목이면 구현체만 교체(Gemini → Clova/Whisper 등)할 수 있게 한다.
 */
public interface VoiceTranscriber {

    /**
     * 오디오를 텍스트로 전사한다.
     *
     * @param userSeq  요청 사용자 — LLM 사용량 기록(F-OBS)의 귀속 대상
     * @param audio    오디오 리소스
     * @param mimeType 오디오 MIME 타입 (예: audio/mpeg, audio/wav, audio/webm)
     * @return 전사된 텍스트
     */
    String transcribe(Long userSeq, Resource audio, String mimeType);
}
