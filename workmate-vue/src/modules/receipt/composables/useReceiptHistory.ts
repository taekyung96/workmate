import { ref } from 'vue'
import { receiptApi } from '../api/receipt.api'
import { extractErrorMessage } from '@/common/utils/error'
import { usePagination } from '@/common/composables/usePagination'
import type { Receipt } from '../types'

/**
 * 영수증 [이력] 탭 상태·동작.
 * 목록 로드(서버 페이징, 공통 usePagination 위임)와 CSV 다운로드를 담당한다.
 *
 * @returns 이력 상태와 액션들
 */
export function useReceiptHistory() {
    const receipts = ref<Receipt[]>([])
    const loading = ref(false)
    const error = ref<string | null>(null)

    // 페이지 이동 시 호출될 로더 — page·size 로 조회해 목록을 채우고 페이지 메타를 반환한다
    const { page, totalPages, totalElements, loadPage, goToPage } = usePagination(
        async (target, size) => {
            const result = await receiptApi.history(target, size)
            receipts.value = result.content
            return result
        },
    )

    /** loading·error 처리를 공통으로 감싸는 실행 래퍼 */
    async function run(action: () => Promise<void>): Promise<void> {
        loading.value = true
        error.value = null
        try {
            await action()
        } catch (e) {
            error.value = extractErrorMessage(e, '영수증 이력을 불러오지 못했습니다.')
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

    /** CSV 다운로드 — blob을 받아 임시 링크로 저장 트리거 (전체 이력) */
    async function downloadCsv(): Promise<void> {
        try {
            const blob = await receiptApi.downloadCsv()
            const url = URL.createObjectURL(blob)
            const a = document.createElement('a')
            a.href = url
            a.download = 'receipts.csv'
            a.click()
            URL.revokeObjectURL(url)
        } catch (e) {
            error.value = extractErrorMessage(e, 'CSV 다운로드에 실패했습니다.')
        }
    }

    return {
        receipts,
        page,
        totalPages,
        totalElements,
        loading,
        error,
        load,
        goToPage: changePage,
        downloadCsv,
    }
}
