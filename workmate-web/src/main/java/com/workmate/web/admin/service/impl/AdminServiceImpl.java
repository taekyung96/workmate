package com.workmate.web.admin.service.impl;

import com.workmate.web.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 관리자 도메인 WEB 프록시 구현체 — RestClient 로 WAS 관리자 API 를 중계한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final RestClient wasRestClient;

    @Override
    public ResponseEntity<String> getUsers(String keyword, int page, int size) {
        return wasRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/admin/users")
                        .queryParamIfPresent("keyword",
                                (keyword == null || keyword.isBlank())
                                        ? java.util.Optional.empty()
                                        : java.util.Optional.of(keyword))
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .toEntity(String.class);
    }

    @Override
    public ResponseEntity<String> getAuditLogs(int page, int size) {
        return wasRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/admin/audit-logs")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .toEntity(String.class);
    }

    @Override
    public ResponseEntity<String> unlock(Long userSeq) {
        return wasRestClient.post()
                .uri("/api/v1/admin/users/{userSeq}/unlock", userSeq)
                .retrieve()
                .toEntity(String.class);
    }

    @Override
    public ResponseEntity<String> resetPassword(Long userSeq) {
        return wasRestClient.post()
                .uri("/api/v1/admin/users/{userSeq}/reset-password", userSeq)
                .retrieve()
                .toEntity(String.class);
    }
}
