import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory, type RouteRecordRaw } from 'vue-router'
import { registerGuards } from './guards'
import { useAuthStore } from '../modules/auth/stores/auth.store'

// 가드가 부르는 세션 복원은 네트워크를 타므로 막는다 — 여기서 보고 싶은 건 이동 규칙이다
vi.mock('../modules/auth/api/auth.api', () => ({
    authApi: { me: vi.fn(async () => null) },
}))

const Blank = { template: '<div />' }

/** 실제 라우터와 같은 모양(공개 로그인 · 보호 화면 · catch-all)으로 최소 구성한다 */
const routes: RouteRecordRaw[] = [
    { path: '/', redirect: '/chat' },
    { path: '/login', name: 'login', component: Blank, meta: { public: true } },
    { path: '/chat', name: 'chat', component: Blank },
    { path: '/admin/usage', name: 'admin-usage', component: Blank, meta: { requiresAdmin: true } },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: Blank },
]

/**
 * 로그인한 상태를 만든다.
 *
 * resolved 를 함께 세우는 이유: 가드가 부르는 resolveSession 이 아직 확인 전이면
 * /me(여기서는 목이라 null)로 사용자를 도로 지워 버린다.
 *
 * @param role 부여할 권한 문자열
 */
function loginAs(role: string) {
    const auth = useAuthStore()
    auth.setUser({ userSeq: 1, userName: '테스터', role })
    auth.resolved = true
}

function newRouter() {
    const router = createRouter({ history: createMemoryHistory(), routes })
    registerGuards(router)
    return router
}

describe('라우터 인증 가드', () => {
    beforeEach(() => {
        setActivePinia(createPinia())
    })

    it('미인증으로 보호 화면에 가면 목적지를 기억한 채 로그인으로 보낸다', async () => {
        const router = newRouter()

        await router.push('/admin/usage')

        expect(router.currentRoute.value.name).toBe('login')
        expect(router.currentRoute.value.query.redirect).toBe('/admin/usage')
    })

    it('없는 주소는 목적지로 기억하지 않는다 — 로그인 직후 404 로 되돌아가면 안 된다', async () => {
        const router = newRouter()

        await router.push('/oldbookmark/from/v2')

        expect(router.currentRoute.value.name).toBe('login')
        // redirect 가 남아 있으면 로그인 성공 후 그 주소로 replace 되어 첫 화면이 404 가 된다
        expect(router.currentRoute.value.query.redirect).toBeUndefined()
    })

    it('로그인한 사용자는 없는 주소에서 404 화면을 본다 — 로그인으로 튕기지 않는다', async () => {
        const router = newRouter()
        loginAs('ROLE_USER')

        await router.push('/nope')

        expect(router.currentRoute.value.name).toBe('not-found')
    })

    it('관리자 전용 화면에 일반 사용자가 가면 홈으로 보낸다', async () => {
        const router = newRouter()
        loginAs('ROLE_USER')

        await router.push('/admin/usage')

        expect(router.currentRoute.value.name).toBe('chat')
    })
})
