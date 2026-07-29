import { ref } from 'vue'
import { adminApi } from '../api/admin.api'
import { extractErrorMessage } from '@/common/utils/error'
import type { AuditLog } from '../types'

/**
 * 관리자 감사 로그 조회 상태·동작 (M4).
 * 최신순 페이징 목록만 제공한다 (검색·필터는 스코프 밖 — useAdminUsers의 축소판).
 *
 * @returns 목록 상태와 로드/페이지 이동 액션
 */
export function useAdminAuditLogs() {
    const logs = ref<AuditLog[]>([])
    const page = ref(0) // 0-based
    const totalPages = ref(0)
    const totalElements = ref(0)
    const loading = ref(false)
    const error = ref<string | null>(null)

    /** 현재 page로 목록 로드 */
    async function load(): Promise<void> {
        loading.value = true
        error.value = null
        try {
            const result = await adminApi.auditLogs(page.value)
            logs.value = result.content
            totalPages.value = result.totalPages
            totalElements.value = result.totalElements
            page.value = result.page
        } catch (e) {
            error.value = extractErrorMessage(e, '감사 로그를 불러오지 못했습니다.')
        } finally {
            loading.value = false
        }
    }

    /** 페이지 이동 (범위 밖이면 무시) */
    async function goToPage(target: number): Promise<void> {
        if (target < 0 || target >= totalPages.value || target === page.value) return
        page.value = target
        await load()
    }

    return { logs, page, totalPages, totalElements, loading, error, load, goToPage }
}
