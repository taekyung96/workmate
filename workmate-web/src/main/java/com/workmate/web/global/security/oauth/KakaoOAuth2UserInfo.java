package com.workmate.web.global.security.oauth;

import java.util.Map;

/**
 * 카카오 프로필 파서 (F1-1).
 *
 * <p>응답이 {@code {"id": 12345, "kakao_account": {"email": ..., "profile": {"nickname": ...}}}}
 * 형태다. 식별자만 최상위에 있고 이메일·닉네임은 두 단계 아래에 들어 있어 중첩을 풀어야 한다.</p>
 *
 * <p>이메일은 동의항목을 선택 동의로 두면 사용자가 거부할 수 있어 null 로 올 수 있다.
 * 그 경우 상위에서 로그인을 거부한다.</p>
 */
public class KakaoOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;
    private final Map<String, Object> account;
    private final Map<String, Object> profile;

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.account = nested(attributes, "kakao_account");
        this.profile = nested(this.account, "profile");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nested(Map<String, Object> source, String key) {
        Object raw = source.get(key);
        // 응답 형식이 바뀌면 빈 맵으로 떨어뜨려 상위에서 로그인 실패로 처리하게 한다
        return (raw instanceof Map) ? (Map<String, Object>) raw : Map.of();
    }

    @Override
    public String getProviderUserId() {
        return asText(attributes.get("id"));
    }

    @Override
    public String getEmail() {
        return asText(account.get("email"));
    }

    @Override
    public String getName() {
        return asText(profile.get("nickname"));
    }

    private String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
