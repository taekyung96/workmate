#!/bin/bash
# =============================================================
# 데모 관리자 계정 승격 (로컬·데모 환경 전용)
#
# 13-seed-demo-data.sql 은 데모 계정 3종을 모두 ROLE_USER 로 넣는다.
# README 에 데모 비밀번호가 공개돼 있어, 공개 배포 인스턴스에서 ROLE_ADMIN 이면
# 누구나 사용자 관리·감사로그에 들어올 수 있기 때문이다.
#
# 그런데 로컬에서는 관리자 화면 확인·README 스크린샷 재현(scripts/capture-all-perfect.js)에
# 관리자 권한이 필요하다. 그래서 승격을 이 파일로 분리하고 환경변수로 켜고 끈다.
#   - docker-compose.yml        (개발·데모) → DEMO_ADMIN_ENABLED=true  → 승격함
#   - docker-compose.deploy.yml (공개 배포) → 값 없음                  → 건너뜀
#
# 19-grafana-readonly.sh 와 같은 이유로 .sh 다 — psql 은 .sql 안에서 env 를 치환하지 못한다.
#
# 기존 볼륨에 수동 적용:
#   docker exec -i -e DEMO_ADMIN_ENABLED=true workmate-db \
#     bash /docker-entrypoint-initdb.d/20-demo-admin-role.sh
#
# 되돌리기(공개 배포 직전 등):
#   docker exec -i workmate-db psql -U workmate -d workmate_db \
#     -c "UPDATE app_user SET role = 'ROLE_USER' WHERE user_name = '관리자 (데모)';"
# =============================================================
set -e

# [주의] 19-grafana-readonly.sh 와 같은 이유로 "exit 0" 을 쓰지 않는다.
# 실행권한 없이 배치되면 엔트리포인트가 source(.) 로 읽어, exit 가 초기화 전체를 끊는다.
if [ "${DEMO_ADMIN_ENABLED}" != "true" ]; then
    echo "[20-demo-admin-role] DEMO_ADMIN_ENABLED 가 true 가 아니라 건너뜁니다. (공개 배포에서는 정상)"
else

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    UPDATE app_user
       SET role = 'ROLE_ADMIN'
     WHERE user_name = '관리자 (데모)'
       AND role <> 'ROLE_ADMIN';
EOSQL

echo "[20-demo-admin-role] '관리자 (데모)' 를 ROLE_ADMIN 으로 승격했습니다. (로컬·데모 전용)"

fi
