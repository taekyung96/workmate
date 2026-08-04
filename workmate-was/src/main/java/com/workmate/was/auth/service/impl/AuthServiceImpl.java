package com.workmate.was.auth.service.impl;

import com.workmate.was.auth.config.AuthLockProperties;
import com.workmate.was.auth.dao.UserRepository;
import com.workmate.was.auth.service.AuthService;
import com.workmate.was.auth.util.EmailNormalizer;
import com.workmate.was.auth.util.PasswordPolicyValidator;
import com.workmate.was.auth.vo.LoginRequestVo;
import com.workmate.was.auth.vo.LoginResponseVo;
import com.workmate.was.auth.vo.SignupRequestVo;
import com.workmate.was.auth.vo.User;
import com.workmate.was.global.exception.AuthenticationFailedException;
import com.workmate.was.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 비즈니스 로직 구현체.
 * 모든 입력 정규화는 이 진입 지점에서 수행한다 (F9 대원칙).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginFailRecorder loginFailRecorder;
    private final AuthLockProperties authLockProperties;

    /**
     * {@inheritDoc}
     *
     * @throws BusinessException 이메일 중복 (409)
     * @throws IllegalArgumentException 형식·정책 위반 (400)
     */
    @Override
    @Transactional
    public void signup(SignupRequestVo request) {
        String email = EmailNormalizer.normalize(request.getEmail());
        PasswordPolicyValidator.validate(request.getPassword());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("이미 사용 중인 이메일입니다.");
        }
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .userName(request.getUserName())
                .phone(normalizePhone(request.getPhone()))
                .build();
        userRepository.save(user);
        log.info("회원가입 완료. userSeq: {}", user.getUserSeq());
    }

    /** 전화번호는 숫자만 남기고 저장한다 (F9-09) */
    private String normalizePhone(String phone) {
        return phone == null ? null : phone.replaceAll("\\D", "");
    }

    /**
     * 로그인 자격 검증 (F1-05~07). 세션 발급은 WEB 담당 — 여기서는 검증·잠금 판정만.
     *
     * @throws AuthenticationFailedException 자격 불일치 (401 — 어느 쪽 오류인지 미노출)
     * @throws BusinessException 계정 잠금 중 (409 — 남은 분 안내)
     */
    @Override
    @Transactional
    public LoginResponseVo login(LoginRequestVo request) {
        String email = EmailNormalizer.normalize(request.getEmail());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("로그인 실패 - 미존재 이메일");
                    return new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다.");
                });

        checkLock(user);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // 실패 기록은 별도 트랜잭션(REQUIRES_NEW)으로 커밋한다 — 이 메서드는 곧 예외로 롤백되지만
            // 실패 카운트/잠금 시각은 반드시 DB 에 남아야 계정 잠금이 성립한다 (F1-06).
            int failCount = loginFailRecorder.recordFailedAttempt(user.getUserSeq());
            log.warn("로그인 실패 - userSeq: {}, 누적 실패: {}", user.getUserSeq(), failCount);
            throw new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        user.resetFailState();
        log.info("로그인 성공 - userSeq: {}", user.getUserSeq());
        return new LoginResponseVo(user.getUserSeq(), user.getUserName(), user.getRole());
    }

    /** 잠금 판정: 잠금 후 1시간 미경과면 409, 경과했으면 자동 해제 (F1-06·07) */
    private void checkLock(User user) {
        if (user.getLockedAt() == null) {
            return;
        }
        java.time.LocalDateTime unlockAt = user.getLockedAt().plusMinutes(authLockProperties.getLockMinutes());
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (now.isBefore(unlockAt)) {
            long remaining = java.time.Duration.between(now, unlockAt).toMinutes() + 1;
            log.warn("잠금 계정 로그인 시도 - userSeq: {}", user.getUserSeq());
            throw new BusinessException("계정이 잠겼습니다. " + remaining + "분 후 다시 시도해주세요.");
        }
        user.resetFailState(); // 1시간 경과 → 자동 해제
    }
}
