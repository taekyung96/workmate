package com.workmate.was.voice.dao;

import com.workmate.was.voice.vo.VoiceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 음성 회의록 엔티티 JPA 리포지토리 (F8-1).
 */
public interface VoiceRecordRepository extends JpaRepository<VoiceRecord, Long> {

    /**
     * 사용자의 회의록을 최신순으로 조회한다 (idx_voice_record_user 인덱스 사용).
     *
     * @param userSeq 사용자 식별자
     * @return 최신순 회의록 목록
     */
    List<VoiceRecord> findByUserSeqOrderByCreatedAtDesc(Long userSeq);

    /**
     * 사용자의 회의록을 페이징 조회한다 (정렬은 Pageable 이 지정).
     *
     * @param userSeq  사용자 식별자
     * @param pageable 페이지·크기·정렬
     * @return 해당 페이지의 회의록
     */
    Page<VoiceRecord> findByUserSeq(Long userSeq, Pageable pageable);
}
