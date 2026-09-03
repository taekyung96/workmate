import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mountPage } from '../../../test/support/mountPage'

const rooms = vi.fn<(...args: unknown[]) => unknown>()
const messages = vi.fn<(...args: unknown[]) => unknown>()
const stream = vi.fn<(...args: unknown[]) => unknown>()
vi.mock('../api/chat.api', () => ({
    chatApi: {
        rooms: (...a: unknown[]) => rooms(...a),
        messages: (...a: unknown[]) => messages(...a),
        deleteRoom: vi.fn<(...args: unknown[]) => unknown>(),
        stream: (...a: unknown[]) => stream(...a),
    },
}))

const codes = vi.fn<(...args: unknown[]) => unknown>()
vi.mock('../../../common/api/commonCode', () => ({
    commonCodeApi: { codes: (...a: unknown[]) => codes(...a) },
}))

import ChatPage from './ChatPage.vue'
import { useChatStore } from '../stores/chat.store'

describe('ChatPage', () => {
    beforeEach(() => {
        rooms.mockReset()
        messages.mockReset()
        stream.mockReset()
        codes.mockReset()
        rooms.mockResolvedValue([])
        messages.mockResolvedValue([])
    })

    it('빈 상태에서 입력 안내를 보여준다', async () => {
        codes.mockResolvedValue([{ code: 'qwen/qwen3.8-27b', codeName: 'Groq · Qwen3.8 27B' }])

        const { wrapper } = await mountPage(ChatPage)

        expect(wrapper.text()).toContain('무엇을 도와드릴까요')
    })

    it('모델 목록을 공통코드에서 받아 그린다', async () => {
        codes.mockResolvedValue([
            { code: 'qwen/qwen3.8-27b', codeName: 'Groq · Qwen3.8 27B' },
            { code: 'gemini-flash-latest', codeName: 'Gemini Flash (latest)' },
        ])

        await mountPage(ChatPage)

        expect(codes).toHaveBeenCalledWith('AI_MODEL')
        // 첫 번째 코드가 기본 선택값이다 — 서버의 sort_order 가 곧 화면 기본 모델이 된다.
        // 드롭다운에 그려진 글자가 아니라 상태를 본다: Reka Select 는 목록을 펼치기 전까지
        // 선택값 라벨을 jsdom 에서 그리지 않아, 렌더 텍스트로 단언하면 환경 탓에 깨진다
        expect(useChatStore().selectedModel).toBe('qwen/qwen3.8-27b')
    })

    it('모델 목록을 못 받아도 화면이 뜬다 — 채팅이 통째로 막히면 안 된다', async () => {
        codes.mockRejectedValue(new Error('boom'))

        const { wrapper } = await mountPage(ChatPage)

        expect(wrapper.exists()).toBe(true)
        expect(wrapper.text()).not.toBe('')
    })

    it('진입만으로 스트리밍을 시작하지 않는다', async () => {
        codes.mockResolvedValue([])

        await mountPage(ChatPage)

        expect(stream).not.toHaveBeenCalled()
    })
})
