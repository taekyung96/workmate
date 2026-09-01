package com.workmate.was.usage.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 사용자별 사용량 (관리자 사용량 대시보드).
 * 이메일은 {@code admin.util.PiiMasker} 로 마스킹된 값만 담는다. 탈퇴 등으로 사용자 정보를
 * 더 이상 찾을 수 없어도 사용량 기록(append-only)은 남아 있을 수 있어, 그런 행은
 * userName="(삭제된 사용자)"·maskedEmail="-" 로 표시한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUsageVo {
    private Long userSeq;
    private String maskedEmail;
    private String userName;
    private long callCount;
    private long inputTokens;
    private long outputTokens;
    /** 토큰 미집계 건수 (해당 사용자 내) */
    private long untrackedCallCount;
    /** 추정 비용(USD) — 단가 미등록 모델의 호출은 계산에서 제외된 값(과소 추정 가능) */
    private BigDecimal estimatedCostUsd;
    /** 추정 비용(KRW) */
    private BigDecimal estimatedCostKrw;
}
