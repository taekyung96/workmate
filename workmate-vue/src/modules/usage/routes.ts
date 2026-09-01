import type { RouteRecordRaw } from 'vue-router'

/**
 * usage 모듈 라우트 — 로그인한 모든 사용자가 본인 사용량을 본다.
 * 관리자 전용이 아니므로 requiresAdmin 을 붙이지 않는다(인증은 전역 가드가 담당).
 */
export const usageRoutes: RouteRecordRaw[] = [
    {
        path: '/usage',
        name: 'my-usage',
        component: () => import('./views/MyUsagePage.vue'),
    },
]
