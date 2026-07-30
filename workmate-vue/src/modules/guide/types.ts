/** 가이드 문서 (WAS GuideResponseVo와 대응) */
export interface Guide {
    guideSeq: number
    userSeq: number
    title: string
    /** 본문 (마크다운 형식) */
    content: string
    /** 공개 여부 (true: 공개, false: 비공개) */
    isPublic: boolean
    createdAt: string
    updatedAt: string
}

/** 가이드 작성·수정 요청 (WAS GuideSaveRequestVo와 대응) */
export interface GuideSaveRequest {
    title: string
    content: string
    isPublic: boolean
}

/** 가이드 목록 카드용 요약 (WAS GuideSummaryVo와 대응) */
export interface GuideSummary {
    guideSeq: number
    title: string
    /** 본문 미리보기 (마크다운 기호 제거 후 앞부분) */
    excerpt: string
    isPublic: boolean
    updatedAt: string
}

/** 가이드 목록 페이지 응답 (WAS GuidePageVo와 대응) */
export interface GuidePage {
    content: GuideSummary[]
    /** 0-based 현재 페이지 */
    page: number
    totalPages: number
    totalElements: number
}
