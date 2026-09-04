<script setup lang="ts">
/**
 * 회의록 [이력] 화면 (/voice/history, F8-1 확장).
 * 등록된 회의록을 최신순으로 보여주고, 행을 누르면 상세로 이동한다.
 * 오디오는 1건당 최대 25MB 라 건별 삭제를 제공한다.
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { Mic, Trash2 } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import LoadingArea from '@/common/components/LoadingArea.vue'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import PageTabs from '@/common/components/PageTabs.vue'
import PageHeader from '@/common/components/PageHeader.vue'
import Pagination from '@/common/components/Pagination.vue'
import ConfirmDialog from '@/common/components/ConfirmDialog.vue'
import { formatDate, formatFileSize } from '@/common/utils/format'
import { useVoiceHistory } from '../composables/useVoiceHistory'
import TableScroller from '@/common/components/TableScroller.vue'
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
    // remove 는 실패 시 error 에 사유를 담고 던지지 않으므로, 그 값으로 성공/실패를 가른다
    if (error.value) toast.error(error.value)
    else toast.success('회의록을 삭제했습니다.')
}
</script>

<template>
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-5xl px-6 py-8">
            <PageHeader
                :icon="Mic"
                title="회의록 요약"
                description="녹음을 올리면 AI가 받아쓰고 핵심을 3단으로 정리합니다."
            />

            <PageTabs :tabs="voiceTabs" />

            <Alert v-if="error" variant="destructive" class="mb-4">
                <AlertDescription>{{ error }}</AlertDescription>
            </Alert>

            <LoadingArea v-if="loading" />

            <p
                v-else-if="totalElements === 0"
                class="py-16 text-center text-sm text-muted-foreground"
            >
                아직 분석한 회의록이 없습니다.
            </p>

            <TableScroller v-else>
                <table class="w-full min-w-[760px] text-sm">
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
                                    {{ r.originFileName }} · {{ formatFileSize(r.fileSize) }}
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
            </TableScroller>

            <!-- 페이징 -->
            <div
                v-if="!loading && totalElements > 0"
                class="mt-4 flex items-center justify-between text-sm text-muted-foreground"
            >
                <span>총 {{ totalElements }}건</span>
                <Pagination :page="page" :total-pages="totalPages" @change="goToPage" />
            </div>

            <ConfirmDialog
                v-model:open="deleteOpen"
                title="이 회의록을 삭제할까요?"
                description="전사문·요약과 저장된 오디오 파일이 함께 삭제되며 되돌릴 수 없습니다."
                @confirm="confirmDelete"
            />
        </div>
    </div>
</template>
