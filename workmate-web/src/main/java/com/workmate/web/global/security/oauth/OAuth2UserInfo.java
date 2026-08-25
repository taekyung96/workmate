package com.workmate.web.global.security.oauth;

import java.util.Map;

/**
 * 제공자별 프로필 응답 차이를 흡수하는 인터페이스 (F1-1).
 *
 * <p>구글은 프로필을 평평한 JSON 으로 주고, 네이버는 {@code {"response": {...}}} 로 한 겹 감싸서 준다.
 * 이 차이를 여기서 흡수해 상위 로직은 제공자를 몰라도 되게 한다.</p>
 */
public interface OAuth2UserInfo {

    /** 제공자가 발급한 고유 식별자 */
    String getProviderUserId();

    /** 제공자가 준 이메일 — 계정 연동 기준값. 동의하지 않았으면 null 일 수 있다 */
    String getEmail();

    /** 제공자가 준 이름 */
    String getName();

    /**
     * registrationId 에 맞는 구현체를 만든다.
     *
     * @param registrationId application.yml 의 registration 키 ('naver' · 'google')
     * @param attributes     DefaultOAuth2UserService 가 가져온 원본 속성
     * @return 제공자별 구현체
     * @throws IllegalArgumentException 지원하지 않는 제공자
     */
    static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "naver" -> new NaverOAuth2UserInfo(attributes);
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 제공자입니다: " + registrationId);
        };
    }
}
