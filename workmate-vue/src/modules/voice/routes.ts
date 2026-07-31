import type { RouteRecordRaw } from 'vue-router'
import type { PageTab } from '@/common/components/pageTabs'

/**
 * voice 모듈 라우트 (인증 필요 — 전역 가드가 보호). F8-1.
 * 분석·이력·상세를 각각 라우트로 두어 새로고침·뒤로가기·링크 공유가 동작한다.
 */
export const voiceRoutes: RouteRecordRaw[] = [
    {
        path: '/voice',
        name: 'voice',
        component: () => import('./views/VoiceAnalyzePage.vue'),
    },
    {
        path: '/voice/history',
        name: 'voice-history',
        component: () => import('./views/VoiceHistoryPage.vue'),
    },
    {
        path: '/voice/history/:recordSeq',
        name: 'voice-record',
        component: () => import('./views/VoiceRecordPage.vue'),
    },
]

/** 회의록 화면 공통 탭 — 상세에서도 '이력' 탭이 활성으로 보이게 match 를 준다 */
export const voiceTabs: PageTab[] = [
    { name: 'voice', label: '분석' },
    { name: 'voice-history', label: '이력', match: ['voice-history', 'voice-record'] },
]
