import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mountPage } from '../../../test/support/mountPage'
import type { AuditLogPage } from '../types'

const auditLogs = vi.fn<(...args: unknown[]) => unknown>()
vi.mock('../api/admin.api', () => ({
    adminApi: { auditLogs: (...a: unknown[]) => auditLogs(...a) },
}))

import AdminAuditLogPage from './AdminAuditLogPage.vue'

function auditLogPage(overrides: Partial<AuditLogPage> = {}): AuditLogPage {
    return {
        content: [
            {
                auditSeq: 1,
                adminUserSeq: 12,
                adminUserName: '관리자 (데모)',
                targetUserSeq: 13,
                targetUserName: '홍길동 (데모)',
                action: 'UNLOCK',
                createdAt: '2026-09-03T04:00:00',
            },
        ],
        page: 0,
        totalPages: 1,
        totalElements: 1,
        ...overrides,
    }
}

describe('AdminAuditLogPage', () => {
    beforeEach(() => {
        auditLogs.mockReset()
    })

    it('감사 로그를 그린다 — 누가 누구에게 무엇을 했는지', async () => {
        auditLogs.mockResolvedValue(auditLogPage())

        const { wrapper } = await mountPage(AdminAuditLogPage)

        expect(wrapper.text()).toContain('관리자 (데모)')
        expect(wrapper.text()).toContain('홍길동 (데모)')
    })

    it('기록이 없으면 빈 상태를 보여준다', async () => {
        auditLogs.mockResolvedValue(auditLogPage({ content: [], totalElements: 0, totalPages: 0 }))

        const { wrapper } = await mountPage(AdminAuditLogPage)

        expect(wrapper.text()).toMatch(/없습니다|없음/)
    })

    it('조회에 실패해도 화면이 터지지 않는다', async () => {
        auditLogs.mockRejectedValue(new Error('boom'))

        const { wrapper } = await mountPage(AdminAuditLogPage)

        expect(wrapper.exists()).toBe(true)
    })
})
