#!/bin/bash
# =============================================================
# Grafana 전용 읽기 전용 DB 롤 생성 (F-OBS)
#
# 이 디렉토리는 원칙적으로 *.sql 로만 관리하지만, 이 파일만 .sh 다.
# 이유: 롤 비밀번호를 환경변수로 받아야 하는데 psql 은 .sql 파일 안에서 env 를 치환하지 못한다.
#       postgres 엔트리포인트는 initdb 단계에서 .sh 도 실행하므로 여기서만 예외를 둔다.
#
# 왜 읽기 전용인가: 대시보드 쿼리가 데이터를 건드릴 수 없어야 한다(글로벌 규칙).
# 왜 app_user 는 제외인가: 이메일·전화번호가 들어 있다. 대시보드는 user_seq 숫자만 쓴다.
#
# 기존 볼륨에 수동 적용:
#   GRAFANA_DB_PASSWORD=... docker exec -i -e GRAFANA_DB_PASSWORD workmate-db \
#     bash /docker-entrypoint-initdb.d/19-grafana-readonly.sh
# =============================================================
set -e

# [주의] 여기서 "exit 0" 을 쓰면 안 된다. 이 파일이 실행권한 없이 배치되면 postgres
# 엔트리포인트가 실행(subprocess)이 아니라 source(.) 로 읽어서, exit 가 엔트리포인트
# 자체를 끝내 버린다 → 뒤따르는 초기화 스크립트가 통째로 실행되지 않는다.
if [ -z "${GRAFANA_DB_PASSWORD}" ]; then
    echo "[19-grafana-readonly] GRAFANA_DB_PASSWORD 가 없어 건너뜁니다. (관측 스택을 안 쓰면 정상)"
else

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'grafana_ro') THEN
            CREATE ROLE grafana_ro LOGIN PASSWORD '${GRAFANA_DB_PASSWORD}';
        ELSE
            ALTER ROLE grafana_ro LOGIN PASSWORD '${GRAFANA_DB_PASSWORD}';
        END IF;
    END
    \$\$;

    -- 최소 권한: 접속 + 스키마 조회 + llm_usage 읽기까지만
    GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO grafana_ro;
    GRANT USAGE ON SCHEMA public TO grafana_ro;
    GRANT SELECT ON llm_usage TO grafana_ro;

    -- 앞으로 만들 테이블에 자동으로 권한이 붙지 않게 한다(명시적으로만 부여)
    ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM grafana_ro;
EOSQL

echo "[19-grafana-readonly] grafana_ro 롤 준비 완료 (llm_usage 읽기 전용)"

fi
