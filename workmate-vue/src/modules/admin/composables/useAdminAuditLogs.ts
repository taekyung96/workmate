import { ref } from 'vue'
import { adminApi } from '../api/admin.api'
import { extractErrorMessage } from '@/common/utils/error'
import { usePagination } from '@/common/composables/usePagination'
import type { AuditLog } from '../types'

/**
 * 관리자 감사 로그 조회 상태·동작 (M4).
 * 최신순 페이징 목록만 제공한다 (검색·필터는 스코프 밖). 페이징은 공통 usePagination 에 위임(페이지 크기 기본 10).
 *
 * @returns 목록 상태와 로드/페이지 이동 액션
 */
export function useAdminAuditLogs() {
    const logs = ref<AuditLog[]>([])
    const loading = ref(false)
    const error = ref<string | null>(null)

    // 페이지 이동 시 호출될 로더 — page·size 로 조회해 목록을 채우고 페이지 메타를 반환한다
    const { page, totalPages, totalElements, loadPage, goToPage } = usePagination(
        async (target, size) => {
            const result = await adminApi.auditLogs(target, size)
            logs.value = result.content
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
            error.value = extractErrorMessage(e, '감사 로그를 불러오지 못했습니다.')
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

    return { logs, page, totalPages, totalElements, loading, error, load, goToPage: changePage }
}
