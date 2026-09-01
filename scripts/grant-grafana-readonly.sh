#!/bin/bash
# =============================================================
# Grafana 읽기 전용 롤(grafana_ro)에 llm_usage SELECT 권한을 준다 (F-OBS)
#
# [왜 필요한가]
# db/init/19-grafana-readonly.sh 는 postgres 엔트리포인트(initdb 단계)에서 돈다 —
# 즉 WAS 가 뜨기 전, Flyway 가 스키마를 만들기 전이다. 그 시점에는 llm_usage 테이블이
# 아직 없어서 `GRANT SELECT ON llm_usage` 를 걸 수 없다(있으면 initdb 자체가 실패한다).
# 그래서 롤 생성까지는 19-grafana-readonly.sh 가 하고, llm_usage 에 대한 실제 SELECT
# 권한은 WAS 가 한 번 떠서(=Flyway 가 V1 을 적용해서) 테이블이 생긴 뒤 이 스크립트가 준다.
#
# [사용법] 저장소 루트에서, DB 컨테이너와 WAS 가 이미 떠 있는 상태로 1회 실행한다.
#   ./scripts/grant-grafana-readonly.sh
#   DB_CONTAINER=workmate-db ./scripts/grant-grafana-readonly.sh
#
# 여러 번 실행해도 안전하다(GRANT 는 멱등). 관측 스택(--profile obs)을 안 쓰면
# grafana_ro 롤 자체가 없으므로 이 스크립트도 건너뛴다.
# =============================================================
set -euo pipefail

ENV_FILE="${ENV_FILE:-.env}"
DB_CONTAINER="${DB_CONTAINER:-workmate-db}"

[ -f "$ENV_FILE" ] || { echo "[오류] $ENV_FILE 이 없다. 저장소 루트에서 실행하라."; exit 1; }

# .env 는 주석·빈 줄이 섞여 있으므로 필요한 값만 뽑는다
get() { grep -E "^$1=" "$ENV_FILE" | head -1 | cut -d= -f2- | tr -d '\r'; }
PG_USER=$(get POSTGRES_USER)
PG_DB=$(get POSTGRES_DB)

for v in PG_USER PG_DB; do
    [ -n "${!v}" ] || { echo "[오류] $ENV_FILE 에 ${v} 에 해당하는 값이 비어 있다."; exit 1; }
done

docker inspect "$DB_CONTAINER" >/dev/null 2>&1 \
    || { echo "[오류] DB 컨테이너 '$DB_CONTAINER' 를 찾을 수 없다. DB_CONTAINER 로 지정하라."; exit 1; }

# grafana_ro 롤이 없으면(관측 스택을 안 켠 경우) 조용히 건너뛴다
ROLE_EXISTS=$(docker exec -i "$DB_CONTAINER" psql -tA -U "$PG_USER" -d "$PG_DB" \
    -c "SELECT 1 FROM pg_roles WHERE rolname = 'grafana_ro';")
if [ "$ROLE_EXISTS" != "1" ]; then
    echo "[grant-grafana-readonly] grafana_ro 롤이 없어 건너뜁니다. (GRAFANA_DB_PASSWORD 없이 뜬 경우 정상)"
    exit 0
fi

# llm_usage 가 아직 없으면(Flyway 미적용 = WAS 미기동) 명확히 안내하고 종료한다
TABLE_EXISTS=$(docker exec -i "$DB_CONTAINER" psql -tA -U "$PG_USER" -d "$PG_DB" \
    -c "SELECT 1 FROM information_schema.tables WHERE table_name = 'llm_usage';")
if [ "$TABLE_EXISTS" != "1" ]; then
    echo "[오류] llm_usage 테이블이 없다. WAS 를 먼저 띄워 Flyway 마이그레이션이 끝난 뒤 다시 실행하라."
    exit 1
fi

docker exec -i "$DB_CONTAINER" psql -v ON_ERROR_STOP=1 -U "$PG_USER" -d "$PG_DB" \
    -c "GRANT SELECT ON llm_usage TO grafana_ro;"

echo "[grant-grafana-readonly] grafana_ro 에 llm_usage SELECT 권한을 부여했다."
