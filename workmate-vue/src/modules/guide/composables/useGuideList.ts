import { ref } from 'vue'
import { extractErrorMessage } from '@/common/utils/error'
import { usePagination } from '@/common/composables/usePagination'
import { guideApi } from '../api/guide.api'
import type { GuideSummary } from '../types'

/**
 * 가이드 목록 화면 로직 (≈ Service). 서버 페이징 + 키워드 검색을 담당한다.
 * 실제 페이지 상태·이동은 공통 usePagination 에 위임하고, 목록 데이터·검색어·에러만 여기서 관리한다.
 */
export function useGuideList() {
    const guides = ref<GuideSummary[]>([])
    const keyword = ref('')
    const loading = ref(false)
    const error = ref('')

    // 페이지 이동 시 호출될 로더 — 지정 페이지를 조회해 목록을 채우고 페이지 메타를 반환한다
    const { page, totalPages, totalElements, loadPage, goToPage, reset } = usePagination(
        async (target) => {
            const result = await guideApi.list({ keyword: keyword.value, page: target })
            guides.value = result.content
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
            error.value = extractErrorMessage(e, '가이드 목록을 불러오지 못했습니다.')
        } finally {
            loading.value = false
        }
    }

    /** 최초·현재 페이지 로드 */
    async function load(): Promise<void> {
        await run(() => loadPage())
    }

    /** 검색 실행 — 첫 페이지부터 다시 조회 */
    async function search(): Promise<void> {
        await run(() => reset())
    }

    /** 페이지 이동 */
    async function changePage(target: number): Promise<void> {
        await run(() => goToPage(target))
    }

    return {
        guides,
        keyword,
        page,
        totalPages,
        totalElements,
        loading,
        error,
        load,
        search,
        changePage,
    }
}
