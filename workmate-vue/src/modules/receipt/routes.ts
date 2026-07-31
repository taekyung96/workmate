import type { RouteRecordRaw } from 'vue-router'
import type { PageTab } from '@/common/components/pageTabs'

/**
 * receipt 모듈 라우트 (인증 필요 — 전역 가드가 보호).
 * 분석·이력을 각각 라우트로 두어 새로고침·뒤로가기·링크 공유가 동작한다.
 */
export const receiptRoutes: RouteRecordRaw[] = [
    {
        path: '/receipt',
        name: 'receipt',
        component: () => import('./views/ReceiptAnalyzePage.vue'),
    },
    {
        path: '/receipt/history',
        name: 'receipt-history',
        component: () => import('./views/ReceiptHistoryPage.vue'),
    },
]

/** 영수증 화면 공통 탭 */
export const receiptTabs: PageTab[] = [
    { name: 'receipt', label: '분석' },
    { name: 'receipt-history', label: '이력' },
]
