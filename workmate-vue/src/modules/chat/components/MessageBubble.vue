<script setup lang="ts">
/**
 * 메시지 말풍선 — 사용자는 우측(평문), AI는 좌측.
 * AI 응답은 스트리밍 중엔 원문+커서, 완료 후 마크다운 렌더. RAG 출처 뱃지 표시.
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
            class="max-w-[80%] rounded-2xl px-4 py-2.5 text-sm"
            :class="isUser ? 'bg-primary text-primary-foreground' : 'bg-muted'"
        >
            <p v-if="isUser" class="whitespace-pre-wrap">{{ message.content }}</p>

            <template v-else>
                <div v-if="message.streaming" class="whitespace-pre-wrap">
                    {{ message.content }}<span class="ml-0.5 inline-block animate-pulse">▌</span>
                </div>
                <div
                    v-else
                    class="markdown-body"
                    :class="{ 'text-destructive': message.error }"
                    v-html="html"
                    @click="onMarkdownClick"
                />

                <button
                    v-if="message.error && message.canRetry"
                    type="button"
                    class="mt-2 flex items-center gap-1 text-xs font-medium text-muted-foreground hover:text-foreground"
                    @click="emit('retry')"
                >
                    <RotateCw class="size-3.5" />
                    다시 시도
                </button>

                <div v-if="message.sources?.length" class="mt-2 flex flex-wrap gap-1">
                    <RouterLink
                        v-for="source in message.sources"
                        :key="source.guideSeq"
                        :to="{ name: 'guide-detail', params: { id: source.guideSeq } }"
                        class="rounded-full bg-background px-2 py-0.5 text-xs text-muted-foreground hover:text-foreground"
                    >
                        📎 {{ source.title }}
                    </RouterLink>
                </div>
            </template>
        </div>
    </div>
</template>
