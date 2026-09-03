/** 도우미 대화 한 턴 */
export interface AssistantTurn {
    role: 'user' | 'assistant'
    content: string
}

/** 도우미 스트리밍 요청 본문 */
export interface AssistantStreamRequest {
    message: string
    /** 지금 보고 있는 화면 (Vue Router 라우트 이름) */
    route: string
    /** 최근 대화 (최대 6개). 서버가 상한을 다시 강제한다 */
    history: AssistantTurn[]
}

/** SSE 이벤트 핸들러 */
export interface AssistantStreamHandlers {
    onToken?: (data: { delta: string }) => void
    onDone?: () => void
    onError?: (data: { message: string; status?: number }) => void
}
