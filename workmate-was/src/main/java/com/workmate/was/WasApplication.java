package com.workmate.was;

import com.workmate.was.auth.config.AuthLockProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Workmate WAS 레이어 진입점.
 * 비즈니스 로직·DB 접근·Spring AI 연동을 담당한다. (포트 8081)
 */
@SpringBootApplication
@EnableConfigurationProperties(AuthLockProperties.class)
public class WasApplication {

    public static void main(String[] args) {
        SpringApplication.run(WasApplication.class, args);
    }
}
