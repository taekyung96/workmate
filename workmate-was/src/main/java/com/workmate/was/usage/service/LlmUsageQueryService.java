package com.workmate.was.usage.service;

import com.workmate.was.usage.vo.UsageSummaryVo;
import com.workmate.was.usage.vo.UserUsagePageVo;

import java.time.LocalDate;

/**
 * LLM 사용량 집계 조회 서비스 (관리자 사용량 대시보드).
 *
 * <p>기록용 {@code LlmUsageService}와 의도적으로 분리했다 — 읽기(집계 조회)와
 * 쓰기(append-only 기록)는 책임이 다르고, 기록 서비스는 실패를 삼키는 정책(REQUIRES_NEW)이라
 * 조회 책임까지 얹으면 그 정책이 조회에도 새어나간다.</p>
 */
public interface LlmUsageQueryService {

    /**
     * 기간 요약 — 합계·기능별·일별 집계를 한 번에 반환한다.
     *
     * @param from 조회 시작일 (null 이면 to 기준 최근 30일의 시작일로 대체)
     * @param to   조회 종료일 (null 이면 오늘)
     * @return 실제 적용된 기간과 집계 결과
     */
    UsageSummaryVo getSummary(LocalDate from, LocalDate to);

    /**
     * 사용자별 집계 — 토큰 합계 내림차순 페이징.
     *
     * @param from 조회 시작일 (null 이면 최근 30일)
     * @param to   조회 종료일 (null 이면 오늘)
     * @param page 0-based 페이지
     * @param size 페이지 크기
     * @return 사용자별 집계 페이지 (이메일은 마스킹된 값)
     */
    UserUsagePageVo getByUser(LocalDate from, LocalDate to, int page, int size);
}
