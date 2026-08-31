#!/bin/bash
# =============================================================
# README 의 라이브 데모 주소 갱신
#
# [왜 필요한가]
# Cloudflare Quick Tunnel(trycloudflare.com)은 도메인 없이 무료로 쓰는 임시 터널이라
# cloudflared 프로세스가 다시 뜰 때마다 주소가 새로 발급된다. 재부팅·도커 재시작·
# WSL 종료가 전부 해당하므로 실질적으로 월 1~2회는 바뀐다고 보면 된다.
# 바뀌어도 알림이 없어서, README 에 적힌 주소가 조용히 죽는다.
#   → 이 스크립트가 현재 주소를 읽어 README 의 표시 구간만 갈아끼운다.
#
# [사용법]
#   ./scripts/update-demo-url.sh                      # 터널 컨테이너에서 자동 탐지
#   ./scripts/update-demo-url.sh https://xxx.trycloudflare.com   # 주소를 직접 지정
#   TUNNEL_CONTAINER=my-tunnel ./scripts/update-demo-url.sh
#
# 자동 탐지는 docker 가 있는 곳에서 실행해야 한다. 저장소와 docker 가 서로 다른 환경에
# 있으면(예: 저장소는 Windows, docker 는 WSL) 주소를 인자로 넘기는 쪽이 편하다.
#
# 갱신 뒤 커밋·푸시는 직접 한다 — 무엇이 바뀌는지 보고 올리는 편이 안전하다.
# =============================================================
set -euo pipefail

README="${README:-README.md}"
CONTAINER="${TUNNEL_CONTAINER:-wm-quicktunnel}"
BEGIN='<!-- demo-url:start -->'
END='<!-- demo-url:end -->'

[ -f "$README" ] || { echo "[오류] $README 이 없다. 저장소 루트에서 실행하라."; exit 1; }

# --- 주소 확보 ---
URL="${1:-}"
if [ -z "$URL" ]; then
    command -v docker >/dev/null 2>&1 \
        || { echo "[오류] docker 가 없다. 주소를 인자로 넘겨라: $0 https://xxx.trycloudflare.com"; exit 1; }
    docker inspect "$CONTAINER" >/dev/null 2>&1 \
        || { echo "[오류] 터널 컨테이너 '$CONTAINER' 를 찾을 수 없다. TUNNEL_CONTAINER 로 지정하거나 주소를 인자로 넘겨라."; exit 1; }
    # 재시작 이력이 있으면 로그에 옛 주소가 함께 남는다. 마지막 것이 현재 주소다
    URL=$(docker logs "$CONTAINER" 2>&1 | grep -oE 'https://[a-z0-9-]+\.trycloudflare\.com' | tail -1)
    [ -n "$URL" ] || { echo "[오류] '$CONTAINER' 로그에서 주소를 찾지 못했다. 아직 발급 전일 수 있다."; exit 1; }
fi

case "$URL" in
    https://*) ;;
    *) echo "[오류] 주소는 https:// 로 시작해야 한다: $URL"; exit 1;;
esac

# --- 살아 있는지 확인하고 쓴다 (죽은 주소를 README 에 박지 않는다) ---
CODE=$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 "$URL/" || echo 000)
if [ "$CODE" != "200" ]; then
    echo "[경고] $URL 응답이 HTTP $CODE 다. 터널이나 앱이 떠 있는지 확인하라."
    printf '그래도 README 에 쓸까? [y/N] '
    read -r ANS
    [ "$ANS" = "y" ] || [ "$ANS" = "Y" ] || { echo "취소했다."; exit 1; }
fi

# --- README 의 표시 구간 교체 ---
grep -q "$BEGIN" "$README" || { echo "[오류] $README 에 $BEGIN 마커가 없다."; exit 1; }

BLOCK_FILE=$(mktemp)
cat > "$BLOCK_FILE" <<BLOCK
$BEGIN
> 🔗 **라이브 데모** — <$URL>
>
> \`demo.admin@example.com\` / \`Workmate!2026\` 으로 로그인하면 아래 화면을 그대로 볼 수 있다.
> 무료 임시 터널(Cloudflare Quick Tunnel)이라 **주소가 바뀌거나 꺼져 있을 수 있다.**
> 소셜 로그인은 고정 도메인에 콜백을 등록해야 해서 데모에서는 이메일 로그인만 동작한다.
$END
BLOCK

awk -v b="$BEGIN" -v e="$END" -v f="$BLOCK_FILE" '
    $0 ~ b { while ((getline line < f) > 0) print line; close(f); skip = 1; next }
    $0 ~ e { skip = 0; next }
    !skip
' "$README" > "$README.tmp"

mv "$README.tmp" "$README"
rm -f "$BLOCK_FILE"

echo "[update-demo-url] $README 갱신 완료 (HTTP $CODE)"
echo "    $URL"
echo
echo "다음:"
echo "    git diff $README"
echo "    git add $README && git commit -m 'docs: 라이브 데모 주소 갱신' && git push"
