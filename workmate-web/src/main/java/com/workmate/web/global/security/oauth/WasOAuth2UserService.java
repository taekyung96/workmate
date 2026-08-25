package com.workmate.web.global.security.oauth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

/**
 * 소셜 프로필을 WAS 계정으로 바꾸는 UserService (F1-1).
 *
 * <p>토큰 교환·프로필 조회까지는 부모({@link DefaultOAuth2UserService})가 처리하고,
 * 여기서는 받은 프로필을 WAS 로 넘겨 계정을 조회·생성한다.
 * 이메일 로그인의 {@code WasAuthenticationProvider} 와 같은 역할이며,
 * <b>WEB 은 이 경로에서도 DB 를 직접 보지 않는다.</b></p>
 */
@Slf4j
@Service
public class WasOAuth2UserService extends DefaultOAuth2UserService {

    private static final String ERROR_CODE = "social_login_failed";

    private final RestClient wasRestClient;

    public WasOAuth2UserService(RestClient wasRestClient) {
        this.wasRestClient = wasRestClient;
    }

    /**
     * 제공자 프로필을 WAS 계정으로 변환한다.
     *
     * @param userRequest 액세스 토큰과 ClientRegistration 을 담은 요청
     * @return 세션 principal 로 쓰일 {@link SocialLoginUser}
     * @throws OAuth2AuthenticationException 프로필이 불완전하거나 WAS 가 로그인을 거부한 경우
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo info;
        try {
            info = OAuth2UserInfo.of(provider, oAuth2User.getAttributes());
        } catch (IllegalArgumentException e) {
            throw fail(e.getMessage());
        }

        if (info.getProviderUserId() == null) {
            // 제공자 응답 형식이 예상과 다른 경우 — 원본을 남겨야 원인 추적이 된다
            log.error("소셜 프로필 파싱 실패 - provider: {}, attributes: {}", provider, oAuth2User.getAttributes());
            throw fail("소셜 로그인 응답을 처리하지 못했습니다.");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("provider", provider);
        body.put("providerUserId", info.getProviderUserId());
        body.put("email", info.getEmail());
        body.put("name", info.getName());

        SocialLoginApiResponse response = callWas(body, provider);
        SocialLoginApiResponse.Result r = response.getResult();
        log.info("소셜 로그인 성공 - provider: {}, userSeq: {}", provider, r.getUserSeq());
        return new SocialLoginUser(r.getUserSeq(), r.getUserName(), r.getRole(), oAuth2User.getAttributes());
    }

    /**
     * WAS 소셜 로그인 API 를 호출한다.
     * 주입되는 RestClient 가 4xx 를 예외로 던지든 무동작 상태 핸들러로 통과시키든 동일하게 처리한다
     * ({@code WasAuthenticationProvider} 와 같은 이유).
     */
    private SocialLoginApiResponse callWas(Map<String, Object> body, String provider) {
        ResponseEntity<SocialLoginApiResponse> entity;
        try {
            entity = wasRestClient.post()
                    .uri("/api/v1/auth/social-login")
                    .body(body)
                    .retrieve()
                    .toEntity(SocialLoginApiResponse.class);
        } catch (HttpClientErrorException e) {
            throw fail(messageOf(e.getResponseBodyAs(SocialLoginApiResponse.class)));
        } catch (RestClientException e) {
            log.error("WAS 소셜 로그인 호출 실패 - provider: {}", provider, e);
            throw fail("일시적인 오류로 로그인하지 못했습니다. 잠시 후 다시 시도해주세요.");
        }

        SocialLoginApiResponse b = entity.getBody();
        if (entity.getStatusCode() == HttpStatus.OK && b != null && b.isSuccess() && b.getResult() != null) {
            return b;
        }
        throw fail(messageOf(b));
    }

    private String messageOf(SocialLoginApiResponse body) {
        return (body != null && body.getMessage() != null) ? body.getMessage() : "소셜 로그인에 실패했습니다.";
    }

    /** 실패 핸들러가 사유를 읽을 수 있도록 OAuth2Error 에 메시지를 담아 던진다 */
    private OAuth2AuthenticationException fail(String message) {
        return new OAuth2AuthenticationException(new OAuth2Error(ERROR_CODE, message, null), message);
    }

    /** WAS ApiResponse&lt;LoginResponseVo&gt; 역직렬화용 내부 DTO */
    @Getter
    @Setter
    @NoArgsConstructor
    static class SocialLoginApiResponse {
        private boolean success;
        private String message;
        private Result result;

        @Getter
        @Setter
        @NoArgsConstructor
        static class Result {
            private Long userSeq;
            private String userName;
            private String role;
        }
    }
}
