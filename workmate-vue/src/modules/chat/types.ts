/** 채팅방 (WAS chat_room과 대응) */
export interface ChatRoom {
    roomSeq: number
    title: string
    createdAt?: string
}

/** RAG 출처 (SSE source 이벤트, F4-07) */
export interface ChatSource {
    guideSeq: number
    title: string
}

/** 대화 메시지 한 건 */
export interface ChatMessage {
    messageSeq?: number
    role: 'user' | 'assistant'
    content: string
    /** 스트리밍 수신 중 여부 (커서 표시용) */
    streaming?: boolean
    /** 응답 실패 여부 (재시도 표시용) */
    error?: boolean
    /** 재시도 가능 여부 — 스트림 시작 전 요청 거절(저장된 게 없음)일 때만 true */
    canRetry?: boolean
    /** RAG 출처 목록 */
    sources?: ChatSource[]
}

/** 스트리밍 전송 요청 (C3). roomSeq null이면 새 방 생성 */
export interface ChatStreamRequest {
    roomSeq: number | null
    message: string
    modelCode?: string
    /** RAG 모드 — true면 가이드 문서에서 유사 청크를 검색해 답변 근거로 사용 (F4-05) */
    ragMode?: boolean
}

/** SSE 이벤트 수신 콜백 (meta/token/source/done/error) */
export interface ChatStreamHandlers {
    onMeta?: (data: { roomSeq: number; title: string }) => void
    onToken?: (data: { delta: string }) => void
    onSource?: (data: ChatSource) => void
    onDone?: (data: { messageSeq: number; modelName: string }) => void
    /**
     * status 가 있으면 HTTP/제공자 오류 코드(429 면 요청제한/할당량 → 상단 배너),
     * retryable=true 면 스트림 시작 전 거절(저장된 게 없어 재시도 안전).
     */
    onError?: (data: { message: string; status?: number; retryable?: boolean }) => void
}
