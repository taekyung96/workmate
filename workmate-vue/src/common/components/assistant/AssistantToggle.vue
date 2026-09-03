<script setup lang="ts">
/**
 * 도우미 열기 버튼 — 우측 하단에 떠 있다.
 *
 * 열림 상태를 여기서 들지 않고 부모(AppLayout)에 맡기는 이유: 패널이 본문과 같은 flex 행에
 * 놓여야 본문이 왼쪽으로 밀려나기 때문이다. 버튼이 패널까지 소유하면 패널을 레이아웃
 * 바깥(fixed)에 그릴 수밖에 없어 본문을 덮는다.
 */
import { MessageCircleQuestion } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'

defineEmits<{
    /** 도우미 패널을 열어 달라는 요청 */
    open: []
}>()
</script>

<template>
    <Button
        class="fixed bottom-6 right-6 z-40 size-12 rounded-full shadow-lg"
        size="icon"
        aria-label="도우미 열기"
        @click="$emit('open')"
    >
        <MessageCircleQuestion class="size-5" />
    </Button>
</template>
