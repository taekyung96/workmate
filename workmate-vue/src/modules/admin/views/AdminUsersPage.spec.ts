import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mountPage, flush } from '../../../test/support/mountPage'
import type { UserPage } from '../types'

const users = vi.fn<(...args: unknown[]) => unknown>()
const unlock = vi.fn<(...args: unknown[]) => unknown>()
const resetPassword = vi.fn<(...args: unknown[]) => unknown>()
vi.mock('../api/admin.api', () => ({
    adminApi: {
        users: (...a: unknown[]) => users(...a),
        unlock: (...a: unknown[]) => unlock(...a),
        resetPassword: (...a: unknown[]) => resetPassword(...a),
    },
}))

import AdminUsersPage from './AdminUsersPage.vue'

function userPage(overrides: Partial<UserPage> = {}): UserPage {
    return {
        content: [
            {
                userSeq: 12,
                maskedEmail: 'd***@example.com',
                userName: '관리자 (데모)',
                maskedPhone: '010-****-0001',
                role: 'ROLE_ADMIN',
                locked: false,
                createdAt: '2026-08-25T03:06:08',
            },
            {
                userSeq: 13,
                maskedEmail: 'h***@example.com',
                userName: '홍길동 (데모)',
                maskedPhone: '010-****-0002',
                role: 'ROLE_USER',
                locked: true,
                createdAt: '2026-08-25T03:06:08',
            },
        ],
        page: 0,
        totalPages: 1,
        totalElements: 2,
        ...overrides,
    }
}

describe('AdminUsersPage', () => {
    beforeEach(() => {
        users.mockReset()
        unlock.mockReset()
        resetPassword.mockReset()
    })

    it('사용자 목록을 그린다', async () => {
        users.mockResolvedValue(userPage())

        const { wrapper } = await mountPage(AdminUsersPage)

        expect(wrapper.text()).toContain('홍길동 (데모)')
        // 이메일·전화번호는 서버가 마스킹한 값을 그대로 보여준다 — 원문이 새면 안 된다
        expect(wrapper.text()).toContain('d***@example.com')
        expect(wrapper.text()).not.toContain('demo.admin@example.com')
    })

    it('결과가 없으면 빈 상태를 보여준다', async () => {
        users.mockResolvedValue(userPage({ content: [], totalElements: 0, totalPages: 0 }))

        const { wrapper } = await mountPage(AdminUsersPage)

        expect(wrapper.text()).toMatch(/없습니다|없음/)
    })

    it('조회에 실패해도 화면이 터지지 않는다', async () => {
        users.mockRejectedValue(new Error('boom'))

        const { wrapper } = await mountPage(AdminUsersPage)

        expect(wrapper.exists()).toBe(true)
        expect(wrapper.text()).not.toBe('')
    })

    it('검색어를 입력하면 서버에 다시 물어본다', async () => {
        users.mockResolvedValue(userPage())
        const { wrapper } = await mountPage(AdminUsersPage)
        const before = users.mock.calls.length

        const search = wrapper.find('input')
        expect(search.exists()).toBe(true)

        await search.setValue('홍길동')
        // 검색은 디바운스가 걸려 있다 — 타이머를 넘긴다
        await new Promise((r) => setTimeout(r, 500))
        await flush()

        expect(users.mock.calls.length).toBeGreaterThan(before)
    })
})
