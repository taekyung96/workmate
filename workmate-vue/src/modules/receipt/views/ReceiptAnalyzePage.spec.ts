import { describe, expect, it, vi } from 'vitest'
import { mountPage } from '../../../test/support/mountPage'

vi.mock('../api/receipt.api', () => ({
    receiptApi: {
        analyze: vi.fn<(...args: unknown[]) => unknown>(),
        save: vi.fn<(...args: unknown[]) => unknown>(),
    },
}))

import ReceiptAnalyzePage from './ReceiptAnalyzePage.vue'

describe('ReceiptAnalyzePage', () => {
    it('업로드 전에는 파일을 올리라는 안내를 보여준다', async () => {
        const { wrapper } = await mountPage(ReceiptAnalyzePage)

        expect(wrapper.text()).toContain('영수증')
        expect(wrapper.text()).toContain('영수증 이미지를 올려주세요')
    })

    it('진입만으로 서버를 부르지 않는다 — 분석은 사용자가 파일을 올린 뒤다', async () => {
        const { receiptApi } = await import('../api/receipt.api')

        await mountPage(ReceiptAnalyzePage)

        expect(receiptApi.analyze).not.toHaveBeenCalled()
    })
})
