/** AI가 추출한 결제 후보 항목 (WAS OcrResultVo 대응, MANUAL 선택 후보) */
export interface ReceiptOcrItem {
    cardName: string | null
    payAmount: number | null
    payDate: string | null
    bizNo: string | null
}

/** 영수증 이미지 분석 결과 (POST /analyze 응답) */
export interface ReceiptAnalysis {
    /** 서버에 임시 저장된 이미지 경로 (저장 시 그대로 전달) */
    imagePath: string
    payAmount: number | null
    /** 사업자등록번호 (하이픈 없는 10자리) */
    bizNo: string | null
    /** 결제일 (YYYYMMDD) */
    payDate: string | null
    cardName: string | null
    /** 사업자번호 체크섬 검증 통과 여부 */
    bizNoValid: boolean | null
    /** 카드 선택 방식 ('AUTO' | 'MANUAL') */
    selectType: string
    /** AI 원본 추출 JSON (저장 시 그대로 전달) */
    rawJson: string | null
    items: ReceiptOcrItem[]
}

/** 영수증 최종 저장 요청 (POST /) — WAS ReceiptSaveRequestVo 대응 */
export interface ReceiptSaveRequest {
    imagePath: string
    payAmount: number
    bizNo: string
    payDate: string
    cardName?: string | null
    selectType: string
    rawJson?: string | null
}

/** 저장된 영수증 (save/history 응답) */
export interface Receipt {
    receiptSeq: number
    userSeq: number
    imagePath: string
    payAmount: number
    bizNo: string
    payDate: string
    cardName: string | null
    bizNoValid: boolean
    selectType: string
    rawJson: string | null
    createdAt: string
}

/** 영수증 이력 페이지 응답 (WAS ReceiptPageVo 대응) */
export interface ReceiptPage {
    content: Receipt[]
    /** 0-based 현재 페이지 */
    page: number
    totalPages: number
    totalElements: number
}
