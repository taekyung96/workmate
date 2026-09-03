import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mountPage, flush } from '../../../test/support/mountPage'

const login = vi.fn<(...args: unknown[]) => unknown>()
vi.mock('../api/auth.api', () => ({
    authApi: {
        login: (...a: unknown[]) => login(...a),
        me: vi.fn<() => Promise<null>>(async () => null),
        logout: vi.fn<(...args: unknown[]) => unknown>(),
    },
}))

import LoginPage from './LoginPage.vue'

/** 이메일 로그인 폼은 버튼을 눌러야 펼쳐진다 */
async function openEmailForm(wrapper: Awaited<ReturnType<typeof mountPage>>['wrapper']) {
    const toggle = wrapper.findAll('button').find((b) => b.text().includes('이메일로 로그인'))
    await toggle!.trigger('click')
    await flush()
}

describe('LoginPage', () => {
    beforeEach(() => {
        login.mockReset()
    })

    it('소셜 로그인 버튼을 보여준다', async () => {
        const { wrapper } = await mountPage(LoginPage)

        expect(wrapper.text()).toContain('네이버')
        expect(wrapper.text()).toContain('카카오')
        expect(wrapper.text()).toContain('Google')
    })

    it('이메일 폼은 눌러야 펼쳐진다', async () => {
        const { wrapper } = await mountPage(LoginPage)

        expect(wrapper.find('input[type="password"]').exists()).toBe(false)

        await openEmailForm(wrapper)

        expect(wrapper.find('input[type="password"]').exists()).toBe(true)
    })

    it('로그인에 성공하면 /chat 으로 간다', async () => {
        login.mockResolvedValue({ userSeq: 12, userName: '관리자 (데모)', role: 'ROLE_ADMIN' })
        const { wrapper, router } = await mountPage(LoginPage)
        await openEmailForm(wrapper)

        await wrapper.find('input[type="email"]').setValue('demo.admin@example.com')
        await wrapper.find('input[type="password"]').setValue('Workmate!2026')
        await wrapper.find('form').trigger('submit')
        await flush(6)

        expect(login).toHaveBeenCalledWith('demo.admin@example.com', 'Workmate!2026')
        expect(router.currentRoute.value.path).toBe('/chat')
    })

    it('로그인에 실패하면 사유를 보여주고 이동하지 않는다', async () => {
        login.mockRejectedValue(new Error('bad credentials'))
        const { wrapper, router } = await mountPage(LoginPage)
        await openEmailForm(wrapper)

        await wrapper.find('input[type="email"]').setValue('demo.admin@example.com')
        await wrapper.find('input[type="password"]').setValue('wrong')
        await wrapper.find('form').trigger('submit')
        await flush(6)

        expect(router.currentRoute.value.path).not.toBe('/chat')
        expect(wrapper.text()).not.toBe('')
    })
})
