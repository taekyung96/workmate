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
 * 대화 상태를 <b>모듈 스코프</b>에 둔다 — 패널이 언마운트돼도 남는다.
 *
 * <p><b>왜 컴포넌트 밖으로 뺐나.</b> 예전에는 패널을 닫으면 언마운트되면서 대화가 함께
 * 사라졌다. 데스크탑에서는 닫기가 "다 썼다"는 뜻이라 자연스러웠지만, 화면이 좁아 패널이
 * 본문을 <b>덮는</b> 모드에서는 닫기가 "본문을 봐야 한다"는 뜻이 된다 — 강제로 닫아야 하는데
 * 그때마다 대화가 날아갔다. <b>같은 버튼이 상황에 따라 다른 의도를 갖는 것</b>이 문제였다.</p>
 *
 * <p>지금은 닫아도 남고, 패널의 "새 대화"로만 비운다. 여전히 <b>브라우저 메모리에만</b> 있어
 * 새로고침하면 사라지고, 로그아웃은 하드 리로드라(useAuth.logout) 자동으로 정리된다 —
 * 사용자 간 상태 누수는 그 리로드가 원천 차단한다.</p>
 */
const messages = ref<AssistantMessage[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
let controller: AbortController | null = null

/**
 * 도우미 패널의 상태와 동작.
 *
 * 대화는 브라우저 메모리에만 둔다 — 서버가 저장하지 않기 때문이고, 일회성 질문이라는
 * 성격에도 맞는다.
 *
 * @returns 메시지 목록, 진행 상태, 오류, 전송·중단·비우기 동작
 */
export function useAssistant(): {
    messages: Ref<AssistantMessage[]>
    loading: Ref<boolean>
    error: Ref<string | null>
    send: (text: string) => Promise<void>
    abort: () => void
    clear: () => void
} {
    const route = useRoute()

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

    /**
     * 진행 중인 요청만 끊는다 — <b>대화는 남긴다.</b>
     *
     * 패널을 닫을 때 부른다. 닫는 것은 "그만 보겠다"이지 "지우겠다"가 아니다.
     * 스트리밍이 돌던 중이면 끊어야 한다 — 화면에 없는 답을 계속 받을 이유가 없다.
     */
    function abort(): void {
        controller?.abort()
        controller = null
        loading.value = false
    }

    /** 새 대화 — 진행 중 요청을 끊고 대화를 비운다. 비우기는 이 경로로만 일어난다 */
    function clear(): void {
        abort()
        messages.value = []
        error.value = null
    }

    return { messages, loading, error, send, abort, clear }
}
