package com.workmate.web.admin.service.impl;

import com.workmate.web.admin.service.AdminUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * 관리자 사용량 대시보드 WEB 프록시 구현체 — RestClient 로 WAS 사용량 API 를 중계한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUsageServiceImpl implements AdminUsageService {

    private final RestClient wasRestClient;

    @Override
    public ResponseEntity<String> getSummary(String from, String to) {
        return wasRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/admin/usage/summary")
                        .queryParamIfPresent("from", Optional.ofNullable(blankToNull(from)))
                        .queryParamIfPresent("to", Optional.ofNullable(blankToNull(to)))
                        .build())
                .retrieve()
                .toEntity(String.class);
    }

    @Override
    public ResponseEntity<String> getByUser(String from, String to, int page, int size) {
        return wasRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/admin/usage/by-user")
                        .queryParamIfPresent("from", Optional.ofNullable(blankToNull(from)))
                        .queryParamIfPresent("to", Optional.ofNullable(blankToNull(to)))
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .toEntity(String.class);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
