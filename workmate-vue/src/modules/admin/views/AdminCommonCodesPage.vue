<script setup lang="ts">
/**
 * 관리자 — 공통코드 관리 (/admin/common-codes, F7).
 * 좌측 그룹 목록 + 우측 선택 그룹의 코드 목록. 그룹·코드 등록/수정/삭제/사용여부 토글.
 * 접근 제어는 라우트 가드(requiresAdmin) + WEB Security가 담당한다.
 */
import { onMounted, reactive, ref } from 'vue'
import { toast } from 'vue-sonner'
import { Pencil, Plus, Trash2, ShieldCheck } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { Input } from '@/common/components/ui/input'
import { Badge } from '@/common/components/ui/badge'
import { Switch } from '@/common/components/ui/switch'
import { Spinner } from '@/common/components/ui/spinner'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/common/components/ui/dialog'
import { extractErrorMessage } from '@/common/utils/error'
import PageTabs from '@/common/components/PageTabs.vue'
import PageHeader from '@/common/components/PageHeader.vue'
import ConfirmDialog from '@/common/components/ConfirmDialog.vue'
import LoadingArea from '@/common/components/LoadingArea.vue'

import { adminTabs } from '../constants'
import { useCommonCodes } from '../composables/useCommonCodes'
import type { CommonCodeGroup, CommonCodeItem } from '../types'

const {
    groups,
    selectedGroupCode,
    codes,
    loading,
    error,
    loadGroups,
    selectGroup,
    createGroup,
    updateGroup,
    deleteGroup,
    createCode,
    updateCode,
    deleteCode,
} = useCommonCodes()

const actionError = ref<string | null>(null)
// 저장 진행 중 플래그 — 중복 제출을 막고 버튼에 스피너를 표시한다
const saving = ref(false)

onMounted(loadGroups)

// ----- 그룹 다이얼로그 -----
const groupOpen = ref(false)
const groupMode = ref<'create' | 'edit'>('create')
const groupForm = reactive({ groupCode: '', groupName: '', description: '', useYn: true })

function openCreateGroup(): void {
    groupMode.value = 'create'
    Object.assign(groupForm, { groupCode: '', groupName: '', description: '', useYn: true })
    actionError.value = null
    groupOpen.value = true
}

function openEditGroup(g: CommonCodeGroup): void {
    groupMode.value = 'edit'
    Object.assign(groupForm, {
        groupCode: g.groupCode,
        groupName: g.groupName,
        description: g.description ?? '',
        useYn: g.useYn,
    })
    actionError.value = null
    groupOpen.value = true
}

async function saveGroup(): Promise<void> {
    if (saving.value) return
    actionError.value = null
    saving.value = true
    try {
        const body = {
            groupCode: groupForm.groupCode,
            groupName: groupForm.groupName,
            description: groupForm.description,
            useYn: groupForm.useYn,
        }
        const creating = groupMode.value === 'create'
        if (creating) await createGroup(body)
        else await updateGroup(groupForm.groupCode, body)
        groupOpen.value = false
        toast.success(creating ? '그룹을 등록했습니다.' : '그룹을 수정했습니다.')
    } catch (e) {
        actionError.value = extractErrorMessage(e, '그룹 저장에 실패했습니다.')
    } finally {
        saving.value = false
    }
}

// ----- 코드 다이얼로그 -----
const codeOpen = ref(false)
const codeMode = ref<'create' | 'edit'>('create')
const codeForm = reactive({ code: '', codeName: '', sortOrder: 0, useYn: true })

function openCreateCode(): void {
    if (!selectedGroupCode.value) return
    codeMode.value = 'create'
    Object.assign(codeForm, { code: '', codeName: '', sortOrder: 0, useYn: true })
    actionError.value = null
    codeOpen.value = true
}

function openEditCode(c: CommonCodeItem): void {
    codeMode.value = 'edit'
    Object.assign(codeForm, {
        code: c.code,
        codeName: c.codeName,
        sortOrder: c.sortOrder,
        useYn: c.useYn,
    })
    actionError.value = null
    codeOpen.value = true
}

async function saveCode(): Promise<void> {
    if (saving.value) return
    actionError.value = null
    saving.value = true
    try {
        const body = {
            code: codeForm.code,
            codeName: codeForm.codeName,
            sortOrder: Number(codeForm.sortOrder),
            useYn: codeForm.useYn,
        }
        const creating = codeMode.value === 'create'
        if (creating) await createCode(body)
        else await updateCode(codeForm.code, body)
        codeOpen.value = false
        toast.success(creating ? '코드를 등록했습니다.' : '코드를 수정했습니다.')
    } catch (e) {
        actionError.value = extractErrorMessage(e, '코드 저장에 실패했습니다.')
    } finally {
        saving.value = false
    }
}

// ----- 삭제 확인 (open과 target 분리 — AdminUsersPage와 동일 패턴) -----
const groupDeleteOpen = ref(false)
const groupDeleteTarget = ref<CommonCodeGroup | null>(null)
const codeDeleteOpen = ref(false)
const codeDeleteTarget = ref<CommonCodeItem | null>(null)

function askDeleteGroup(g: CommonCodeGroup): void {
    groupDeleteTarget.value = g
    groupDeleteOpen.value = true
}

async function confirmDeleteGroup(): Promise<void> {
    const target = groupDeleteTarget.value
    groupDeleteOpen.value = false
    if (!target) return
    actionError.value = null
    try {
        await deleteGroup(target.groupCode)
        toast.success('그룹을 삭제했습니다.')
    } catch (e) {
        // 확인창이 이미 닫혀 인라인 에러가 안 보이므로 toast 로 알린다
        toast.error(extractErrorMessage(e, '그룹 삭제에 실패했습니다.'))
    }
}

function askDeleteCode(c: CommonCodeItem): void {
    codeDeleteTarget.value = c
    codeDeleteOpen.value = true
}

async function confirmDeleteCode(): Promise<void> {
    const target = codeDeleteTarget.value
    codeDeleteOpen.value = false
    if (!target) return
    actionError.value = null
    try {
        await deleteCode(target.code)
        toast.success('코드를 삭제했습니다.')
    } catch (e) {
        // 확인창이 이미 닫혀 인라인 에러가 안 보이므로 toast 로 알린다
        toast.error(extractErrorMessage(e, '코드 삭제에 실패했습니다.'))
    }
}
</script>

<template>
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-5xl px-6 py-8">
            <PageHeader
                :icon="ShieldCheck"
                title="관리자"
                description="사용자·공통코드·감사 로그를 관리합니다."
            />

            <PageTabs :tabs="adminTabs" />

            <Alert v-if="error || actionError" variant="destructive" class="mb-4">
                <AlertDescription>{{ error || actionError }}</AlertDescription>
            </Alert>

            <LoadingArea v-if="loading" />

            <div v-else class="grid grid-cols-1 gap-4 md:grid-cols-[240px_1fr]">
                <!-- 그룹 목록 -->
                <section class="rounded-lg border">
                    <div class="flex items-center justify-between border-b px-3 py-2">
                        <span class="text-sm font-medium">그룹</span>
                        <Button size="sm" variant="ghost" @click="openCreateGroup">
                            <Plus class="size-4" />
                        </Button>
                    </div>
                    <ul class="max-h-[60vh] overflow-y-auto">
                        <li
                            v-for="g in groups"
                            :key="g.groupCode"
                            class="group flex items-center gap-1 border-b px-3 py-2 last:border-0"
                            :class="
                                g.groupCode === selectedGroupCode
                                    ? 'bg-accent'
                                    : 'hover:bg-accent/40'
                            "
                        >
                            <button
                                class="min-w-0 flex-1 text-left"
                                @click="selectGroup(g.groupCode)"
                            >
                                <div class="flex items-center gap-1.5">
                                    <span class="truncate text-sm font-medium">{{
                                        g.groupName
                                    }}</span>
                                    <Badge v-if="!g.useYn" variant="secondary" class="shrink-0">
                                        비활성
                                    </Badge>
                                </div>
                                <span class="block truncate text-xs text-muted-foreground">
                                    {{ g.groupCode }}
                                </span>
                            </button>
                            <button
                                class="hidden shrink-0 rounded p-1 text-muted-foreground hover:text-foreground group-hover:block"
                                title="수정"
                                @click="openEditGroup(g)"
                            >
                                <Pencil class="size-3.5" />
                            </button>
                            <button
                                class="hidden shrink-0 rounded p-1 text-muted-foreground hover:text-destructive group-hover:block"
                                title="삭제"
                                @click="askDeleteGroup(g)"
                            >
                                <Trash2 class="size-3.5" />
                            </button>
                        </li>
                        <li
                            v-if="groups.length === 0"
                            class="px-3 py-6 text-center text-sm text-muted-foreground"
                        >
                            그룹이 없습니다.
                        </li>
                    </ul>
                </section>

                <!-- 선택 그룹의 코드 -->
                <section class="rounded-lg border">
                    <div class="flex items-center justify-between border-b px-3 py-2">
                        <span class="text-sm font-medium">
                            코드
                            <span v-if="selectedGroupCode" class="text-muted-foreground">
                                · {{ selectedGroupCode }}
                            </span>
                        </span>
                        <Button
                            size="sm"
                            variant="outline"
                            :disabled="!selectedGroupCode"
                            @click="openCreateCode"
                        >
                            <Plus class="mr-1 size-4" />
                            코드 추가
                        </Button>
                    </div>

                    <p
                        v-if="!selectedGroupCode"
                        class="py-16 text-center text-sm text-muted-foreground"
                    >
                        좌측에서 그룹을 선택하세요.
                    </p>
                    <p
                        v-else-if="codes.length === 0"
                        class="py-16 text-center text-sm text-muted-foreground"
                    >
                        코드가 없습니다.
                    </p>

                    <div v-else class="overflow-x-auto">
                        <table class="w-full text-sm">
                            <thead class="border-b bg-muted/40 text-left text-muted-foreground">
                                <tr>
                                    <th class="px-3 py-2 font-medium">코드</th>
                                    <th class="px-3 py-2 font-medium">이름</th>
                                    <th class="px-3 py-2 font-medium">정렬</th>
                                    <th class="px-3 py-2 font-medium">상태</th>
                                    <th class="px-3 py-2 text-right font-medium">관리</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr
                                    v-for="c in codes"
                                    :key="c.code"
                                    class="border-b last:border-0 hover:bg-accent/40"
                                >
                                    <td class="px-3 py-2 font-mono text-xs">{{ c.code }}</td>
                                    <td class="px-3 py-2">{{ c.codeName }}</td>
                                    <td class="px-3 py-2 tabular-nums">{{ c.sortOrder }}</td>
                                    <td class="px-3 py-2">
                                        <Badge v-if="c.useYn" variant="secondary">사용</Badge>
                                        <Badge v-else variant="outline">비활성</Badge>
                                    </td>
                                    <td class="px-3 py-2">
                                        <div class="flex justify-end gap-1.5">
                                            <Button
                                                size="sm"
                                                variant="outline"
                                                @click="openEditCode(c)"
                                            >
                                                수정
                                            </Button>
                                            <Button
                                                size="sm"
                                                variant="outline"
                                                @click="askDeleteCode(c)"
                                            >
                                                삭제
                                            </Button>
                                        </div>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </section>
            </div>

            <!-- 그룹 등록/수정 다이얼로그 -->
            <Dialog v-model:open="groupOpen">
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>{{
                            groupMode === 'create' ? '그룹 추가' : '그룹 수정'
                        }}</DialogTitle>
                        <DialogDescription>공통코드 그룹 정보를 입력하세요.</DialogDescription>
                    </DialogHeader>
                    <div class="space-y-3">
                        <div>
                            <label class="mb-1 block text-sm font-medium">그룹 코드</label>
                            <Input
                                v-model="groupForm.groupCode"
                                :disabled="groupMode === 'edit'"
                                placeholder="예: AI_MODEL"
                            />
                        </div>
                        <div>
                            <label class="mb-1 block text-sm font-medium">그룹명</label>
                            <Input v-model="groupForm.groupName" placeholder="예: AI 답변 모델" />
                        </div>
                        <div>
                            <label class="mb-1 block text-sm font-medium">설명</label>
                            <Input v-model="groupForm.description" placeholder="(선택)" />
                        </div>
                        <div class="flex items-center gap-2">
                            <Switch v-model="groupForm.useYn" />
                            <span class="text-sm">사용</span>
                        </div>
                    </div>
                    <DialogFooter>
                        <Button variant="outline" :disabled="saving" @click="groupOpen = false">
                            취소
                        </Button>
                        <Button
                            :disabled="saving || groupForm.groupName.trim() === ''"
                            @click="saveGroup"
                        >
                            <Spinner v-if="saving" class="mr-2 size-4" />
                            저장
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            <!-- 코드 등록/수정 다이얼로그 -->
            <Dialog v-model:open="codeOpen">
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>{{
                            codeMode === 'create' ? '코드 추가' : '코드 수정'
                        }}</DialogTitle>
                        <DialogDescription>
                            {{ selectedGroupCode }} 그룹의 코드 정보를 입력하세요.
                        </DialogDescription>
                    </DialogHeader>
                    <div class="space-y-3">
                        <div>
                            <label class="mb-1 block text-sm font-medium">코드</label>
                            <Input
                                v-model="codeForm.code"
                                :disabled="codeMode === 'edit'"
                                placeholder="예: gemini-2.5-flash"
                            />
                        </div>
                        <div>
                            <label class="mb-1 block text-sm font-medium">코드명</label>
                            <Input v-model="codeForm.codeName" placeholder="예: Gemini 2.5 Flash" />
                        </div>
                        <div>
                            <label class="mb-1 block text-sm font-medium">정렬순</label>
                            <Input v-model="codeForm.sortOrder" type="number" min="0" />
                        </div>
                        <div class="flex items-center gap-2">
                            <Switch v-model="codeForm.useYn" />
                            <span class="text-sm">사용</span>
                        </div>
                    </div>
                    <DialogFooter>
                        <Button variant="outline" :disabled="saving" @click="codeOpen = false">
                            취소
                        </Button>
                        <Button
                            :disabled="saving || codeForm.codeName.trim() === ''"
                            @click="saveCode"
                        >
                            <Spinner v-if="saving" class="mr-2 size-4" />
                            저장
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            <!-- 그룹 삭제 확인 -->
            <ConfirmDialog
                v-model:open="groupDeleteOpen"
                title="그룹을 삭제할까요?"
                :description="`&quot;${groupDeleteTarget?.groupName}&quot; 그룹을 삭제합니다. 하위 코드가 있으면 삭제되지 않습니다.`"
                @confirm="confirmDeleteGroup"
            />

            <!-- 코드 삭제 확인 -->
            <ConfirmDialog
                v-model:open="codeDeleteOpen"
                title="코드를 삭제할까요?"
                :description="`&quot;${codeDeleteTarget?.codeName}&quot;(${codeDeleteTarget?.code}) 코드를 삭제합니다.`"
                @confirm="confirmDeleteCode"
            />
        </div>
    </div>
</template>
