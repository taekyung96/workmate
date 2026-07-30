<script setup lang="ts">
/**
 * 가이드 목록 화면 (/guide) — 본인+공개 문서를 카드 그리드로 표시.
 * 서버 페이징 + 키워드 검색(입력 디바운스). 각 카드는 제목·공개여부·수정일·본문 미리보기를 보여준다.
 */
import { onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { Input } from '@/common/components/ui/input'
import { Badge } from '@/common/components/ui/badge'
import { Skeleton } from '@/common/components/ui/skeleton'
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from '@/common/components/ui/card'
import { formatDate } from '@/common/utils/format'
import { useGuideList } from '../composables/useGuideList'

const router = useRouter()
const {
    guides,
    keyword,
    page,
    totalPages,
    totalElements,
    loading,
    error,
    load,
    search,
    changePage,
} = useGuideList()

onMounted(load)

// 키워드 입력 디바운스 — 입력이 멈춘 뒤 300ms 후 첫 페이지부터 재검색(연타로 요청 폭주 방지)
let searchTimer: ReturnType<typeof setTimeout> | undefined
watch(keyword, () => {
    clearTimeout(searchTimer)
    searchTimer = setTimeout(() => search(), 300)
})

function openDetail(guideSeq: number): void {
    router.push({ name: 'guide-detail', params: { id: guideSeq } })
}
</script>

<template>
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-5xl px-6 py-8">
            <div class="mb-6 flex items-center justify-between gap-3">
                <h1 class="text-2xl font-semibold">가이드 문서</h1>
                <Button @click="router.push({ name: 'guide-new' })">+ 새 문서</Button>
            </div>

            <!-- 검색 -->
            <div class="relative mb-5 max-w-sm">
                <Search
                    class="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
                />
                <Input v-model="keyword" placeholder="제목·본문 검색" class="pl-9" />
            </div>

            <!-- 로딩: 카드 스켈레톤 -->
            <div v-if="loading" class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                <Skeleton v-for="n in 6" :key="n" class="h-36 w-full" />
            </div>

            <p v-else-if="error" class="text-destructive">{{ error }}</p>

            <!-- 빈 상태 -->
            <div
                v-else-if="guides.length === 0"
                class="rounded-lg border border-dashed p-10 text-center"
            >
                <p class="text-muted-foreground">
                    {{ keyword ? '검색 결과가 없습니다.' : '첫 가이드 문서를 작성해보세요.' }}
                </p>
                <Button v-if="!keyword" class="mt-4" @click="router.push({ name: 'guide-new' })">
                    + 새 문서
                </Button>
            </div>

            <template v-else>
                <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    <Card
                        v-for="guide in guides"
                        :key="guide.guideSeq"
                        class="cursor-pointer transition-colors hover:bg-accent/40"
                        @click="openDetail(guide.guideSeq)"
                    >
                        <CardHeader>
                            <div class="flex items-start justify-between gap-2">
                                <CardTitle class="line-clamp-1 text-base">
                                    {{ guide.title }}
                                </CardTitle>
                                <Badge
                                    :variant="guide.isPublic ? 'default' : 'secondary'"
                                    class="shrink-0"
                                >
                                    {{ guide.isPublic ? '공개' : '비공개' }}
                                </Badge>
                            </div>
                            <CardDescription class="line-clamp-2 min-h-[2.5rem]">
                                {{ guide.excerpt || '내용 미리보기가 없습니다.' }}
                            </CardDescription>
                        </CardHeader>
                        <CardContent class="pt-0 text-xs text-muted-foreground">
                            {{ formatDate(guide.updatedAt) }}
                        </CardContent>
                    </Card>
                </div>

                <!-- 페이징 -->
                <div class="mt-6 flex items-center justify-between text-sm text-muted-foreground">
                    <span>총 {{ totalElements }}건</span>
                    <div class="flex items-center gap-3">
                        <Button
                            size="sm"
                            variant="outline"
                            :disabled="page <= 0"
                            @click="changePage(page - 1)"
                        >
                            이전
                        </Button>
                        <span>{{ page + 1 }} / {{ Math.max(totalPages, 1) }}</span>
                        <Button
                            size="sm"
                            variant="outline"
                            :disabled="page >= totalPages - 1"
                            @click="changePage(page + 1)"
                        >
                            다음
                        </Button>
                    </div>
                </div>
            </template>
        </div>
    </div>
</template>
