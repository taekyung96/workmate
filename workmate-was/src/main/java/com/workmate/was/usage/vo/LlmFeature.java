package com.workmate.was.usage.vo;

/**
 * LLM 을 호출하는 기능 구분 (F-OBS).
 * 사용량 기록의 집계 축이며, {@code llm_usage.feature} 컬럼에 이름 그대로 저장된다.
 */
public enum LlmFeature {

    /** 채팅 스트리밍 (F2) */
    CHAT,
    /** 영수증 이미지 분석 — 멀티모달 (F3) */
    OCR,
    /** 회의 음성 → 텍스트 (F8-1) */
    STT,
    /** 회의록 요약 (F8-1) */
    SUMMARY,
    /** 가이드 문서 임베딩 (F4) */
    EMBEDDING
}
