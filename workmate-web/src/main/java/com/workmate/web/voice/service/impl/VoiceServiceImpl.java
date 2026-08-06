package com.workmate.web.voice.service.impl;

import com.workmate.web.voice.service.VoiceService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;

/**
 * 음성 회의록 WEB 프록시 구현체 (F8-1).
 * MultipartFile(오디오)을 다시 multipart/form-data 로 WAS 에 중계한다. (영수증 분석 프록시와 동일 방식)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceServiceImpl implements VoiceService {

    private final RestClient wasRestClient;

    @Override
    public ResponseEntity<String> analyze(MultipartFile file, String title) throws IOException {
        log.info("음성 회의록 분석 프록시 요청. 파일명: {}", file.getOriginalFilename());

        // MultipartBodyBuilder 는 서블릿 스택(WEB)에 없는 reactive-streams 의존성을 요구하므로
        // ByteArrayResource + LinkedMultiValueMap 로 구성한다. (ReceiptServiceImpl 과 동일)
        byte[] bytes = file.getBytes();
        String filename = file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "audio.webm";
        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename; // 없으면 WAS 가 파일명을 인식하지 못한다
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);          // WAS @RequestParam("file") 과 파트명 일치
        if (title != null && !title.isBlank()) {
            body.add("title", title);        // WAS @RequestParam("title")
        }

        return wasRestClient.post()
                .uri("/api/v1/voice/analyze")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toEntity(String.class);
    }

    @Override
    public ResponseEntity<String> history(Integer page, Integer size) {
        return wasRestClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/v1/voice");
                    // page·size 는 있을 때만 붙인다 — 없으면 WAS 가 전체 조회한다
                    if (page != null) {
                        uriBuilder.queryParam("page", page);
                    }
                    if (size != null) {
                        uriBuilder.queryParam("size", size);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .toEntity(String.class);
    }

    @Override
    public ResponseEntity<String> getRecord(Long recordSeq) {
        return wasRestClient.get()
                .uri("/api/v1/voice/{recordSeq}", recordSeq)
                .retrieve()
                .toEntity(String.class);
    }

    @Override
    public ResponseEntity<String> delete(Long recordSeq) {
        log.info("회의록 삭제 프록시 요청. recordSeq: {}", recordSeq);
        return wasRestClient.post()
                .uri("/api/v1/voice/{recordSeq}/delete", recordSeq)
                .retrieve()
                .toEntity(String.class);
    }

    @Override
    public ResponseEntity<String> updateTitle(Long recordSeq, String title) {
        log.info("회의록 제목 수정 프록시 요청. recordSeq: {}", recordSeq);
        // WAS 는 { "title": ... } JSON 바디를 기대한다. 값 검증(공백 불가)은 WAS 가 하고 그 응답을 그대로 중계한다.
        return wasRestClient.post()
                .uri("/api/v1/voice/{recordSeq}/title", recordSeq)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Collections.singletonMap("title", title))
                .retrieve()
                .toEntity(String.class);
    }

    /**
     * {@inheritDoc}
     *
     * <p>WebClient 대신 RestClient.exchange() 를 쓴다. wasWebClient 는 X-User-Seq 자동 주입이
     * 없고(WebClientConfig 주석 참고) 리액티브 스트림 소비가 서블릿 스택에서 번거롭기 때문이다.
     * exchange 콜백 안에서 곧바로 복사하므로 파일 전체가 메모리에 올라가지 않는다.</p>
     */
    @Override
    public void relayAudio(Long recordSeq, String rangeHeader, HttpServletResponse response) {
        wasRestClient.get()
                .uri("/api/v1/voice/{recordSeq}/audio", recordSeq)
                .headers(headers -> {
                    if (rangeHeader != null && !rangeHeader.isBlank()) {
                        headers.set(HttpHeaders.RANGE, rangeHeader);
                    }
                })
                .exchange((request, wasResponse) -> {
                    response.setStatus(wasResponse.getStatusCode().value());
                    copyHeader(wasResponse, response, HttpHeaders.CONTENT_TYPE);
                    copyHeader(wasResponse, response, HttpHeaders.CONTENT_LENGTH);
                    copyHeader(wasResponse, response, HttpHeaders.ACCEPT_RANGES);
                    copyHeader(wasResponse, response, HttpHeaders.CONTENT_RANGE);
                    StreamUtils.copy(wasResponse.getBody(), response.getOutputStream());
                    return null;
                });
        // exchange 는 상태 핸들러를 적용하지 않으므로 4xx·5xx 도 예외 없이 그대로 중계된다.
        // 응답 스트림은 콜백 안에서 전부 소비하며, exchange 가 반환 시 자동으로 닫는다.
    }

    /** WAS 응답 헤더 하나를 브라우저 응답으로 옮긴다 (없으면 아무것도 하지 않음). */
    private void copyHeader(org.springframework.http.client.ClientHttpResponse wasResponse,
                            HttpServletResponse response, String name) {
        String value = wasResponse.getHeaders().getFirst(name);
        if (value != null) {
            response.setHeader(name, value);
        }
    }
}
