package com.workmate.web.global.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 소셜 로그인 성공 처리 (F1-1).
 *
 * <p>이메일 로그인({@code LoginSuccessHandler})은 200 JSON 을 돌려주지만, 소셜 로그인은
 * XHR 이 아니라 제공자에서 돌아오는 <b>전체 페이지 이동</b>이라 JSON 을 줄 곳이 없다.
 * 그래서 SPA 화면으로 리다이렉트한다.</p>
 */
@Slf4j
@Component
public class SocialLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final String successRedirectUri;

    public SocialLoginSuccessHandler(@Value("${app.oauth2.success-redirect-uri}") String successRedirectUri) {
        this.successRedirectUri = successRedirectUri;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        response.sendRedirect(successRedirectUri);
    }
}
