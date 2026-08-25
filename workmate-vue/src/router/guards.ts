import type { Router } from 'vue-router'
import { useAuthStore } from '@/modules/auth/stores/auth.store'

/**
 * 전역 인증 가드를 라우터에 등록한다 (백엔드의 SecurityFilter에 대응).
 *
 * - 최초 진입 시 /me로 세션을 복원(1회)
 * - 보호 화면인데 미인증 → /login(원래 목적지 기억)
 * - 관리자 전용(meta.requiresAdmin)인데 관리자 아님 → /chat
 * - 이미 로그인 상태로 /login 접근 → /chat
 *
 * @param router 가드를 부착할 라우터 인스턴스
 */
export function registerGuards(router: Router): void {
    router.beforeEach(async (to) => {
        const auth = useAuthStore()

        // 새로고침/최초 진입 시 서버 세션을 한 번 확인해 상태 복원
        await auth.resolveSession()

        const isPublic = to.meta.public === true

        // 보호 화면인데 미인증 → 로그인으로 (원래 목적지 redirect 쿼리에 보관)
        if (!isPublic && !auth.isAuthenticated) {
            return { name: 'login', query: { redirect: to.fullPath } }
        }

        // 관리자 전용 화면 권한 확인
        if (to.meta.requiresAdmin && !auth.isAdmin) {
            return { name: 'chat' }
        }

        // 이미 로그인했는데 로그인 화면 접근 → 홈으로
        if (isPublic && auth.isAuthenticated && to.name === 'login') {
            return { name: 'chat' }
        }

        return true
    })
}
