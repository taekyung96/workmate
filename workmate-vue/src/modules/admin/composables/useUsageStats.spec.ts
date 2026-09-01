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

    // ── 차트 버킷 묶기 ──
    // 서버는 항상 일별로 주지만, 30일치를 그대로 그리면 막대가 30개라 읽을 수 없다.
    // 그래서 표현 단계에서 주 단위로 묶는다. 그 경계와 합계가 맞는지 고정한다.

    /** 날짜 오름차순으로 n일치 일별 데이터를 만든다 (i번째 날의 호출 수 = i+1) */
    function makeDaily(n: number) {
        return Array.from({ length: n }, (_, i) => {
            const d = new Date(Date.UTC(2026, 7, 3 + i))
            return {
                date: d.toISOString().slice(0, 10),
                callCount: i + 1,
                inputTokens: (i + 1) * 10,
                outputTokens: i + 1,
                untrackedCallCount: 0,
            }
        })
    }

    it('7일 이하면 일별 그대로 그린다 (막대 수 = 일수)', async () => {
        vi.mocked(adminApi.usageSummary).mockResolvedValue({
            ...emptySummary,
            daily: makeDaily(7),
        })
        vi.mocked(adminApi.usageByUser).mockResolvedValue(emptyUserPage)

        const stats = useUsageStats()
        await stats.load()

        expect(stats.chartUnit.value).toBe('일별')
        expect(stats.chartBuckets.value).toHaveLength(7)
        expect(stats.chartBuckets.value[0]!.label).toBe('08-03')
        expect(stats.chartBuckets.value[0]!.callCount).toBe(1)
    })

    it('8일을 넘으면 주 단위로 묶고 합계를 낸다 (30일 → 막대 5개)', async () => {
        vi.mocked(adminApi.usageSummary).mockResolvedValue({
            ...emptySummary,
            daily: makeDaily(30),
        })
        vi.mocked(adminApi.usageByUser).mockResolvedValue(emptyUserPage)

        const stats = useUsageStats()
        await stats.load()

        expect(stats.chartUnit.value).toBe('주별')
        // 30일 ÷ 7 = 막대 5개 (마지막은 2일짜리 부분 주)
        expect(stats.chartBuckets.value).toHaveLength(5)
        // 첫 주 = 1..7 일차 → 합 28
        expect(stats.chartBuckets.value[0]!.callCount).toBe(28)
        expect(stats.chartBuckets.value[0]!.inputTokens).toBe(280)
        // 마지막 주는 29·30 일차만 남는다 → 합 59
        expect(stats.chartBuckets.value[4]!.callCount).toBe(59)
        // 묶어도 전체 합은 보존돼야 한다
        const sum = stats.chartBuckets.value.reduce((a, b) => a + b.callCount, 0)
        expect(sum).toBe((30 * 31) / 2)
    })

    it('일별이 비어 있으면 막대도 비운다 (빈 기간에서 화면이 깨지지 않아야 한다)', async () => {
        vi.mocked(adminApi.usageSummary).mockResolvedValue(emptySummary)
        vi.mocked(adminApi.usageByUser).mockResolvedValue(emptyUserPage)

        const stats = useUsageStats()
        await stats.load()

        expect(stats.chartBuckets.value).toEqual([])
        expect(stats.chartUnit.value).toBe('일별')
    })
})
