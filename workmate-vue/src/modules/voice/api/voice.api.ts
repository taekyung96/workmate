import client from '@/common/api/client'
import type { ApiResponse } from '@/common/types/api'
import type { VoiceAnalysisResult } from '../types'

/**
 * 음성 회의록 API (WEB의 /api/v1/voice/* 프록시 호출 → WAS).
 * 계층 규칙: HTTP 통신만 담당.
 */
export const voiceApi = {
    /** 오디오 업로드 → 전사(STT) + 구조화 요약 — F8-1 */
    async analyze(file: File, title: string): Promise<VoiceAnalysisResult> {
        const form = new FormData()
        form.append('file', file)
        if (title.trim()) form.append('title', title.trim())
        // 멀티파트는 axios가 Content-Type(boundary 포함)을 자동 설정한다
        const { data } = await client.post<ApiResponse<VoiceAnalysisResult>>(
            '/v1/voice/analyze',
            form,
        )
        return data.result
    },
}
