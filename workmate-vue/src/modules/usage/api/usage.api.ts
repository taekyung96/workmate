import client from '@/common/api/client'
import type { ApiResponse } from '@/common/types/api'
import type { UsageSummary } from '@/common/types/usage'

/**
 * 본인 사용량 API.
 *
 * 조회 대상을 지정하는 파라미터가 없다 — 서버가 세션의 로그인 사용자로 고정한다.
 * 사용자 번호를 보낼 수 있게 만드는 순간 남의 사용량을 조회할 길이 열리므로 두지 않는다.
 */
export const usageApi = {
    /** 본인 기간 요약 (from·to 미지정 시 서버가 최근 30일로 채운다) */
    async mySummary(from?: string, to?: string): Promise<UsageSummary> {
        const { data } = await client.get<ApiResponse<UsageSummary>>('/v1/usage/me', {
            params: { from, to },
        })
        return data.result
    },
}
