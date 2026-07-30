<script setup lang="ts">
/**
 * 관리자 — 감사 로그 조회 (/admin/audit-logs, M4).
 * 관리자 조치(잠금 해제·비밀번호 초기화) 이력을 최신순 페이징으로 열람한다.
 * append-only 기록이라 조회만 가능하며, 접근 제어는 라우트 가드(requiresAdmin) + WEB Security가 담당한다.
 */
import { onMounted } from 'vue'
import { Button } from '@/common/components/ui/button'
import { Badge } from '@/common/components/ui/badge'
import { Spinner } from '@/common/components/ui/spinner'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import { formatDateTime } from '@/common/utils/format'
import AdminTabs from '../components/AdminTabs.vue'
import { useAdminAuditLogs } from '../composables/useAdminAuditLogs'

const { logs, page, totalPages, totalElements, loading, error, load, goToPage } =
    useAdminAuditLogs()

onMounted(load)

/** 행위 코드 → 한글 표기 */
function actionLabel(action: string): string {
    if (action === 'UNLOCK') return '잠금 해제'
    if (action === 'RESET_PASSWORD') return '비밀번호 초기화'
    return action
}
</script>

<template>
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-5xl px-6 py-8">
            <h1 class="mb-6 text-2xl font-semibold">관리자</h1>

            <AdminTabs />

            <Alert v-if="error" variant="destructive" class="mb-4">
                <AlertDescription>{{ error }}</AlertDescription>
            </Alert>

            <div v-if="loading" class="flex justify-center py-16">
                <Spinner class="size-6" />
            </div>

            <p
                v-else-if="logs.length === 0"
                class="py-16 text-center text-sm text-muted-foreground"
            >
                감사 로그가 없습니다.
            </p>

            <template v-else>
                <div class="overflow-x-auto rounded-lg border">
                    <table class="w-full text-sm">
                        <thead class="border-b bg-muted/40 text-left text-muted-foreground">
                            <tr>
                                <th class="px-4 py-2.5 font-medium">시각</th>
                                <th class="px-4 py-2.5 font-medium">행위자</th>
                                <th class="px-4 py-2.5 font-medium">대상</th>
                                <th class="px-4 py-2.5 font-medium">행위</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr
                                v-for="row in logs"
                                :key="row.auditSeq"
                                class="border-b last:border-0 hover:bg-accent/40"
                            >
                                <td class="px-4 py-2.5 tabular-nums">
                                    {{ formatDateTime(row.createdAt) }}
                                </td>
                                <td class="px-4 py-2.5">{{ row.adminUserName }}</td>
                                <td class="px-4 py-2.5">{{ row.targetUserName }}</td>
                                <td class="px-4 py-2.5">
                                    <Badge
                                        :variant="
                                            row.action === 'RESET_PASSWORD'
                                                ? 'destructive'
                                                : 'secondary'
                                        "
                                    >
                                        {{ actionLabel(row.action) }}
                                    </Badge>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <!-- 페이징 -->
                <div class="mt-4 flex items-center justify-between text-sm text-muted-foreground">
                    <span>총 {{ totalElements }}건</span>
                    <div class="flex items-center gap-3">
                        <Button
                            size="sm"
                            variant="outline"
                            :disabled="page <= 0"
                            @click="goToPage(page - 1)"
                        >
                            이전
                        </Button>
                        <span>{{ page + 1 }} / {{ totalPages }}</span>
                        <Button
                            size="sm"
                            variant="outline"
                            :disabled="page >= totalPages - 1"
                            @click="goToPage(page + 1)"
                        >
                            다음
                        </Button>
                    </div>
                </div>
            </template>
        </div>
    </div>
</template>
