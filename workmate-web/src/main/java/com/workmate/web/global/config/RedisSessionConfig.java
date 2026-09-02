package com.workmate.web.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;

/**
 * Redis 세션 저장소 설정 (분산 상태 — ADR-0006).
 *
 * <p><b>왜 SessionRegistryImpl 을 쓰면 안 되는가.</b> 기본 구현은 세션 목록을 JVM 힙에 둔다.
 * 인스턴스가 2개가 되면 각자 "이 사용자의 세션 1개"를 따로 세므로 총 2개가 살아남는다 —
 * F1-08(중복 로그인 차단)이 <b>에러 없이</b> 무력화된다.
 * {@link SpringSessionBackedSessionRegistry} 는 Redis 에 저장된 세션을 직접 조회하므로
 * 인스턴스가 몇 개든 판단이 하나다.</p>
 *
 * <p>이 빈이 동작하려면 {@code spring.session.redis.repository-type: indexed} 가 필요하다 —
 * 사용자 이름으로 세션을 찾는 인덱스가 그 설정에서만 만들어진다.</p>
 */
@Configuration
public class RedisSessionConfig {

    /**
     * Spring Session(Redis) 을 근거로 동작하는 세션 레지스트리.
     *
     * @param sessionRepository Spring Session 이 자동 등록하는 인덱스 조회 가능 저장소
     * @return 인스턴스 간에 공유되는 SessionRegistry
     */
    @Bean
    public <S extends Session> SessionRegistry sessionRegistry(
            FindByIndexNameSessionRepository<S> sessionRepository) {
        return new SpringSessionBackedSessionRegistry<>(sessionRepository);
    }
}
