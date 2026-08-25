package com.workmate.web.global.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 소셜 로그인 실패 처리 (F1-1).
 *
 * <p>실패해도 세션은 만들지 않고, 사유를 쿼리 파라미터에 실어 SPA 로그인 화면으로 돌려보낸다.
 * 사용자가 동의 화면에서 취소한 경우도 여기로 들어오므로 오류처럼 보이지 않게 문구를 구분한다.</p>
 */
@Slf4j
@Component
public class SocialLoginFailureHandler implements AuthenticationFailureHandler {

    /** 사용자가 제공자 동의 화면에서 취소했을 때 제공자가 실어 보내는 표준 오류 코드 */
    private static final String ACCESS_DENIED = "access_denied";

    private final String failureRedirectUri;

    public SocialLoginFailureHandler(@Value("${app.oauth2.failure-redirect-uri}") String failureRedirectUri) {
        this.failureRedirectUri = failureRedirectUri;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        if (isUserCancelled(exception)) {
            response.sendRedirect(failureRedirectUri);
            return;
        }

        log.warn("소셜 로그인 실패 - {}", exception.getMessage());
        String reason = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
        String separator = failureRedirectUri.contains("?") ? "&" : "?";
        response.sendRedirect(failureRedirectUri + separator + "error=" + reason);
    }

    /** 동의 취소는 오류가 아니라 사용자의 선택이므로 안내 문구 없이 로그인 화면으로만 돌려보낸다 */
    private boolean isUserCancelled(AuthenticationException exception) {
        return exception instanceof OAuth2AuthenticationException oauthEx
                && ACCESS_DENIED.equals(oauthEx.getError().getErrorCode());
    }
}
