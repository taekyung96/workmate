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

    // 가이드 등록 상태 (F8-1-6) — 등록 진행/완료(생성된 guideSeq)
    const registering = ref(false)
    const registeredGuideSeq = ref<number | null>(null)

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

    /** 현재 회의록을 사내 가이드로 등록 (F8-1-6). 성공 시 생성된 guideSeq 를 보관한다. */
    async function convertToGuide(): Promise<void> {
        if (!result.value) return
        registering.value = true
        error.value = ''
        try {
            registeredGuideSeq.value = await voiceApi.convertToGuide(result.value.recordSeq)
        } catch (e) {
            error.value = extractErrorMessage(e, '가이드 등록에 실패했습니다.')
        } finally {
            registering.value = false
        }
    }

    /** 결과·에러 초기화 (새로 분석하기) */
    function reset(): void {
        result.value = null
        error.value = ''
        registeredGuideSeq.value = null
    }

    return {
        result,
        loading,
        error,
        registering,
        registeredGuideSeq,
        analyze,
        convertToGuide,
        reset,
    }
}
