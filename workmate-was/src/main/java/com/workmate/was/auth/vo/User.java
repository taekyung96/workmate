package com.workmate.was.auth.vo;

import com.workmate.was.global.crypto.AesCryptoConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 회원 Entity (admin_user 테이블 매핑, 04 문서 §4.1).
 * email·phone 은 AES-256 투명 암호화 컬럼 — DB 에는 암호문이 저장된다 (F1-03).
 */
@Entity
@Table(name = "admin_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_seq")
    private Long userSeq;

    /** 로그인 ID — 소문자 정규형만 저장 (F1-01a). 결정적 암호화라 UK·동등검색 유효 */
    @Convert(converter = AesCryptoConverter.class)
    @Column(name = "email", nullable = false, unique = true, length = 512)
    private String email;

    /**
     * BCrypt 해시 (F1-02).
     * 소셜로 가입한 사용자는 비밀번호가 없어 null 이다 (F1-1) — 이메일 로그인 경로에서만 값이 존재한다.
     */
    @Column(name = "password", length = 60)
    private String password;

    @Column(name = "user_name", nullable = false, length = 50)
    private String userName;

    @Convert(converter = AesCryptoConverter.class)
    @Column(name = "phone", length = 512)
    private String phone;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    /** 연속 로그인 실패 횟수 (F1-06) */
    @Column(name = "login_fail_count", nullable = false)
    private int loginFailCount;

    /** 계정 잠금 시각 — null 이면 잠금 아님. +1시간 경과 시 자동 해제 */
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "use_yn", nullable = false)
    private boolean useYn;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public User(String email, String password, String userName, String phone, String role) {
        this.email = email;
        this.password = password;
        this.userName = userName;
        this.phone = phone;
        this.role = role != null ? role : "ROLE_USER";
        this.loginFailCount = 0;
        this.useYn = true;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 로그인 실패 1회 누적 — 임계 횟수 도달 시 잠금 시각 기록 (F1-06).
     * 엔티티는 설정을 주입받을 수 없으므로 임계값을 호출자(LoginFailRecorder)가 넘긴다.
     *
     * @param maxFailCount 계정을 잠글 연속 실패 임계 횟수 (app.auth.max-fail-count)
     */
    public void increaseFailCount(int maxFailCount) {
        this.loginFailCount++;
        if (this.loginFailCount >= maxFailCount) {
            this.lockedAt = LocalDateTime.now();
        }
    }

    /** 로그인 성공·잠금 만료 시 실패 상태 초기화 */
    public void resetFailState() {
        this.loginFailCount = 0;
        this.lockedAt = null;
    }

    /** 비밀번호 변경 (관리자 초기화 등) — 이미 BCrypt 로 인코딩된 값을 받는다 (F6-03) */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
