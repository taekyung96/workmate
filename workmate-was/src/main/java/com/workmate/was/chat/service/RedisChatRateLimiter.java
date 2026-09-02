package com.workmate.was.chat.service;

import com.workmate.was.global.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 기반 분당 요청 제한기 (F2-11) — 인스턴스가 여러 개여도 한도가 하나로 지켜진다.
 *
 * <p><b>키에 분(minute)을 넣는 이유.</b> {@code INCR} 과 {@code EXPIRE} 는 두 번의 명령이라
 * 그 사이에 프로세스가 죽으면 TTL 없는 키가 남는다. 키 이름에 분을 넣으면 그 키는 다음 분에
 * 다시 쓰이지 않으므로, TTL 이 빠진 키가 남더라도 <b>판정에 영향을 주지 않는다</b>.
 * (그래도 쓰레기가 쌓이지 않게 첫 증가에서 TTL 을 건다.)</p>
 *
 * <p><b>Redis 장애 시 통과시킨다(fail-open).</b> 레이트리밋은 남용 방지 장치이지 인증이 아니다.
 * Redis 가 죽었다고 채팅 전체를 막으면 장애 범위가 커진다. 대신 경고 로그를 남긴다.
 * 세션은 반대다 — 그쪽은 Redis 가 죽으면 로그인이 안 되는 게 맞다(fail-closed).</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.chat.rate-limit-store", havingValue = "redis", matchIfMissing = true)
public class RedisChatRateLimiter implements ChatRateLimiter {

    /** 키 네임스페이스 — WEB 의 세션 키(workmate:session:*)와 섞이지 않게 나눈다 */
    private static final String KEY_PREFIX = "workmate:ratelimit:";

    /** 윈도우(1분)가 지나도 잠시 남겨 두는 여유. 시계 오차로 키가 조기 삭제되는 것을 막는다 */
    private static final Duration KEY_TTL = Duration.ofSeconds(90);

    /** 분당 허용 요청 수 */
    @Value("${app.chat.rate-limit-per-minute:20}")
    private int limitPerMinute;

    private final StringRedisTemplate redisTemplate;

    public RedisChatRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void check(Long userSeq) {
        long currentMinute = System.currentTimeMillis() / 60_000L;
        String key = KEY_PREFIX + userSeq + ":" + currentMinute;

        Long count;
        try {
            // INCR 은 그 자체로 원자적이라 여러 인스턴스가 동시에 불러도 카운터가 어긋나지 않는다
            count = redisTemplate.opsForValue().increment(key);
            // 첫 증가에서만 TTL 을 건다. 매번 걸면 윈도우가 계속 연장돼 키가 안 사라진다
            if (count != null && count == 1L) {
                redisTemplate.expire(key, KEY_TTL);
            }
        } catch (RuntimeException e) {
            // fail-open — 위 javadoc 참고
            log.warn("레이트리밋 조회 실패, 요청을 통과시킨다 - userSeq: {}", userSeq, e);
            return;
        }

        if (count != null && count > limitPerMinute) {
            throw new RateLimitExceededException("요청이 많습니다. 잠시 후 이용해주세요.");
        }
    }
}
