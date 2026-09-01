-- =============================================================
-- F4-07 보완: RAG 출처를 대화 이력에 보존
--
-- 스트리밍 중에는 SSE source 이벤트로 출처를 내려주지만 저장하지 않아,
-- 다른 방에 갔다 돌아오면 출처 뱃지가 사라졌다. assistant 메시지에 함께 남긴다.
--
-- 기존 행(이 변경 이전 대화)은 출처 정보 자체가 없어 NULL 로 남는다.
--
-- 기존 볼륨 환경에 수동 적용:
--   docker exec -i workmate-db psql -U workmate -d workmate_db < db/init/15-chat-message-sources.sql
-- =============================================================

-- [{"guideSeq": 32, "title": "..."}] 형태. RAG 를 쓰지 않은 답변과 사용자 메시지는 NULL 이다.
ALTER TABLE chat_message ADD COLUMN IF NOT EXISTS sources jsonb;
