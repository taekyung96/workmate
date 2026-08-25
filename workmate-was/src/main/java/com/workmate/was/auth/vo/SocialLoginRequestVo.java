package com.workmate.was.auth.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 소셜 로그인 요청 VO (F1-1).
 * WEB 이 OAuth 제공자에게서 받아온 프로필을 그대로 넘긴다 — 토큰 교환은 WEB 이 이미 끝낸 상태다.
 */
@Getter
@Setter
@NoArgsConstructor
public class SocialLoginRequestVo {

    /** 'naver' | 'google' */
    private String provider;

    /** 제공자가 발급한 고유 식별자 */
    private String providerUserId;

    /** 제공자가 준 이메일 — 계정 연동 기준값 */
    private String email;

    /** 제공자가 준 이름 — 신규 가입 시 user_name 으로 쓴다 */
    private String name;
}
