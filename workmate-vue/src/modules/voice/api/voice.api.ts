import client from '@/common/api/client'
import type { ApiResponse } from '@/common/types/api'
import type { VoiceAnalysisResult, VoiceRecordPage } from '../types'

/**
 * 음성 회의록 API (WEB의 /api/v1/voice/* 프록시 호출 → WAS).
 * 계층 규칙: HTTP 통신만 담당.
 */
export const voiceApi = {
    /**
     * 오디오 업로드 → 전사(STT) + 구조화 요약 — F8-1.
     * 제목은 분석 후 결과 화면에서 별도로 저장하므로(updateTitle) 여기선 파일만 보낸다.
     * 제목을 비워 보내면 WAS 가 "회의록 {날짜}" 기본 제목을 붙인다.
     */
    async analyze(file: File): Promise<VoiceAnalysisResult> {
        const form = new FormData()
        form.append('file', file)
        // 멀티파트는 axios가 Content-Type(boundary 포함)을 자동 설정한다
        const { data } = await client.post<ApiResponse<VoiceAnalysisResult>>(
            '/v1/voice/analyze',
            form,
        )
        return data.result
    },

    /**
     * 회의록 제목 수정 — 분석 후 결과 화면에서 확정한 제목을 저장한다.
     * @param recordSeq 회의록 식별자
     * @param title     새 회의 제목
     * @returns 제목이 반영된 회의록 상세
     */
    async updateTitle(recordSeq: number, title: string): Promise<VoiceAnalysisResult> {
        const { data } = await client.post<ApiResponse<VoiceAnalysisResult>>(
            `/v1/voice/${recordSeq}/title`,
            { title },
        )
        return data.result
    },

    /**
     * 내 회의록 이력 (최신순, 페이징). page·size 를 넘기면 해당 페이지만,
     * 생략하면 서버가 전체를 한 페이지로 반환한다.
     */
    async history(page?: number, size?: number): Promise<VoiceRecordPage> {
        const { data } = await client.get<ApiResponse<VoiceRecordPage>>('/v1/voice', {
            params: { page, size },
        })
        return data.result
    },

    /** 회의록 상세 (전사문·요약 전문) */
    async getRecord(recordSeq: number): Promise<VoiceAnalysisResult> {
        const { data } = await client.get<ApiResponse<VoiceAnalysisResult>>(
            `/v1/voice/${recordSeq}`,
        )
        return data.result
    },

    /** 회의록 삭제 (DB 행 + 오디오 파일) */
    async remove(recordSeq: number): Promise<void> {
        await client.post<ApiResponse<void>>(`/v1/voice/${recordSeq}/delete`)
    },

    /**
     * 오디오 스트리밍 URL. <audio src> 에 직접 넣어 브라우저가 Range 요청을 하게 한다
     * (axios 로 받아 Blob 으로 만들면 구간 이동이 안 되므로 URL 을 그대로 쓴다).
     */
    audioUrl(recordSeq: number): string {
        return `/api/v1/voice/${recordSeq}/audio`
    },
}
