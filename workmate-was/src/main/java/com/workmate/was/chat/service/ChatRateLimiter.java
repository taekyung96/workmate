package com.workmate.was.chat.service;

import com.workmate.was.global.exception.RateLimitExceededException;

/**
 * 사용자별 분당 AI 요청 횟수 제한기 (F2-11).
 *
 * <p>구현이 둘이다 — 인스턴스가 여러 개인 환경에서는 카운터를 공유해야 한도가 지켜지므로
 * {@link RedisChatRateLimiter} 를 기본으로 쓰고, Redis 가 없는 로컬·테스트 환경에서는
 * {@link InMemoryChatRateLimiter} 로 폴백한다 ({@code app.chat.rate-limit-store} 로 선택).</p>
 */
public interface ChatRateLimiter {

    /**
     * 요청 1건을 기록하고 한도 초과 시 예외를 던진다.
     *
     * @param userSeq 요청 사용자
     * @throws RateLimitExceededException 분당 한도 초과 (429)
     */
    void check(Long userSeq);
}
