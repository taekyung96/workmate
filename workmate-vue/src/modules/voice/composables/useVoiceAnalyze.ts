import { ref } from 'vue'
import { extractErrorMessage } from '@/common/utils/error'
import { voiceApi } from '../api/voice.api'
import type { VoiceAnalysisResult } from '../types'

/**
 * 음성 회의록 분석 화면 로직 (≈ Service).
 * 오디오 업로드 → 전사+요약 결과 상태를 관리한다.
 */
export function useVoiceAnalyze() {
    const result = ref<VoiceAnalysisResult | null>(null)
    const loading = ref(false)
    const error = ref('')

    /** 오디오 분석 실행 */
    async function analyze(file: File, title: string): Promise<void> {
        loading.value = true
        error.value = ''
        try {
            result.value = await voiceApi.analyze(file, title)
        } catch (e) {
            error.value = extractErrorMessage(e, '회의록 분석에 실패했습니다.')
        } finally {
            loading.value = false
        }
    }

    /** 결과·에러 초기화 (새로 분석하기) */
    function reset(): void {
        result.value = null
        error.value = ''
    }

    return {
        result,
        loading,
        error,
        analyze,
        reset,
    }
}
