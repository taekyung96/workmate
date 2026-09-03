import { describe, expect, it, vi } from 'vitest'
import { mountPage } from '../../../test/support/mountPage'

vi.mock('../api/voice.api', () => ({
    voiceApi: {
        analyze: vi.fn<(...args: unknown[]) => unknown>(),
        updateTitle: vi.fn<(...args: unknown[]) => unknown>(),
        audioUrl: (seq: number) => `/api/voice/${seq}/audio`,
    },
}))

import VoiceAnalyzePage from './VoiceAnalyzePage.vue'

describe('VoiceAnalyzePage', () => {
    it('업로드 전에는 파일을 올리라는 안내를 보여준다', async () => {
        const { wrapper } = await mountPage(VoiceAnalyzePage)

        expect(wrapper.text()).toContain('회의록 요약')
        expect(wrapper.text()).toContain('오디오 파일을 올려주세요')
    })

    it('진입만으로 전사를 시작하지 않는다', async () => {
        const { voiceApi } = await import('../api/voice.api')

        await mountPage(VoiceAnalyzePage)

        expect(voiceApi.analyze).not.toHaveBeenCalled()
    })
})
