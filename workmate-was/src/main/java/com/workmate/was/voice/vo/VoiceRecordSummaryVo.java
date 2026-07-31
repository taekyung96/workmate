package com.workmate.was.voice.vo;

import java.time.LocalDateTime;
import lombok.Getter;

/**
 * 회의록 이력 목록 항목 VO (F8-1 확장).
 * 전사 원문·요약 본문은 담지 않는다 — 목록 응답이 수백 KB 로 커지는 것을 막는다.
 */
@Getter
public class VoiceRecordSummaryVo {

    private final Long recordSeq;
    private final String title;
    private final String originFileName;
    private final Long fileSize;
    private final boolean hasAudio;
    private final LocalDateTime createdAt;

    public VoiceRecordSummaryVo(VoiceRecord record) {
        this.recordSeq = record.getRecordSeq();
        this.title = record.getTitle();
        this.originFileName = record.getOriginFileName();
        this.fileSize = record.getFileSize();
        this.hasAudio = record.getAudioFileName() != null;
        this.createdAt = record.getCreatedAt();
    }
}
