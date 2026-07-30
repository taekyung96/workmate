<script setup lang="ts">
/**
 * 가이드 목록 화면 (/guide) — 본인+공개 문서 목록, 새 문서 작성 진입, 빈 상태 안내.
 */
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Button } from '@/common/components/ui/button'
import { Badge } from '@/common/components/ui/badge'
import { Skeleton } from '@/common/components/ui/skeleton'
import { formatDate } from '@/common/utils/format'
import { useGuideList } from '../composables/useGuideList'

const router = useRouter()
const { guides, loading, error, load } = useGuideList()

onMounted(load)

function openDetail(guideSeq: number): void {
    router.push({ name: 'guide-detail', params: { id: guideSeq } })
}
</script>

<template>
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-3xl px-6 py-8">
            <div class="mb-6 flex items-center justify-between">
                <h1 class="text-2xl font-semibold">가이드 문서</h1>
                <Button @click="router.push({ name: 'guide-new' })">+ 새 문서</Button>
            </div>

            <div v-if="loading" class="space-y-3">
                <Skeleton v-for="n in 3" :key="n" class="h-14 w-full" />
            </div>

            <p v-else-if="error" class="text-destructive">{{ error }}</p>

            <div
                v-else-if="guides.length === 0"
                class="rounded-lg border border-dashed p-10 text-center"
            >
                <p class="text-muted-foreground">첫 가이드 문서를 작성해보세요.</p>
                <Button class="mt-4" @click="router.push({ name: 'guide-new' })">+ 새 문서</Button>
            </div>

            <ul v-else class="divide-y rounded-lg border">
                <li
                    v-for="guide in guides"
                    :key="guide.guideSeq"
                    class="flex cursor-pointer items-center justify-between gap-3 px-4 py-3 hover:bg-accent"
                    @click="openDetail(guide.guideSeq)"
                >
                    <span class="truncate font-medium">{{ guide.title }}</span>
                    <span class="flex shrink-0 items-center gap-3 text-sm text-muted-foreground">
                        <Badge :variant="guide.isPublic ? 'default' : 'secondary'">
                            {{ guide.isPublic ? '공개' : '비공개' }}
                        </Badge>
                        {{ formatDate(guide.updatedAt) }}
                    </span>
                </li>
            </ul>
        </div>
    </div>
</template>
