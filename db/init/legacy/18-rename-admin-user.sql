-- =============================================================
-- admin_user → app_user 테이블명 정정
--
-- 왜 바꾸나: 테이블명이 admin_user 였지만 관리자 전용이 아니라 <전체 사용자>를 담는다
--   (일반/관리자 구분은 role 컬럼). 이름과 내용이 어긋나 읽는 사람을 오해시켰다.
-- 왜 user 가 아닌가: user 는 PostgreSQL <예약어>라 CREATE TABLE user 가 문법 오류다.
--   따옴표("user")로 감싸면 되지만 모든 쿼리에서 감싸야 해 관례대로 app_ 접두를 붙였다.
-- 왜 tb_ 접두가 아닌가: 나머지 10개 테이블에 접두사가 없어 하나만 붙이면 규칙이 둘로 갈린다.
--
-- 새 볼륨에서는 03-schema.sql 이 처음부터 app_user 로 만들므로 이 파일은 아무것도 하지 않는다.
-- 기존 볼륨에서만 실제로 이름을 바꾼다. 여러 번 실행해도 안전하다(멱등).
--
-- FK(voice_record·user_social_account)와 컬럼 코멘트는 RENAME 이 그대로 끌고 간다.
-- 주의: admin_audit_log.admin_user_seq 는 <컬럼명>이라 바꾸지 않는다.
--
-- 기존 볼륨에 수동 적용:
--   docker exec -i workmate-db psql -U workmate -d workmate_db < db/init/18-rename-admin-user.sql
-- =============================================================

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'admin_user')
       AND NOT EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'app_user')
    THEN
        ALTER TABLE admin_user RENAME TO app_user;
        RAISE NOTICE 'admin_user -> app_user 로 이름을 바꿨습니다.';
    ELSE
        RAISE NOTICE 'admin_user 가 없거나 app_user 가 이미 있어 건너뜁니다.';
    END IF;
END $$;

-- 제약조건 이름도 규칙(테이블명_컬럼명_제약)에 맞춘다. 존재할 때만 바꾼다
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'admin_user_user_seq_pk') THEN
        ALTER TABLE app_user RENAME CONSTRAINT admin_user_user_seq_pk TO app_user_user_seq_pk;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'admin_user_email_uk') THEN
        ALTER TABLE app_user RENAME CONSTRAINT admin_user_email_uk TO app_user_email_uk;
    END IF;
END $$;
