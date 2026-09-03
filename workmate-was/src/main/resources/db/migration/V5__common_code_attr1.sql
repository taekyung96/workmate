-- =============================================================
-- common_code 에 부가 속성 컬럼(attr1) 추가
--
-- 배경: AI_MODEL 그룹이 여러 제공자(Gemini·Groq)의 모델을 함께 담게 되면서
-- "이 모델은 어느 제공자인가"를 어딘가에 적어야 한다. 그 값이 없으면
-- 제공자 전환에 서버 재시작이 필요하고, AI_MODEL 드롭다운이 반쪽이 된다.
--
-- 공통코드에 두는 이유: 모델 목록의 단일 출처를 공통코드로 유지하기 위해서다.
-- 설정 파일에 따로 매핑을 두면 모델 하나 추가할 때 DB 와 설정 두 곳을 고쳐야 하고,
-- 운영 중 추가하려면 재배포까지 필요해진다.
--
-- 다른 그룹에서는 NULL 이다 — 그룹마다 의미가 다른 범용 칸이므로 NOT NULL 로 두지 않는다.
-- =============================================================

ALTER TABLE common_code ADD COLUMN attr1 varchar(100);

COMMENT ON COLUMN common_code.attr1 IS
    '그룹별 부가 속성(의미는 그룹마다 다름) — AI_MODEL 에서는 LLM 제공자: google-genai(Gemini) | openai(Groq, OpenAI 호환)';

-- AI_MODEL 제공자 채우기
UPDATE common_code SET attr1 = 'google-genai'
 WHERE group_code = 'AI_MODEL' AND code LIKE 'gemini-%';

UPDATE common_code SET attr1 = 'openai'
 WHERE group_code = 'AI_MODEL' AND code IN ('openai/gpt-oss-120b', 'qwen/qwen3.8-27b');
