package com.workmate.web.common.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * 공통코드 관리 WEB 프록시 컨트롤러 (F7) — 화면의 /api/v1/admin/common-codes/** 를 WAS 로 중계한다.
 * 접근 제어(ROLE_ADMIN)는 SecurityConfig 의 /api/v1/admin/** 규칙이 담당한다.
 */
@RestController
@RequestMapping("/api/v1/admin/common-codes")
@RequiredArgsConstructor
public class CommonCodeAdminController {

    private final RestClient wasRestClient;

    @GetMapping("/groups")
    public ResponseEntity<String> getGroups() {
        return pass(wasRestClient.get().uri("/api/v1/admin/common-codes/groups")
                .retrieve().toEntity(String.class));
    }

    @PostMapping("/groups")
    public ResponseEntity<String> createGroup(@RequestBody String body) {
        return pass(wasRestClient.post().uri("/api/v1/admin/common-codes/groups")
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().toEntity(String.class));
    }

    @PutMapping("/groups/{groupCode}")
    public ResponseEntity<String> updateGroup(@PathVariable("groupCode") String groupCode, @RequestBody String body) {
        return pass(wasRestClient.put().uri("/api/v1/admin/common-codes/groups/{groupCode}", groupCode)
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().toEntity(String.class));
    }

    @DeleteMapping("/groups/{groupCode}")
    public ResponseEntity<String> deleteGroup(@PathVariable("groupCode") String groupCode) {
        return pass(wasRestClient.delete().uri("/api/v1/admin/common-codes/groups/{groupCode}", groupCode)
                .retrieve().toEntity(String.class));
    }

    @GetMapping("/groups/{groupCode}/codes")
    public ResponseEntity<String> getCodes(@PathVariable("groupCode") String groupCode) {
        return pass(wasRestClient.get().uri("/api/v1/admin/common-codes/groups/{groupCode}/codes", groupCode)
                .retrieve().toEntity(String.class));
    }

    @PostMapping("/groups/{groupCode}/codes")
    public ResponseEntity<String> createCode(@PathVariable("groupCode") String groupCode, @RequestBody String body) {
        return pass(wasRestClient.post().uri("/api/v1/admin/common-codes/groups/{groupCode}/codes", groupCode)
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().toEntity(String.class));
    }

    @PutMapping("/groups/{groupCode}/codes/{code}")
    public ResponseEntity<String> updateCode(
            @PathVariable("groupCode") String groupCode, @PathVariable("code") String code, @RequestBody String body) {
        return pass(wasRestClient.put().uri("/api/v1/admin/common-codes/groups/{groupCode}/codes/{code}", groupCode, code)
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().toEntity(String.class));
    }

    @DeleteMapping("/groups/{groupCode}/codes/{code}")
    public ResponseEntity<String> deleteCode(
            @PathVariable("groupCode") String groupCode, @PathVariable("code") String code) {
        return pass(wasRestClient.delete().uri("/api/v1/admin/common-codes/groups/{groupCode}/codes/{code}", groupCode, code)
                .retrieve().toEntity(String.class));
    }

    /** WAS 응답의 상태·본문을 JSON 으로 그대로 전달 */
    private ResponseEntity<String> pass(ResponseEntity<String> was) {
        return ResponseEntity.status(was.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(was.getBody());
    }
}
