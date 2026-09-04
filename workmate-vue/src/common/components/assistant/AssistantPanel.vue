<script setup lang="ts">
/**
 * 도우미 패널 — 지금 보고 있는 화면에 대해 묻는다.
 *
 * 채팅(MessageBubble 등)과 렌더링이 겹치지만 복사하지 않는다. 도우미는 출처 표시·이미지·
 * 모델 배지가 없어 훨씬 단순하다. 겹치는 부분이 2회 이상 실제로 중복되면 그때 common/ 으로 올린다.
 */
import { nextTick, ref, watch } from 'vue'
import { Bot, Send, SquarePen, X } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { Input } from '@/common/components/ui/input'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import { useAssistant } from '@/common/composables/useAssistant'

const emit = defineEmits<{ (e: 'close'): void }>()

const { messages, loading, error, send, abort, clear } = useAssistant()
const draft = ref('')
const listRef = ref<HTMLElement | null>(null)

async function submit(): Promise<void> {
    const text = draft.value
    draft.value = ''
    await send(text)
}

// 새 토큰이 올 때마다 아래로 스크롤 — 스트리밍 중에도 따라가야 한다
watch(
    () => messages.value.map((m) => m.content).join(''),
    async () => {
        await nextTick()
        if (listRef.value) listRef.value.scrollTop = listRef.value.scrollHeight
    },
)

/**
 * 패널을 닫는다 — <b>대화는 남긴다.</b>
 *
 * 화면이 좁을 때 패널은 본문을 덮으므로, 닫기는 "다 썼다"가 아니라 "본문을 봐야 한다"에
 * 가깝다. 그때마다 대화가 사라지면 쓸 수가 없다. 비우는 것은 "새 대화" 로만 한다.
 * 진행 중인 스트리밍은 끊는다 — 화면에 없는 답을 계속 받을 이유가 없다.
 */
function handleClose(): void {
    abort()
    emit('close')
}
</script>

<template>
    <aside
        class="flex h-full w-full flex-col border-l bg-background shadow-xl"
        role="complementary"
        aria-label="도우미"
    >
        <header class="flex items-center justify-between border-b px-4 py-3">
            <div class="flex items-center gap-2">
                <Bot class="size-4" />
                <span class="text-sm font-semibold">도우미</span>
            </div>
            <div class="flex items-center gap-1">
                <!-- 대화가 있을 때만 보인다 — 비어 있으면 누를 이유가 없다 -->
                <Button
                    v-if="messages.length > 0"
                    variant="ghost"
                    size="icon"
                    aria-label="새 대화"
                    title="새 대화"
                    @click="clear"
                >
                    <SquarePen class="size-4" />
                </Button>
                <Button variant="ghost" size="icon" aria-label="닫기" @click="handleClose">
                    <X class="size-4" />
                </Button>
            </div>
        </header>

        <div ref="listRef" class="slim-scroll min-h-0 flex-1 overflow-y-auto px-4 py-3">
            <p v-if="messages.length === 0" class="text-sm text-muted-foreground">
                지금 보고 있는 화면에 대해 물어보세요. 사용법과 내 사용량을 답해드립니다.
            </p>

            <div v-for="(m, i) in messages" :key="i" class="mb-3">
                <div
                    class="whitespace-pre-wrap rounded-lg px-3 py-2 text-sm"
                    :class="
                        m.role === 'user'
                            ? 'ml-6 bg-primary text-primary-foreground'
                            : 'mr-6 bg-muted'
                    "
                >
                    <span v-if="m.content">{{ m.content }}</span>
                    <span v-else class="text-muted-foreground">…</span>
                </div>
            </div>

            <Alert v-if="error" variant="destructive" class="mt-2">
                <AlertDescription>{{ error }}</AlertDescription>
            </Alert>
        </div>

        <form class="flex gap-2 border-t p-3" @submit.prevent="submit">
            <Input v-model="draft" placeholder="질문을 입력하세요" :disabled="loading" />
            <Button
                type="submit"
                size="icon"
                :disabled="loading || !draft.trim()"
                aria-label="보내기"
            >
                <Send class="size-4" />
            </Button>
        </form>
    </aside>
</template>
