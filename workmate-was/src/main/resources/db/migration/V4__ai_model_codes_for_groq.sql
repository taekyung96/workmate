-- =============================================================
-- AI_MODEL 공통코드 정비 — Groq 모델 추가 · Gemini Pro 제외
--
-- 모델 허용 목록(화이트리스트)은 공통코드가 단일 출처다.
-- application.yml 의 model 값은 "사용자가 안 고를 때 쓸 기본값"일 뿐이고,
-- 실제 허용 여부는 ChatServiceImpl.resolveModel 이 이 표로 검증한다 (F5-05·F9-04).
--
-- Gemini Pro 를 내리는 이유: 무료 티어만 쓰는데 Pro 는 한도가 훨씬 빡빡하고 느리다.
-- 삭제가 아니라 use_yn=false 로 내린다 — 과거 llm_usage 기록의 모델명이 남아 있어
-- 코드를 지우면 이력 해석이 어려워진다.
--
-- Groq 모델명은 2026-09-03 에 https://api.groq.com/openai/v1/models 로 실조회해 확인했다.
-- (Llama 3.x·Mixtral 은 현재 이 계정에서 제공되지 않는다)
-- =============================================================

-- Gemini Pro 제외
UPDATE common_code
   SET use_yn = false
 WHERE group_code = 'AI_MODEL'
   AND code = 'gemini-pro-latest';

-- Groq 모델 추가 (OpenAI 호환 엔드포인트로 호출)
INSERT INTO common_code (group_code, code, code_name, sort_order, use_yn)
VALUES ('AI_MODEL', 'openai/gpt-oss-120b', 'Groq · GPT-OSS 120B', 10, true),
       ('AI_MODEL', 'qwen/qwen3.8-27b',    'Groq · Qwen3.8 27B',  11, true)
ON CONFLICT (group_code, code) DO NOTHING;
