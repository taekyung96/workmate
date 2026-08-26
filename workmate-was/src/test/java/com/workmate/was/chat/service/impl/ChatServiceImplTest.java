package com.workmate.was.chat.service.impl;

import com.workmate.was.chat.dao.ChatMessageRepository;
import com.workmate.was.chat.dao.ChatRoomRepository;
import com.workmate.was.chat.vo.ChatMessage;
import com.workmate.was.chat.vo.ChatMessageVo;
import com.workmate.was.chat.vo.ChatRoom;
import com.workmate.was.chat.vo.ChatRoomVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ChatServiceImpl 단위 테스트 (조회·삭제 CRUD + 소유권 검증).
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    // 출처 역직렬화는 실제 동작을 검증해야 하므로 목이 아닌 실객체를 주입한다
    @org.mockito.Spy
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper =
            new com.fasterxml.jackson.databind.ObjectMapper();

    @InjectMocks
    private ChatServiceImpl chatService;

    /** roomSeq 등 DB 생성 필드를 세팅한 ChatRoom 을 만든다 (엔티티는 setter 가 없어 리플렉션 사용) */
    private ChatRoom room(Long roomSeq, Long userSeq, String title) {
        ChatRoom r = ChatRoom.builder().userSeq(userSeq).title(title).build();
        ReflectionTestUtils.setField(r, "roomSeq", roomSeq);
        return r;
    }

    private ChatMessage message(Long messageSeq, Long roomSeq, String role, String content) {
        ChatMessage m = ChatMessage.builder().roomSeq(roomSeq).role(role).content(content).build();
        ReflectionTestUtils.setField(m, "messageSeq", messageSeq);
        return m;
    }

    @Test
    @DisplayName("getRooms: 본인 방을 최신순 그대로 VO 로 매핑한다")
    void getRooms_maps_rooms() {
        when(chatRoomRepository.findByUserSeqAndUseYnTrueOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(room(2L, 1L, "두번째"), room(1L, 1L, "첫번째")));

        List<ChatRoomVo> result = chatService.getRooms(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRoomSeq()).isEqualTo(2L);
        assertThat(result.get(0).getTitle()).isEqualTo("두번째");
    }

    @Test
    @DisplayName("getMessages: 본인 방이면 메시지를 시간순 VO 로 반환한다")
    void getMessages_returns_history() {
        when(chatRoomRepository.findByRoomSeqAndUserSeqAndUseYnTrue(10L, 1L))
                .thenReturn(Optional.of(room(10L, 1L, "방")));
        when(chatMessageRepository.findByRoomSeqOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(
                        message(1L, 10L, ChatMessage.ROLE_USER, "질문"),
                        message(2L, 10L, ChatMessage.ROLE_ASSISTANT, "답변")));

        List<ChatMessageVo> result = chatService.getMessages(1L, 10L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRole()).isEqualTo("user");
        assertThat(result.get(1).getContent()).isEqualTo("답변");
    }

    @Test
    @DisplayName("getMessages: 타인의 방이면 IllegalArgumentException")
    void getMessages_rejects_other_user_room() {
        when(chatRoomRepository.findByRoomSeqAndUserSeqAndUseYnTrue(10L, 99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getMessages(99L, 10L))
                .isInstanceOf(IllegalArgumentException.class);
        verify(chatMessageRepository, never()).findByRoomSeqOrderByCreatedAtAsc(any());
    }

    @Test
    @DisplayName("deleteRoom: 본인 방이면 use_yn 을 false 로 논리 삭제한다")
    void deleteRoom_soft_deletes() {
        ChatRoom target = room(10L, 1L, "방");
        when(chatRoomRepository.findByRoomSeqAndUserSeqAndUseYnTrue(10L, 1L))
                .thenReturn(Optional.of(target));

        chatService.deleteRoom(1L, 10L);

        assertThat(target.isUseYn()).isFalse();
    }

    @Test
    @DisplayName("deleteRoom: 타인의 방이면 IllegalArgumentException")
    void deleteRoom_rejects_other_user_room() {
        when(chatRoomRepository.findByRoomSeqAndUserSeqAndUseYnTrue(eq(10L), eq(99L)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.deleteRoom(99L, 10L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getMessages: 저장된 RAG 출처를 목록으로 복원한다 (F4-07)")
    void getMessages_restores_sources() {
        ChatMessage answer = ChatMessage.builder()
                .roomSeq(10L).role(ChatMessage.ROLE_ASSISTANT).content("답변")
                .sources("[{\"guideSeq\":32,\"title\":\"RAG 파이프라인 가이드\"}]")
                .build();
        ReflectionTestUtils.setField(answer, "messageSeq", 2L);
        when(chatRoomRepository.findByRoomSeqAndUserSeqAndUseYnTrue(10L, 1L))
                .thenReturn(Optional.of(room(10L, 1L, "방")));
        when(chatMessageRepository.findByRoomSeqOrderByCreatedAtAsc(10L)).thenReturn(List.of(answer));

        List<ChatMessageVo> result = chatService.getMessages(1L, 10L);

        // 방을 나갔다 돌아와도 출처 뱃지가 그대로 보여야 한다
        assertThat(result.get(0).getSources()).hasSize(1);
        assertThat(result.get(0).getSources().get(0).guideSeq()).isEqualTo(32L);
        assertThat(result.get(0).getSources().get(0).title()).isEqualTo("RAG 파이프라인 가이드");
    }

    @Test
    @DisplayName("getMessages: 출처 JSON 이 깨져 있어도 이력 조회는 성공한다")
    void getMessages_tolerates_broken_sources() {
        ChatMessage answer = ChatMessage.builder()
                .roomSeq(10L).role(ChatMessage.ROLE_ASSISTANT).content("답변")
                .sources("{망가진 JSON")
                .build();
        ReflectionTestUtils.setField(answer, "messageSeq", 3L);
        when(chatRoomRepository.findByRoomSeqAndUserSeqAndUseYnTrue(10L, 1L))
                .thenReturn(Optional.of(room(10L, 1L, "방")));
        when(chatMessageRepository.findByRoomSeqOrderByCreatedAtAsc(10L)).thenReturn(List.of(answer));

        List<ChatMessageVo> result = chatService.getMessages(1L, 10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSources()).isEmpty();
    }
}
