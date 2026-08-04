package com.workmate.was.receipt.dao;

import com.workmate.was.receipt.vo.Receipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 영수증 엔티티에 대한 JPA 리포지토리 인터페이스.
 */
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    /**
     * 특정 사용자의 영수증 목록을 최신 등록순으로 조회한다. (CSV 내보내기·집계 등 전체가 필요할 때)
     *
     * @param userSeq 사용자 식별자
     * @return 최신 등록순 영수증 목록
     */
    List<Receipt> findByUserSeqOrderByCreatedAtDesc(Long userSeq);

    /**
     * 특정 사용자의 영수증 목록을 페이징 조회한다 (정렬은 Pageable 이 지정).
     *
     * @param userSeq  사용자 식별자
     * @param pageable 페이지·크기·정렬
     * @return 해당 페이지의 영수증
     */
    Page<Receipt> findByUserSeq(Long userSeq, Pageable pageable);

    /**
     * 특정 사용자의 결제일(YYYYMMDD) 범위 내 영수증을 조회한다 (Tool 집계용, F5-01).
     * pay_date 는 YYYYMMDD 문자열이라 문자열 범위 비교가 날짜 비교와 동일하게 동작한다.
     *
     * @param userSeq 사용자 식별자
     * @param start   시작일 (YYYYMMDD, 포함)
     * @param end     종료일 (YYYYMMDD, 포함)
     * @return 해당 기간 영수증 목록
     */
    List<Receipt> findByUserSeqAndPayDateBetween(Long userSeq, String start, String end);
}
