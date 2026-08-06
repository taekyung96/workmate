<script setup lang="ts">
/**
 * 가이드 상세 화면 (/guide/:id) — 문서 내용 표시, 수정 이동, 삭제(확인 후).
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { useAuthStore } from '@/modules/auth/stores/auth.store'
import { extractErrorMessage } from '@/common/utils/error'
import { Button } from '@/common/components/ui/button'
import { Badge } from '@/common/components/ui/badge'
import LoadingArea from '@/common/components/LoadingArea.vue'
import ConfirmDialog from '@/common/components/ConfirmDialog.vue'
import { formatDate } from '@/common/utils/format'
import { renderMarkdown } from '@/common/utils/markdown'
import { useMarkdownCopy } from '@/common/composables/useMarkdownCopy'
import { useGuideDetail } from '../composables/useGuideDetail'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const guideSeq = computed(() => Number(route.params.id))
const { guide, loading, error, load, remove } = useGuideDetail()

// 삭제 확인창 열림 상태 (공용 ConfirmDialog 를 v-model 로 제어)
const deleteOpen = ref(false)

// 수정·삭제 권한: 문서 소유자 본인이거나 관리자일 때만 버튼을 노출한다(백엔드도 동일 기준으로 방어).
const canManage = computed(
    () => !!guide.value && (guide.value.userSeq === authStore.user?.userSeq || authStore.isAdmin),
)

// 본문(마크다운)을 살균된 HTML 로 렌더 + 코드블록 복사 버튼 처리(채팅과 공유)
const renderedContent = computed(() => (guide.value ? renderMarkdown(guide.value.content) : ''))
const { onMarkdownClick } = useMarkdownCopy()

onMounted(() => load(guideSeq.value))

/** 삭제 확정 → 성공 시 목록으로, 실패 시 사유를 토스트로 알린다 */
async function onDelete(): Promise<void> {
    try {
        await remove(guideSeq.value)
        toast.success('가이드를 삭제했습니다.')
        router.replace({ name: 'guide-list' })
    } catch (e) {
        // 권한 없음 등 서버 오류 — 화면 이동 없이 사유를 알린다
        toast.error(extractErrorMessage(e, '가이드 삭제에 실패했습니다.'))
    }
}
</script>

<template>
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-3xl px-6 py-8">
            <button
                class="mb-4 text-sm text-muted-foreground hover:underline"
                @click="router.push({ name: 'guide-list' })"
            >
                ← 목록으로
            </button>

            <LoadingArea v-if="loading" />

            <p v-else-if="error" class="text-destructive">{{ error }}</p>

            <template v-else-if="guide">
                <div class="mb-4 flex items-start justify-between gap-4">
                    <div>
                        <h1 class="text-2xl font-semibold">{{ guide.title }}</h1>
                        <div class="mt-2 flex items-center gap-2 text-sm text-muted-foreground">
                            <Badge :variant="guide.isPublic ? 'default' : 'secondary'">
                                {{ guide.isPublic ? '공개' : '비공개' }}
                            </Badge>
                            <span>수정 {{ formatDate(guide.updatedAt) }}</span>
                        </div>
                    </div>
                    <div v-if="canManage" class="flex shrink-0 gap-2">
                        <Button
                            variant="outline"
                            @click="
                                router.push({ name: 'guide-edit', params: { id: guide.guideSeq } })
                            "
                        >
                            수정
                        </Button>
                        <Button variant="destructive" @click="deleteOpen = true">삭제</Button>
                        <ConfirmDialog
                            v-model:open="deleteOpen"
                            title="가이드를 삭제할까요?"
                            description="삭제하면 되돌릴 수 없습니다."
                            @confirm="onDelete"
                        />
                    </div>
                </div>

                <!-- 본문: 작성 시와 동일하게 마크다운으로 렌더 (평문 대신) -->
                <div
                    class="markdown-body markdown-doc rounded-lg border p-6"
                    v-html="renderedContent"
                    @click="onMarkdownClick"
                />
            </template>
        </div>
    </div>
</template>
