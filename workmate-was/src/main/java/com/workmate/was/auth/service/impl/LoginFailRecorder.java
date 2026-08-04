package com.workmate.was.auth.service.impl;

import com.workmate.was.auth.config.AuthLockProperties;
import com.workmate.was.auth.dao.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 실패 카운트를 독립 트랜잭션으로 커밋하는 헬퍼 (F1-06).
 *
 * <p>login() 은 자격 불일치 시 예외를 던져 전체 트랜잭션이 롤백된다. 그래도 실패 기록만은
 * 남아야 계정 잠금이 성립하므로, 이 작업을 REQUIRES_NEW(별도 트랜잭션)로 분리해 독립 커밋한다.
 * AuthServiceImpl 내부 메서드로 두면 Spring 프록시 self-invocation 함정으로 REQUIRES_NEW 가
 * 무시되므로(같은 빈 내부 호출은 프록시를 거치지 않음), 반드시 별도 빈으로 분리해 프록시를 경유시킨다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginFailRecorder {

    private final UserRepository userRepository;
    private final AuthLockProperties authLockProperties;

    /**
     * 실패 카운트를 1 증가시켜 별도 트랜잭션으로 커밋한다.
     * 잠금 임계 횟수(app.auth.max-fail-count)는 엔티티에 넘겨 잠금 여부를 판정하게 한다.
     *
     * @param userSeq 실패한 사용자 식별자
     * @return 증가 후 누적 실패 횟수 (사용자 미존재 시 0)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recordFailedAttempt(Long userSeq) {
        return userRepository.findById(userSeq).map(user -> {
            user.increaseFailCount(authLockProperties.getMaxFailCount());
            userRepository.save(user);
            return user.getLoginFailCount();
        }).orElse(0);
    }
}
