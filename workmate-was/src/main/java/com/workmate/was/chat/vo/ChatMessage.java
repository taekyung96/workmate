package com.workmate.was.chat.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 Entity (chat_message 테이블 매핑, 04 문서 §4.2).
 * user/assistant 메시지를 모두 기록하며(F2-08), assistant 메시지에는 생성 모델명을 남긴다(F2-09).
 */
@Entity
@Table(name = "chat_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    /** 발화 주체 — 사용자 메시지 */
    public static final String ROLE_USER = "user";
    /** 발화 주체 — AI 응답 */
    public static final String ROLE_ASSISTANT = "assistant";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_seq")
    private Long messageSeq;

    /** 소속 채팅방 (chat_room.room_seq) */
    @Column(name = "room_seq", nullable = false)
    private Long roomSeq;

    /** 'user' | 'assistant' */
    @Column(name = "role", nullable = false, length = 10)
    private String role;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    /** 응답 생성 모델명 — assistant 만 기록 (F2-09) */
    @Column(name = "model_name", length = 50)
    private String modelName;

    /**
     * RAG 출처 목록의 JSON 문자열 — {@code [{"guideSeq":32,"title":"..."}]} (F4-07).
     * 스트리밍 중에만 보이고 이력에서 사라지던 출처 뱃지를 되살리기 위해 함께 저장한다.
     * RAG 를 쓰지 않은 답변과 사용자 메시지는 null 이다.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sources", columnDefinition = "jsonb")
    private String sources;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public ChatMessage(Long roomSeq, String role, String content, String modelName, String sources) {
        this.roomSeq = roomSeq;
        this.role = role;
        this.content = content;
        this.modelName = modelName;
        this.sources = sources;
        this.createdAt = LocalDateTime.now();
    }
}
