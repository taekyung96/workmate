<script setup lang="ts">
/**
 * 관리자 — 사용자 관리 (/admin/users, M1~M3).
 * 목록·검색·페이징 + 계정 잠금 해제(확인 모달) + 비밀번호 초기화(임시 비번 1회 표시).
 * 접근 제어는 라우트 가드(requiresAdmin) + WEB Security가 담당한다.
 */
import { onMounted, ref } from 'vue'
import { Check, Copy, Search } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { Input } from '@/common/components/ui/input'
import { Badge } from '@/common/components/ui/badge'
import { Spinner } from '@/common/components/ui/spinner'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/common/components/ui/alert-dialog'
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/common/components/ui/dialog'
import { formatDate } from '@/common/utils/format'
import { extractErrorMessage } from '@/common/utils/error'
import AdminTabs from '../components/AdminTabs.vue'
import { useAdminUsers } from '../composables/useAdminUsers'
import type { AdminUser } from '../types'

const {
    users,
    keyword,
    page,
    totalPages,
    totalElements,
    loading,
    error,
    load,
    search,
    goToPage,
    unlock,
    resetPassword,
} = useAdminUsers()

// 확인 모달 — 열림 상태(boolean)와 대상(target)을 분리한다.
// 확정 버튼 클릭 시 다이얼로그가 닫히며 open이 false로 바뀌는데, 이때 target까지 비우면
// 확정 핸들러가 대상을 못 읽는 경합이 생긴다. 그래서 닫힘은 open만 끄고 target은 유지한다.
const unlockOpen = ref(false)
const unlockTarget = ref<AdminUser | null>(null)
const resetOpen = ref(false)
const resetTarget = ref<AdminUser | null>(null)
// 비번 초기화 결과 (1회 표시)
const tempPassword = ref<string | null>(null)
const tempCopied = ref(false)
const actionError = ref<string | null>(null)

onMounted(load)

/** 잠금 해제 확인 모달 열기 */
function askUnlock(user: AdminUser): void {
    unlockTarget.value = user
    unlockOpen.value = true
}

/** 비번 초기화 확인 모달 열기 */
function askReset(user: AdminUser): void {
    resetTarget.value = user
    resetOpen.value = true
}

/** 잠금 해제 확정 */
async function confirmUnlock(): Promise<void> {
    const target = unlockTarget.value
    unlockOpen.value = false
    if (!target) return
    actionError.value = null
    try {
        await unlock(target.userSeq)
    } catch (e) {
        actionError.value = extractErrorMessage(e, '잠금 해제에 실패했습니다.')
    }
}

/** 비번 초기화 확정 → 임시 비밀번호 모달 오픈 */
async function confirmReset(): Promise<void> {
    const target = resetTarget.value
    resetOpen.value = false
    if (!target) return
    actionError.value = null
    try {
        tempPassword.value = await resetPassword(target.userSeq)
        tempCopied.value = false
    } catch (e) {
        actionError.value = extractErrorMessage(e, '비밀번호 초기화에 실패했습니다.')
    }
}

/** 임시 비밀번호 복사 */
async function copyTemp(): Promise<void> {
    if (!tempPassword.value) return
    try {
        await navigator.clipboard.writeText(tempPassword.value)
        tempCopied.value = true
    } catch {
        // 복사 실패는 조용히 무시
    }
}

/** 역할 한글 표기 */
function roleLabel(role: string): string {
    return role === 'ADMIN' || role === 'ROLE_ADMIN' ? '관리자' : '사용자'
}
</script>

<template>
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-5xl px-6 py-8">
            <h1 class="mb-6 text-2xl font-semibold">관리자</h1>

            <AdminTabs />

            <!-- 검색 -->
            <form class="mb-4 flex gap-2" @submit.prevent="search">
                <Input v-model="keyword" placeholder="이메일 또는 이름 검색" class="max-w-xs" />
                <Button type="submit" variant="outline">
                    <Search class="mr-2 size-4" />
                    검색
                </Button>
            </form>

            <Alert v-if="error || actionError" variant="destructive" class="mb-4">
                <AlertDescription>{{ error || actionError }}</AlertDescription>
            </Alert>

            <div v-if="loading" class="flex justify-center py-16">
                <Spinner class="size-6" />
            </div>

            <p
                v-else-if="users.length === 0"
                class="py-16 text-center text-sm text-muted-foreground"
            >
                조건에 맞는 사용자가 없습니다.
            </p>

            <template v-else>
                <div class="overflow-x-auto rounded-lg border">
                    <table class="w-full text-sm">
                        <thead class="border-b bg-muted/40 text-left text-muted-foreground">
                            <tr>
                                <th class="px-4 py-2.5 font-medium">이름</th>
                                <th class="px-4 py-2.5 font-medium">이메일</th>
                                <th class="px-4 py-2.5 font-medium">전화번호</th>
                                <th class="px-4 py-2.5 font-medium">권한</th>
                                <th class="px-4 py-2.5 font-medium">상태</th>
                                <th class="px-4 py-2.5 font-medium">가입일</th>
                                <th class="px-4 py-2.5 text-right font-medium">관리</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr
                                v-for="u in users"
                                :key="u.userSeq"
                                class="border-b last:border-0 hover:bg-accent/40"
                            >
                                <td class="px-4 py-2.5">{{ u.userName }}</td>
                                <td class="px-4 py-2.5 tabular-nums">{{ u.maskedEmail }}</td>
                                <td class="px-4 py-2.5 tabular-nums">{{ u.maskedPhone || '—' }}</td>
                                <td class="px-4 py-2.5">{{ roleLabel(u.role) }}</td>
                                <td class="px-4 py-2.5">
                                    <Badge v-if="u.locked" variant="destructive">잠금</Badge>
                                    <Badge v-else variant="secondary">정상</Badge>
                                </td>
                                <td class="px-4 py-2.5">{{ formatDate(u.createdAt) }}</td>
                                <td class="px-4 py-2.5">
                                    <div class="flex justify-end gap-1.5">
                                        <Button
                                            size="sm"
                                            variant="outline"
                                            :disabled="!u.locked"
                                            @click="askUnlock(u)"
                                        >
                                            잠금 해제
                                        </Button>
                                        <Button size="sm" variant="outline" @click="askReset(u)">
                                            비번 초기화
                                        </Button>
                                    </div>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <!-- 페이징 -->
                <div class="mt-4 flex items-center justify-between text-sm text-muted-foreground">
                    <span>총 {{ totalElements }}명</span>
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

            <!-- 잠금 해제 확인 -->
            <AlertDialog v-model:open="unlockOpen">
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>계정 잠금을 해제할까요?</AlertDialogTitle>
                        <AlertDialogDescription>
                            {{ unlockTarget?.userName }}님의 로그인 실패 횟수를 초기화하고 잠금을
                            해제합니다.
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>취소</AlertDialogCancel>
                        <AlertDialogAction @click="confirmUnlock">잠금 해제</AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            <!-- 비밀번호 초기화 확인 -->
            <AlertDialog v-model:open="resetOpen">
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>비밀번호를 초기화할까요?</AlertDialogTitle>
                        <AlertDialogDescription>
                            {{ resetTarget?.userName }}님의 비밀번호를 임시 비밀번호로 변경합니다.
                            임시 비밀번호는 이 창을 닫으면 다시 볼 수 없습니다.
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>취소</AlertDialogCancel>
                        <AlertDialogAction @click="confirmReset">초기화</AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            <!-- 임시 비밀번호 1회 표시 -->
            <Dialog :open="tempPassword !== null" @update:open="(v) => !v && (tempPassword = null)">
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>임시 비밀번호</DialogTitle>
                        <DialogDescription>
                            아래 임시 비밀번호를 사용자에게 전달하세요. 이 창을 닫으면 다시 확인할
                            수 없습니다.
                        </DialogDescription>
                    </DialogHeader>
                    <div class="flex items-center gap-2 rounded-md border bg-muted/40 px-3 py-2">
                        <code class="flex-1 font-mono text-base">{{ tempPassword }}</code>
                        <button
                            type="button"
                            class="flex items-center gap-1 rounded-md border px-2 py-1.5 text-xs hover:bg-accent"
                            @click="copyTemp"
                        >
                            <Check v-if="tempCopied" class="size-4 text-green-600" />
                            <Copy v-else class="size-4" />
                            {{ tempCopied ? '복사됨' : '복사' }}
                        </button>
                    </div>
                    <DialogFooter>
                        <Button @click="tempPassword = null">닫기</Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    </div>
</template>
