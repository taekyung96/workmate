import { ref, type Ref } from 'vue'
import { useRoute } from 'vue-router'
import { streamAssistant } from '@/common/api/assistant.api'
import type { AssistantTurn } from '@/common/types/assistant'

/** 서버로 보내는 최근 대화 개수. 서버도 같은 상한을 강제한다 */
const HISTORY_LIMIT = 6

/** 화면에 그리는 메시지 — 스트리밍 중에는 content 가 계속 늘어난다 */
export interface AssistantMessage {
    role: 'user' | 'assistant'
    content: string
}

/**
 * 도우미 패널의 상태와 동작.
 *
 * 대화는 브라우저 메모리에만 둔다 — 서버가 저장하지 않기 때문이고, 일회성 질문이라는
 * 성격에도 맞는다. 패널을 닫으면 비운다.
 *
 * @returns 메시지 목록, 진행 상태, 오류, 전송·닫기 동작
 */
export function useAssistant(): {
    messages: Ref<AssistantMessage[]>
    loading: Ref<boolean>
    error: Ref<string | null>
    send: (text: string) => Promise<void>
    close: () => void
} {
    const route = useRoute()
    const messages = ref<AssistantMessage[]>([])
    const loading = ref(false)
    const error = ref<string | null>(null)
    let controller: AbortController | null = null

    /** 서버로 보낼 최근 대화 — 현재 질문은 아직 넣기 전이라 포함되지 않는다 */
    function recentHistory(): AssistantTurn[] {
        return messages.value
            .slice(-HISTORY_LIMIT)
            .map((m) => ({ role: m.role, content: m.content }))
    }

    async function send(text: string): Promise<void> {
        const message = text.trim()
        if (!message || loading.value) return

        // 현재 질문을 넣기 전에 스냅샷을 뜬다 — 안 그러면 질문이 history 에도 실려 중복된다
        const history = recentHistory()
        messages.value.push({ role: 'user', content: message })

        // 스트리밍 토큰을 이어붙일 자리를 미리 만든다
        const reply: AssistantMessage = { role: 'assistant', content: '' }
        messages.value.push(reply)

        loading.value = true
        error.value = null
        controller = new AbortController()

        try {
            await streamAssistant(
                { message, route: String(route.name ?? ''), history },
                {
                    onToken: (d) => {
                        reply.content += d.delta
                    },
                    onDone: () => {
                        loading.value = false
                    },
                    onError: (d) => {
                        error.value = d.message
                        loading.value = false
                    },
                },
                controller.signal,
            )
        } catch (e) {
            // 사용자가 패널을 닫아 중단한 것은 오류가 아니다
            if (!(e instanceof DOMException && e.name === 'AbortError')) {
                error.value = '도우미를 불러오지 못했습니다.'
            }
        } finally {
            loading.value = false
            controller = null
        }
    }

    /** 패널을 닫는다 — 진행 중 요청을 끊고 대화를 비운다 */
    function close(): void {
        controller?.abort()
        controller = null
        messages.value = []
        error.value = null
        loading.value = false
    }

    return { messages, loading, error, send, close }
}
