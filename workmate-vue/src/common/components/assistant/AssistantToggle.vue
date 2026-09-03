<script setup lang="ts">
/**
 * 도우미 토글 — 우측 하단에 떠 있는 버튼.
 *
 * 데스크탑은 오른쪽에서 밀려나오는 패널, 모바일은 전체 화면으로 띄운다.
 * 패널을 v-if 로 마운트/언마운트하는 이유: 닫을 때 useAssistant 의 상태가 함께 사라져야
 * "닫으면 대화가 비워진다"는 규칙이 컴포넌트 생명주기로 보장된다.
 */
import { ref } from 'vue'
import { MessageCircleQuestion } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import AssistantPanel from './AssistantPanel.vue'

const open = ref(false)
</script>

<template>
    <!-- 패널: 데스크탑은 우측 고정, 모바일은 전체 화면 -->
    <div v-if="open" class="fixed inset-0 z-50 md:inset-y-0 md:left-auto md:right-0 md:w-96">
        <AssistantPanel @close="open = false" />
    </div>

    <!-- 토글 버튼: 패널이 열려 있으면 숨긴다(패널 안에 닫기 버튼이 있다) -->
    <Button
        v-if="!open"
        class="fixed bottom-6 right-6 z-40 size-12 rounded-full shadow-lg"
        size="icon"
        aria-label="도우미 열기"
        @click="open = true"
    >
        <MessageCircleQuestion class="size-5" />
    </Button>
</template>
