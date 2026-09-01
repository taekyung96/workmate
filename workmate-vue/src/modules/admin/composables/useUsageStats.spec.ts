import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useUsageStats } from './useUsageStats'
import { adminApi } from '../api/admin.api'
import type { UsageSummary, UserUsagePage } from '../types'

// api 계층을 목으로 대체 — 컴포저블의 로딩·에러·빈 데이터 상태 전이만 검증한다 (실제 HTTP는 안 탄다)
vi.mock('../api/admin.api', () => ({
    adminApi: {
        usageSummary: vi.fn(),
        usageByUser: vi.fn(),
    },
}))

const emptySummary: UsageSummary = {
    period: { from: '2026-08-02', to: '2026-08-31' },
    total: {
        callCount: 0,
        inputTokens: 0,
        outputTokens: 0,
        untrackedCallCount: 0,
        unpricedCallCount: 0,
        estimatedCostUsd: 0,
        estimatedCostKrw: 0,
    },
    byFeature: [],
    daily: [],
}

const emptyUserPage: UserUsagePage = { content: [], page: 0, totalPages: 0, totalElements: 0 }

describe('useUsageStats', () => {
    beforeEach(() => {
        vi.mocked(adminApi.usageSummary).mockReset()
        vi.mocked(adminApi.usageByUser).mockReset()
    })

    it('load 성공 시 summary·users 를 채우고 loading 을 다시 false 로 되돌린다', async () => {
        const summary: UsageSummary = {
            ...emptySummary,
            total: { ...emptySummary.total, callCount: 32, untrackedCallCount: 30 },
        }
        vi.mocked(adminApi.usageSummary).mockResolvedValue(summary)
        vi.mocked(adminApi.usageByUser).mockResolvedValue({
            content: [
                {
                    userSeq: 1,
                    maskedEmail: 'k**@g***.com',
                    userName: '김태경',
                    callCount: 2,
                    inputTokens: 1770,
                    outputTokens: 92,
                    untrackedCallCount: 0,
                    estimatedCostUsd: 0,
                    estimatedCostKrw: 0,
                },
            ],
            page: 0,
            totalPages: 1,
            totalElements: 1,
        })

        const stats = useUsageStats()
        const loadPromise = stats.load()
        expect(stats.loading.value).toBe(true)
        await loadPromise

        expect(stats.loading.value).toBe(false)
        expect(stats.error.value).toBeNull()
        expect(stats.summary.value?.total.callCount).toBe(32)
        expect(stats.summary.value?.total.untrackedCallCount).toBe(30)
        expect(stats.users.value).toHaveLength(1)
        expect(stats.totalElements.value).toBe(1)
    })

    it('요약 조회 실패 시 error 메시지를 채우고 loading 을 false 로 되돌린다', async () => {
        vi.mocked(adminApi.usageSummary).mockRejectedValue(new Error('network error'))

        const stats = useUsageStats()
        await stats.load()

        expect(stats.loading.value).toBe(false)
        expect(stats.error.value).toBe('사용량 통계를 불러오지 못했습니다.')
        expect(stats.summary.value).toBeNull()
    })

    it('데이터가 없는 기간을 조회해도 빈 배열 상태로 정상 반환된다 (빈 상태)', async () => {
        vi.mocked(adminApi.usageSummary).mockResolvedValue(emptySummary)
        vi.mocked(adminApi.usageByUser).mockResolvedValue(emptyUserPage)

        const stats = useUsageStats()
        await stats.load()

        expect(stats.error.value).toBeNull()
        expect(stats.summary.value?.total.callCount).toBe(0)
        expect(stats.users.value).toEqual([])
        expect(stats.totalElements.value).toBe(0)
    })

    it("setPreset('7')은 서버에 최근 7일(from~to) 범위를 계산해 넘긴다", async () => {
        vi.mocked(adminApi.usageSummary).mockResolvedValue(emptySummary)
        vi.mocked(adminApi.usageByUser).mockResolvedValue(emptyUserPage)

        const stats = useUsageStats()
        stats.setPreset('7')
        await vi.waitFor(() => expect(stats.loading.value).toBe(false))

        expect(adminApi.usageSummary).toHaveBeenCalledTimes(1)
        const [from, to] = vi.mocked(adminApi.usageSummary).mock.calls[0]!
        expect(from).toBeDefined()
        expect(to).toBeDefined()
        // 7일 프리셋은 from~to 가 정확히 6일 차이(오늘 포함 7일)여야 한다
        const diffDays =
            (new Date(to!).getTime() - new Date(from!).getTime()) / (1000 * 60 * 60 * 24)
        expect(diffDays).toBe(6)
    })

    it("preset='30'(기본값)은 from/to 없이 서버 기본 기간(최근 30일)을 그대로 쓴다", async () => {
        vi.mocked(adminApi.usageSummary).mockResolvedValue(emptySummary)
        vi.mocked(adminApi.usageByUser).mockResolvedValue(emptyUserPage)

        const stats = useUsageStats()
        await stats.load()

        expect(adminApi.usageSummary).toHaveBeenCalledWith(undefined, undefined)
    })
})
