package com.workmate.was.auth.service.impl;

import com.workmate.was.auth.config.AuthLockProperties;
import com.workmate.was.auth.vo.User;
import com.workmate.was.auth.dao.UserRepository;
import com.workmate.was.auth.vo.SignupRequestVo;
import com.workmate.was.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.Spy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.workmate.was.auth.dao.UserSocialAccountRepository userSocialAccountRepository;

    // 실제 BCrypt 로 해시가 만들어지는지 검증하기 위해 Spy 실객체 사용
    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 실패 카운트 증가는 별도 트랜잭션 빈(REQUIRES_NEW)에 위임한다 — 여기서는 호출 여부만 검증하고
    // 실제 DB 커밋(롤백 생존)은 AuthServiceImplIntegrationTest 가 담당한다.
    @Mock
    private LoginFailRecorder loginFailRecorder;

    // 잠금 정책은 실제 기본값(실패 5회·60분)을 그대로 쓰면 되므로 Spy 실객체로 주입한다
    @Spy
    private AuthLockProperties authLockProperties = new AuthLockProperties(5, 60);

    @InjectMocks
    private AuthServiceImpl authService;

    private SignupRequestVo signupRequest(String email) {
        SignupRequestVo request = new SignupRequestVo();
        request.setEmail(email);
        request.setPassword("abcd123!");
        request.setUserName("김태경");
        request.setPhone("010-1234-5678");
        return request;
    }

    @Test
    void 가입_시_이메일은_정규화되고_비밀번호는_BCrypt_해시로_저장된다() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);

        authService.signup(signupRequest("  User@Example.COM "));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("user@example.com");          // F1-01a
        assertThat(saved.getPassword()).startsWith("$2a$");                   // BCrypt (F1-02)
        assertThat(saved.getPhone()).isEqualTo("01012345678");                // F9-09 숫자만
    }

    @Test
    void 정규화된_이메일_기준으로_중복이면_거부한다() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(signupRequest("USER@example.com")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 사용 중인 이메일");
    }

    // ===== 로그인 (F1-05~07) =====

    private User savedUser() {
        return User.builder()
                .email("user@example.com")
                .password(new BCryptPasswordEncoder().encode("abcd123!"))
                .userName("김태경")
                .build();
    }

    @Test
    void 자격이_맞으면_사용자_정보를_반환하고_실패_카운트를_초기화한다() {
        User user = savedUser();
        user.increaseFailCount(5); // 기존 실패 1회 있던 상태
        when(userRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(user));

        com.workmate.was.auth.vo.LoginRequestVo request = new com.workmate.was.auth.vo.LoginRequestVo();
        request.setEmail("USER@example.com"); // 대문자 입력도 성공해야 한다 (F1-01a)
        request.setPassword("abcd123!");

        com.workmate.was.auth.vo.LoginResponseVo response = authService.login(request);

        assertThat(response.getUserName()).isEqualTo("김태경");
        assertThat(user.getLoginFailCount()).isZero();
    }

    @Test
    void 비밀번호가_틀리면_실패_기록을_남기고_401_예외가_난다() {
        User user = savedUser();
        when(userRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(user));

        com.workmate.was.auth.vo.LoginRequestVo request = new com.workmate.was.auth.vo.LoginRequestVo();
        request.setEmail("user@example.com");
        request.setPassword("wrong-pw1!");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(com.workmate.was.global.exception.AuthenticationFailedException.class)
                .hasMessageContaining("이메일 또는 비밀번호"); // 어느 쪽인지 미노출 (F1.3)
        // 실패 기록은 별도 트랜잭션 빈에 위임된다 — 호출 여부만 검증 (실 커밋은 통합테스트 담당)
        verify(loginFailRecorder).recordFailedAttempt(user.getUserSeq());
    }

    @Test
    void 잠긴_계정은_올바른_비밀번호여도_남은_시간을_안내하며_거부한다() {
        User user = savedUser();
        for (int i = 0; i < 5; i++) {
            user.increaseFailCount(5); // 5회 누적 → lockedAt 설정 (F1-06)
        }
        assertThat(user.getLockedAt()).isNotNull();
        when(userRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(user));

        // 잠금 상태에서는 올바른 비밀번호여도 409 + 남은 시간 안내 (F1-07)
        com.workmate.was.auth.vo.LoginRequestVo correct = new com.workmate.was.auth.vo.LoginRequestVo();
        correct.setEmail("user@example.com");
        correct.setPassword("abcd123!");
        assertThatThrownBy(() -> authService.login(correct))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("분 후 다시 시도");
    }

    @Test
    void 존재하지_않는_이메일도_같은_메시지로_401_이다() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(java.util.Optional.empty());

        com.workmate.was.auth.vo.LoginRequestVo request = new com.workmate.was.auth.vo.LoginRequestVo();
        request.setEmail("ghost@example.com");
        request.setPassword("abcd123!");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(com.workmate.was.global.exception.AuthenticationFailedException.class)
                .hasMessageContaining("이메일 또는 비밀번호");
    }

    // ── 소셜 로그인 (F1-1) ───────────────────────────────────────────────

    private com.workmate.was.auth.vo.SocialLoginRequestVo socialRequest(String email) {
        com.workmate.was.auth.vo.SocialLoginRequestVo request = new com.workmate.was.auth.vo.SocialLoginRequestVo();
        request.setProvider("naver");
        request.setProviderUserId("naver-1234");
        request.setEmail(email);
        request.setName("홍길동");
        return request;
    }

    /** userSeq 는 JPA 가 채우는 값이라 테스트에서는 직접 심는다 */
    private User userWithSeq(String email, long userSeq) {
        User user = User.builder().email(email).password(null).userName("홍길동").build();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "userSeq", userSeq);
        return user;
    }

    @Test
    void 이미_연결된_소셜_계정이면_기존_계정으로_로그인한다() {
        User existing = userWithSeq("user@example.com", 7L);
        when(userSocialAccountRepository.findByProviderAndProviderUserId("naver", "naver-1234"))
                .thenReturn(java.util.Optional.of(
                        com.workmate.was.auth.vo.UserSocialAccount.builder()
                                .userSeq(7L).provider("naver").providerUserId("naver-1234").build()));
        when(userRepository.findById(7L)).thenReturn(java.util.Optional.of(existing));

        com.workmate.was.auth.vo.LoginResponseVo response = authService.socialLogin(socialRequest("user@example.com"));

        assertThat(response.getUserSeq()).isEqualTo(7L);
        // 이미 연결돼 있으므로 계정도 연결도 새로 만들지 않는다
        verify(userRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
        verify(userSocialAccountRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 이메일이_같은_계정이_있으면_새_계정을_만들지_않고_연결만_추가한다() {
        when(userSocialAccountRepository.findByProviderAndProviderUserId("naver", "naver-1234"))
                .thenReturn(java.util.Optional.empty());
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(java.util.Optional.of(userWithSeq("user@example.com", 7L)));

        // 제공자가 대문자·공백 섞인 이메일을 줘도 이메일 로그인과 같은 정규형으로 대조돼야 연동된다 (F1-01a)
        authService.socialLogin(socialRequest("  User@Example.COM "));

        verify(userRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
        ArgumentCaptor<com.workmate.was.auth.vo.UserSocialAccount> captor =
                ArgumentCaptor.forClass(com.workmate.was.auth.vo.UserSocialAccount.class);
        verify(userSocialAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getUserSeq()).isEqualTo(7L);
        assertThat(captor.getValue().getProvider()).isEqualTo("naver");
    }

    @Test
    void 계정이_없으면_비밀번호_없이_새로_만든다() {
        when(userSocialAccountRepository.findByProviderAndProviderUserId("naver", "naver-1234"))
                .thenReturn(java.util.Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(java.util.Optional.empty());
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.socialLogin(socialRequest("new@example.com"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User created = captor.getValue();
        assertThat(created.getPassword()).isNull();     // 소셜 가입자는 비밀번호가 없다
        assertThat(created.getPhone()).isNull();        // 전화번호도 받지 않는다
        assertThat(created.getEmail()).isEqualTo("new@example.com");
        assertThat(created.getRole()).isEqualTo("ROLE_USER");
        verify(userSocialAccountRepository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 제공자가_이메일을_주지_않으면_로그인을_거부한다() {
        assertThatThrownBy(() -> authService.socialLogin(socialRequest(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일 제공에 동의");
    }

    @Test
    void 비밀번호가_없는_계정은_이메일_로그인을_할_수_없다() {
        when(userRepository.findByEmail("social@example.com"))
                .thenReturn(java.util.Optional.of(userWithSeq("social@example.com", 8L)));

        com.workmate.was.auth.vo.LoginRequestVo request = new com.workmate.was.auth.vo.LoginRequestVo();
        request.setEmail("social@example.com");
        request.setPassword("abcd123!");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(com.workmate.was.global.exception.AuthenticationFailedException.class)
                .hasMessageContaining("이메일 또는 비밀번호");
    }
}
