package com.workmate.web.guide.service.impl;

import com.workmate.web.guide.service.GuideService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 가이드 도메인 WEB 프록시 구현체 — RestClient 로 WAS 가이드 API 를 중계한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuideServiceImpl implements GuideService {

    private final RestClient wasRestClient;

    @Override
    public ResponseEntity<String> list(String keyword, int page, int size) {
        return wasRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/guides")
                        .queryParamIfPresent("keyword",
                                (keyword == null || keyword.isBlank())
                                        ? java.util.Optional.empty()
                                        : java.util.Optional.of(keyword))
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve().toEntity(String.class);
    }

    @Override
    public ResponseEntity<String> detail(Long guideSeq) {
        return wasRestClient.get().uri("/api/v1/guides/{guideSeq}", guideSeq).retrieve().toEntity(String.class);
    }

    @Override
    public ResponseEntity<String> create(String body) {
        return wasRestClient.post().uri("/api/v1/guides")
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toEntity(String.class);
    }

    @Override
    public ResponseEntity<String> update(Long guideSeq, String body) {
        return wasRestClient.post().uri("/api/v1/guides/{guideSeq}/update", guideSeq)
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toEntity(String.class);
    }

    @Override
    public ResponseEntity<String> delete(Long guideSeq) {
        return wasRestClient.post().uri("/api/v1/guides/{guideSeq}/delete", guideSeq).retrieve().toEntity(String.class);
    }
}
