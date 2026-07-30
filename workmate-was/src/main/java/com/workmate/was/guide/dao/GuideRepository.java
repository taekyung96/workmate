package com.workmate.was.guide.dao;

import com.workmate.was.guide.vo.Guide;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

/**
 * 가이드 문서 엔티티에 대한 JPA 리포지토리 인터페이스.
 */
public interface GuideRepository extends JpaRepository<Guide, Long> {

    /**
     * 특정 작성자가 등록한 가이드 문서 리스트를 최신순으로 조회한다.
     *
     * @param userSeq 사용자 식별자
     * @return 가이드 문서 목록
     */
    List<Guide> findByUserSeqOrderByCreatedAtDesc(Long userSeq);

    /**
     * 공개 상태인 가이드 문서 리스트를 최신순으로 조회한다.
     *
     * @param isPublic 공개 여부 (true)
     * @return 공개 가이드 문서 목록
     */
    List<Guide> findByIsPublicOrderByCreatedAtDesc(boolean isPublic);

    /**
     * 접근 가능한 가이드(본인 문서 + 공개 문서)를 키워드로 검색해 페이징 조회한다 (G1).
     * 단일 OR 조건으로 본인·공개를 한 번에 조회하므로 별도 병합·중복제거가 필요 없다.
     * keyword 가 빈 문자열("")이면 전체(접근 가능분)를 반환하고, 있으면 제목·본문 부분일치(대소문자 무시)로 거른다.
     * 정렬은 Pageable 의 Sort 를 따른다.
     *
     * <p>주의: keyword 는 null 이 아닌 빈 문자열로 넘겨야 한다. null 을 바인딩하면 PostgreSQL 이
     * 파라미터 타입을 추론하지 못해 bytea 로 취급 → {@code lower(bytea)} 오류가 난다.
     *
     * @param userSeq  요청 사용자 (본인 문서 판별용)
     * @param keyword  검색어 (빈 문자열이면 전체)
     * @param pageable 페이징·정렬 정보
     * @return 접근 가능 가이드 페이지
     */
    @Query("SELECT g FROM Guide g "
            + "WHERE (g.userSeq = :userSeq OR g.isPublic = true) "
            + "AND (:keyword = '' "
            + "     OR LOWER(g.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "     OR LOWER(g.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Guide> searchAccessible(@Param("userSeq") Long userSeq,
                                 @Param("keyword") String keyword,
                                 Pageable pageable);
}
