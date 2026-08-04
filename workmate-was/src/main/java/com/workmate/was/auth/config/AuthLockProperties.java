package com.workmate.was.auth.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 로그인 잠금 정책 프로퍼티 바인딩 (application.yml 의 app.auth.*). F1-06.
 *
 * <p>연속 로그인 실패 허용 횟수와 잠금 유지 시간을 한 곳에서 관리한다.
 * AuthServiceImpl(잠금 판정)·AdminServiceImpl(잠금 표시)·LoginFailRecorder(실패 누적)가
 * 이 값을 공유해, 정책 값이 여러 파일에 하드코딩·중복되지 않게 한다.</p>
 */
@Getter
@ConfigurationProperties(prefix = "app.auth")
public class AuthLockProperties {

    /** 계정 잠금까지 허용하는 연속 로그인 실패 횟수 (기본 5) */
    private final int maxFailCount;

    /** 잠금 유지 시간(분) — 경과 시 자동 해제 (기본 60) */
    private final long lockMinutes;

    /**
     * @param maxFailCount 잠금 임계 실패 횟수 (미설정 시 5)
     * @param lockMinutes  잠금 유지 시간(분) (미설정 시 60)
     */
    public AuthLockProperties(
            @DefaultValue("5") int maxFailCount,
            @DefaultValue("60") long lockMinutes) {
        this.maxFailCount = maxFailCount;
        this.lockMinutes = lockMinutes;
    }
}
