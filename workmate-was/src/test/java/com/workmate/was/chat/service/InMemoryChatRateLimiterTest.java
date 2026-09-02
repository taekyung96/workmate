package com.workmate.was.chat.service;

import com.workmate.was.global.exception.RateLimitExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * InMemoryChatRateLimiter 단위 테스트 (F2-11).
 */
class InMemoryChatRateLimiterTest {

    private ChatRateLimiter newLimiter(int limit) {
        InMemoryChatRateLimiter limiter = new InMemoryChatRateLimiter();
        ReflectionTestUtils.setField(limiter, "limitPerMinute", limit);
        return limiter;
    }

    @Test
    @DisplayName("한도까지는 통과하고 초과하면 429 예외")
    void allows_up_to_limit_then_throws() {
        ChatRateLimiter limiter = newLimiter(3);

        assertThatCode(() -> {
            limiter.check(1L);
            limiter.check(1L);
            limiter.check(1L);
        }).doesNotThrowAnyException();

        assertThatThrownBy(() -> limiter.check(1L))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("사용자별로 카운터가 독립적이다")
    void counts_per_user_independently() {
        ChatRateLimiter limiter = newLimiter(1);

        limiter.check(1L);
        assertThatCode(() -> limiter.check(2L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> limiter.check(1L))
                .isInstanceOf(RateLimitExceededException.class);
    }
}
