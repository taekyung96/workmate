import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mountPage } from '../../../test/support/mountPage'
import { emptyUsageSummary, usageSummary } from '../../../test/support/fixtures'
import type { UserUsagePage } from '../types'

const usageSummaryApi = vi.fn<(...args: unknown[]) => unknown>()
const usageByUser = vi.fn<(...args: unknown[]) => unknown>()
vi.mock('../api/admin.api', () => ({
    adminApi: {
        usageSummary: (...a: unknown[]) => usageSummaryApi(...a),
        usageByUser: (...a: unknown[]) => usageByUser(...a),
    },
}))

import AdminUsagePage from './AdminUsagePage.vue'

function userUsagePage(): UserUsagePage {
    return {
        content: [
            {
                userSeq: 12,
                maskedEmail: 'd***@example.com',
                userName: '관리자 (데모)',
                callCount: 30,
                inputTokens: 40000,
                outputTokens: 5000,
                untrackedCallCount: 2,
                estimatedCostUsd: 0.3,
                estimatedCostKrw: 400,
            },
        ],
        page: 0,
        totalPages: 1,
        totalElements: 1,
    }
}

describe('AdminUsagePage', () => {
    beforeEach(() => {
        usageSummaryApi.mockReset()
        usageByUser.mockReset()
        usageByUser.mockResolvedValue(userUsagePage())
    })

    it('전체 집계와 사용자별 목록을 함께 그린다', async () => {
        usageSummaryApi.mockResolvedValue(usageSummary())

        const { wrapper } = await mountPage(AdminUsagePage)

        expect(wrapper.text()).toContain('관리자 (데모)')
        expect(usageSummaryApi).toHaveBeenCalled()
        expect(usageByUser).toHaveBeenCalled()
    })

    it('사용자 목록에는 마스킹된 이메일만 나온다', async () => {
        usageSummaryApi.mockResolvedValue(usageSummary())

        const { wrapper } = await mountPage(AdminUsagePage)

        expect(wrapper.text()).not.toMatch(/@example\.com$/m)
        expect(wrapper.text()).toContain('d***@example.com')
    })

    it('기록이 없어도 화면이 그려진다', async () => {
        usageSummaryApi.mockResolvedValue(emptyUsageSummary())
        usageByUser.mockResolvedValue({ content: [], page: 0, totalPages: 0, totalElements: 0 })

        const { wrapper } = await mountPage(AdminUsagePage)

        expect(wrapper.exists()).toBe(true)
        expect(wrapper.text()).not.toBe('')
    })

    it('조회에 실패해도 화면이 터지지 않는다', async () => {
        usageSummaryApi.mockRejectedValue(new Error('boom'))
        usageByUser.mockRejectedValue(new Error('boom'))

        const { wrapper } = await mountPage(AdminUsagePage)

        expect(wrapper.exists()).toBe(true)
    })
})
