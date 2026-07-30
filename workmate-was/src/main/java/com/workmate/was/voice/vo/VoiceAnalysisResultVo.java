package com.workmate.was.voice.vo;

import java.time.LocalDateTime;
import lombok.Getter;

/** 음성 회의록 분석 결과 응답 VO (F8-1). 전사 원문 + 마크다운 요약을 함께 반환한다. */
@Getter
public class VoiceAnalysisResultVo {

    private final Long recordSeq;
    private final String title;
    private final String sttText;
    private final String summaryMd;
    private final LocalDateTime createdAt;

    public VoiceAnalysisResultVo(VoiceRecord record) {
        this.recordSeq = record.getRecordSeq();
        this.title = record.getTitle();
        this.sttText = record.getSttText();
        this.summaryMd = record.getSummaryMd();
        this.createdAt = record.getCreatedAt();
    }
}
