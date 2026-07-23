import { ref } from 'vue'
import { adminApi } from '../api/admin.api'
import { extractErrorMessage } from '@/common/utils/error'
import type { AdminUser } from '../types'

/**
 * 관리자 사용자 관리 상태·동작 (M1~M3).
 * 목록 조회(검색·페이징), 계정 잠금 해제, 비밀번호 초기화를 담당한다.
 *
 * @returns 목록 상태와 액션들
 */
export function useAdminUsers() {
    const users = ref<AdminUser[]>([])
    const keyword = ref('')
    const page = ref(0) // 0-based
    const totalPages = ref(0)
    const totalElements = ref(0)
    const loading = ref(false)
    const error = ref<string | null>(null)

    /** 현재 keyword·page로 목록 로드 */
    async function load(): Promise<void> {
        loading.value = true
        error.value = null
        try {
            const result = await adminApi.users(keyword.value, page.value)
            users.value = result.content
            totalPages.value = result.totalPages
            totalElements.value = result.totalElements
            page.value = result.page
        } catch (e) {
            error.value = extractErrorMessage(e, '사용자 목록을 불러오지 못했습니다.')
        } finally {
            loading.value = false
        }
    }

    /** 검색 실행 — 첫 페이지부터 다시 조회 */
    async function search(): Promise<void> {
        page.value = 0
        await load()
    }

    /** 페이지 이동 (범위 밖이면 무시) */
    async function goToPage(target: number): Promise<void> {
        if (target < 0 || target >= totalPages.value || target === page.value) return
        page.value = target
        await load()
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
        goToPage,
        unlock,
        resetPassword,
    }
}
