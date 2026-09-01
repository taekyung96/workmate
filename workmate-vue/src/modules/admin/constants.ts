import type { PageTab } from '@/common/components/pageTabs'

/**
 * 관리자 하위 화면 탭 정의.
 * 사이드바는 관리자 진입점 하나만 유지하고, 화면 간 이동은 이 탭이 담당한다.
 * 각 화면에 흩어져 있던 같은 배열을 한 곳으로 모았다 — 탭이 늘 때 한 군데만 고치면 된다.
 */
export const adminTabs: PageTab[] = [
    { name: 'admin-users', label: '사용자 관리' },
    { name: 'admin-audit-logs', label: '감사 로그' },
    { name: 'admin-usage', label: '사용량' },
    { name: 'admin-common-codes', label: '공통코드' },
]
