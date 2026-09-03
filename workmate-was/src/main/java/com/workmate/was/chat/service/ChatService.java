package com.workmate.was.chat.service;

import com.workmate.was.chat.vo.ChatMessageVo;
import com.workmate.was.chat.vo.ChatRoomVo;
import com.workmate.was.chat.vo.ChatStreamRequestVo;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 채팅 비즈니스 로직 인터페이스.
 */
public interface ChatService {

    /** 본인 채팅방 최신순 목록 (F2-03) */
    List<ChatRoomVo> getRooms(Long userSeq);

    /** 방 대화 이력 시간순 조회 — 본인 방만 (F2-08) */
    List<ChatMessageVo> getMessages(Long userSeq, Long roomSeq);

    /** 방 논리 삭제 — 본인 방만 (F2-04) */
    void deleteRoom(Long userSeq, Long roomSeq);

    /**
     * 메시지 전송 + AI 응답 SSE 스트리밍 (C3, F2-02·05·06).
     * roomSeq null 이면 방 생성 후 meta 이벤트 → token 반복 → done(assistant 저장) 순으로 흘린다.
     *
     * @return meta/token/done/error 이벤트 Flux (text/event-stream)
     */
    Flux<ServerSentEvent<String>> streamChat(Long userSeq, String role, ChatStreamRequestVo request);
}
