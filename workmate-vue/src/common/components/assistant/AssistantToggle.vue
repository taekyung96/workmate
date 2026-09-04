<script setup lang="ts">
/**
 * 도우미 열기 버튼 — 우측 하단에 떠 있다.
 *
 * 열림 상태를 여기서 들지 않고 부모(AppLayout)에 맡기는 이유: 패널이 본문과 같은 flex 행에
 * 놓여야 본문이 왼쪽으로 밀려나기 때문이다. 버튼이 패널까지 소유하면 패널을 레이아웃
 * 바깥(fixed)에 그릴 수밖에 없어 본문을 덮는다.
 *
 * <b>bottom-24 인 이유 — 내리지 말 것.</b> 채팅 입력 바가 화면 하단에 고정돼 있고
 * 전송 버튼은 아래에서 25~61px 를 차지한다. bottom-6(24px) 이면 이 버튼과 완전히 겹쳐,
 * 전송을 누르면 도우미가 열렸다. 겹침은 <b>본문 폭이 좁을수록</b> 심해진다 — 입력 바는
 * max-w-5xl(1024px) 이라 그보다 좁은 화면에서는 오른쪽 끝까지 차 뷰포트 모서리에 닿기
 * 때문이다(1280px 에서 재현, 1440px 에서는 여백이 남아 재현되지 않는다).
 * 입력 바에 오른쪽 여백을 주는 방식은 쓰지 않았다 — MessageList 와 같은 컬럼을 공유해
 * 넓은 화면에서 메시지와 입력창의 정렬이 어긋난다. e2e/chat-composer.spec.ts 가 지킨다.
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
        class="fixed bottom-24 right-6 z-40 size-12 rounded-full shadow-lg"
        size="icon"
        aria-label="도우미 열기"
        @click="$emit('open')"
    >
        <MessageCircleQuestion class="size-5" />
    </Button>
</template>
