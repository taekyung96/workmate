package com.workmate.was.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workmate.was.chat.dao.ChatMessageRepository;
import com.workmate.was.chat.dao.ChatRoomRepository;
import com.workmate.was.chat.service.ChatRateLimiter;
import com.workmate.was.chat.service.ChatStreamClient;
import com.workmate.was.chat.vo.ChatStreamRequestVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ChatServiceImpl.streamChat 의 SSE 이벤트 조립 단위 테스트.
 * 실제 Gemini 호출 없이 ChatStreamClient 를 목으로 대체해 meta/token/done/error 순서를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceImplStreamTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatStreamClient chatStreamClient;
    @Mock private ChatMessagePersister persister;
    @Mock private ChatRateLimiter rateLimiter;
    @Mock private com.workmate.was.guide.service.GuideRetriever guideRetriever;
    @Mock private com.workmate.was.common.service.CommonCodeService commonCodeService;

    private ChatServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ChatServiceImpl(chatRoomRepository, chatMessageRepository,
                chatStreamClient, persister, rateLimiter, new ObjectMapper(), guideRetriever, commonCodeService);
        ReflectionTestUtils.setField(service, "modelName", "gemini-2.5-flash");
    }

    private ChatStreamRequestVo request(Long roomSeq, String message) {
        ChatStreamRequestVo req = new ChatStreamRequestVo();
        req.setRoomSeq(roomSeq);
        req.setMessage(message);
        return req;
    }

    private List<ServerSentEvent<String>> collect(Flux<ServerSentEvent<String>> flux) {
        return flux.collectList().block();
    }

    @Test
    @DisplayName("신규 방: meta → token* → done 순서로 이벤트를 흘리고 assistant 를 저장한다")
    void streams_meta_tokens_done_for_new_room() {
        when(persister.prepare(eq(1L), any())).thenReturn(
                new PreparedChat(12L, "지난달 영수증 총액?", true, List.of()));
        when(chatStreamClient.stream(any(), any(), anyString(), anyList(), eq("지난달 영수증 총액?"), any(), any()))
                .thenReturn(Flux.just("6", "월"));
        when(persister.saveAssistant(eq(12L), eq("6월"), eq("gemini-2.5-flash"), anyList())).thenReturn(45L);

        List<ServerSentEvent<String>> events = collect(service.streamChat(1L, request(null, "지난달 영수증 총액?")));

        assertThat(events).hasSize(4);
        assertThat(events.get(0).event()).isEqualTo("meta");
        assertThat(events.get(0).data()).contains("\"roomSeq\":12").contains("지난달 영수증 총액?");
        assertThat(events.get(1).event()).isEqualTo("token");
        assertThat(events.get(1).data()).isEqualTo("{\"delta\":\"6\"}");
        assertThat(events.get(2).data()).isEqualTo("{\"delta\":\"월\"}");
        assertThat(events.get(3).event()).isEqualTo("done");
        assertThat(events.get(3).data()).contains("\"messageSeq\":45");
    }

    @Test
    @DisplayName("기존 방: meta 이벤트 없이 token → done 만 흘린다")
    void streams_without_meta_for_existing_room() {
        when(persister.prepare(eq(1L), any())).thenReturn(
                new PreparedChat(12L, "기존방", false, List.of()));
        when(chatStreamClient.stream(any(), any(), anyString(), anyList(), anyString(), any(), any()))
                .thenReturn(Flux.just("안녕"));
        when(persister.saveAssistant(eq(12L), eq("안녕"), anyString(), anyList())).thenReturn(46L);

        List<ServerSentEvent<String>> events = collect(service.streamChat(1L, request(12L, "hi")));

        assertThat(events).hasSize(2);
        assertThat(events.get(0).event()).isEqualTo("token");
        assertThat(events.get(1).event()).isEqualTo("done");
    }

    @Test
    @DisplayName("스트림 중 오류: error 이벤트로 대체하고 assistant 는 저장하지 않는다")
    void emits_error_event_on_failure() {
        when(persister.prepare(eq(1L), any())).thenReturn(
                new PreparedChat(12L, "기존방", false, List.of()));
        when(chatStreamClient.stream(any(), any(), anyString(), anyList(), anyString(), any(), any()))
                .thenReturn(Flux.error(new RuntimeException("gemini down")));

        List<ServerSentEvent<String>> events = collect(service.streamChat(1L, request(12L, "hi")));

        assertThat(events).hasSize(1);
        assertThat(events.get(0).event()).isEqualTo("error");
        assertThat(events.get(0).data()).contains("응답이 중단되었습니다");
        verify(persister, never()).saveAssistant(any(), any(), any(), any());
    }

    @Test
    @DisplayName("빈 메시지: 스트림 진입 전 IllegalArgumentException, 방 준비도 안 함")
    void rejects_blank_message() {
        assertThatThrownBy(() -> service.streamChat(1L, request(null, "   ")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(persister, never()).prepare(any(), any());
    }
}
