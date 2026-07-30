package com.workmate.was.voice.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 음성 회의록 엔티티 (F8-1).
 * 회의 오디오를 Gemini 로 전사·요약한 결과를 보관한다. 오디오 원본은 저장하지 않는다(프라이버시).
 */
@Entity
@Table(name = "voice_record")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class VoiceRecord {

    /** 회의록 식별자 (PK) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_seq")
    private Long recordSeq;

    /** 작성자(사용자) 식별자 */
    @Column(name = "user_seq", nullable = false)
    private Long userSeq;

    /** 회의 제목 */
    @Column(name = "title", nullable = false)
    private String title;

    /** STT 전사 원문 */
    @Column(name = "stt_text", nullable = false, columnDefinition = "text")
    private String sttText;

    /** AI 구조화 요약 (마크다운) */
    @Column(name = "summary_md", nullable = false, columnDefinition = "text")
    private String summaryMd;

    /** 생성 일시 */
    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
