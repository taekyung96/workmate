package com.workmate.was.usage.dao;

import com.workmate.was.usage.vo.LlmUsage;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * LLM 사용량 기록 저장소 (F-OBS).
 * append-only 라 저장만 하고, 집계 조회는 관리자 대시보드(Grafana → PostgreSQL)가 직접 한다.
 */
public interface LlmUsageRepository extends JpaRepository<LlmUsage, Long> {
}
