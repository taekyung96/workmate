package com.workmate.web.guide.service;

import org.springframework.http.ResponseEntity;

/**
 * 가이드 도메인 WEB 프록시 서비스 — /api/v1/guides/** 를 WAS 로 중계한다 (F4).
 * 사용자 식별(X-User-Seq)은 RestClient 인터셉터가 세션에서 자동 주입한다.
 */
public interface GuideService {

    ResponseEntity<String> list(String keyword, int page, int size);

    ResponseEntity<String> detail(Long guideSeq);

    ResponseEntity<String> create(String body);

    ResponseEntity<String> update(Long guideSeq, String body);

    ResponseEntity<String> delete(Long guideSeq);
}
