import client from '@/common/api/client'
import type { ApiResponse } from '@/common/types/api'
import type { Guide, GuidePage, GuideSaveRequest } from '../types'

/** 목록 조회 파라미터 */
export interface GuideListParams {
    keyword?: string
    /** 0-based 페이지 */
    page?: number
    size?: number
}

/**
 * 가이드 API (WEB의 /api/v1/guides/* 프록시 호출 → WAS).
 * 계층 규칙: HTTP 통신만 담당. GET/POST만 사용(수정/삭제도 POST — 03 API 스펙).
 */
export const guideApi = {
    /** 목록 조회 (본인 + 공개 문서, 키워드 검색·페이징) — G1 */
    async list(params?: GuideListParams): Promise<GuidePage> {
        const { data } = await client.get<ApiResponse<GuidePage>>('/v1/guides', {
            params: {
                // 빈 검색어는 아예 보내지 않아 서버가 전체 조회로 처리하게 한다
                keyword: params?.keyword?.trim() || undefined,
                page: params?.page ?? 0,
                size: params?.size ?? 6,
            },
        })
        return data.result
    },

    /** 상세 조회 — G2 */
    async detail(guideSeq: number): Promise<Guide> {
        const { data } = await client.get<ApiResponse<Guide>>(`/v1/guides/${guideSeq}`)
        return data.result
    },

    /** 등록 (+WAS에서 임베딩) — G3 */
    async create(payload: GuideSaveRequest): Promise<Guide> {
        const { data } = await client.post<ApiResponse<Guide>>('/v1/guides', payload)
        return data.result
    },

    /** 수정 (+재임베딩) — G4 */
    async update(guideSeq: number, payload: GuideSaveRequest): Promise<Guide> {
        const { data } = await client.post<ApiResponse<Guide>>(
            `/v1/guides/${guideSeq}/update`,
            payload,
        )
        return data.result
    },

    /** 삭제 (+청크 삭제) — G5 */
    async remove(guideSeq: number): Promise<void> {
        await client.post(`/v1/guides/${guideSeq}/delete`)
    },
}
