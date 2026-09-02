import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'

// 라우트를 갈아끼울 수 있게 모듈을 목킹한다
const routeName = ref('my-usage')
vi.mock('vue-router', () => ({
    useRoute: () => ({
        get name() {
            return routeName.value
        },
    }),
}))

const streamAssistant = vi.fn()
vi.mock('@/common/api/assistant.api', () => ({
    streamAssistant: (...args: unknown[]) => streamAssistant(...args),
}))

import { useAssistant } from './useAssistant'

/** 토큰 하나를 흘리고 끝내는 기본 스텁 */
function respondWith(text: string) {
    streamAssistant.mockImplementation(
        async (
            _req: unknown,
            handlers: { onToken: (d: { delta: string }) => void; onDone: () => void },
        ) => {
            handlers.onToken({ delta: text })
            handlers.onDone()
        },
    )
}

describe('useAssistant', () => {
    beforeEach(() => {
        streamAssistant.mockReset()
        routeName.value = 'my-usage'
    })

    it('현재 라우트 이름을 요청에 담는다', async () => {
        respondWith('답')
        const { send } = useAssistant()

        await send('이 화면 뭔가요')

        expect(streamAssistant).toHaveBeenCalledTimes(1)
        expect(streamAssistant.mock.calls[0]![0]).toMatchObject({
            message: '이 화면 뭔가요',
            route: 'my-usage',
        })
    })

    it('라우트가 바뀌면 다음 요청에 새 라우트가 담긴다', async () => {
        respondWith('답')
        const { send } = useAssistant()

        await send('첫 질문')
        routeName.value = 'receipt'
        await send('두 번째 질문')

        expect(streamAssistant.mock.calls[1]![0]).toMatchObject({ route: 'receipt' })
    })

    it('최근 6개까지만 history 로 보낸다 — 토큰이 곧 비용이다', async () => {
        respondWith('답')
        const { send } = useAssistant()

        // 4번 주고받으면 메시지가 8개 쌓인다
        for (let i = 0; i < 4; i++) await send(`질문${i}`)

        const lastRequest = streamAssistant.mock.calls[3]![0] as { history: unknown[] }
        expect(lastRequest.history.length).toBeLessThanOrEqual(6)
    })

    it('현재 질문은 history 에 포함하지 않는다 — 중복 전송 방지', async () => {
        respondWith('답')
        const { send } = useAssistant()

        await send('첫 질문')
        await send('두 번째 질문')

        const second = streamAssistant.mock.calls[1]![0] as {
            message: string
            history: { content: string }[]
        }
        expect(second.message).toBe('두 번째 질문')
        expect(second.history.map((h) => h.content)).not.toContain('두 번째 질문')
    })

    it('스트리밍 토큰이 답변 메시지에 이어붙는다', async () => {
        streamAssistant.mockImplementation(
            async (
                _req: unknown,
                handlers: { onToken: (d: { delta: string }) => void; onDone: () => void },
            ) => {
                handlers.onToken({ delta: '안' })
                handlers.onToken({ delta: '녕' })
                handlers.onDone()
            },
        )
        const { send, messages } = useAssistant()

        await send('인사')

        expect(messages.value.at(-1)?.content).toBe('안녕')
    })

    it('패널을 닫으면 대화가 비워진다', async () => {
        respondWith('답')
        const { send, messages, close } = useAssistant()

        await send('질문')
        expect(messages.value.length).toBeGreaterThan(0)

        close()
        expect(messages.value).toHaveLength(0)
    })

    it('빈 질문은 보내지 않는다', async () => {
        const { send } = useAssistant()

        await send('   ')

        expect(streamAssistant).not.toHaveBeenCalled()
    })

    it('오류가 오면 error 에 담고 로딩을 푼다', async () => {
        streamAssistant.mockImplementation(
            async (_req: unknown, handlers: { onError: (d: { message: string }) => void }) => {
                handlers.onError({ message: '요청이 많습니다.' })
            },
        )
        const { send, error, loading } = useAssistant()

        await send('질문')

        expect(error.value).toBe('요청이 많습니다.')
        expect(loading.value).toBe(false)
    })
})
