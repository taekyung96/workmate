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
import type { ChatMessage } from '../types'

const props = defineProps<{ message: ChatMessage }>()
const emit = defineEmits<{ retry: [] }>()

const isUser = computed(() => props.message.role === 'user')
const html = computed(() => renderMarkdown(props.message.content))

/**
 * 마크다운 영역 내부 클릭 이벤트 처리 (이벤트 위임).
 * 코드 블록 복사 버튼(.copy-btn) 클릭 시 data-code 속성을 추출해 클립보드에 복사한다.
 *
 * @param event 마우스 클릭 이벤트
 */
async function onMarkdownClick(event: MouseEvent): Promise<void> {
    const target = event.target as HTMLElement | null
    const btn = target?.closest('.copy-btn') as HTMLButtonElement | null
    if (!btn) return

    const rawCodeEnc = btn.getAttribute('data-code')
    if (!rawCodeEnc) return

    try {
        const rawCode = decodeURIComponent(rawCodeEnc)
        await navigator.clipboard.writeText(rawCode)

        // 복사 성공 시 2초간 "복사됨!" 체크 피드백 표시
        const textSpan = btn.querySelector('.copy-text')
        if (textSpan) {
            const originalText = textSpan.textContent
            textSpan.textContent = '✓ 복사됨!'
            btn.classList.add('text-green-600', 'dark:text-green-400')

            setTimeout(() => {
                textSpan.textContent = originalText ?? '복사'
                btn.classList.remove('text-green-600', 'dark:text-green-400')
            }, 2000)
        }
    } catch (err) {
        console.error('코드 복사 실패:', err)
    }
}
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

<style scoped>
/* v-html로 삽입되는 마크다운 요소 기본 스타일 (typography 플러그인 미사용) */
.markdown-body :deep(p) {
    margin: 0.25rem 0;
}
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
    margin: 0.25rem 0;
    padding-left: 1.25rem;
    list-style: revert;
}
.markdown-body :deep(pre) {
    margin: 0.5rem 0;
    padding: 0.75rem;
    overflow-x: auto;
    border-radius: 0.5rem;
    background: color-mix(in oklch, currentColor 8%, transparent);
}
.markdown-body :deep(code) {
    padding: 0.1rem 0.3rem;
    border-radius: 0.25rem;
    background: color-mix(in oklch, currentColor 8%, transparent);
    font-size: 0.85em;
}
.markdown-body :deep(pre code) {
    padding: 0;
    background: transparent;
}
.markdown-body :deep(a) {
    text-decoration: underline;
}
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
    margin: 0.5rem 0 0.25rem;
    font-weight: 600;
}
</style>
