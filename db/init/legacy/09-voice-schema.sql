-- =============================================================
-- F8 테이블: 음성 회의록 (F8-1)
-- 회의 오디오를 Gemini 로 전사(STT)·구조화 요약한 결과를 보관한다.
-- 프라이버시·용량 문제로 오디오 원본은 저장하지 않고 텍스트(전사문·요약)만 남긴다.
-- 기존 볼륨 환경에 수동 적용:
--   docker exec -i workmate-db psql -U workmate -d workmate_db < db/init/09-voice-schema.sql
-- =============================================================

CREATE TABLE IF NOT EXISTS voice_record (
    record_seq  bigserial    NOT NULL,
    user_seq    bigint       NOT NULL,             -- 작성자(회의록 소유자)
    title       varchar(200) NOT NULL,             -- 회의 제목
    stt_text    text         NOT NULL,             -- STT 전사 원문
    summary_md  text         NOT NULL,             -- AI 구조화 요약(마크다운)
    created_at  timestamptz  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT voice_record_pk PRIMARY KEY (record_seq),
    CONSTRAINT voice_record_user_seq_fk FOREIGN KEY (user_seq) REFERENCES app_user(user_seq)
);

CREATE INDEX IF NOT EXISTS idx_voice_record_user ON voice_record(user_seq, created_at DESC);
