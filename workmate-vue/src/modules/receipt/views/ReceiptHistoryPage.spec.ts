import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mountPage } from '../../../test/support/mountPage'
import type { ReceiptPage } from '../types'

const history = vi.fn<(...args: unknown[]) => unknown>()
const downloadCsv = vi.fn<(...args: unknown[]) => unknown>()
vi.mock('../api/receipt.api', () => ({
    receiptApi: {
        history: (...a: unknown[]) => history(...a),
        downloadCsv: (...a: unknown[]) => downloadCsv(...a),
    },
}))

import ReceiptHistoryPage from './ReceiptHistoryPage.vue'

function receiptPage(overrides: Partial<ReceiptPage> = {}): ReceiptPage {
    return {
        content: [
            {
                receiptSeq: 1,
                userSeq: 12,
                imagePath: '/uploads/r1.jpg',
                payAmount: 12500,
                bizNo: '1234567890',
                payDate: '2026-09-01',
                cardName: '신한카드',
                bizNoValid: true,
                selectType: 'AUTO',
                rawJson: null,
                createdAt: '2026-09-01T10:00:00',
            },
        ],
        page: 0,
        totalPages: 1,
        totalElements: 1,
        ...overrides,
    }
}

describe('ReceiptHistoryPage', () => {
    beforeEach(() => {
        history.mockReset()
        downloadCsv.mockReset()
    })

    it('영수증 이력을 그린다', async () => {
        history.mockResolvedValue(receiptPage())

        const { wrapper } = await mountPage(ReceiptHistoryPage)

        expect(wrapper.text()).toContain('영수증')
        expect(wrapper.text()).toContain('신한카드')
        // 금액은 천 단위 구분 기호로 표시된다 — 포맷이 빠지면 12500 이 그대로 나온다
        expect(wrapper.text()).toContain('12,500')
    })

    it('결과가 없어도 화면이 그려진다', async () => {
        history.mockResolvedValue(receiptPage({ content: [], totalElements: 0, totalPages: 0 }))

        const { wrapper } = await mountPage(ReceiptHistoryPage)

        expect(wrapper.exists()).toBe(true)
        expect(wrapper.text()).toContain('영수증')
    })

    it('조회에 실패해도 화면이 터지지 않는다', async () => {
        history.mockRejectedValue(new Error('boom'))

        const { wrapper } = await mountPage(ReceiptHistoryPage)

        expect(wrapper.exists()).toBe(true)
    })
})
