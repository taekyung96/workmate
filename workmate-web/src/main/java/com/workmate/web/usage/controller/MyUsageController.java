package com.workmate.web.usage.controller;

import com.workmate.web.usage.service.MyUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 본인 사용량 조회 WEB 프록시 컨트롤러 — 화면의 /api/v1/usage/me 요청을 WAS 로 중계한다.
 *
 * <p>관리자 대시보드와 달리 <b>로그인한 모든 사용자</b>가 쓰므로 {@code /api/v1/admin/**} 밖에 둔다.
 * 인증은 SecurityConfig 의 기본 규칙(authenticated)이 담당한다 — 미인증이면 401 이다.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/usage")
@RequiredArgsConstructor
public class MyUsageController {

    private final MyUsageService myUsageService;

    /** 본인 기간 요약 중계 */
    @GetMapping("/me")
    public ResponseEntity<String> getMySummary(
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to) {
        ResponseEntity<String> wasResponse = myUsageService.getMySummary(from, to);
        return ResponseEntity.status(wasResponse.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(wasResponse.getBody());
    }
}
