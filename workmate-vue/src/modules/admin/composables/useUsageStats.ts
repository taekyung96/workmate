import { ref } from 'vue'
import { adminApi } from '../api/admin.api'
import { extractErrorMessage } from '@/common/utils/error'
import { usePagination } from '@/common/composables/usePagination'
import type { UsageSummary, UserUsage } from '../types'

/** 기간 선택 프리셋 — '7'·'30'은 클라이언트가 오늘 기준으로 계산, 'custom'은 사용자가 직접 지정 */
export type UsagePreset = '7' | '30' | 'custom'

/**
 * 관리자 사용량 대시보드 상태·동작 (관리자 사용량 대시보드).
 * 기간(최근 7일/30일/직접 지정) 선택에 따라 요약(summary)과 사용자별(by-user) 목록을 함께 갱신한다.
 * 날짜 계산의 단일 출처는 서버다 — '30일' 프리셋은 from/to를 비워 서버 기본값을 그대로 쓰고,
 * '7일'만 클라이언트에서 계산해 넘긴다(서버에 "최근 N일" 파라미터가 없어서다).
 *
 * @returns 기간 상태, 요약·사용자별 목록 상태, 조회/페이지 이동 액션
 */
export function useUsageStats() {
    const preset = ref<UsagePreset>('30')
    const customFrom = ref('')
    const customTo = ref('')

    const summary = ref<UsageSummary | null>(null)
    const users = ref<UserUsage[]>([])
    const loading = ref(false)
    const error = ref<string | null>(null)

    // 현재 선택된 기간을 요청 파라미터(from/to)로 변환한다.
    function resolvedRange(): { from?: string; to?: string } {
        if (preset.value === 'custom') {
            return { from: customFrom.value || undefined, to: customTo.value || undefined }
        }
        if (preset.value === '7') {
            const to = new Date()
            const from = new Date()
            from.setDate(from.getDate() - 6)
            return { from: toIsoDate(from), to: toIsoDate(to) }
        }
        return {} // 최근 30일 — from/to 를 비워 서버 기본값을 쓴다
    }

    // by-user 목록은 페이지 이동 시 현재 선택된 기간으로 다시 조회한다
    const { page, totalPages, totalElements, goToPage, reset } = usePagination(
        async (target, size) => {
            const { from, to } = resolvedRange()
            const result = await adminApi.usageByUser(from, to, target, size)
            users.value = result.content
            return result
        },
    )

    /** 요약 + 사용자별 목록 첫 페이지를 함께 조회한다 (최초 진입·기간 변경 시) */
    async function load(): Promise<void> {
        loading.value = true
        error.value = null
        try {
            const { from, to } = resolvedRange()
            summary.value = await adminApi.usageSummary(from, to)
            await reset()
        } catch (e) {
            error.value = extractErrorMessage(e, '사용량 통계를 불러오지 못했습니다.')
        } finally {
            loading.value = false
        }
    }

    /** 사용자별 목록 페이지 이동 */
    async function changePage(target: number): Promise<void> {
        loading.value = true
        error.value = null
        try {
            await goToPage(target)
        } catch (e) {
            error.value = extractErrorMessage(e, '사용자별 목록을 불러오지 못했습니다.')
        } finally {
            loading.value = false
        }
    }

    /** 프리셋 전환(최근 7일/30일) — 즉시 재조회 */
    function setPreset(next: UsagePreset): void {
        preset.value = next
        void load()
    }

    return {
        preset,
        customFrom,
        customTo,
        summary,
        users,
        page,
        totalPages,
        totalElements,
        loading,
        error,
        load,
        setPreset,
        changePage,
    }
}

/** Date → 'YYYY-MM-DD' (서버 요청 파라미터 형식) */
function toIsoDate(d: Date): string {
    return d.toISOString().slice(0, 10)
}
