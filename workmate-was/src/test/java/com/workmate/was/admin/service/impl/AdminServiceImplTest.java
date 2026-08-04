package com.workmate.was.admin.service.impl;

import com.workmate.was.admin.dao.AdminAuditLogRepository;
import com.workmate.was.admin.vo.AdminAuditLog;
import com.workmate.was.admin.vo.AuditLogPageVo;
import com.workmate.was.admin.vo.AuditLogVo;
import com.workmate.was.admin.vo.ResetPasswordResultVo;
import com.workmate.was.admin.vo.UserPageVo;
import com.workmate.was.auth.config.AuthLockProperties;
import com.workmate.was.auth.dao.UserRepository;
import com.workmate.was.auth.util.PasswordPolicyValidator;
import com.workmate.was.auth.vo.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AdminServiceImpl 단위 테스트 (목록 마스킹·잠금 해제·비밀번호 초기화·감사 기록).
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AdminAuditLogRepository adminAuditLogRepository;

    private AdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminServiceImpl(userRepository, adminAuditLogRepository, new BCryptPasswordEncoder(),
                new AuthLockProperties(5, 60));
    }

    private User user(Long userSeq, String email, String name, String phone, LocalDateTime lockedAt) {
        User u = User.builder().email(email).password("hash").userName(name).phone(phone).role("ROLE_USER").build();
        ReflectionTestUtils.setField(u, "userSeq", userSeq);
        if (lockedAt != null) {
            ReflectionTestUtils.setField(u, "lockedAt", lockedAt);
            ReflectionTestUtils.setField(u, "loginFailCount", 5);
        }
        return u;
    }

    @Test
    @DisplayName("getUsers(이름검색): 이메일·전화 마스킹 + 페이징 메타 매핑")
    void getUsers_masks_and_maps() {
        User u = user(1L, "kim@gmail.com", "김태경", "01012345678", null);
        when(userRepository.findByUserNameContainingIgnoreCase(eq("김"), any()))
                .thenReturn(new PageImpl<>(List.of(u), PageRequest.of(0, 20), 1));

        UserPageVo result = adminService.getUsers("김", 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getMaskedEmail()).isEqualTo("k**@g***.com");
        assertThat(result.getContent().get(0).getMaskedPhone()).isEqualTo("010****5678");
        assertThat(result.getContent().get(0).getUserName()).isEqualTo("김태경");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getUsers: 잠긴 계정은 locked=true 로 표시")
    void getUsers_marks_locked() {
        User locked = user(2L, "a@b.com", "박민수", "01099998888", LocalDateTime.now());
        when(userRepository.findByUserNameContainingIgnoreCase(any(), any()))
                .thenReturn(new PageImpl<>(List.of(locked)));

        UserPageVo result = adminService.getUsers("박", 0, 20);

        assertThat(result.getContent().get(0).isLocked()).isTrue();
    }

    @Test
    @DisplayName("unlock: 실패 상태를 초기화하고 감사 로그를 남긴다")
    void unlock_resets_and_audits() {
        User locked = user(5L, "a@b.com", "홍길동", "01011112222", LocalDateTime.now());
        when(userRepository.findById(5L)).thenReturn(Optional.of(locked));

        adminService.unlock(1L, 5L);

        assertThat(locked.getLockedAt()).isNull();
        assertThat(locked.getLoginFailCount()).isZero();
        verify(adminAuditLogRepository).save(any(AdminAuditLog.class));
    }

    @Test
    @DisplayName("resetPassword: 정책 충족 임시비번 발급·BCrypt 저장·감사 로그, 평문 반환")
    void resetPassword_issues_temp() {
        User u = user(5L, "a@b.com", "홍길동", "01011112222", null);
        when(userRepository.findById(5L)).thenReturn(Optional.of(u));

        ResetPasswordResultVo result = adminService.resetPassword(1L, 5L);

        assertThat(result.getTempPassword()).isNotBlank();
        // 정책(8+·영문·숫자·특수) 충족
        assertThatCode(() -> PasswordPolicyValidator.validate(result.getTempPassword()))
                .doesNotThrowAnyException();
        // 저장된 비번이 발급 평문의 BCrypt 해시
        assertThat(new BCryptPasswordEncoder().matches(result.getTempPassword(), u.getPassword())).isTrue();
        verify(adminAuditLogRepository).save(any(AdminAuditLog.class));
    }

    private AdminAuditLog auditLog(Long auditSeq, Long adminSeq, Long targetSeq, String action) {
        AdminAuditLog log = AdminAuditLog.builder()
                .adminUserSeq(adminSeq).targetUserSeq(targetSeq).action(action).build();
        ReflectionTestUtils.setField(log, "auditSeq", auditSeq);
        return log;
    }

    @Test
    @DisplayName("getAuditLogs: 행위자·대상 이름 조인 + 페이징 매핑, 삭제된 사용자는 대체 표기")
    void getAuditLogs_joins_names() {
        AdminAuditLog unlockLog = auditLog(10L, 1L, 5L, AdminAuditLog.ACTION_UNLOCK);
        // 대상(99)은 이후 삭제되어 이름 조회에서 빠진다
        AdminAuditLog resetLog = auditLog(11L, 1L, 99L, AdminAuditLog.ACTION_RESET_PASSWORD);
        when(adminAuditLogRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(unlockLog, resetLog), PageRequest.of(0, 20), 2));
        when(userRepository.findAllById(any()))
                .thenReturn(List.of(
                        user(1L, "admin@b.com", "관리자", "01000000000", null),
                        user(5L, "a@b.com", "홍길동", "01011112222", null)));

        AuditLogPageVo result = adminService.getAuditLogs(0, 20);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);

        AuditLogVo first = result.getContent().get(0);
        assertThat(first.getAdminUserName()).isEqualTo("관리자");
        assertThat(first.getTargetUserName()).isEqualTo("홍길동");
        assertThat(first.getAction()).isEqualTo("UNLOCK");

        // 삭제된 대상(99)은 이름을 못 찾아 대체 문구로 표기
        assertThat(result.getContent().get(1).getTargetUserName()).isEqualTo("(삭제된 사용자)");
    }

    @Test
    @DisplayName("존재하지 않는 사용자에 대한 조치는 IllegalArgumentException")
    void action_on_missing_user_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.unlock(1L, 99L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
