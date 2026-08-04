package com.workmate.web.receipt.service.impl;

import com.workmate.web.receipt.service.ReceiptService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

/**
 * 영수증 도메인 WEB 프록시 서비스 구현체.
 * WEB 레이어는 DB에 직접 접근하지 않으므로, 모든 요청을 RestClient 로 WAS REST API 에 중계한다.
 * WAS 의 ApiResponse JSON 본문과 상태코드를 가공 없이 그대로 화면에 전달한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private final RestClient wasRestClient;

    @Override
    public ResponseEntity<String> analyze(MultipartFile file) throws IOException {
        log.info("영수증 분석 프록시 요청. 파일명: {}", file.getOriginalFilename());

        // MultipartFile 을 WAS 로 다시 multipart/form-data 로 전달하기 위한 바디 구성.
        // MultipartBodyBuilder 는 org.reactivestreams.Publisher 를 참조해 서블릿 스택(WEB)에는
        // 없는 reactive-streams 의존성을 요구하므로, ByteArrayResource + LinkedMultiValueMap 로 구성한다.
        byte[] bytes = file.getBytes();
        String filename = file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "receipt.jpg";
        // 익명 클래스로 getFilename() 을 오버라이드 — 이것이 없으면 WAS 가 파일명을 인식하지 못한다
        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource); // WAS @RequestParam("file") 과 파트명 일치

        return wasRestClient.post()
                .uri("/api/v1/receipts/analyze")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toEntity(String.class);
    }

    @Override
    public ResponseEntity<String> save(String requestBody) {
        log.info("영수증 저장 프록시 요청");
        return wasRestClient.post()
                .uri("/api/v1/receipts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .toEntity(String.class);
    }

    @Override
    public ResponseEntity<String> getHistory(Integer page, Integer size) {
        return wasRestClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/v1/receipts");
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
    public ResponseEntity<byte[]> downloadCsv() {
        ResponseEntity<byte[]> wasResponse = wasRestClient.get()
                .uri("/api/v1/receipts/csv")
                .retrieve()
                .toEntity(byte[].class);

        // 파일 다운로드 헤더(Content-Disposition)를 유지한 채 그대로 전달
        return ResponseEntity.status(wasResponse.getStatusCode())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        wasResponse.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(wasResponse.getBody());
    }
}
