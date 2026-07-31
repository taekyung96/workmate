package com.workmate.was.voice.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.io.Resource;

/** 회의 오디오 스트리밍 응답 재료 (F8-1 확장). 리소스와 재생에 필요한 메타를 함께 넘긴다. */
@Getter
@AllArgsConstructor
public class VoiceAudioVo {

    /** 오디오 파일 리소스 */
    private final Resource resource;

    /** 응답 Content-Type — 저장 시 기록한 MIME 타입 */
    private final String contentType;

    /** 사용자가 올린 원본 파일명 (다운로드 파일명으로 사용) */
    private final String originFileName;
}
