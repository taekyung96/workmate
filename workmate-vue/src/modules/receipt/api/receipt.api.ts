import client from '@/common/api/client'
import type { ApiResponse } from '@/common/types/api'
import type { Receipt, ReceiptAnalysis, ReceiptPage, ReceiptSaveRequest } from '../types'

/**
 * 영수증 API (WEB의 /api/v1/receipts/* 프록시 호출 → WAS).
 * 계층 규칙: HTTP 통신만 담당. 사용자 식별은 WEB이 세션에서 X-User-Seq로 주입하므로 여기선 안 넘긴다.
 */
export const receiptApi = {
    /**
     * 영수증 이미지 분석 (multipart). WAS가 OCR·AI 분석 후 후보값을 반환하고 이력에 자동 저장한다.
     * FormData를 넘기면 axios가 multipart/form-data 경계를 자동 설정한다.
     */
    async analyze(file: File): Promise<ReceiptAnalysis> {
        const form = new FormData()
        form.append('file', file) // WAS @RequestParam("file")와 파트명 일치
        const { data } = await client.post<ApiResponse<ReceiptAnalysis>>(
            '/v1/receipts/analyze',
            form,
        )
        return data.result
    },

    /** 사용자가 확인·수정한 최종 데이터를 영구 저장 */
    async save(payload: ReceiptSaveRequest): Promise<Receipt> {
        const { data } = await client.post<ApiResponse<Receipt>>('/v1/receipts', payload)
        return data.result
    },

    /**
     * 내 영수증 이력 (최신순, 페이징). page·size 를 넘기면 해당 페이지만,
     * 생략하면 서버가 전체를 한 페이지로 반환한다.
     */
    async history(page?: number, size?: number): Promise<ReceiptPage> {
        const { data } = await client.get<ApiResponse<ReceiptPage>>('/v1/receipts', {
            params: { page, size },
        })
        return data.result
    },

    /** 이력 CSV 다운로드 (blob) */
    async downloadCsv(): Promise<Blob> {
        const { data } = await client.get<Blob>('/v1/receipts/csv', { responseType: 'blob' })
        return data
    },
}
