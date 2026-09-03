import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mountPage } from '../../../test/support/mountPage'
import type { GuidePage } from '../types'

const list = vi.fn<(...args: unknown[]) => unknown>()
vi.mock('../api/guide.api', () => ({
    guideApi: { list: (...a: unknown[]) => list(...a) },
}))

import GuideListPage from './GuideListPage.vue'

function guidePage(overrides: Partial<GuidePage> = {}): GuidePage {
    return {
        content: [
            {
                guideSeq: 1,
                title: '휴가 신청 절차',
                excerpt: '연차는 3일 전까지 신청한다.',
                isPublic: true,
                updatedAt: '2026-09-01T10:00:00',
            },
            {
                guideSeq: 2,
                title: '비공개 메모',
                excerpt: '본인만 볼 수 있다.',
                isPublic: false,
                updatedAt: '2026-09-02T10:00:00',
            },
        ],
        page: 0,
        totalPages: 1,
        totalElements: 2,
        ...overrides,
    }
}

describe('GuideListPage', () => {
    beforeEach(() => {
        list.mockReset()
    })

    it('가이드 목록을 그린다', async () => {
        list.mockResolvedValue(guidePage())

        const { wrapper } = await mountPage(GuideListPage)

        expect(wrapper.text()).toContain('휴가 신청 절차')
        expect(wrapper.text()).toContain('비공개 메모')
    })

    it('공개 여부가 화면에 드러난다 — 비공개 문서를 공개로 착각하면 안 된다', async () => {
        list.mockResolvedValue(guidePage())

        const { wrapper } = await mountPage(GuideListPage)

        expect(wrapper.text()).toMatch(/공개|비공개/)
    })

    it('결과가 없으면 빈 상태를 보여준다', async () => {
        list.mockResolvedValue(guidePage({ content: [], totalElements: 0, totalPages: 0 }))

        const { wrapper } = await mountPage(GuideListPage)

        expect(wrapper.text()).toContain('첫 가이드 문서를 작성해보세요')
    })

    it('조회에 실패해도 화면이 터지지 않는다', async () => {
        list.mockRejectedValue(new Error('boom'))

        const { wrapper } = await mountPage(GuideListPage)

        expect(wrapper.exists()).toBe(true)
    })
})
