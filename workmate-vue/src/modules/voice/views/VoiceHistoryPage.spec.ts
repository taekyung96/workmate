import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mountPage } from '../../../test/support/mountPage'
import type { VoiceRecordPage } from '../types'

const history = vi.fn<(...args: unknown[]) => unknown>()
const remove = vi.fn<(...args: unknown[]) => unknown>()
vi.mock('../api/voice.api', () => ({
    voiceApi: {
        history: (...a: unknown[]) => history(...a),
        remove: (...a: unknown[]) => remove(...a),
        audioUrl: (seq: number) => `/api/voice/${seq}/audio`,
    },
}))

import VoiceHistoryPage from './VoiceHistoryPage.vue'

function recordPage(overrides: Partial<VoiceRecordPage> = {}): VoiceRecordPage {
    return {
        content: [
            {
                recordSeq: 1,
                title: '9월 정기 회의',
                originFileName: 'meeting.wav',
                fileSize: 1024,
                hasAudio: true,
                createdAt: '2026-09-01T10:00:00',
            },
            {
                recordSeq: 2,
                title: '오디오가 지워진 회의',
                originFileName: null,
                fileSize: null,
                hasAudio: false,
                createdAt: '2026-09-02T10:00:00',
            },
        ],
        page: 0,
        totalPages: 1,
        totalElements: 2,
        ...overrides,
    }
}

describe('VoiceHistoryPage', () => {
    beforeEach(() => {
        history.mockReset()
        remove.mockReset()
    })

    it('회의록 목록을 그린다', async () => {
        history.mockResolvedValue(recordPage())

        const { wrapper } = await mountPage(VoiceHistoryPage)

        expect(wrapper.text()).toContain('9월 정기 회의')
    })

    it('오디오가 없는 기록을 구분해 보여준다', async () => {
        history.mockResolvedValue(recordPage())

        const { wrapper } = await mountPage(VoiceHistoryPage)

        // hasAudio=false 인 항목은 재생할 수 없다는 것이 화면에 드러나야 한다
        expect(wrapper.text()).toContain('오디오 없음')
    })

    it('결과가 없어도 화면이 그려진다', async () => {
        history.mockResolvedValue(recordPage({ content: [], totalElements: 0, totalPages: 0 }))

        const { wrapper } = await mountPage(VoiceHistoryPage)

        expect(wrapper.text()).toContain('회의록')
    })

    it('조회에 실패해도 화면이 터지지 않는다', async () => {
        history.mockRejectedValue(new Error('boom'))

        const { wrapper } = await mountPage(VoiceHistoryPage)

        expect(wrapper.exists()).toBe(true)
    })
})
