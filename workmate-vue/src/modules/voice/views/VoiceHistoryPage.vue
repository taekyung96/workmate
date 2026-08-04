<script setup lang="ts">
/**
 * 회의록 [이력] 화면 (/voice/history, F8-1 확장).
 * 등록된 회의록을 최신순으로 보여주고, 행을 누르면 상세로 이동한다.
 * 오디오는 1건당 최대 25MB 라 건별 삭제를 제공한다.
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Mic, Trash2 } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
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
import PageTabs from '@/common/components/PageTabs.vue'
import { useVoiceHistory } from '../composables/useVoiceHistory'
import { voiceTabs } from '../routes'

const router = useRouter()
const { records, page, totalPages, totalElements, loading, error, load, goToPage, remove } =
    useVoiceHistory()

const deleteOpen = ref(false)
const deleteTarget = ref<number | null>(null)

onMounted(load)

/** 상세로 이동 */
function openRecord(recordSeq: number): void {
    router.push({ name: 'voice-record', params: { recordSeq } })
}

/** 삭제 확인창 열기 (행 클릭으로 상세 진입하는 것과 섞이지 않게 이벤트 전파를 막는다) */
function askDelete(recordSeq: number): void {
    deleteTarget.value = recordSeq
    deleteOpen.value = true
}

/** 삭제 확정 */
async function confirmDelete(): Promise<void> {
    if (deleteTarget.value === null) return
    await remove(deleteTarget.value)
    deleteTarget.value = null
}

/** YYYY.MM.DD 표기 */
function formatDate(iso: string): string {
    const d = new Date(iso)
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    return `${d.getFullYear()}.${mm}.${dd}`
}

/** 사람이 읽기 좋은 파일 크기 표기 */
function formatSize(bytes: number | null): string {
    if (bytes === null) return ''
    return bytes < 1024 * 1024
        ? `${(bytes / 1024).toFixed(0)} KB`
        : `${(bytes / 1024 / 1024).toFixed(1)} MB`
}
</script>

<template>
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-5xl px-6 py-8">
            <div class="mb-6 flex items-center gap-2">
                <Mic class="size-6" />
                <h1 class="text-2xl font-semibold">회의록 요약</h1>
            </div>

            <PageTabs :tabs="voiceTabs" />

            <p class="mb-3 text-sm text-muted-foreground">
                총 <span class="font-medium text-foreground">{{ totalElements }}</span
                >건
            </p>

            <Alert v-if="error" variant="destructive" class="mb-4">
                <AlertDescription>{{ error }}</AlertDescription>
            </Alert>

            <div v-if="loading" class="flex justify-center py-16">
                <Spinner class="size-6" />
            </div>

            <p
                v-else-if="totalElements === 0"
                class="py-16 text-center text-sm text-muted-foreground"
            >
                아직 분석한 회의록이 없습니다.
            </p>

            <div v-else class="overflow-x-auto rounded-lg border">
                <table class="w-full text-sm">
                    <thead class="border-b bg-muted/40 text-left text-muted-foreground">
                        <tr>
                            <th class="px-4 py-2.5 font-medium">제목</th>
                            <th class="px-4 py-2.5 font-medium">파일</th>
                            <th class="px-4 py-2.5 font-medium">생성일</th>
                            <th class="px-4 py-2.5 font-medium"></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr
                            v-for="r in records"
                            :key="r.recordSeq"
                            class="cursor-pointer border-b last:border-0 hover:bg-accent/40"
                            @click="openRecord(r.recordSeq)"
                        >
                            <td class="px-4 py-2.5 font-medium">{{ r.title }}</td>
                            <td class="px-4 py-2.5 text-muted-foreground">
                                <template v-if="r.hasAudio">
                                    {{ r.originFileName }} · {{ formatSize(r.fileSize) }}
                                </template>
                                <span v-else>오디오 없음</span>
                            </td>
                            <td class="px-4 py-2.5 tabular-nums">{{ formatDate(r.createdAt) }}</td>
                            <td class="px-4 py-2.5 text-right">
                                <Button
                                    size="sm"
                                    variant="ghost"
                                    aria-label="회의록 삭제"
                                    @click.stop="askDelete(r.recordSeq)"
                                >
                                    <Trash2 class="size-4" />
                                </Button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- 페이징 -->
            <div
                v-if="!loading && totalElements > 0"
                class="mt-4 flex items-center justify-end gap-3 text-sm text-muted-foreground"
            >
                <Button
                    size="sm"
                    variant="outline"
                    :disabled="page <= 0"
                    @click="goToPage(page - 1)"
                >
                    이전
                </Button>
                <span>{{ page + 1 }} / {{ Math.max(totalPages, 1) }}</span>
                <Button
                    size="sm"
                    variant="outline"
                    :disabled="page >= totalPages - 1"
                    @click="goToPage(page + 1)"
                >
                    다음
                </Button>
            </div>

            <AlertDialog v-model:open="deleteOpen">
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>이 회의록을 삭제할까요?</AlertDialogTitle>
                        <AlertDialogDescription>
                            전사문·요약과 저장된 오디오 파일이 함께 삭제되며 되돌릴 수 없습니다.
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>취소</AlertDialogCancel>
                        <AlertDialogAction @click="confirmDelete">삭제</AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </div>
    </div>
</template>
