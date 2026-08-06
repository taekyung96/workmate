import { defineStore } from 'pinia'
import { ref } from 'vue'
import { extractErrorMessage } from '@/common/utils/error'
import { voiceApi } from '../api/voice.api'
import type { VoiceAnalysisResult } from '../types'

/**
 * 회의록 분석 상태 (F8-1).
 * 탭이 라우터 이동형이라 화면을 벗어나면 컴포넌트가 파괴된다.
 * 수십 초 걸리는 분석 결과를 잃지 않도록 상태를 store 에 둔다.
 */
export const useVoiceStore = defineStore('voice', () => {
    const result = ref<VoiceAnalysisResult | null>(null)
    const loading = ref(false)
    const savingTitle = ref(false)
    const error = ref('')

    /** 오디오 분석 실행 — 제목은 분석 후 saveTitle 로 따로 저장한다 */
    async function analyze(file: File): Promise<void> {
        loading.value = true
        error.value = ''
        try {
            result.value = await voiceApi.analyze(file)
        } catch (e) {
            error.value = extractErrorMessage(e, '회의록 분석에 실패했습니다.')
        } finally {
            loading.value = false
        }
    }

    /**
     * 분석된 회의록의 제목을 저장한다 (결과 화면에서 확정).
     * 성공하면 서버가 돌려준 최신 상세로 result 를 교체한다.
     * @param title 사용자가 입력한 회의 제목
     * @returns 저장 성공 여부
     */
    async function saveTitle(title: string): Promise<boolean> {
        if (!result.value) return false
        savingTitle.value = true
        error.value = ''
        try {
            result.value = await voiceApi.updateTitle(result.value.recordSeq, title)
            return true
        } catch (e) {
            error.value = extractErrorMessage(e, '제목 저장에 실패했습니다.')
            return false
        } finally {
            savingTitle.value = false
        }
    }

    /** 결과·에러 초기화 (새로 분석하기) */
    function reset(): void {
        result.value = null
        error.value = ''
    }

    return { result, loading, savingTitle, error, analyze, saveTitle, reset }
})
