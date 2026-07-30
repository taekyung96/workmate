package com.workmate.web.voice.service.impl;

import com.workmate.web.voice.service.VoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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
}
