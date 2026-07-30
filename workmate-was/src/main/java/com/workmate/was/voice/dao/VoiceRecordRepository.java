package com.workmate.was.voice.dao;

import com.workmate.was.voice.vo.VoiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 음성 회의록 엔티티 JPA 리포지토리 (F8-1).
 */
public interface VoiceRecordRepository extends JpaRepository<VoiceRecord, Long> {
}
