-- =============================================================
-- F8-1 확장: 회의록 이력 · 오디오 보관
-- MVP 는 프라이버시 이유로 오디오를 저장하지 않았으나, 이력에서 "올린 파일이 무엇이었는지"
-- 확인·재생할 수 있어야 한다는 요구에 따라 오디오 원본을 보관하도록 방침을 변경했다.
-- 기존 볼륨 환경에 수동 적용:
--   docker exec -i workmate-db psql -U workmate -d workmate_db < db/init/12-voice-history.sql
-- =============================================================

-- 기존 행(MVP 기간 분석분)은 오디오가 없으므로 전부 nullable 이다.
ALTER TABLE voice_record ADD COLUMN IF NOT EXISTS audio_file_name  varchar(200);  -- 서버 저장 파일명(UUID.ext), 경로는 설정값
ALTER TABLE voice_record ADD COLUMN IF NOT EXISTS origin_file_name varchar(255);  -- 사용자가 올린 원본 파일명
ALTER TABLE voice_record ADD COLUMN IF NOT EXISTS file_size        bigint;        -- 파일 크기(바이트)
ALTER TABLE voice_record ADD COLUMN IF NOT EXISTS content_type     varchar(100);  -- 재생 응답의 Content-Type

-- 목록 조회(사용자별 최신순)는 기존 idx_voice_record_user 가 이미 커버한다.
