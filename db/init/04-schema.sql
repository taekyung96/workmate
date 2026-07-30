-- =============================================================
-- 마일스톤 4 테이블: 공통코드 (04 §4.5, F7)
-- 이미 볼륨이 있는 기존 환경에 수동 적용:
--   docker exec -i workmate-db psql -U workmate -d workmate_db < db/init/04-schema.sql
-- =============================================================

CREATE TABLE IF NOT EXISTS common_code_group (
    group_code   varchar(30)  NOT NULL,
    group_name   varchar(100) NOT NULL,
    description  varchar(255),
    use_yn       boolean      NOT NULL DEFAULT true,
    CONSTRAINT common_code_group_pk PRIMARY KEY (group_code)
);

CREATE TABLE IF NOT EXISTS common_code (
    group_code  varchar(30)  NOT NULL,
    code        varchar(50)  NOT NULL,
    code_name   varchar(100) NOT NULL,
    sort_order  integer      NOT NULL DEFAULT 0,
    use_yn      boolean      NOT NULL DEFAULT true,
    CONSTRAINT common_code_pk PRIMARY KEY (group_code, code),
    CONSTRAINT common_code_group_code_fk FOREIGN KEY (group_code) REFERENCES common_code_group(group_code)
);

-- 초기 데이터: AI 답변 모델 (F7-03·04, F5-05)
-- gemini-2.5-* 는 신규 사용자에게 404(폐기)되어, 항상 최신을 가리키는 별칭 모델을 사용한다.
INSERT INTO common_code_group (group_code, group_name) VALUES ('AI_MODEL', 'AI 답변 모델')
    ON CONFLICT (group_code) DO NOTHING;
INSERT INTO common_code (group_code, code, code_name, sort_order) VALUES
    ('AI_MODEL', 'gemini-flash-latest', 'Gemini Flash (latest)', 1),
    ('AI_MODEL', 'gemini-pro-latest',   'Gemini Pro (latest)',   2)
    ON CONFLICT (group_code, code) DO NOTHING;
