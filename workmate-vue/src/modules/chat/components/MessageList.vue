<script setup lang="ts">
/**
 * 메시지 목록 — 새 메시지·스트리밍 토큰이 올 때 자동으로 하단 스크롤.
 * 첫 토큰 전(응답 대기)엔 로딩 점 3개를 보여준다.
 */
import { nextTick, ref, watch } from 'vue'
import MessageBubble from './MessageBubble.vue'
import type { ChatMessage } from '../types'

const props = defineProps<{ messages: ChatMessage[] }>()
const emit = defineEmits<{ retry: [] }>()

const scrollEl = ref<HTMLElement | null>(null)

// 메시지 수 또는 마지막 메시지 내용이 바뀌면(토큰 수신) 하단으로 스크롤
watch(
    () => [props.messages.length, props.messages.at(-1)?.content],
    async () => {
        await nextTick()
        scrollEl.value?.scrollTo({ top: scrollEl.value.scrollHeight })
    },
)
</script>

<template>
    <div ref="scrollEl" class="slim-scroll flex-1 overflow-y-auto">
        <div class="mx-auto flex max-w-5xl flex-col gap-4 px-4 py-6">
            <template v-for="(message, index) in messages" :key="index">
                <div
                    v-if="
                        message.role === 'assistant' && message.streaming && message.content === ''
                    "
                    class="flex justify-start"
                >
                    <div class="flex gap-1 rounded-2xl bg-muted px-4 py-3.5">
                        <span class="size-2 animate-bounce rounded-full bg-muted-foreground/60" />
                        <span
                            class="size-2 animate-bounce rounded-full bg-muted-foreground/60"
                            style="animation-delay: 0.15s"
                        />
                        <span
                            class="size-2 animate-bounce rounded-full bg-muted-foreground/60"
                            style="animation-delay: 0.3s"
                        />
                    </div>
                </div>
                <MessageBubble v-else :message="message" @retry="emit('retry')" />
            </template>
        </div>
    </div>
</template>
