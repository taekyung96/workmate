import { ref } from 'vue'

/** 서버 페이징 응답의 공통 메타 (WAS의 *PageVo와 대응) */
export interface PageMeta {
    /** 0-based 현재 페이지 */
    page: number
    totalPages: number
    totalElements: number
}

/**
 * 서버 사이드 페이징 공통 상태·동작 (관리자·가이드·영수증·회의록 등 공유).
 *
 * 실제 데이터 조회는 호출자가 넘긴 `loader(page, size)`가 담당하고(목록 데이터는 호출자가 채움),
 * 이 컴포저블은 한 페이지 크기(pageSize)와 페이지 메타(page·totalPages·totalElements),
 * 페이지 이동만 관리한다. 페이지 크기의 단일 출처가 여기이고, 서버 요청 size로 loader에 그대로 넘긴다.
 *
 * @param loader   지정 페이지·크기를 조회하고 페이지 메타를 반환하는 함수
 * @param pageSize 한 페이지에 요청할 문서 개수 (기본 10)
 * @returns 페이지 상태(pageSize 포함)와 이동/리셋 액션
 */
export function usePagination(
    loader: (page: number, size: number) => Promise<PageMeta>,
    pageSize = 10,
) {
    const page = ref(0)
    const totalPages = ref(0)
    const totalElements = ref(0)

    /** 지정 페이지(기본: 현재 페이지)를 pageSize 크기로 로드하고 메타를 갱신한다 */
    async function loadPage(target: number = page.value): Promise<void> {
        const meta = await loader(target, pageSize)
        page.value = meta.page
        totalPages.value = meta.totalPages
        totalElements.value = meta.totalElements
    }

    /** 페이지 이동 — 범위 밖이거나 현재 페이지면 무시 */
    async function goToPage(target: number): Promise<void> {
        if (target < 0 || target >= totalPages.value || target === page.value) return
        await loadPage(target)
    }

    /** 첫 페이지부터 다시 로드 (검색어 변경 등 조건이 바뀔 때) */
    async function reset(): Promise<void> {
        await loadPage(0)
    }

    return { page, pageSize, totalPages, totalElements, loadPage, goToPage, reset }
}
