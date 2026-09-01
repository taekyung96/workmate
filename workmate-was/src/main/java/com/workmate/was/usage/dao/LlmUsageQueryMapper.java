package com.workmate.was.usage.dao;

import com.workmate.was.usage.dao.row.DailyAggregateRow;
import com.workmate.was.usage.dao.row.FeatureAggregateRow;
import com.workmate.was.usage.dao.row.ModelAggregateRow;
import com.workmate.was.usage.dao.row.TotalAggregateRow;
import com.workmate.was.usage.dao.row.UserAggregateRow;
import com.workmate.was.usage.dao.row.UserModelAggregateRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * LLM 사용량 집계 조회 매퍼 (관리자 사용량 대시보드).
 *
 * <p>기록용 {@code LlmUsageRepository}(JPA)와 책임을 분리한 읽기 전용 집계 전용 매퍼다.
 * 동적 기간·GROUP BY 는 JPQL 보다 SQL 이 읽기 쉬워 MyBatis 를 쓴다(계획서 §6-3).
 * 모든 조회는 {@code created_at >= from AND created_at < toExclusive} 로 기간을 받는다
 * (to 날짜를 포함하려고 서비스 계층에서 to+1일을 toExclusive 로 넘긴다).</p>
 */
@Mapper
public interface LlmUsageQueryMapper {

    /**
     * 기간 전체 합계 1건 (호출 0건이어도 COUNT(*)=0 인 행이 반환된다).
     *
     * @param userSeq 특정 사용자로 한정할 때의 사용자 번호. null 이면 전체 집계(관리자용)
     */
    TotalAggregateRow selectTotal(
            @Param("from") LocalDate from,
            @Param("toExclusive") LocalDate toExclusive,
            @Param("userSeq") Long userSeq);

    /** 기능(feature)별 집계 — 호출이 있는 기능만 반환(0건 기능은 서비스 계층에서 채운다). userSeq null 이면 전체 */
    List<FeatureAggregateRow> selectByFeature(
            @Param("from") LocalDate from,
            @Param("toExclusive") LocalDate toExclusive,
            @Param("userSeq") Long userSeq);

    /** 날짜별 집계 — 데이터 있는 날짜만 반환(빈 날짜는 서비스 계층에서 0으로 채운다). userSeq null 이면 전체 */
    List<DailyAggregateRow> selectDaily(
            @Param("from") LocalDate from,
            @Param("toExclusive") LocalDate toExclusive,
            @Param("userSeq") Long userSeq);

    /** 모델명별 집계(기간 전체) — 총 추정 비용·단가 미등록 건수 계산용. userSeq null 이면 전체 */
    List<ModelAggregateRow> selectModelUsageTotal(
            @Param("from") LocalDate from,
            @Param("toExclusive") LocalDate toExclusive,
            @Param("userSeq") Long userSeq);

    /** 사용자별 집계 한 페이지 — 토큰 합계(입력+출력) 내림차순, offset/limit 페이징 */
    List<UserAggregateRow> selectUserPage(
            @Param("from") LocalDate from,
            @Param("toExclusive") LocalDate toExclusive,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /** 기간 내 사용량을 남긴 서로 다른 사용자 수 — by-user 페이징 totalElements 산출용 */
    long countDistinctUsers(@Param("from") LocalDate from, @Param("toExclusive") LocalDate toExclusive);

    /** 지정한 사용자들의 (user_seq, model_name)별 집계 — by-user 페이지의 사용자별 추정 비용 계산용 */
    List<UserModelAggregateRow> selectModelUsageByUsers(
            @Param("userSeqs") List<Long> userSeqs,
            @Param("from") LocalDate from,
            @Param("toExclusive") LocalDate toExclusive);
}
