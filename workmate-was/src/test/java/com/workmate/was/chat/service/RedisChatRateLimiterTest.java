package com.workmate.was.chat.service;

import com.workmate.was.global.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RedisChatRateLimiter 통합 테스트 (F2-11).
 *
 * <p>실제 Redis 에 붙어 검증한다 — 인메모리 가짜로는 INCR 의 원자성을 증명할 수 없고,
 * 이 클래스의 존재 이유가 바로 그 원자성이기 때문이다.</p>
 *
 * <p><b>compose 의 Redis 를 쓴다</b>(`docker compose up -d redis`). Testcontainers 를 쓰지 않는 이유는
 * Gradle 테스트 JVM 이 Windows 프로세스인데 Docker 데몬이 WSL2 안에 있어 데몬을 찾지 못하기 때문이다.
 * 기존 DB 통합 테스트가 WSL2 의 PostgreSQL 에 {@code localhost:5432} 로 붙는 것과 같은 방식이고,
 * CI 의 service 컨테이너도 같은 주소라 로컬·CI 가 같은 코드로 돈다.</p>
 */
class RedisChatRateLimiterTest {

    /** compose(로컬) 와 CI service 컨테이너가 모두 이 주소로 뜬다 */
    private static final String REDIS_HOST =
            System.getProperty("redis.host", System.getenv().getOrDefault("SPRING_DATA_REDIS_HOST", "localhost"));
    private static final int REDIS_PORT = Integer.parseInt(
            System.getProperty("redis.port", System.getenv().getOrDefault("SPRING_DATA_REDIS_PORT", "6379")));

    private StringRedisTemplate redisTemplate;

    /**
     * Redis 가 떠 있는지 먼저 확인한다.
     * 안 떠 있으면 <b>조용히 건너뛰지 않고 실패시킨다</b> — 이 테스트가 스킵되면
     * 분산 레이트리밋이 깨져도 아무도 모르게 되기 때문이다.
     */
    @BeforeEach
    void connectAndFlush() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(REDIS_HOST, REDIS_PORT);
        factory.afterPropertiesSet();
        this.redisTemplate = new StringRedisTemplate(factory);
        this.redisTemplate.afterPropertiesSet();
        try {
            redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Redis(" + REDIS_HOST + ":" + REDIS_PORT + ")에 붙지 못했다. "
                            + "`docker compose up -d redis` 로 띄우고 다시 실행하라.", e);
        }
    }

    /** 같은 Redis 를 보는 리미터를 만든다. 여러 번 부르면 "인스턴스 여러 개"를 흉내낼 수 있다 */
    private RedisChatRateLimiter newLimiter(int limit) {
        RedisChatRateLimiter limiter = new RedisChatRateLimiter(redisTemplate);
        ReflectionTestUtils.setField(limiter, "limitPerMinute", limit);
        return limiter;
    }

    @Test
    @DisplayName("한도까지는 통과하고 초과하면 429 예외")
    void allows_up_to_limit_then_throws() {
        RedisChatRateLimiter limiter = newLimiter(3);

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
        RedisChatRateLimiter limiter = newLimiter(1);

        limiter.check(10L);
        assertThatCode(() -> limiter.check(20L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> limiter.check(10L))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("인스턴스가 2개여도 한도가 곱해지지 않는다 — 이 클래스의 존재 이유")
    void limit_is_shared_across_instances() {
        // 같은 Redis 를 보는 리미터 2개 = 인스턴스 2개와 같은 상황
        RedisChatRateLimiter instanceA = newLimiter(3);
        RedisChatRateLimiter instanceB = newLimiter(3);

        instanceA.check(30L);
        instanceB.check(30L);
        instanceA.check(30L);

        // 4번째는 어느 인스턴스로 가든 막혀야 한다
        assertThatThrownBy(() -> instanceB.check(30L))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("동시에 들어와도 정확히 한도만큼만 통과한다 — INCR 원자성")
    void concurrent_requests_respect_limit_exactly() throws Exception {
        int limit = 10;
        int threads = 50;
        RedisChatRateLimiter limiter = newLimiter(limit);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger passed = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    limiter.check(40L);
                    passed.incrementAndGet();
                } catch (RateLimitExceededException expected) {
                    // 한도 초과는 정상
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        startGate.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        // 인메모리 구현이라면 여기서 10을 넘길 수 있다. Redis INCR 은 정확히 10이어야 한다
        assertThat(passed.get()).isEqualTo(limit);
    }

    @Test
    @DisplayName("카운터 키에 TTL 이 걸린다 — 인메모리 구현의 키 누수를 해소한다")
    void counter_key_has_ttl() {
        RedisChatRateLimiter limiter = newLimiter(5);
        limiter.check(50L);

        String key = redisTemplate.keys("workmate:ratelimit:50:*").iterator().next();
        Long ttl = redisTemplate.getExpire(key);

        assertThat(ttl).isNotNull().isPositive();
        assertThat(Duration.ofSeconds(ttl)).isLessThanOrEqualTo(Duration.ofMinutes(2));
    }
}
