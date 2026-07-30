<script setup lang="ts">
/**
 * 가이드 상세 화면 (/guide/:id) — 문서 내용 표시, 수정 이동, 삭제(확인 후).
 */
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Button } from '@/common/components/ui/button'
import { Badge } from '@/common/components/ui/badge'
import { Skeleton } from '@/common/components/ui/skeleton'
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
    AlertDialogTrigger,
} from '@/common/components/ui/alert-dialog'
import { formatDate } from '@/common/utils/format'
import { renderMarkdown } from '@/common/utils/markdown'
import { useMarkdownCopy } from '@/common/composables/useMarkdownCopy'
import { useGuideDetail } from '../composables/useGuideDetail'

const route = useRoute()
const router = useRouter()
const guideSeq = computed(() => Number(route.params.id))
const { guide, loading, error, load, remove } = useGuideDetail()

// 본문(마크다운)을 살균된 HTML 로 렌더 + 코드블록 복사 버튼 처리(채팅과 공유)
const renderedContent = computed(() => (guide.value ? renderMarkdown(guide.value.content) : ''))
const { onMarkdownClick } = useMarkdownCopy()

onMounted(() => load(guideSeq.value))

/** 삭제 확정 → 삭제 후 목록으로 */
async function onDelete(): Promise<void> {
    await remove(guideSeq.value)
    router.replace({ name: 'guide-list' })
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

            <div v-if="loading" class="space-y-3">
                <Skeleton class="h-8 w-1/2" />
                <Skeleton class="h-40 w-full" />
            </div>

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
                    <div class="flex shrink-0 gap-2">
                        <Button
                            variant="outline"
                            @click="
                                router.push({ name: 'guide-edit', params: { id: guide.guideSeq } })
                            "
                        >
                            수정
                        </Button>
                        <AlertDialog>
                            <AlertDialogTrigger as-child>
                                <Button variant="destructive">삭제</Button>
                            </AlertDialogTrigger>
                            <AlertDialogContent>
                                <AlertDialogHeader>
                                    <AlertDialogTitle>가이드를 삭제할까요?</AlertDialogTitle>
                                    <AlertDialogDescription>
                                        삭제하면 되돌릴 수 없습니다.
                                    </AlertDialogDescription>
                                </AlertDialogHeader>
                                <AlertDialogFooter>
                                    <AlertDialogCancel>취소</AlertDialogCancel>
                                    <AlertDialogAction @click="onDelete">삭제</AlertDialogAction>
                                </AlertDialogFooter>
                            </AlertDialogContent>
                        </AlertDialog>
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
