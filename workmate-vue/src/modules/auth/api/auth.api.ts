import client from '@/common/api/client'
import type { ApiResponse } from '@/common/types/api'
import type { LoginUser, SignupRequest } from '../types'

/**
 * 상태변경 요청(로그인·회원가입) 전에 XSRF-TOKEN 쿠키가 있는지 보장한다.
 *
 * 서버는 로그아웃 시 Spring Security 가 CSRF 쿠키를 삭제하는데, SPA 가드는 부팅 때
 * 딱 1번만 /me 로 쿠키를 발급받으므로(그 뒤 resolved 플래그로 재호출을 막음),
 * 로그아웃 이후 재로그인/가입 첫 시도가 토큰 없이 나가 403(접근 권한 없음)으로 실패했다.
 * 쿠키가 없으면 GET /me 로 서버의 CsrfCookieFilter 가 토큰을 재발급하게 한 뒤 진행한다.
 */
async function ensureCsrfToken(): Promise<void> {
    if (document.cookie.includes('XSRF-TOKEN=')) return
    try {
        // 미로그인 401 이어도 응답에 XSRF-TOKEN 쿠키가 실린다. 전역 401 리다이렉트는 제외.
        await client.get('/auth/me', { skipAuthRedirect: true })
    } catch {
        /* 401 은 정상 흐름 — 쿠키만 재발급받으면 된다 */
    }
}

/**
 * 인증 API (얇은 WEB의 /api/auth/* 호출).
 * 계층 규칙: HTTP 통신만 담당하고 비즈니스 로직/상태변경은 하지 않는다.
 */
export const authApi = {
    /**
     * 로그인 — Spring Security 폼 로그인 필터가 처리하므로 **form-urlencoded**로 보낸다.
     * @returns 로그인 사용자 정보
     */
    async login(email: string, password: string): Promise<LoginUser> {
        await ensureCsrfToken() // 로그아웃 후 첫 로그인이 CSRF 토큰 없이 나가 403 되던 문제 방지
        const body = new URLSearchParams({ email, password })
        const { data } = await client.post<ApiResponse<LoginUser>>('/auth/login', body, {
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            // 로그인 실패(401)는 화면에서 메시지로 처리하므로 전역 리다이렉트 제외
            skipAuthRedirect: true,
        })
        return data.result
    },

    /**
     * 회원가입 (JSON). 검증은 WAS가 담당하고 실패 사유는 응답 message로 온다.
     * @returns 공통 응답 (success=false면 message에 사유)
     */
    async signup(payload: SignupRequest): Promise<ApiResponse<void>> {
        await ensureCsrfToken() // 쿠키가 삭제된 상태(로그아웃 후 등)에서 가입 첫 시도 403 방지
        const { data } = await client.post<ApiResponse<void>>('/auth/signup', payload)
        return data
    },

    /**
     * 현재 세션 사용자 조회 (앱 부팅/새로고침 시 세션 복원용).
     * 미로그인이면 401이므로 null로 정규화한다 (전역 리다이렉트 제외).
     */
    async me(): Promise<LoginUser | null> {
        try {
            const { data } = await client.get<ApiResponse<LoginUser>>('/auth/me', {
                skipAuthRedirect: true,
            })
            return data.result
        } catch {
            return null
        }
    },

    /** 로그아웃 (세션 무효화) */
    async logout(): Promise<void> {
        await client.post('/auth/logout')
    },
}
