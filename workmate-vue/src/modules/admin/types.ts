/** 관리자 사용자 목록 항목 (WAS AdminUserVo 대응). 이메일·전화는 서버에서 마스킹된 값 */
export interface AdminUser {
    userSeq: number
    maskedEmail: string
    userName: string
    maskedPhone: string
    role: string
    /** 현재 잠금 상태 여부 */
    locked: boolean
    createdAt: string
}

/** 사용자 목록 페이지 응답 (WAS UserPageVo 대응) */
export interface UserPage {
    content: AdminUser[]
    /** 0-based 현재 페이지 */
    page: number
    totalPages: number
    totalElements: number
}

/** 비밀번호 초기화 결과 — 임시 비밀번호 평문(1회 표시) */
export interface ResetPasswordResult {
    tempPassword: string
}
