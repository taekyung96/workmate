/** 음성 회의록 분석 결과 (WAS VoiceAnalysisResultVo와 대응) */
export interface VoiceAnalysisResult {
    recordSeq: number
    title: string
    /** STT 전사 원문 */
    sttText: string
    /** AI 구조화 요약 (마크다운) */
    summaryMd: string
    createdAt: string
}
