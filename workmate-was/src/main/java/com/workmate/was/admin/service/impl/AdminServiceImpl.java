package com.workmate.was.admin.service.impl;

import com.workmate.was.admin.dao.AdminAuditLogRepository;
import com.workmate.was.admin.service.AdminService;
import com.workmate.was.admin.util.PiiMasker;
import com.workmate.was.admin.util.TempPasswordGenerator;
import com.workmate.was.admin.vo.AdminAuditLog;
import com.workmate.was.admin.vo.AdminUserVo;
import com.workmate.was.admin.vo.AuditLogPageVo;
import com.workmate.was.admin.vo.AuditLogVo;
import com.workmate.was.admin.vo.ResetPasswordResultVo;
import com.workmate.was.admin.vo.UserPageVo;
import com.workmate.was.auth.dao.UserRepository;
import com.workmate.was.auth.vo.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 관리자 사용자 관리 구현체.
 * 개인정보는 복호화된 값을 마스킹해 반환하고(§3.5), 모든 조치는 감사 로그로 남긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final PasswordEncoder passwordEncoder;

    /** 계정 잠금 유지 시간 — AuthServiceImpl 과 동일 (F1-06) */
    private static final long LOCK_MINUTES = 60;

    /** 감사 로그의 행위자·대상이 이미 삭제돼 이름을 찾을 수 없을 때의 대체 표기 */
    private static final String DELETED_USER = "(삭제된 사용자)";

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public UserPageVo getUsers(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 이메일은 결정적 AES 암호문이라 부분검색 불가 → keyword 에 @ 있으면 정확 일치 검색
        if (keyword != null && keyword.contains("@")) {
            Optional<User> found = userRepository.findByEmail(keyword.trim().toLowerCase());
            List<AdminUserVo> content = found.map(u -> List.of(toVo(u))).orElse(List.of());
            return UserPageVo.builder()
                    .content(content)
                    .page(0)
                    .totalPages(found.isPresent() ? 1 : 0)
                    .totalElements(found.isPresent() ? 1 : 0)
                    .build();
        }

        Page<User> result = (keyword == null || keyword.isBlank())
                ? userRepository.findAll(pageable)
                : userRepository.findByUserNameContainingIgnoreCase(keyword.trim(), pageable);

        return UserPageVo.builder()
                .content(result.getContent().stream().map(this::toVo).toList())
                .page(result.getNumber())
                .totalPages(result.getTotalPages())
                .totalElements(result.getTotalElements())
                .build();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public AuditLogPageVo getAuditLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminAuditLog> result = adminAuditLogRepository.findAll(pageable);

        // 행위자·대상 seq를 한데 모아 이름을 한 번의 쿼리로 조회한다 (행마다 조회하는 N+1 방지)
        Set<Long> userSeqs = new HashSet<>();
        result.getContent().forEach(row -> {
            userSeqs.add(row.getAdminUserSeq());
            userSeqs.add(row.getTargetUserSeq());
        });
        Map<Long, String> nameMap = userRepository.findAllById(userSeqs).stream()
                .collect(Collectors.toMap(User::getUserSeq, User::getUserName));

        List<AuditLogVo> content = result.getContent().stream()
                .map(row -> AuditLogVo.builder()
                        .auditSeq(row.getAuditSeq())
                        .adminUserSeq(row.getAdminUserSeq())
                        .adminUserName(nameMap.getOrDefault(row.getAdminUserSeq(), DELETED_USER))
                        .targetUserSeq(row.getTargetUserSeq())
                        .targetUserName(nameMap.getOrDefault(row.getTargetUserSeq(), DELETED_USER))
                        .action(row.getAction())
                        .createdAt(row.getCreatedAt())
                        .build())
                .toList();

        return AuditLogPageVo.builder()
                .content(content)
                .page(result.getNumber())
                .totalPages(result.getTotalPages())
                .totalElements(result.getTotalElements())
                .build();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void unlock(Long adminUserSeq, Long targetUserSeq) {
        User target = findUser(targetUserSeq);
        target.resetFailState();
        adminAuditLogRepository.save(AdminAuditLog.builder()
                .adminUserSeq(adminUserSeq)
                .targetUserSeq(targetUserSeq)
                .action(AdminAuditLog.ACTION_UNLOCK)
                .build());
        log.info("계정 잠금 해제 - admin: {}, target: {}", adminUserSeq, targetUserSeq);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ResetPasswordResultVo resetPassword(Long adminUserSeq, Long targetUserSeq) {
        User target = findUser(targetUserSeq);
        String tempPassword = TempPasswordGenerator.generate();
        target.changePassword(passwordEncoder.encode(tempPassword));
        // 임시 비밀번호를 즉시 쓸 수 있도록 실패 카운트·잠금도 초기화
        target.resetFailState();
        adminAuditLogRepository.save(AdminAuditLog.builder()
                .adminUserSeq(adminUserSeq)
                .targetUserSeq(targetUserSeq)
                .action(AdminAuditLog.ACTION_RESET_PASSWORD)
                .build());
        log.info("비밀번호 초기화 - admin: {}, target: {}", adminUserSeq, targetUserSeq);
        return new ResetPasswordResultVo(tempPassword);
    }

    private User findUser(Long userSeq) {
        return userRepository.findById(userSeq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }

    /** User → 마스킹된 AdminUserVo */
    private AdminUserVo toVo(User user) {
        return AdminUserVo.builder()
                .userSeq(user.getUserSeq())
                .maskedEmail(PiiMasker.maskEmail(user.getEmail()))
                .userName(user.getUserName())
                .maskedPhone(PiiMasker.maskPhone(user.getPhone()))
                .role(user.getRole())
                .locked(isLocked(user))
                .createdAt(user.getCreatedAt())
                .build();
    }

    /** 현재 잠금 상태 — 잠금 시각이 있고 60분 이내면 잠금 (F1-06 과 동일 판정) */
    private boolean isLocked(User user) {
        return user.getLockedAt() != null
                && user.getLockedAt().plusMinutes(LOCK_MINUTES).isAfter(LocalDateTime.now());
    }
}
