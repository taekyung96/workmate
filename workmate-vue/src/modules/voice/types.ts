/** 음성 회의록 분석·상세 결과 (WAS VoiceAnalysisResultVo와 대응) */
export interface VoiceAnalysisResult {
    recordSeq: number
    title: string
    /** STT 전사 원문 */
    sttText: string
    /** AI 구조화 요약 (마크다운) */
    summaryMd: string
    /** 사용자가 올린 원본 파일명 (오디오 미보유 시 null) */
    originFileName: string | null
    /** 파일 크기(바이트, 오디오 미보유 시 null) */
    fileSize: number | null
    /** 재생 가능한 오디오 보유 여부 */
    hasAudio: boolean
    createdAt: string
}

/** 회의록 이력 목록 항목 (WAS VoiceRecordSummaryVo와 대응) */
export interface VoiceRecordSummary {
    recordSeq: number
    title: string
    originFileName: string | null
    fileSize: number | null
    hasAudio: boolean
    createdAt: string
}
