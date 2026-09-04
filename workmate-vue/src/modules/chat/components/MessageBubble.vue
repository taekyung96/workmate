<script setup lang="ts">
/**
 * 메시지 말풍선 — 사용자는 우측(평문), AI는 좌측.
 * AI 응답은 스트리밍 중에도 마크다운으로 렌더하고 커서를 덧붙인다. RAG 출처 뱃지 표시.
 * 마크다운 코드 블록 상단 복사 버튼 클릭 시 이벤트 위임으로 클립보드 복사 및 피드백 처리.
 */
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { RotateCw } from 'lucide-vue-next'
import { renderMarkdown } from '@/common/utils/markdown'
import { useMarkdownCopy } from '@/common/composables/useMarkdownCopy'
import type { ChatMessage } from '../types'

const props = defineProps<{ message: ChatMessage }>()
const emit = defineEmits<{ retry: [] }>()

const isUser = computed(() => props.message.role === 'user')
const html = computed(() => renderMarkdown(props.message.content))

// 코드블록 복사 버튼 처리(이벤트 위임) — 가이드 상세 등과 공유하는 공통 컴포저블
const { onMarkdownClick } = useMarkdownCopy()
</script>

<template>
    <div class="flex" :class="isUser ? 'justify-end' : 'justify-start'">
        <div
            class="rounded-2xl px-4 py-2.5 text-sm"
            :class="
                isUser
                    ? 'max-w-[80%] bg-primary text-primary-foreground'
                    : 'w-full min-w-0 bg-muted'
            "
        >
            <p v-if="isUser" class="whitespace-pre-wrap">{{ message.content }}</p>

            <template v-else>
                <!--
                    스트리밍 중에도 마크다운으로 그린다. 원문을 그대로 흘리면 답변이 끝날 때까지
                    '##'·'|' 같은 기호가 그대로 보이다가 완료 순간 화면이 통째로 바뀐다.
                    markdown-it 은 미완성 입력도 그때까지의 내용으로 렌더하므로 중간 상태도 읽을 만하다.
                -->
                <div
                    class="markdown-body"
                    :class="{ 'text-destructive': message.error }"
                    v-html="html"
                    @click="onMarkdownClick"
                />
                <span v-if="message.streaming" class="ml-0.5 inline-block animate-pulse">▌</span>

                <button
                    v-if="message.error && message.canRetry"
                    type="button"
                    class="mt-2 flex items-center gap-1 text-xs font-medium text-muted-foreground hover:text-foreground"
                    @click="emit('retry')"
                >
                    <RotateCw class="size-3.5" />
                    다시 시도
                </button>

                <!--
                    "출처"가 아니라 "찾은 문서"라고 쓴다.

                    이 목록은 <b>검색이 임계값을 넘겨 가져온 문서</b>이지, 답변이 실제로 인용한
                    문서가 아니다. 코퍼스에 답이 없는 질문에서도 유사도만 넘기면 딸려 나온다 —
                    실제로 "연차 휴가 며칠?"에 무관한 개발 가이드 4건이 붙었고, 답변 본문은
                    "자료에 없습니다"라고 말하는데 화면만 출처를 단 것처럼 보였다.
                    이 목록이 얼마나 헛도는지는 평가 하네스의 오탐률로 잰다
                    (rageval/negative-queries.json).
                -->
                <div v-if="message.sources?.length" class="mt-2">
                    <p class="mb-1 text-xs text-muted-foreground">가이드에서 찾은 문서</p>
                    <div class="flex flex-wrap gap-1">
                        <RouterLink
                            v-for="source in message.sources"
                            :key="source.guideSeq"
                            :to="{ name: 'guide-detail', params: { id: source.guideSeq } }"
                            class="rounded-full bg-background px-2 py-0.5 text-xs text-muted-foreground hover:text-foreground"
                        >
                            📎 {{ source.title }}
                        </RouterLink>
                    </div>
                </div>
            </template>
        </div>
    </div>
</template>
