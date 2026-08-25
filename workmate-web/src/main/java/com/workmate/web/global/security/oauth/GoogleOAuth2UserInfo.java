package com.workmate.web.global.security.oauth;

import java.util.Map;

/**
 * 구글 프로필 파서 (F1-1).
 * 구글은 OIDC 표준 클레임을 평평하게 내려준다 — sub·email·name 을 그대로 읽으면 된다.
 */
public class GoogleOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderUserId() {
        return asText(attributes.get("sub"));
    }

    @Override
    public String getEmail() {
        return asText(attributes.get("email"));
    }

    @Override
    public String getName() {
        return asText(attributes.get("name"));
    }

    private String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
