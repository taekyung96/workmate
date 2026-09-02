import type { AssistantStreamHandlers, AssistantStreamRequest } from '@/common/types/assistant'

/** 쿠키에서 값 읽기 (CSRF 토큰 추출용) */
function readCookie(name: string): string {
    const match = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'))
    return match ? decodeURIComponent(match[1]!) : ''
}

/** SSE 이벤트 블록 하나(event/data 줄)를 파싱해 핸들러로 분배한다 */
function dispatchEvent(rawEvent: string, handlers: AssistantStreamHandlers): void {
    let eventName = 'message'
    const dataLines: string[] = []
    for (const line of rawEvent.split('\n')) {
        if (line.startsWith('event:')) eventName = line.slice(6).trim()
        else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
    }
    if (dataLines.length === 0) return

    let data: unknown
    try {
        data = JSON.parse(dataLines.join('\n'))
    } catch {
        return
    }

    switch (eventName) {
        case 'token':
            handlers.onToken?.(data as { delta: string })
            break
        case 'done':
            handlers.onDone?.()
            break
        case 'error':
            handlers.onError?.(data as { message: string; status?: number })
            break
    }
}

/**
 * 도우미 응답을 SSE 로 받는다.
 *
 * EventSource 를 쓰지 않는 이유는 채팅과 같다 — EventSource 는 GET 만 지원하고
 * 커스텀 헤더(CSRF)를 붙일 수 없다. fetch + ReadableStream 으로 직접 읽는다.
 *
 * @param request 질문·화면·최근 대화
 * @param handlers 토큰·완료·오류 콜백
 * @param signal 취소용 AbortSignal (패널을 닫으면 중단한다)
 */
export async function streamAssistant(
    request: AssistantStreamRequest,
    handlers: AssistantStreamHandlers,
    signal?: AbortSignal,
): Promise<void> {
    const response = await fetch('/api/assistant/stream', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-XSRF-TOKEN': readCookie('XSRF-TOKEN'),
        },
        body: JSON.stringify(request),
        signal,
    })

    if (!response.ok || !response.body) {
        handlers.onError?.({
            message:
                response.status === 429
                    ? '요청이 많습니다. 잠시 후 이용해주세요.'
                    : '도우미를 불러오지 못했습니다.',
            status: response.status,
        })
        return
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    // SSE 는 빈 줄로 이벤트를 구분한다. 청크 경계가 이벤트 중간을 자를 수 있어 버퍼에 모아 처리한다
    for (;;) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const blocks = buffer.split('\n\n')
        buffer = blocks.pop() ?? ''
        for (const block of blocks) {
            if (block.trim()) dispatchEvent(block, handlers)
        }
    }
}
