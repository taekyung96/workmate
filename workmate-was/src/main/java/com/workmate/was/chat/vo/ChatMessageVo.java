package com.workmate.was.chat.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** 채팅 메시지 이력 응답 VO (C2). */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageVo {
    private Long messageSeq;
    private String role;
    private String content;
    private String modelName;
    /** RAG 출처 목록 — 없으면 빈 목록 (F4-07) */
    private List<ChatSourceVo> sources;
    private LocalDateTime createdAt;
}
