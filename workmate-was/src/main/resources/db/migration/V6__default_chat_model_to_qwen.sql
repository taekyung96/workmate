-- 채팅 화면의 기본 모델을 Groq Qwen3.8 27B 로 바꾼다.
--
-- [왜 sort_order 인가]
-- 프론트(chat.store.ts loadModels)는 AI_MODEL 공통코드를 sort_order 순으로 받아 첫 번째를
-- 기본 선택값으로 쓴다. 즉 '화면에서 아무것도 안 골랐을 때 무엇이 쓰이나'는 이 sort_order 가 정한다.
-- (서버 쪽 기본값 — modelCode 없이 들어온 요청과 도우미가 쓰는 모델 — 은 LLM_CHAT_MODEL 환경변수와
--  ChatModelResolver 가 따로 정한다. 둘은 다른 기본값이라 함께 맞춰야 한다.)
--
-- [왜 Gemini 를 1번에서 내리나]
--   1. Gemini 무료 티어가 generate_content_free_tier_requests 한도 20건에서 429 를 돌려준다.
--      실사용 중 자주 막혀 첫 화면 기본값으로 두기 어렵다.
--   2. Groq gpt-oss-120b 는 도구 호출이 필요한 질문("내 사용량 얼마나 썼어?")에서 예외 없이
--      빈 응답을 돌려준다 — 같은 질문을 qwen3.8-27b 로 보내면 도구가 정상 호출된다(재현 확인).
--   3. qwen3.8-27b 는 도구 호출이 동작하고 Groq 무료 한도가 Gemini 보다 넉넉하다.
--
-- 모델을 지우거나 끄지는 않는다 — 사용자가 드롭다운에서 여전히 고를 수 있어야 한다.
-- 순서만 바꾼다.
UPDATE common_code SET sort_order = 1 WHERE group_code = 'AI_MODEL' AND code = 'qwen/qwen3.8-27b';
UPDATE common_code SET sort_order = 2 WHERE group_code = 'AI_MODEL' AND code = 'gemini-flash-latest';
UPDATE common_code SET sort_order = 3 WHERE group_code = 'AI_MODEL' AND code = 'openai/gpt-oss-120b';
UPDATE common_code SET sort_order = 4 WHERE group_code = 'AI_MODEL' AND code = 'gemini-pro-latest';
