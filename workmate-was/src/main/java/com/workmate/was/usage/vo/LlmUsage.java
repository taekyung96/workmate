package com.workmate.was.usage.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * LLM 사용량 기록 엔티티 (F-OBS) — append-only.
 *
 * <p>과금·쿼터·남용 조사의 근거라 수정·삭제하지 않는다. 사용자별 집계가 주 용도이므로
 * Prometheus 가 아니라 DB 에 남긴다 (user_seq 를 지표 라벨로 쓰면 카디널리티가 폭발한다).
 */
@Entity
@Table(name = "llm_usage")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LlmUsage {

    /** 사용량 기록 식별자 (PK) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usage_seq")
    private Long usageSeq;

    /** 사용한 사용자 식별자 */
    @Column(name = "user_seq", nullable = false)
    private Long userSeq;

    /** 호출한 기능 (CHAT·OCR·STT·SUMMARY·EMBEDDING) */
    @Enumerated(EnumType.STRING)
    @Column(name = "feature", nullable = false, length = 20)
    private LlmFeature feature;

    /** 실제 호출된 모델명 */
    @Column(name = "model_name", length = 50)
    private String modelName;

    /** 입력(프롬프트) 토큰 수 — 제공자가 usage 를 주지 않는 경로에서는 null */
    @Column(name = "input_tokens")
    private Integer inputTokens;

    /** 출력(생성) 토큰 수 — 제공자가 usage 를 주지 않는 경로에서는 null */
    @Column(name = "output_tokens")
    private Integer outputTokens;

    /** 기록 일시 */
    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
