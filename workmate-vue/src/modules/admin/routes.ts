import type { RouteRecordRaw } from 'vue-router'

/**
 * admin 모듈 라우트 — ROLE_ADMIN 전용(meta.requiresAdmin, 가드가 비관리자를 /chat으로 돌림).
 * 서버 측 접근 차단(WEB SecurityConfig)과 이중으로 보호된다.
 */
export const adminRoutes: RouteRecordRaw[] = [
    {
        path: '/admin/users',
        name: 'admin-users',
        component: () => import('./views/AdminUsersPage.vue'),
        meta: { requiresAdmin: true },
    },
    {
        path: '/admin/audit-logs',
        name: 'admin-audit-logs',
        component: () => import('./views/AdminAuditLogPage.vue'),
        meta: { requiresAdmin: true },
    },
    {
        path: '/admin/usage',
        name: 'admin-usage',
        component: () => import('./views/AdminUsagePage.vue'),
        meta: { requiresAdmin: true },
    },
    {
        path: '/admin/common-codes',
        name: 'admin-common-codes',
        component: () => import('./views/AdminCommonCodesPage.vue'),
        meta: { requiresAdmin: true },
    },
]
