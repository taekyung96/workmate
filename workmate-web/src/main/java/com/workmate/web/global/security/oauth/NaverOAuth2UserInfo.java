package com.workmate.web.global.security.oauth;

import java.util.Map;

/**
 * 네이버 프로필 파서 (F1-1).
 * 응답이 {@code {"resultcode": "00", "message": "success", "response": {"id": ..., "email": ..., "name": ...}}}
 * 형태라 실제 값은 response 아래에 들어 있다.
 */
public class NaverOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> response;

    @SuppressWarnings("unchecked")
    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        Object raw = attributes.get("response");
        // 응답 형식이 바뀌면 여기서 걸러 상위에서 로그인 실패로 처리하게 한다
        this.response = (raw instanceof Map) ? (Map<String, Object>) raw : Map.of();
    }

    @Override
    public String getProviderUserId() {
        return asText(response.get("id"));
    }

    @Override
    public String getEmail() {
        return asText(response.get("email"));
    }

    @Override
    public String getName() {
        return asText(response.get("name"));
    }

    private String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
