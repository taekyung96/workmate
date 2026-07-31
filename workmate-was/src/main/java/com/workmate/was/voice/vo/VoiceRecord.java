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
 * 회의 오디오를 Gemini 로 전사·요약한 결과를 보관한다. 이력 재생을 위해 오디오 파일 정보도 함께 남긴다.
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

    /** 서버에 저장된 오디오 파일명 (UUID.확장자). 저장 경로는 설정값이라 파일명만 남긴다 */
    @Column(name = "audio_file_name", length = 200)
    private String audioFileName;

    /** 사용자가 업로드한 원본 파일명 — 이력 목록에 표시 */
    @Column(name = "origin_file_name", length = 255)
    private String originFileName;

    /** 오디오 파일 크기 (바이트) */
    @Column(name = "file_size")
    private Long fileSize;

    /** 오디오 MIME 타입 — 재생 응답의 Content-Type 으로 사용 */
    @Column(name = "content_type", length = 100)
    private String contentType;

    /** 생성 일시 */
    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
