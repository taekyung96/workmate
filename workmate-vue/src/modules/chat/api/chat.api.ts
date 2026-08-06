import client from '@/common/api/client'
import type { ApiResponse } from '@/common/types/api'
import type { ChatMessage, ChatRoom, ChatStreamHandlers, ChatStreamRequest } from '../types'

/** 쿠키에서 값 읽기 (CSRF 토큰 추출용) */
function readCookie(name: string): string {
    const match = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'))
    return match ? decodeURIComponent(match[1]!) : ''
}

/** 하나의 SSE 이벤트 블록(event/data 줄)을 파싱해 핸들러로 분배한다 */
function dispatchEvent(rawEvent: string, handlers: ChatStreamHandlers): void {
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
        case 'meta':
            handlers.onMeta?.(data as { roomSeq: number; title: string })
            break
        case 'token':
            handlers.onToken?.(data as { delta: string })
            break
        case 'source':
            handlers.onSource?.(data as { guideSeq: number; title: string })
            break
        case 'done':
            handlers.onDone?.(data as { messageSeq: number; modelName: string })
            break
        case 'error':
            handlers.onError?.(data as { message: string; status?: number; retryable?: boolean })
            break
    }
}

export const chatApi = {
    /** 내 채팅방 목록 (C1) */
    async rooms(): Promise<ChatRoom[]> {
        const { data } = await client.get<ApiResponse<ChatRoom[]>>('/v1/chat/rooms')
        return data.result
    },

    /** 방 대화 이력 (C2) */
    async messages(roomSeq: number): Promise<ChatMessage[]> {
        const { data } = await client.get<ApiResponse<ChatMessage[]>>(
            `/v1/chat/rooms/${roomSeq}/messages`,
        )
        return data.result
    },

    /** 방 논리 삭제 (C4) */
    async deleteRoom(roomSeq: number): Promise<void> {
        await client.post(`/v1/chat/rooms/${roomSeq}/delete`)
    },

    /**
     * 메시지 전송 + SSE 스트리밍 수신 (C3).
     *
     * <p>C3는 POST + 본문 + CSRF 헤더가 필요해 EventSource(GET 전용)를 못 쓴다.
     * fetch + ReadableStream으로 text/event-stream을 직접 읽어 이벤트를 파싱한다.</p>
     *
     * @param body 전송 본문 (roomSeq null이면 새 방)
     * @param handlers SSE 이벤트 콜백
     * @param signal 중단용 AbortSignal (선택)
     */
    async stream(
        body: ChatStreamRequest,
        handlers: ChatStreamHandlers,
        signal?: AbortSignal,
    ): Promise<void> {
        const response = await fetch('/api/v1/chat/stream', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                // 쿠키 기반 CSRF: XSRF-TOKEN 쿠키를 헤더로 수동 첨부
                'X-XSRF-TOKEN': readCookie('XSRF-TOKEN'),
            },
            credentials: 'include',
            body: JSON.stringify(body),
            signal,
        })

        if (!response.ok || !response.body) {
            handlers.onError?.({ message: '요청을 처리하지 못했습니다.' })
            return
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        while (true) {
            const { done, value } = await reader.read()
            if (done) break
            // \r 제거로 CRLF/LF 구분자를 통일하고, 빈 줄(\n\n)로 이벤트를 구분
            buffer += decoder.decode(value, { stream: true }).replace(/\r/g, '')
            let sepIndex: number
            while ((sepIndex = buffer.indexOf('\n\n')) !== -1) {
                const rawEvent = buffer.slice(0, sepIndex)
                buffer = buffer.slice(sepIndex + 2)
                dispatchEvent(rawEvent, handlers)
            }
        }
    },
}
