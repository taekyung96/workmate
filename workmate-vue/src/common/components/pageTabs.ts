/** 공통 페이지 탭 정의 (라우터 이동형) */
export interface PageTab {
    /** 이동할 라우트 name */
    name: string
    /** 탭에 표시할 라벨 */
    label: string
    /**
     * 이 탭을 활성으로 표시할 라우트 name 목록.
     * 하위 화면(예: 이력 상세)에서도 목록 탭이 활성으로 보이게 할 때 쓴다.
     * 미지정 시 name 자체로만 판정한다.
     */
    match?: string[]
}

/**
 * 현재 라우트가 해당 탭에 속하는지 판정한다.
 *
 * @param tab              탭 정의
 * @param currentRouteName 현재 라우트 name (없으면 빈 문자열)
 * @returns 활성 여부
 */
export function isTabActive(tab: PageTab, currentRouteName: string): boolean {
    if (!currentRouteName) return false
    const names = tab.match ?? [tab.name]
    return names.includes(currentRouteName)
}
