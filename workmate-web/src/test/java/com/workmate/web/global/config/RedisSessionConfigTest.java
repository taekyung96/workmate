package com.workmate.web.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 세션 설정 검증.
 *
 * <p>여기서 막고 싶은 회귀는 하나다 — 누군가 {@code SessionRegistryImpl} 을 다시 등록하면
 * 앱은 정상 기동하지만 <b>중복 로그인 차단이 조용히 깨진다</b>. 타입을 단언해 그걸 잡는다.</p>
 */
@SpringBootTest
class RedisSessionConfigTest {

    @Autowired
    private SessionRegistry sessionRegistry;

    @Test
    @DisplayName("SessionRegistry 는 Redis 를 보는 구현이어야 한다 (인메모리면 F1-08 이 조용히 깨진다)")
    void sessionRegistry_is_backed_by_spring_session() {
        assertThat(sessionRegistry).isInstanceOf(SpringSessionBackedSessionRegistry.class);
    }
}
