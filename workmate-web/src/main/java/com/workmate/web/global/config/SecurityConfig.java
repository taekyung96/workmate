package com.workmate.web.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workmate.web.global.response.ApiResponse;
import com.workmate.web.global.security.CsrfCookieFilter;
import com.workmate.web.global.security.LoginFailureHandler;
import com.workmate.web.global.security.LoginSuccessHandler;
import com.workmate.web.global.security.WasAuthenticationProvider;
import com.workmate.web.global.security.oauth.SocialLoginFailureHandler;
import com.workmate.web.global.security.oauth.SocialLoginSuccessHandler;
import com.workmate.web.global.security.oauth.WasOAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * WEB 보안 설정 (SPA 대응 — F1-05·08·10).
 *
 * <p>v2의 Thymeleaf 방식(리다이렉트)에서 <b>SPA용 JSON 계약</b>으로 전환한다.
 * <ul>
 *   <li>미인증 접근 → 302 리다이렉트 대신 <b>401 JSON</b> (SPA 라우터가 /login 으로 유도)</li>
 *   <li>로그인 성공/실패 → 리다이렉트 대신 <b>JSON</b> (성공 시 사용자 정보 / 실패 시 사유)</li>
 *   <li>CSRF → 쿠키 기반(XSRF-TOKEN). SPA가 쿠키를 읽어 X-XSRF-TOKEN 헤더로 전송</li>
 *   <li>정적 자원·SPA 라우트는 permitAll, 화면 접근 제어는 프론트 가드 + API 레벨 권한이 담당</li>
 * </ul>
 * 세션 동시성 1개(중복 로그인 방지, F1-08)와 WAS 인증 위임은 v2 그대로 유지한다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final WasAuthenticationProvider wasAuthenticationProvider;
    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;
    private final WasOAuth2UserService wasOAuth2UserService;
    private final SocialLoginSuccessHandler socialLoginSuccessHandler;
    private final SocialLoginFailureHandler socialLoginFailureHandler;
    private final ObjectMapper objectMapper;
    private final SessionRegistry sessionRegistry;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(wasAuthenticationProvider)
            .authorizeHttpRequests(auth -> auth
                // 인증 없이 허용: 로그인 API
                .requestMatchers("/api/auth/login").permitAll()
                // 소셜 로그인 시작·콜백 경로 (F1-1) — 로그인 전이므로 열어둬야 한다
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                // 회원가입은 관리자 전용으로 전환 (F1-1) — 일반 사용자는 소셜로만 가입한다
                .requestMatchers("/api/auth/signup").hasRole("ADMIN")
                // 관리자 API는 ROLE_ADMIN 만 (F6-04 — URL 직접 접근도 서버 차단)
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // 그 외 모든 API는 세션 인증 필요 (/api/auth/me 포함 → 미로그인 시 401)
                .requestMatchers("/api/**").authenticated()
                // 정적 자원·SPA 라우트(index.html 등)는 허용 — 화면 제어는 프론트 가드가 담당
                .anyRequest().permitAll())
            // 미인증 접근: 401 JSON
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authEx) ->
                    writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요합니다."))
                // 권한 부족: 403 JSON (F6.3)
                .accessDeniedHandler((request, response, denied) ->
                    writeJson(response, HttpServletResponse.SC_FORBIDDEN, "접근 권한이 없습니다.")))
            .formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")   // POST /api/auth/login 은 Security 가 처리
                .usernameParameter("email")
                .successHandler(loginSuccessHandler)      // 성공 → 200 JSON + 사용자 정보
                .failureHandler(loginFailureHandler))     // 실패 → 401 JSON + 사유
            // 소셜 로그인 (F1-1) — 토큰 교환은 Spring 이, 계정 조회·생성은 WasOAuth2UserService 가 WAS 에 위임
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.userService(wasOAuth2UserService))
                .successHandler(socialLoginSuccessHandler)   // 전체 페이지 이동이라 JSON 대신 리다이렉트
                .failureHandler(socialLoginFailureHandler))
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, authentication) ->
                    writeJson(response, HttpServletResponse.SC_OK, "success")))
            // CSRF: 쿠키 기반(XSRF-TOKEN). SPA가 쿠키를 읽어 X-XSRF-TOKEN 헤더로 전송
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            // 매 응답에 CSRF 토큰 쿠키가 실리도록 토큰을 강제 렌더링
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
            .sessionManagement(session -> session
                .maximumSessions(1)                      // 중복 로그인 방지 (F1-08)
                .maxSessionsPreventsLogin(false)          // 새 로그인 허용, 기존 세션 만료
                // Redis 를 보는 레지스트리다 (RedisSessionConfig) — 인스턴스가 늘어도 판단이 하나다
                .sessionRegistry(sessionRegistry));
        return http.build();
    }

    /**
     * WEB 자체 생성 JSON 응답을 공통 포맷(ApiResponse)으로 내려준다.
     *
     * @param response HTTP 응답
     * @param status   HTTP 상태코드
     * @param message  결과 메시지 (success 이면 "success")
     */
    private void writeJson(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<Void> body = (status == HttpServletResponse.SC_OK)
            ? ApiResponse.success()
            : ApiResponse.error(message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    /** 세션 만료 이벤트를 세션 레지스트리에 전달 — 동시성 제어에 필수 */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
