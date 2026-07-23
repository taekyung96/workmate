import { createRouter, createWebHistory } from 'vue-router'
import { authRoutes } from '@/modules/auth/routes'
import { chatRoutes } from '@/modules/chat/routes'
import { guideRoutes } from '@/modules/guide/routes'
import { receiptRoutes } from '@/modules/receipt/routes'
import { adminRoutes } from '@/modules/admin/routes'
import { registerGuards } from './guards'

/**
 * 각 모듈이 내보낸 라우트를 취합해 라우터를 구성한다.
 * 기능 추가 = 모듈의 routes.ts를 여기 spread로 더하기.
 */
const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        { path: '/', redirect: '/chat' },
        ...authRoutes,
        ...chatRoutes,
        ...guideRoutes,
        ...receiptRoutes,
        ...adminRoutes,
        {
            path: '/:pathMatch(.*)*',
            name: 'not-found',
            component: () => import('@/common/components/NotFoundPage.vue'),
        },
    ],
})

registerGuards(router)

export default router
