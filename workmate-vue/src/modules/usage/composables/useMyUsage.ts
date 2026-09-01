import { computed, ref } from 'vue'
import { usageApi } from '../api/usage.api'
import { extractErrorMessage } from '@/common/utils/error'
import { toUsageBuckets, type UsageBucket } from '@/common/utils/usageBuckets'
import type { UsageSummary } from '@/common/types/usage'

/** 기간 선택 프리셋 — '7'은 클라이언트가 오늘 기준으로 계산, '30'은 서버 기본값을 그대로 쓴다 */
export type MyUsagePreset = '7' | '30'

/**
 * 본인 사용량 화면 상태·동작.
 *
 * 관리자 대시보드(useUsageStats)와 달리 사용자별 목록이 없다 — 본인 것만 본다.
 * 조회 대상은 서버가 세션에서 정하므로 여기서 사용자 번호를 다루지 않는다.
 *
 * @returns 기간 상태, 요약·차트 상태, 조회 액션
 */
export function useMyUsage() {
    const preset = ref<MyUsagePreset>('30')
    const summary = ref<UsageSummary | null>(null)
    const loading = ref(false)
    const error = ref<string | null>(null)

    // 현재 선택된 기간을 요청 파라미터로 변환한다. '30'은 비워서 서버 기본값(최근 30일)을 쓴다.
    function resolvedRange(): { from?: string; to?: string } {
        if (preset.value !== '7') return {}
        const to = new Date()
        const from = new Date()
        from.setDate(from.getDate() - 6)
        return { from: toIsoDate(from), to: toIsoDate(to) }
    }

    /** 본인 요약을 조회한다 (최초 진입·기간 변경 시) */
    async function load(): Promise<void> {
        loading.value = true
        error.value = null
        try {
            const { from, to } = resolvedRange()
            summary.value = await usageApi.mySummary(from, to)
        } catch (e) {
            error.value = extractErrorMessage(e, '사용량을 불러오지 못했습니다.')
        } finally {
            loading.value = false
        }
    }

    /** 프리셋 전환 — 즉시 재조회 */
    function setPreset(next: MyUsagePreset): void {
        preset.value = next
        void load()
    }

    /**
     * 차트 막대 — 기간이 길면 주 단위로 묶어 막대 수를 7개 이하로 유지한다.
     * 관리자 화면과 같은 규칙을 쓴다(common/utils/usageBuckets).
     */
    const chartBuckets = computed<UsageBucket[]>(() => {
        const daily = summary.value?.daily ?? []
        return toUsageBuckets(daily, daily.length > 8 ? 7 : 1)
    })

    /** 차트 막대의 단위 — 화면에 표시해 오해를 막는다 */
    const chartUnit = computed<'일별' | '주별'>(() =>
        (summary.value?.daily.length ?? 0) > 8 ? '주별' : '일별',
    )

    return { preset, summary, chartBuckets, chartUnit, loading, error, load, setPreset }
}

/** Date → 'YYYY-MM-DD' (서버 요청 파라미터 형식) */
function toIsoDate(d: Date): string {
    return d.toISOString().slice(0, 10)
}
