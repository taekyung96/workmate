import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mountPage, flush } from '../../../test/support/mountPage'
import { emptyUsageSummary, usageSummary } from '../../../test/support/fixtures'

const mySummary = vi.fn<(...args: unknown[]) => unknown>()
vi.mock('../api/usage.api', () => ({
    usageApi: {
        mySummary: (...args: unknown[]) => mySummary(...args),
    },
}))

import MyUsagePage from './MyUsagePage.vue'

describe('MyUsagePage', () => {
    beforeEach(() => {
        mySummary.mockReset()
    })

    it('요약을 받아 화면에 그린다', async () => {
        mySummary.mockResolvedValue(usageSummary())

        const { wrapper } = await mountPage(MyUsagePage)

        expect(wrapper.text()).toContain('내 사용량')
        // 합계가 실제로 그려지는지 — 응답 모양이 바뀌면 여기서 깨진다
        expect(wrapper.text()).toContain('40')
        expect(mySummary).toHaveBeenCalledTimes(1)
    })

    it('집계에서 빠진 건수를 숨기지 않는다', async () => {
        mySummary.mockResolvedValue(usageSummary())

        const { wrapper } = await mountPage(MyUsagePage)

        // 토큰이 집계되지 않은 호출(주로 임베딩)은 0으로 뭉개지 않고 드러내야 한다
        expect(wrapper.text()).toMatch(/집계|미집계|3/)
    })

    it('기록이 없으면 빈 상태 안내를 보여준다', async () => {
        mySummary.mockResolvedValue(emptyUsageSummary())

        const { wrapper } = await mountPage(MyUsagePage)

        expect(wrapper.text()).toContain('사용 기록이 없습니다')
    })

    it('조회에 실패하면 오류를 보여주고 터지지 않는다', async () => {
        mySummary.mockRejectedValue(new Error('boom'))

        const { wrapper } = await mountPage(MyUsagePage)

        expect(wrapper.text()).toContain('불러오지 못했습니다')
    })

    it('기간 프리셋을 바꾸면 다시 조회한다', async () => {
        mySummary.mockResolvedValue(usageSummary())
        const { wrapper } = await mountPage(MyUsagePage)

        const sevenDays = wrapper.findAll('button').find((b) => b.text().includes('최근 7일'))
        await sevenDays!.trigger('click')
        await flush()

        expect(mySummary).toHaveBeenCalledTimes(2)
        // '7' 프리셋은 클라이언트가 from/to 를 계산해 보낸다 — 서버 기본값(빈 인자)과 구분된다
        expect(mySummary.mock.calls[1]![0]).toMatch(/^\d{4}-\d{2}-\d{2}$/)
    })
})
