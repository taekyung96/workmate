package com.workmate.was.chat.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workmate.was.chat.dao.ChatMessageRepository;
import com.workmate.was.chat.dao.ChatRoomRepository;
import com.workmate.was.chat.vo.ChatMessage;
import com.workmate.was.chat.vo.ChatRoom;
import com.workmate.was.chat.vo.ChatSourceVo;
import com.workmate.was.chat.vo.ChatStreamRequestVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 채팅 스트리밍의 트랜잭션 영속화 담당 빈.
 * 스트리밍 서비스와 분리해 별도 빈으로 둔 이유: 리액티브 스트림(무트랜잭션)과 JPA 트랜잭션을
 * 섞지 않고, 자기호출 프록시 우회(1-A 핫픽스에서 겪은 함정)를 방지하기 위함이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class ChatMessagePersister {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;

    /** 방 제목 최대 길이 — 첫 질문 앞 30자 (F2-02) */
    private static final int TITLE_MAX = 30;

    /** 프롬프트에 포함할 직전 대화 개수 (F2-10, 토큰 한도 고려 설정값) */
    @Value("${app.chat.context-size:10}")
    private int contextSize;

    /**
     * 스트리밍 시작 전 동기 준비: 방 확보(없으면 생성) → 맥락 로드 → 사용자 메시지 저장.
     * 맥락은 현재 질문을 저장하기 전에 로드해 자기 질문이 히스토리에 중복되지 않게 한다.
     *
     * @throws IllegalArgumentException 기존 roomSeq 가 본인 방이 아닌 경우
     */
    @Transactional
    public PreparedChat prepare(Long userSeq, ChatStreamRequestVo request) {
        String message = request.getMessage();

        ChatRoom room;
        boolean isNew;
        if (request.getRoomSeq() == null) {
            room = chatRoomRepository.save(ChatRoom.builder()
                    .userSeq(userSeq)
                    .title(makeTitle(message))
                    .build());
            isNew = true;
            log.info("채팅방 신규 생성 - userSeq: {}, roomSeq: {}", userSeq, room.getRoomSeq());
        } else {
            room = chatRoomRepository.findByRoomSeqAndUserSeqAndUseYnTrue(request.getRoomSeq(), userSeq)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));
            isNew = false;
        }

        List<Message> history = loadContext(room.getRoomSeq());

        chatMessageRepository.save(ChatMessage.builder()
                .roomSeq(room.getRoomSeq())
                .role(ChatMessage.ROLE_USER)
                .content(message)
                .build());

        return new PreparedChat(room.getRoomSeq(), room.getTitle(), isNew, history);
    }

    /**
     * AI 응답 완료 후 assistant 메시지를 저장한다 (F2-08·09).
     * 스트림 완료 콜백(별도 스레드)에서 호출되며, 리포지토리 자체 트랜잭션으로 커밋된다.
     *
     * @param sources RAG 출처 목록 — 비어 있으면 null 로 저장한다 (F4-07)
     * @return 저장된 메시지 식별자
     */
    @Transactional
    public Long saveAssistant(Long roomSeq, String content, String modelName, List<ChatSourceVo> sources) {
        ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                .roomSeq(roomSeq)
                .role(ChatMessage.ROLE_ASSISTANT)
                .content(content)
                .modelName(modelName)
                .sources(toJson(sources))
                .build());
        return saved.getMessageSeq();
    }

    /**
     * 출처 목록을 저장용 JSON 문자열로 바꾼다.
     * 직렬화가 실패해도 답변 자체는 남아야 하므로 로그만 남기고 출처를 버린다 —
     * 출처 때문에 응답 저장이 통째로 실패하는 게 더 나쁘다.
     */
    private String toJson(List<ChatSourceVo> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (JsonProcessingException e) {
            log.error("RAG 출처 직렬화 실패 — 출처 없이 저장한다", e);
            return null;
        }
    }

    /** 최근 contextSize 개 메시지를 시간순 Spring AI Message 로 변환 (F2-10) */
    private List<Message> loadContext(Long roomSeq) {
        List<ChatMessage> recent = chatMessageRepository.findByRoomSeqOrderByCreatedAtDesc(
                roomSeq, PageRequest.of(0, contextSize));
        List<ChatMessage> ordered = new ArrayList<>(recent);
        Collections.reverse(ordered); // 최신순 → 시간순
        return ordered.stream()
                .map(m -> ChatMessage.ROLE_ASSISTANT.equals(m.getRole())
                        ? (Message) new AssistantMessage(m.getContent())
                        : (Message) new UserMessage(m.getContent()))
                .toList();
    }

    /** 첫 질문 앞 30자로 방 제목 생성 (F2-02) */
    private String makeTitle(String message) {
        String trimmed = message == null ? "" : message.strip();
        if (trimmed.isEmpty()) {
            trimmed = "새 채팅";
        }
        return trimmed.length() > TITLE_MAX ? trimmed.substring(0, TITLE_MAX) : trimmed;
    }
}
