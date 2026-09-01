package com.workmate.web.usage.service;

import org.springframework.http.ResponseEntity;

/**
 * 본인 사용량 조회 WEB 프록시 서비스.
 *
 * <p>조회 대상 사용자는 여기서 지정하지 않는다 — RestClient 인터셉터가 세션의 로그인 사용자로
 * {@code X-User-Seq} 를 넣고, WAS 가 그 헤더만 보고 본인 것을 집계한다.
 * 사용자 번호를 인자로 받지 않는 것 자체가 남의 사용량 조회를 막는 장치다.</p>
 */
public interface MyUsageService {

    /**
     * 본인 기간 요약을 WAS 에서 받아 그대로 중계한다.
     *
     * @param from 조회 시작일 (yyyy-MM-dd, 빈 값이면 미지정)
     * @param to   조회 종료일 (yyyy-MM-dd, 빈 값이면 미지정)
     * @return WAS 응답 본문(JSON) 그대로
     */
    ResponseEntity<String> getMySummary(String from, String to);
}
