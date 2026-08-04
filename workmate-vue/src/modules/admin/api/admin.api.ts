import client from '@/common/api/client'
import type { ApiResponse } from '@/common/types/api'
import type {
    AuditLogPage,
    CommonCodeGroup,
    CommonCodeGroupSave,
    CommonCodeItem,
    CommonCodeSave,
    ResetPasswordResult,
    UserPage,
} from '../types'

/**
 * 관리자 API (WEB의 /api/v1/admin/* 프록시 → WAS).
 * 접근 제어(ROLE_ADMIN)는 WEB SecurityConfig가 담당하고, 감사 주체는 세션의 X-User-Seq로 식별된다.
 */
export const adminApi = {
    /** 사용자 목록·검색 (M1) — 페이징, 이메일·전화는 마스킹된 값 */
    async users(keyword: string, page: number, size = 10): Promise<UserPage> {
        const { data } = await client.get<ApiResponse<UserPage>>('/v1/admin/users', {
            params: { keyword: keyword || undefined, page, size },
        })
        return data.result
    },

    /** 감사 로그 목록 (M4) — 최신순 페이징, 행위자·대상 이름 포함 */
    async auditLogs(page: number, size = 10): Promise<AuditLogPage> {
        const { data } = await client.get<ApiResponse<AuditLogPage>>('/v1/admin/audit-logs', {
            params: { page, size },
        })
        return data.result
    },

    /** 계정 잠금 해제 (M2) — 로그인 실패 횟수 초기화 */
    async unlock(userSeq: number): Promise<void> {
        await client.post(`/v1/admin/users/${userSeq}/unlock`)
    },

    /** 비밀번호 초기화 (M3) — 임시 비밀번호 평문을 1회 반환 */
    async resetPassword(userSeq: number): Promise<ResetPasswordResult> {
        const { data } = await client.post<ApiResponse<ResetPasswordResult>>(
            `/v1/admin/users/${userSeq}/reset-password`,
        )
        return data.result
    },

    // ----- 공통코드 관리 (F7) -----

    /** 그룹 목록 (비활성 포함) */
    async codeGroups(): Promise<CommonCodeGroup[]> {
        const { data } = await client.get<ApiResponse<CommonCodeGroup[]>>(
            '/v1/admin/common-codes/groups',
        )
        return data.result
    },

    /** 그룹 등록 */
    async createGroup(body: CommonCodeGroupSave): Promise<CommonCodeGroup> {
        const { data } = await client.post<ApiResponse<CommonCodeGroup>>(
            '/v1/admin/common-codes/groups',
            body,
        )
        return data.result
    },

    /** 그룹 수정 */
    async updateGroup(groupCode: string, body: CommonCodeGroupSave): Promise<CommonCodeGroup> {
        const { data } = await client.put<ApiResponse<CommonCodeGroup>>(
            `/v1/admin/common-codes/groups/${groupCode}`,
            body,
        )
        return data.result
    },

    /** 그룹 삭제 (하위 코드 없을 때만) */
    async deleteGroup(groupCode: string): Promise<void> {
        await client.delete(`/v1/admin/common-codes/groups/${groupCode}`)
    },

    /** 그룹 내 코드 목록 (비활성 포함) */
    async codes(groupCode: string): Promise<CommonCodeItem[]> {
        const { data } = await client.get<ApiResponse<CommonCodeItem[]>>(
            `/v1/admin/common-codes/groups/${groupCode}/codes`,
        )
        return data.result
    },

    /** 코드 등록 */
    async createCode(groupCode: string, body: CommonCodeSave): Promise<CommonCodeItem> {
        const { data } = await client.post<ApiResponse<CommonCodeItem>>(
            `/v1/admin/common-codes/groups/${groupCode}/codes`,
            body,
        )
        return data.result
    },

    /** 코드 수정 */
    async updateCode(
        groupCode: string,
        code: string,
        body: CommonCodeSave,
    ): Promise<CommonCodeItem> {
        const { data } = await client.put<ApiResponse<CommonCodeItem>>(
            `/v1/admin/common-codes/groups/${groupCode}/codes/${code}`,
            body,
        )
        return data.result
    },

    /** 코드 삭제 */
    async deleteCode(groupCode: string, code: string): Promise<void> {
        await client.delete(`/v1/admin/common-codes/groups/${groupCode}/codes/${code}`)
    },
}
