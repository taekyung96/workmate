package com.workmate.web.chat.service;

import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * 채팅 도메인 WEB 프록시 서비스.
 * WEB 은 DB 에 직접 접근하지 않고 WAS REST API 로 중계만 한다.
 * 조회·삭제는 RestClient(블로킹), 스트리밍은 WebClient(무버퍼 relay)를 쓴다.
 */
public interface ChatService {

    /** 내 채팅방 목록 중계 (C1) */
    ResponseEntity<String> getRooms();

    /** 방 대화 이력 중계 (C2) */
    ResponseEntity<String> getMessages(Long roomSeq);

    /** 방 논리 삭제 중계 (C4) */
    ResponseEntity<String> deleteRoom(Long roomSeq);

    /**
     * 메시지 전송 + AI 응답 SSE 무버퍼 relay (C3, F2-06).
     * 인증 헤더·요청 추적 ID 는 컨트롤러가 요청 스레드에서 읽어 넘긴 값을 그대로 WAS 로 전달한다.
     *
     * @param userSeq     로그인 사용자 식별자 (요청 스레드에서 캡처)
     * @param role        로그인 사용자 권한
     * @param requestId   요청 추적 ID (F-OBS, 요청 스레드에서 MDC 로 캡처 — 없을 수 있음)
     * @param requestBody 브라우저가 보낸 스트리밍 요청 JSON 원문
     * @return WAS 이벤트 스트림을 그대로 통과시키는 Flux
     */
    Flux<ServerSentEvent<String>> stream(Long userSeq, String role, String requestId, String requestBody);
}
