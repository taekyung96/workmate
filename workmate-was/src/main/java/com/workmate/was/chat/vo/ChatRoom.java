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

import java.time.LocalDateTime;

/**
 * 채팅방 Entity (chat_room 테이블 매핑, 04 문서 §4.2).
 * FK 제약이 없는 최소 스펙이라 user_seq 는 스칼라 필드로 매핑하고, 소유권 검증은 서비스에서 수행한다.
 */
@Entity
@Table(name = "chat_room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_seq")
    private Long roomSeq;

    /** 방 소유자 (app_user.user_seq) */
    @Column(name = "user_seq", nullable = false)
    private Long userSeq;

    /** 방 제목 — 첫 질문 앞 30자 (F2-02) */
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    /** 논리 삭제 플래그 — false 면 목록에서 제외 (F2-04) */
    @Column(name = "use_yn", nullable = false)
    private boolean useYn;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public ChatRoom(Long userSeq, String title) {
        this.userSeq = userSeq;
        this.title = title;
        this.useYn = true;
        this.createdAt = LocalDateTime.now();
    }

    /** 논리 삭제 — 이력 데이터는 보존하고 목록에서만 제외 (F2-04) */
    public void delete() {
        this.useYn = false;
    }
}
