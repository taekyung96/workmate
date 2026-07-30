import type { RouteRecordRaw } from 'vue-router'

/**
 * voice 모듈 라우트 (인증 필요 — 전역 가드가 보호). F8-1.
 */
export const voiceRoutes: RouteRecordRaw[] = [
    {
        path: '/voice',
        name: 'voice',
        component: () => import('./views/VoicePage.vue'),
    },
]
