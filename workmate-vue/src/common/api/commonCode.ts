import client from '@/common/api/client'
import type { ApiResponse } from '@/common/types/api'

/** 공통코드 항목 (WAS CommonCodeVo 대응) */
export interface CommonCode {
    code: string
    codeName: string
}

/**
 * 공통코드 API (K1, F7-04) — WEB의 /api/common/codes/* 프록시 → WAS.
 * 예: 채팅 모델 드롭다운 구성용 AI_MODEL 조회.
 */
export const commonCodeApi = {
    /**
     * 그룹 코드 목록 조회.
     * @param groupCode 그룹 코드 (예: 'AI_MODEL'). 없는 그룹은 빈 배열
     * @returns 코드 목록
     */
    async codes(groupCode: string): Promise<CommonCode[]> {
        const { data } = await client.get<ApiResponse<CommonCode[]>>(`/common/codes/${groupCode}`)
        return data.result
    },
}
