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

/** 관리자 감사 로그 항목 (WAS AuditLogVo 대응) — 행위자·대상 이름 포함 */
export interface AuditLog {
    auditSeq: number
    adminUserSeq: number
    adminUserName: string
    targetUserSeq: number
    targetUserName: string
    /** 'UNLOCK' | 'RESET_PASSWORD' */
    action: string
    createdAt: string
}

/** 감사 로그 페이지 응답 (WAS AuditLogPageVo 대응) */
export interface AuditLogPage {
    content: AuditLog[]
    /** 0-based 현재 페이지 */
    page: number
    totalPages: number
    totalElements: number
}

/** 공통코드 그룹 (WAS CommonCodeGroupVo 대응) */
export interface CommonCodeGroup {
    groupCode: string
    groupName: string
    description: string | null
    useYn: boolean
}

/** 공통코드 항목 (WAS CommonCodeAdminVo 대응) — 비활성 포함 */
export interface CommonCodeItem {
    code: string
    codeName: string
    sortOrder: number
    useYn: boolean
}

/** 그룹 등록/수정 요청 (groupCode는 등록 시에만 사용) */
export interface CommonCodeGroupSave {
    groupCode?: string
    groupName: string
    description?: string | null
    useYn: boolean
}

/** 코드 등록/수정 요청 (code는 등록 시에만 사용) */
export interface CommonCodeSave {
    code?: string
    codeName: string
    sortOrder: number
    useYn: boolean
}

// 사용량 타입은 admin·usage 두 모듈이 공유하므로 common 으로 옮겼다.
// 기존 import 경로를 깨지 않도록 여기서 다시 내보낸다.
export type {
    UsagePeriod,
    UsageTotal,
    FeatureUsage,
    DailyUsage,
    UsageSummary,
    UserUsage,
    UserUsagePage,
} from '@/common/types/usage'
