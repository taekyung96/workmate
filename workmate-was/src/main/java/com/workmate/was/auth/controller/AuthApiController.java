package com.workmate.was.auth.controller;

import com.workmate.was.auth.vo.LoginRequestVo;
import com.workmate.was.auth.vo.LoginResponseVo;
import com.workmate.was.auth.vo.SignupRequestVo;
import com.workmate.was.auth.vo.SocialLoginRequestVo;
import com.workmate.was.auth.service.AuthService;
import com.workmate.was.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 REST API (04 문서 A1·A2).
 * 예외는 GlobalExceptionHandler 공통 처리 — 개별 try-catch 금지 (가이드 §7).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthService authService;

    /**
     * A1 회원가입.
     *
     * @param request 가입 요청 VO (email·password·userName·phone)
     * @return 성공 응답 (result 없음)
     */
    @PostMapping("/signup")
    public ApiResponse<Void> signup(@RequestBody SignupRequestVo request) {
        authService.signup(request);
        return ApiResponse.success();
    }

    /**
     * A2 로그인 자격 검증 — 세션 발급은 WEB 담당.
     *
     * @param request 로그인 요청 VO (email·password)
     * @return 로그인 성공 응답 VO
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponseVo> login(@RequestBody LoginRequestVo request) {
        return ApiResponse.success(authService.login(request));
    }

    /**
     * A3 소셜 로그인 (F1-1) — 토큰 교환·프로필 조회는 WEB 이 끝낸 뒤 호출한다.
     * 계정이 없으면 만들고, 이메일이 같은 계정이 있으면 그 계정에 연결한다.
     *
     * @param request 제공자 프로필 VO (provider·providerUserId·email·name)
     * @return 로그인 성공 응답 VO
     */
    @PostMapping("/social-login")
    public ApiResponse<LoginResponseVo> socialLogin(@RequestBody SocialLoginRequestVo request) {
        return ApiResponse.success(authService.socialLogin(request));
    }
}
