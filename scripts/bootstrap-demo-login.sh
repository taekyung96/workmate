#!/bin/bash
# =============================================================
# 배포 후 1회 실행 — 데모 계정 로그인 활성화 (F1-1 / 배포 가이드 §2)
#
# [왜 필요한가]
# db/init/13-seed-demo-data.sql 의 email·phone 은 <개발 환경 .env 의 AES 키>로
# 암호화된 암호문이다. AesCipher 는 고정 IV 의 결정적 암호화이고 로그인은
# UserRepository.findByEmail 이 암호문끼리 비교하므로, 배포 서버에서 새 AES 키를
# 쓰면 시드된 데모 계정은 조회되지 않아 로그인이 실패한다.
#   → 이 스크립트가 데모 계정의 email·phone 을 <이 배포의 AES 키>로 다시 암호화한다.
# 비밀번호는 BCrypt(단방향, 키와 무관)라 그대로 쓰면 된다.
#
# [무엇을 하지 않는가]
# 권한은 건드리지 않는다. 데모 계정은 ROLE_USER 로 남는다
# (README 에 비밀번호가 공개돼 있어 공개 인스턴스에서 관리자면 안 된다).
# 관리자 화면 시연이 필요하면 --grant-admin 을 붙인다. 공개 배포에는 쓰지 말 것.
#
# [사용법] 저장소 루트에서, 스택이 떠 있는 상태로 실행한다.
#   ./scripts/bootstrap-demo-login.sh
#   ./scripts/bootstrap-demo-login.sh --grant-admin     # 비공개 시연용
#   DB_CONTAINER=workmate-db ./scripts/bootstrap-demo-login.sh
#
# 여러 번 실행해도 안전하다(멱등).
# =============================================================
set -euo pipefail

GRANT_ADMIN=false
[ "${1:-}" = "--grant-admin" ] && GRANT_ADMIN=true

ENV_FILE="${ENV_FILE:-.env}"
DB_CONTAINER="${DB_CONTAINER:-workmate-db}"

[ -f "$ENV_FILE" ] || { echo "[오류] $ENV_FILE 이 없다. 저장소 루트에서 실행하라."; exit 1; }

# .env 는 주석·빈 줄이 섞여 있으므로 필요한 값만 뽑는다
get() { grep -E "^$1=" "$ENV_FILE" | head -1 | cut -d= -f2- | tr -d '\r'; }
AES_KEY=$(get AES_SECRET_KEY)
AES_IV=$(get AES_SECRET_IV)
PG_USER=$(get POSTGRES_USER)
PG_DB=$(get POSTGRES_DB)

for v in AES_KEY AES_IV PG_USER PG_DB; do
    [ -n "${!v}" ] || { echo "[오류] $ENV_FILE 에 ${v} 에 해당하는 값이 비어 있다."; exit 1; }
done

docker inspect "$DB_CONTAINER" >/dev/null 2>&1 \
    || { echo "[오류] DB 컨테이너 '$DB_CONTAINER' 를 찾을 수 없다. DB_CONTAINER 로 지정하라."; exit 1; }

# AesCipher 와 동일: AES-256-CBC / PKCS5 패딩 / 고정 IV / Base64
KEY_HEX=$(printf '%s' "$AES_KEY" | base64 -d | xxd -p -c 64)
IV_HEX=$(printf '%s' "$AES_IV" | base64 -d | xxd -p -c 64)
enc() { printf '%s' "$1" | openssl enc -aes-256-cbc -K "$KEY_HEX" -iv "$IV_HEX" -base64 -A; }

# 데모 계정의 평문 — 13-seed-demo-data.sql 이 넣는 세 계정과 같다.
# user_name 은 암호화 대상이 아니라서 키가 달라도 그대로 매칭된다.
NAMES=('관리자 (데모)'      '홍길동 (데모)'   '김서연 (데모)')
MAILS=('demo.admin@example.com' 'hong@example.com' 'kim@example.com')
PHONES=('01000000001'          '01000000002'     '01000000003')

SQL=""
for i in 0 1 2; do
    SQL+="UPDATE app_user SET email = '$(enc "${MAILS[$i]}")', phone = '$(enc "${PHONES[$i]}")' WHERE user_name = '${NAMES[$i]}';
"
done

if [ "$GRANT_ADMIN" = true ]; then
    SQL+="UPDATE app_user SET role = 'ROLE_ADMIN' WHERE user_name = '관리자 (데모)';
"
fi

SQL+="SELECT user_name, role FROM app_user ORDER BY user_seq;"

echo "[bootstrap-demo-login] '$DB_CONTAINER' 에 적용한다 (AES 키: $ENV_FILE)"
printf '%s\n' "$SQL" | docker exec -i "$DB_CONTAINER" psql -v ON_ERROR_STOP=1 -U "$PG_USER" -d "$PG_DB"

echo
echo "[bootstrap-demo-login] 완료. 아래 계정으로 로그인된다."
echo "    demo.admin@example.com / Workmate!2026"
echo "    hong@example.com       / Workmate!2026"
echo "    kim@example.com        / Workmate!2026"
if [ "$GRANT_ADMIN" = true ]; then
    echo
    echo "    [경고] --grant-admin 으로 '관리자 (데모)' 를 ROLE_ADMIN 으로 올렸다."
    echo "           README 에 비밀번호가 공개돼 있으므로 공개 인스턴스에서는 되돌려라:"
    echo "           docker exec -i $DB_CONTAINER psql -U $PG_USER -d $PG_DB \\"
    echo "             -c \"UPDATE app_user SET role = 'ROLE_USER' WHERE user_name = '관리자 (데모)';\""
fi
