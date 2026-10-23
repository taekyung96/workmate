package com.workmate.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Workmate WEB 레이어 진입점.
 * Vue SPA 정적 서빙·세션 인증·WAS 프록시·SSE 중계를 담당하는 얇은 BFF 다. (포트 8080)
 */
@SpringBootApplication
public class WebApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }
}
