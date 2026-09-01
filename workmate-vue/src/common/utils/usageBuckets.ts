import type { DailyUsage } from '@/common/types/usage'

/** 차트에 그릴 막대 하나 — 일별이든 주별이든 같은 모양으로 그린다 */
export interface UsageBucket {
    /** x축 라벨 (일별 '08-27', 주별 '8/24~') */
    label: string
    /** 마우스오버 툴팁에 쓸 전체 기간 설명 */
    title: string
    callCount: number
    inputTokens: number
    outputTokens: number
}

/**
 * 서버가 준 일별 데이터를 차트용 막대로 묶는다.
 *
 * 서버 응답은 항상 일별이다(0인 날도 채워서 온다). 30일치를 그대로 그리면 막대가 30개라
 * 좁아서 읽기 어렵고, 실제로는 대부분이 0인 경우가 많다. 그래서 **표현 단계에서** 묶는다.
 * 집계 자체는 서버가 진실이고, 여기서는 합만 낸다.
 *
 * @param daily     서버가 준 일별 목록 (날짜 오름차순)
 * @param groupDays 한 막대에 묶을 일수 (1이면 일별 그대로)
 * @returns 차트용 막대 목록
 */
export function toUsageBuckets(daily: DailyUsage[], groupDays: number): UsageBucket[] {
    if (groupDays <= 1) {
        return daily.map((d) => ({
            label: d.date.slice(5),
            title: `${d.date} — ${d.callCount}건`,
            callCount: d.callCount,
            inputTokens: d.inputTokens,
            outputTokens: d.outputTokens,
        }))
    }

    const buckets: UsageBucket[] = []
    for (let i = 0; i < daily.length; i += groupDays) {
        const slice = daily.slice(i, i + groupDays)
        const first = slice[0]
        const last = slice[slice.length - 1]
        // 루프 조건상 비어 있을 수 없지만, noUncheckedIndexedAccess 를 만족시키려면 좁혀야 한다
        if (!first || !last) continue
        buckets.push({
            // '2026-08-24' → '8/24' (앞의 0 을 떼서 좁은 폭에 들어가게)
            label: `${Number(first.date.slice(5, 7))}/${Number(first.date.slice(8, 10))}`,
            title: `${first.date} ~ ${last.date} — ${slice.reduce((a, d) => a + d.callCount, 0)}건`,
            callCount: slice.reduce((a, d) => a + d.callCount, 0),
            inputTokens: slice.reduce((a, d) => a + d.inputTokens, 0),
            outputTokens: slice.reduce((a, d) => a + d.outputTokens, 0),
        })
    }
    return buckets
}
