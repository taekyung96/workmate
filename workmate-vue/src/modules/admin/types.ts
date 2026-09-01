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

// ----- 사용량 대시보드 (WAS usage.vo.* 대응) -----

/** 사용량 집계 조회 기간 — 서버가 실제 적용한 from/to (WAS UsagePeriodVo 대응) */
export interface UsagePeriod {
    from: string
    to: string
}

/**
 * 기간 합계 (WAS UsageTotalVo 대응).
 * untrackedCallCount는 토큰이 NULL이라 합계에서 빠진 건수(주로 EMBEDDING), unpricedCallCount는
 * 단가가 등록되지 않은 모델이라 비용 계산에서 빠진 건수 — 둘 다 0으로 뭉개지 않고 화면에 표시해야 한다.
 */
export interface UsageTotal {
    callCount: number
    inputTokens: number
    outputTokens: number
    untrackedCallCount: number
    unpricedCallCount: number
    /** 추정 비용(USD) — 실제 청구액이 아니다 */
    estimatedCostUsd: number
    /** 추정 비용(KRW) — 고정 환율 환산, 실제 청구액이 아니다 */
    estimatedCostKrw: number
}

/** 기능별 사용량 (WAS FeatureUsageVo 대응) — 호출 0건인 기능도 서버가 채워서 5종 전부 내려준다 */
export interface FeatureUsage {
    feature: string
    callCount: number
    inputTokens: number
    outputTokens: number
    untrackedCallCount: number
}

/** 일별 사용량 (WAS DailyUsageVo 대응) — 데이터 없는 날도 서버가 0으로 채워 반환한다 */
export interface DailyUsage {
    date: string
    callCount: number
    inputTokens: number
    outputTokens: number
    untrackedCallCount: number
}

/** 사용량 요약 응답 (WAS UsageSummaryVo 대응) — 합계·기능별·일별을 한 번에 담는다 */
export interface UsageSummary {
    period: UsagePeriod
    total: UsageTotal
    byFeature: FeatureUsage[]
    daily: DailyUsage[]
}

/** 사용자별 사용량 항목 (WAS UserUsageVo 대응). 이메일은 서버에서 마스킹된 값 */
export interface UserUsage {
    userSeq: number
    maskedEmail: string
    userName: string
    callCount: number
    inputTokens: number
    outputTokens: number
    untrackedCallCount: number
    estimatedCostUsd: number
    estimatedCostKrw: number
}

/** 사용자별 사용량 페이지 응답 (WAS UserUsagePageVo 대응) */
export interface UserUsagePage {
    content: UserUsage[]
    /** 0-based 현재 페이지 */
    page: number
    totalPages: number
    totalElements: number
}
