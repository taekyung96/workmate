package com.workmate.web.usage.service.impl;

import com.workmate.web.usage.service.MyUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * 본인 사용량 조회 WEB 프록시 구현체 — RestClient 로 WAS 의 /api/v1/usage/me 를 중계한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MyUsageServiceImpl implements MyUsageService {

    private final RestClient wasRestClient;

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<String> getMySummary(String from, String to) {
        return wasRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/usage/me")
                        .queryParamIfPresent("from", Optional.ofNullable(blankToNull(from)))
                        .queryParamIfPresent("to", Optional.ofNullable(blankToNull(to)))
                        .build())
                .retrieve()
                .toEntity(String.class);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
