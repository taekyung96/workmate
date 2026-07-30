-- =============================================================
-- AI_MODEL 공통코드 갱신 — 폐기 모델 교체
-- gemini-2.5-flash / gemini-2.5-pro 가 신규 사용자에게 404(no longer available) 되어
-- 항상 최신을 가리키는 별칭 모델(gemini-flash-latest / gemini-pro-latest)로 교체한다.
-- 기존 볼륨 환경에 수동 적용:
--   docker exec -i workmate-db psql -U workmate -d workmate_db < db/init/10-update-model-code.sql
-- =============================================================

-- 폐기된 모델 코드 제거 (chat_message.model_name 은 단순 문자열 기록이라 FK 영향 없음)
DELETE FROM common_code WHERE group_code = 'AI_MODEL' AND code IN ('gemini-2.5-flash', 'gemini-2.5-pro');

-- 사용 가능한 최신 별칭 모델 등록
INSERT INTO common_code (group_code, code, code_name, sort_order, use_yn) VALUES
    ('AI_MODEL', 'gemini-flash-latest', 'Gemini Flash (latest)', 1, true),
    ('AI_MODEL', 'gemini-pro-latest',   'Gemini Pro (latest)',   2, true)
    ON CONFLICT (group_code, code)
    DO UPDATE SET code_name = EXCLUDED.code_name, sort_order = EXCLUDED.sort_order, use_yn = true;
