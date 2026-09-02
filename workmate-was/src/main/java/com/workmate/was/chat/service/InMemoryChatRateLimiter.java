package com.workmate.was.chat.service;

import com.workmate.was.global.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 인메모리 고정 윈도우(분 단위) 레이트리미터 — 단일 인스턴스 전용.
 *
 * <p><b>인스턴스가 2개 이상이면 한도가 인스턴스 수만큼 곱해진다.</b> 각 JVM 이 자기 카운터만
 * 보기 때문이다. 분산 환경에서는 {@link RedisChatRateLimiter} 를 써야 한다.</p>
 *
 * <p>또한 이 구현은 <b>{@code windows} 맵의 키를 지우지 않는다</b> — 분이 바뀌면 값만 교체되고
 * {@code userSeq} 키는 남는다. 단일 인스턴스·소규모 데모 기준으로 감수한 것이며,
 * Redis 구현은 {@code EXPIRE} 로 이 문제가 없다.</p>
 */
@Component
@ConditionalOnProperty(name = "app.chat.rate-limit-store", havingValue = "memory")
public class InMemoryChatRateLimiter implements ChatRateLimiter {

    /** 분당 허용 요청 수 */
    @Value("${app.chat.rate-limit-per-minute:20}")
    private int limitPerMinute;

    private final ConcurrentMap<Long, Window> windows = new ConcurrentHashMap<>();

    @Override
    public void check(Long userSeq) {
        long currentMinute = System.currentTimeMillis() / 60_000L;
        // compute 는 키 단위로 원자적이라 카운터 증가가 스레드 안전하다
        Window window = windows.compute(userSeq, (key, existing) -> {
            if (existing == null || existing.minute != currentMinute) {
                return new Window(currentMinute);
            }
            existing.count++;
            return existing;
        });
        if (window.count > limitPerMinute) {
            throw new RateLimitExceededException("요청이 많습니다. 잠시 후 이용해주세요.");
        }
    }

    /** 분 단위 카운팅 윈도우 */
    private static final class Window {
        private final long minute;
        private int count;

        private Window(long minute) {
            this.minute = minute;
            this.count = 1;
        }
    }
}
