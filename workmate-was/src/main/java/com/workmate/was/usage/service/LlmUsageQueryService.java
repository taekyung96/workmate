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
     * 특정 사용자 본인의 기간 요약.
     *
     * <p><b>userSeq 는 반드시 세션에서 유래한 값이어야 한다.</b> WEB 이 RestClient 인터셉터로
     * {@code X-User-Seq} 를 세션 값으로 덮어써서 넘긴다. 요청 파라미터로 받은 값을 그대로
     * 넘기면 남의 사용량을 조회할 수 있게 되므로, 컨트롤러에서 헤더 외의 경로로 받지 않는다.</p>
     *
     * @param userSeq 조회 대상 사용자 (null 불가 — null 이면 전체 집계가 되어 정보가 샌다)
     * @param from    조회 시작일 (null 이면 최근 30일)
     * @param to      조회 종료일 (null 이면 오늘)
     * @return 그 사용자만의 집계 결과
     */
    UsageSummaryVo getMySummary(Long userSeq, LocalDate from, LocalDate to);

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
