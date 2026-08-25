package com.workmate.web.global.security.oauth;

import com.workmate.web.global.security.LoginUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * 소셜 로그인 세션 principal (F1-1).
 *
 * <p>{@link LoginUser} 를 상속해 이메일 로그인과 동일한 타입으로 세션에 들어간다.
 * 덕분에 {@code @AuthenticationPrincipal LoginUser} 로 받는 기존 코드가 그대로 동작하고,
 * SessionRegistry 의 userSeq 값 동등성(F1-08 중복 로그인 차단)도 그대로 유지된다.</p>
 *
 * <p>동시에 {@link OAuth2User} 를 구현해 Spring Security 의 oauth2Login 계약도 만족시킨다.</p>
 */
public class SocialLoginUser extends LoginUser implements OAuth2User {

    /** 제공자 원본 속성 — 디버깅·확장용으로만 보관하며 세션 판단에는 쓰지 않는다 */
    private final transient Map<String, Object> attributes;

    public SocialLoginUser(Long userSeq, String userName, String role, Map<String, Object> attributes) {
        super(userSeq, userName, role);
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(getRole()));
    }

    /** OAuth2User 규약상의 이름 — 세션 식별은 userSeq 로 하므로 그 값을 그대로 쓴다 */
    @Override
    public String getName() {
        return String.valueOf(getUserSeq());
    }
}
