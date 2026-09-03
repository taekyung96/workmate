import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mountPage } from '../../../test/support/mountPage'
import type { VoiceAnalysisResult } from '../types'

const getRecord = vi.fn<(...args: unknown[]) => unknown>()
vi.mock('../api/voice.api', () => ({
    voiceApi: {
        getRecord: (...a: unknown[]) => getRecord(...a),
        updateTitle: vi.fn<(...args: unknown[]) => unknown>(),
        remove: vi.fn<(...args: unknown[]) => unknown>(),
        audioUrl: (seq: number) => `/api/voice/${seq}/audio`,
    },
}))

import VoiceRecordPage from './VoiceRecordPage.vue'

function record(overrides: Partial<VoiceAnalysisResult> = {}): VoiceAnalysisResult {
    return {
        recordSeq: 1,
        title: '9월 정기 회의',
        sttText: '오늘 회의를 시작하겠습니다.',
        summaryMd: '## 요약\n\n- 일정 확정\n- 담당자 배정',
        originFileName: 'meeting.wav',
        fileSize: 1024,
        hasAudio: true,
        createdAt: '2026-09-01T10:00:00',
        ...overrides,
    }
}

describe('VoiceRecordPage', () => {
    beforeEach(() => {
        getRecord.mockReset()
    })

    it('전문과 요약을 함께 그린다', async () => {
        getRecord.mockResolvedValue(record())

        const { wrapper } = await mountPage(VoiceRecordPage, { params: { recordSeq: '1' } })

        expect(wrapper.text()).toContain('9월 정기 회의')
        expect(wrapper.text()).toContain('오늘 회의를 시작하겠습니다')
        // 요약은 마크다운이라 '##' 이 그대로 보이면 렌더가 안 된 것이다
        expect(wrapper.text()).toContain('일정 확정')
        expect(wrapper.text()).not.toContain('## 요약')
    })

    it('조회에 실패해도 화면이 터지지 않는다', async () => {
        getRecord.mockRejectedValue(new Error('boom'))

        const { wrapper } = await mountPage(VoiceRecordPage, { params: { recordSeq: '1' } })

        expect(wrapper.exists()).toBe(true)
    })
})
