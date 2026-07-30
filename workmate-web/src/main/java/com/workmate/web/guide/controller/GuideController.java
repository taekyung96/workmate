package com.workmate.web.guide.controller;

import com.workmate.web.guide.service.GuideService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 가이드 도메인 WEB 프록시 컨트롤러 — 화면(fetch)의 /api/v1/guides/** 를 WAS 로 중계한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/guides")
@RequiredArgsConstructor
public class GuideController {

    private final GuideService guideService;

    @GetMapping
    public ResponseEntity<String> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "12") int size) {
        return jsonPassthrough(guideService.list(keyword, page, size));
    }

    @GetMapping("/{guideSeq}")
    public ResponseEntity<String> detail(@PathVariable("guideSeq") Long guideSeq) {
        return jsonPassthrough(guideService.detail(guideSeq));
    }

    @PostMapping
    public ResponseEntity<String> create(@RequestBody String body) {
        return jsonPassthrough(guideService.create(body));
    }

    @PostMapping("/{guideSeq}/update")
    public ResponseEntity<String> update(@PathVariable("guideSeq") Long guideSeq, @RequestBody String body) {
        return jsonPassthrough(guideService.update(guideSeq, body));
    }

    @PostMapping("/{guideSeq}/delete")
    public ResponseEntity<String> delete(@PathVariable("guideSeq") Long guideSeq) {
        return jsonPassthrough(guideService.delete(guideSeq));
    }

    private ResponseEntity<String> jsonPassthrough(ResponseEntity<String> wasResponse) {
        return ResponseEntity.status(wasResponse.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(wasResponse.getBody());
    }
}
