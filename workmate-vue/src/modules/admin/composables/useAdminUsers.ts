import { ref } from 'vue'
import { adminApi } from '../api/admin.api'
import { extractErrorMessage } from '@/common/utils/error'
import { usePagination } from '@/common/composables/usePagination'
import type { AdminUser } from '../types'

/**
 * 관리자 사용자 관리 상태·동작 (M1~M3).
 * 목록 조회(검색·페이징), 계정 잠금 해제, 비밀번호 초기화를 담당한다.
 * 페이징은 공통 usePagination 에 위임한다(페이지 크기 기본 10).
 *
 * @returns 목록 상태와 액션들
 */
export function useAdminUsers() {
    const users = ref<AdminUser[]>([])
    const keyword = ref('')
    const loading = ref(false)
    const error = ref<string | null>(null)

    // 페이지 이동 시 호출될 로더 — keyword·page·size 로 조회해 목록을 채우고 페이지 메타를 반환한다
    const { page, totalPages, totalElements, loadPage, goToPage, reset } = usePagination(
        async (target, size) => {
            const result = await adminApi.users(keyword.value, target, size)
            users.value = result.content
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
            error.value = extractErrorMessage(e, '사용자 목록을 불러오지 못했습니다.')
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

    /** 계정 잠금 해제 후 목록 갱신 */
    async function unlock(userSeq: number): Promise<void> {
        await adminApi.unlock(userSeq)
        await load()
    }

    /** 비밀번호 초기화 — 임시 비밀번호 평문 반환 (호출부에서 1회 표시) */
    async function resetPassword(userSeq: number): Promise<string> {
        const result = await adminApi.resetPassword(userSeq)
        return result.tempPassword
    }

    return {
        users,
        keyword,
        page,
        totalPages,
        totalElements,
        loading,
        error,
        load,
        search,
        goToPage: changePage,
        unlock,
        resetPassword,
    }
}
