package com.workmate.was.auth.dao;

import com.workmate.was.auth.vo.UserSocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 소셜 계정 연결 조회/저장 Repository (F1-1) */
public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {

    /** 제공자 + 제공자 식별자로 이미 연결된 계정을 찾는다 (재로그인 판정) */
    Optional<UserSocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
}
