import { defineStore } from 'pinia'
import { ref } from 'vue'
import { chatApi } from '../api/chat.api'
import { commonCodeApi, type CommonCode } from '@/common/api/commonCode'
import type { ChatMessage, ChatRoom } from '../types'

/**
 * 채팅 전역 상태 (SSE 수신 중 화면을 이동해도 유지되어야 하므로 store로 관리).
 * 방 목록·현재 방·메시지·스트리밍 상태를 보유하고, 전송/수신을 오케스트레이션한다.
 */
export const useChatStore = defineStore('chat', () => {
    const rooms = ref<ChatRoom[]>([])
    const currentRoomSeq = ref<number | null>(null)
    const messages = ref<ChatMessage[]>([])
    const streaming = ref(false)
    const roomsLoaded = ref(false)
    // RAG 모드 — 켜면 전송 시 가이드 문서 검색 근거를 요청한다(출처 뱃지로 표시). 대화 내내 유지되는 선호값이라 store에 둔다
    const ragMode = ref(false)
    // 선택 가능한 AI 모델 목록(공통코드 AI_MODEL)과 현재 선택값. 대화 내내 유지되는 선호값이라 store에 둔다
    const models = ref<CommonCode[]>([])
    const selectedModel = ref<string | null>(null)
    const modelsLoaded = ref(false)
    // 요청제한(429) 안내 배너 메시지 (null이면 미표시)
    const rateLimited = ref<string | null>(null)
    // 재시도 대상 사용자 입력 (스트림 시작 전 거절된 마지막 메시지)
    const retryText = ref<string | null>(null)

    /** 방 목록 로드 (C1) */
    async function loadRooms(): Promise<void> {
        rooms.value = await chatApi.rooms()
        roomsLoaded.value = true
    }

    /** 선택 가능한 AI 모델 목록 로드 (AI_MODEL 공통코드). 선택값이 없으면 첫 모델을 기본으로 */
    async function loadModels(): Promise<void> {
        models.value = await commonCodeApi.codes('AI_MODEL')
        if (!selectedModel.value && models.value.length > 0) {
            selectedModel.value = models.value[0]!.code
        }
        modelsLoaded.value = true
    }

    /** 새 채팅 시작 — 현재 방/메시지 초기화 (빈 상태) */
    function startNewChat(): void {
        if (streaming.value) return
        currentRoomSeq.value = null
        messages.value = []
    }

    /** 방 선택 → 대화 이력 로드 (C2) */
    async function selectRoom(roomSeq: number): Promise<void> {
        if (streaming.value) return
        currentRoomSeq.value = roomSeq
        messages.value = await chatApi.messages(roomSeq)
    }

    /** 방 삭제 (C4). 현재 보던 방이면 새 채팅 상태로 전환 */
    async function deleteRoom(roomSeq: number): Promise<void> {
        await chatApi.deleteRoom(roomSeq)
        rooms.value = rooms.value.filter((r) => r.roomSeq !== roomSeq)
        if (currentRoomSeq.value === roomSeq) startNewChat()
    }

    /**
     * 메시지 전송 + SSE 스트리밍 수신 (C3).
     * 사용자 메시지와 빈 AI 메시지를 먼저 넣고, 토큰이 올 때마다 AI 메시지에 이어붙인다.
     */
    async function sendMessage(text: string): Promise<void> {
        const trimmed = text.trim()
        if (streaming.value || trimmed === '') return

        // 새 전송 시작 — 이전 요청제한 배너는 걷어낸다
        rateLimited.value = null
        messages.value.push({ role: 'user', content: trimmed })
        messages.value.push({ role: 'assistant', content: '', streaming: true })
        // 배열에 들어간 반응형 프록시를 잡아야 이후 변경이 화면에 반영된다
        const ai = messages.value[messages.value.length - 1]!
        streaming.value = true

        try {
            await chatApi.stream(
                {
                    roomSeq: currentRoomSeq.value,
                    message: trimmed,
                    modelCode: selectedModel.value ?? undefined,
                    ragMode: ragMode.value,
                },
                {
                    onMeta: (d) => {
                        // 새 방 생성 시: 현재 방으로 지정 + 목록 맨 위에 추가
                        currentRoomSeq.value = d.roomSeq
                        if (!rooms.value.some((r) => r.roomSeq === d.roomSeq)) {
                            rooms.value.unshift({
                                roomSeq: d.roomSeq,
                                title: d.title,
                                createdAt: new Date().toISOString(),
                            })
                        }
                    },
                    onToken: (d) => {
                        ai.content += d.delta
                    },
                    onSource: (d) => {
                        ;(ai.sources ??= []).push(d)
                    },
                    onDone: (d) => {
                        ai.streaming = false
                        ai.messageSeq = d.messageSeq
                    },
                    onError: (d) => {
                        ai.streaming = false
                        ai.error = true
                        // status가 있으면 스트림 시작 전 요청 거절 → 저장된 게 없어 재시도 안전
                        const preSave = d.status !== undefined
                        ai.canRetry = preSave
                        if (!ai.content) ai.content = d.message
                        if (preSave) retryText.value = trimmed
                        // 429(요청제한)는 상단 배너로도 안내
                        if (d.status === 429) rateLimited.value = d.message
                    },
                },
            )
        } catch {
            ai.error = true
            if (!ai.content) ai.content = '응답 중 오류가 발생했습니다.'
        } finally {
            streaming.value = false
            ai.streaming = false
        }
    }

    /**
     * 마지막 실패(요청 거절) 메시지를 재시도한다.
     * 스트림 시작 전 거절이라 DB에 저장된 게 없으므로, 낙관적으로 추가했던 사용자·AI 말풍선 한 쌍을
     * 걷어내고 같은 내용을 다시 전송한다(중복 저장 방지).
     */
    async function retryLast(): Promise<void> {
        if (streaming.value) return
        const last = messages.value[messages.value.length - 1]
        if (!last || last.role !== 'assistant' || !last.canRetry) return
        const text = retryText.value ?? ''
        messages.value.splice(messages.value.length - 2, 2)
        retryText.value = null
        if (text) await sendMessage(text)
    }

    /** 요청제한 배너 닫기 */
    function dismissRateLimit(): void {
        rateLimited.value = null
    }

    return {
        rooms,
        currentRoomSeq,
        messages,
        streaming,
        roomsLoaded,
        ragMode,
        models,
        selectedModel,
        modelsLoaded,
        rateLimited,
        retryLast,
        dismissRateLimit,
        loadRooms,
        loadModels,
        startNewChat,
        selectRoom,
        deleteRoom,
        sendMessage,
    }
})
