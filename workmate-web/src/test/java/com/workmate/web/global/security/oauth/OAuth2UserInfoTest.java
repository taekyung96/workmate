package com.workmate.web.global.security.oauth;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 제공자별 프로필 파서 테스트 (F1-1).
 * 응답 구조가 제공자마다 달라(평평/한 겹/두 겹) 중첩을 푸는 지점이 버그가 나기 쉬운 곳이다.
 */
class OAuth2UserInfoTest {

    @Test
    void 네이버는_response_한_겹을_풀어_읽는다() {
        OAuth2UserInfo info = OAuth2UserInfo.of("naver", Map.of(
                "response", Map.of("id", "naver-1", "email", "user@example.com", "name", "홍길동")));

        assertThat(info.getProviderUserId()).isEqualTo("naver-1");
        assertThat(info.getEmail()).isEqualTo("user@example.com");
        assertThat(info.getName()).isEqualTo("홍길동");
    }

    @Test
    void 카카오는_식별자만_최상위이고_이메일_닉네임은_두_겹_아래에_있다() {
        OAuth2UserInfo info = OAuth2UserInfo.of("kakao", Map.of(
                "id", 1234567890L,
                "kakao_account", Map.of(
                        "email", "user@example.com",
                        "profile", Map.of("nickname", "홍길동"))));

        assertThat(info.getProviderUserId()).isEqualTo("1234567890");   // 숫자로 와도 문자열로 정규화
        assertThat(info.getEmail()).isEqualTo("user@example.com");
        assertThat(info.getName()).isEqualTo("홍길동");
    }

    @Test
    void 구글은_표준_클레임을_평평하게_읽는다() {
        OAuth2UserInfo info = OAuth2UserInfo.of("google", Map.of(
                "sub", "google-1", "email", "user@example.com", "name", "홍길동"));

        assertThat(info.getProviderUserId()).isEqualTo("google-1");
        assertThat(info.getEmail()).isEqualTo("user@example.com");
        assertThat(info.getName()).isEqualTo("홍길동");
    }

    @Test
    void 카카오가_이메일_동의를_받지_못하면_이메일이_null_이다() {
        // 동의항목을 선택 동의로 두면 사용자가 거부할 수 있다 — 상위에서 로그인 거부로 이어진다
        OAuth2UserInfo info = OAuth2UserInfo.of("kakao", Map.of(
                "id", 1L,
                "kakao_account", Map.of("profile", Map.of("nickname", "홍길동"))));

        assertThat(info.getProviderUserId()).isEqualTo("1");
        assertThat(info.getEmail()).isNull();
    }

    @Test
    void 응답_형식이_예상과_다르면_식별자가_null_이라_상위에서_걸러진다() {
        OAuth2UserInfo naver = OAuth2UserInfo.of("naver", Map.of("response", "예상과 다른 형식"));
        OAuth2UserInfo kakao = OAuth2UserInfo.of("kakao", Map.of("kakao_account", "예상과 다른 형식"));

        assertThat(naver.getProviderUserId()).isNull();
        assertThat(kakao.getEmail()).isNull();
    }

    @Test
    void 지원하지_않는_제공자는_거부한다() {
        assertThatThrownBy(() -> OAuth2UserInfo.of("facebook", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 소셜 제공자");
    }
}
