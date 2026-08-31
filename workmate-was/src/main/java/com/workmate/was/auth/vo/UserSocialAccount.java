package com.workmate.was.auth.vo;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 소셜 계정 연결 Entity (user_social_account 테이블 매핑, F1-1).
 *
 * <p>계정 하나에 제공자를 여러 개 매달기 위해 app_user 와 분리했다.
 * 구글로 가입한 뒤 같은 이메일의 네이버로 로그인하면 이 테이블에 행이 하나 더 생긴다.</p>
 */
@Entity
@Table(name = "user_social_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_seq")
    private Long socialSeq;

    /** 연결된 계정 (app_user.user_seq) */
    @Column(name = "user_seq", nullable = false)
    private Long userSeq;

    /** 'naver' | 'google' — 소문자 registrationId 를 그대로 쓴다 */
    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    /** 제공자가 발급한 고유 식별자. 제공자 안에서만 유일하므로 provider 와 묶어야 계정을 특정할 수 있다 */
    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserSocialAccount(Long userSeq, String provider, String providerUserId) {
        this.userSeq = userSeq;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.createdAt = LocalDateTime.now();
    }
}
