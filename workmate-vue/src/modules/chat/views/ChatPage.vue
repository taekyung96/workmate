<script setup lang="ts">
/**
 * 채팅 화면 (/chat) — 빈 상태(방 미생성)와 대화 상태.
 * 방 목록·선택은 공통 사이드바가 담당하고, 여기선 메시지 영역 + 입력창만 그린다.
 */
import { computed, onMounted } from 'vue'
import { X } from 'lucide-vue-next'
import { useChatStore } from '../stores/chat.store'
import MessageList from '../components/MessageList.vue'
import MessageInput from '../components/MessageInput.vue'

const chat = useChatStore()
const isEmpty = computed(() => chat.messages.length === 0)

// 모델 선택 드롭다운 구성용 AI_MODEL 공통코드 로드 (1회)
onMounted(() => {
    if (!chat.modelsLoaded) chat.loadModels()
})

function onSend(text: string): void {
    chat.sendMessage(text)
}
</script>

<template>
    <div class="flex h-full flex-col">
        <!-- 요청제한(429) 안내 배너 -->
        <div
            v-if="chat.rateLimited"
            class="flex items-center justify-between gap-3 border-b border-amber-500/30 bg-amber-500/10 px-4 py-2 text-sm text-amber-700 dark:text-amber-400"
        >
            <span>{{ chat.rateLimited }}</span>
            <button
                type="button"
                class="shrink-0 rounded p-0.5 hover:bg-amber-500/20"
                title="닫기"
                @click="chat.dismissRateLimit"
            >
                <X class="size-4" />
            </button>
        </div>

        <!-- 빈 상태: 중앙 안내 + 입력창 -->
        <div v-if="isEmpty" class="flex flex-1 flex-col items-center justify-center">
            <h1 class="mb-8 text-2xl font-semibold text-muted-foreground">무엇을 도와드릴까요?</h1>
            <MessageInput
                v-model:rag-mode="chat.ragMode"
                v-model:selected-model="chat.selectedModel"
                :models="chat.models"
                :disabled="chat.streaming"
                @send="onSend"
            />
        </div>

        <!-- 대화 상태: 메시지 목록 + 하단 입력창 -->
        <template v-else>
            <MessageList :messages="chat.messages" @retry="chat.retryLast" />
            <MessageInput
                v-model:rag-mode="chat.ragMode"
                v-model:selected-model="chat.selectedModel"
                :models="chat.models"
                :disabled="chat.streaming"
                @send="onSend"
            />
        </template>
    </div>
</template>
