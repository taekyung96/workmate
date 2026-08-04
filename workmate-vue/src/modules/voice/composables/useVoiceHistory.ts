import { ref } from 'vue'
import { extractErrorMessage } from '@/common/utils/error'
import { usePagination } from '@/common/composables/usePagination'
import { voiceApi } from '../api/voice.api'
import type { VoiceRecordSummary } from '../types'

/**
 * 회의록 이력 화면 로직 (≈ Service).
 * 목록 조회(서버 페이징, 공통 usePagination 위임)와 삭제를 담당한다.
 *
 * @returns 이력 상태와 액션들
 */
export function useVoiceHistory() {
    const records = ref<VoiceRecordSummary[]>([])
    const loading = ref(false)
    const error = ref('')

    // 페이지 이동 시 호출될 로더 — page·size 로 조회해 목록을 채우고 페이지 메타를 반환한다
    const { page, totalPages, totalElements, loadPage, goToPage } = usePagination(
        async (target, size) => {
            const result = await voiceApi.history(target, size)
            records.value = result.content
            return result
        },
    )

    /** loading·error 처리를 공통으로 감싸는 실행 래퍼 */
    async function run(action: () => Promise<void>): Promise<void> {
        loading.value = true
        error.value = ''
        try {
            await action()
        } catch (e) {
            error.value = extractErrorMessage(e, '회의록 이력을 불러오지 못했습니다.')
        } finally {
            loading.value = false
        }
    }

    /** 최초·현재 페이지 로드 */
    async function load(): Promise<void> {
        await run(() => loadPage())
    }

    /** 페이지 이동 */
    async function changePage(target: number): Promise<void> {
        await run(() => goToPage(target))
    }

    /** 회의록 삭제 후 현재 페이지를 다시 로드해 목록·총건수를 갱신한다 */
    async function remove(recordSeq: number): Promise<void> {
        error.value = ''
        try {
            await voiceApi.remove(recordSeq)
            await load()
        } catch (e) {
            error.value = extractErrorMessage(e, '회의록 삭제에 실패했습니다.')
        }
    }

    return {
        records,
        page,
        totalPages,
        totalElements,
        loading,
        error,
        load,
        goToPage: changePage,
        remove,
    }
}
