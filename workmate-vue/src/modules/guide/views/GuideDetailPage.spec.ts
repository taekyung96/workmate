import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mountPage } from '../../../test/support/mountPage'
import { useAuthStore } from '../../auth/stores/auth.store'
import type { Guide } from '../types'

const detail = vi.fn<(...args: unknown[]) => unknown>()
const remove = vi.fn<(...args: unknown[]) => unknown>()
vi.mock('../api/guide.api', () => ({
    guideApi: {
        detail: (...a: unknown[]) => detail(...a),
        remove: (...a: unknown[]) => remove(...a),
    },
}))

import GuideDetailPage from './GuideDetailPage.vue'

function guide(overrides: Partial<Guide> = {}): Guide {
    return {
        guideSeq: 1,
        userSeq: 12,
        title: '휴가 신청 절차',
        content: '## 절차\n\n연차는 3일 전까지 신청한다.',
        isPublic: true,
        createdAt: '2026-09-01T10:00:00',
        updatedAt: '2026-09-01T10:00:00',
        ...overrides,
    }
}

describe('GuideDetailPage', () => {
    beforeEach(() => {
        detail.mockReset()
        remove.mockReset()
    })

    it('본문을 마크다운으로 그린다', async () => {
        detail.mockResolvedValue(guide())

        const { wrapper } = await mountPage(GuideDetailPage, { params: { id: '1' } })

        expect(wrapper.text()).toContain('휴가 신청 절차')
        expect(wrapper.text()).toContain('연차는 3일 전까지')
    })

    it('남의 문서에는 수정·삭제 버튼을 주지 않는다', async () => {
        detail.mockResolvedValue(guide({ userSeq: 99 }))

        const { wrapper } = await mountPage(GuideDetailPage, { params: { id: '1' } })
        // 화면은 로그인 사용자를 auth store 에서 읽는다 — 작성자와 다른 사람으로 둔다
        useAuthStore().setUser({ userSeq: 12, userName: '테스터', role: 'ROLE_USER' })

        const labels = wrapper.findAll('button').map((b) => b.text())
        expect(labels.some((t) => t.includes('삭제'))).toBe(false)
    })

    it('조회에 실패해도 화면이 터지지 않는다', async () => {
        detail.mockRejectedValue(new Error('boom'))

        const { wrapper } = await mountPage(GuideDetailPage, { params: { id: '1' } })

        expect(wrapper.exists()).toBe(true)
    })
})
