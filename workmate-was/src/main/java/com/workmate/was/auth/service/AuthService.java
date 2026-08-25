package com.workmate.was.auth.service;

import com.workmate.was.auth.vo.LoginRequestVo;
import com.workmate.was.auth.vo.LoginResponseVo;
import com.workmate.was.auth.vo.SignupRequestVo;
import com.workmate.was.auth.vo.SocialLoginRequestVo;

/**
 * 인증 비즈니스 로직 인터페이스 — 가입·로그인·계정 잠금 (F1).
 */
public interface AuthService {

    /**
     * 회원가입 (F1-01): 이메일 정규화 → 정책 검증 → 중복 검사 → BCrypt 저장.
     *
     * @param request 가입 요청 VO
     */
    void signup(SignupRequestVo request);

    /**
     * 로그인 자격 검증 (F1-05~07). 세션 발급은 WEB 담당 — 여기서는 검증·잠금 판정만.
     *
     * @param request 로그인 요청 VO
     * @return 로그인 성공 응답 VO
     */
    LoginResponseVo login(LoginRequestVo request);

    /**
     * 소셜 로그인 (F1-1). 토큰 교환·프로필 조회는 WEB 이 끝낸 상태로 들어온다.
     *
     * <p>세 갈래로 처리한다.
     * <ol>
     *   <li>(provider, providerUserId) 가 이미 연결돼 있으면 그 계정으로 로그인</li>
     *   <li>연결은 없지만 같은 이메일의 계정이 있으면 그 계정에 연결 후 로그인 (자동 연동)</li>
     *   <li>둘 다 없으면 계정을 만들고 연결 (비밀번호·전화번호 없음)</li>
     * </ol>
     *
     * @param request 제공자가 준 프로필 (provider·providerUserId·email·name)
     * @return 로그인 성공 응답 VO
     */
    LoginResponseVo socialLogin(SocialLoginRequestVo request);
}
