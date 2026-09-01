#!/bin/bash
# =============================================================
# 배포 후 1회 실행 — 데모 계정·콘텐츠 생성 + 로그인 활성화 (F1-1 / 배포 가이드 §2)
#
# [로컬·데모 전용 — 공개 배포 인스턴스에서 실행 금지]
# 데모 계정 비밀번호(Workmate!2026)가 README 에 공개돼 있다. 이 스크립트를 돌리면
# 그 비밀번호로 로그인 가능한 계정이 생긴다 — 방문자 체험이 필요한 로컬·데모 환경에서만 쓴다.
#
# [왜 필요한가]
# Flyway 마이그레이션(V1·V2)은 데모 계정을 넣지 않는다 — 데모 계정의 이메일·전화번호는
# 이 스크립트가 쥔 AES 키로 암호화해야 배포마다 로그인이 되기 때문이다(개발 키로 고정된
# 암호문을 마이그레이션에 넣으면 다른 키를 쓰는 배포에서는 영원히 복호화가 안 된다).
# 그래서 계정 생성 자체를 이 스크립트가 맡는다 — 실제 AES 키를 쥔 쪽이 만들어야
# 처음부터 올바른 암호문이 들어가고, 메커니즘이 하나로 끝난다.
#
# [무엇을 하는가]
#   1. 데모 계정 3종을 (없으면) 만든다 — user_name·BCrypt 비밀번호·ROLE_USER,
#      email·phone 은 이 배포의 AES 키로 암호화
#   2. 이미 있는 계정(예: AES 키를 나중에 바꾼 경우)은 email·phone 을 현재 키로 재암호화한다
#   3. README 캡처용 데모 콘텐츠(채팅·영수증·회의록·감사로그)를 (없으면) 채운다 —
#      AES 와 무관한 평문 데이터라 계정과 달리 고정값 그대로 넣는다
#   4. --grant-admin 이면 '관리자 (데모)' 를 ROLE_ADMIN 으로 승격한다(비공개 시연용)
# 전부 존재 검사(WHERE NOT EXISTS) 기반이라 여러 번 실행해도 안전하다(멱등).
#
# [무엇을 하지 않는가]
# 권한은 기본적으로 건드리지 않는다. 데모 계정은 ROLE_USER 로 남는다
# (README 에 비밀번호가 공개돼 있어 공개 인스턴스에서 관리자면 안 된다).
#
# [사용법] 저장소 루트에서, DB 컨테이너와 WAS(Flyway 적용 완료)가 이미 떠 있는 상태로 실행한다.
#   ./scripts/bootstrap-demo-data.sh
#   ./scripts/bootstrap-demo-data.sh --grant-admin     # 비공개 시연용
#   DB_CONTAINER=workmate-db ./scripts/bootstrap-demo-data.sh
#
# 옛 이름은 scripts/bootstrap-demo-login.sh 였다 — 재암호화만 하던 것을
# 계정·콘텐츠 생성까지 하도록 확장하면서 실제 동작에 맞게 이름을 바꿨다.
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

# 데모 계정 평문 — db/init/legacy/13-seed-demo-data.sql 이 넣던 세 계정과 같다.
# user_name 은 암호화 대상이 아니라서 키가 달라도 그대로 매칭된다.
NAMES=('관리자 (데모)'      '홍길동 (데모)'   '김서연 (데모)')
MAILS=('demo.admin@example.com' 'hong@example.com' 'kim@example.com')
PHONES=('01000000001'          '01000000002'     '01000000003')
# 비밀번호(Workmate!2026)의 BCrypt 해시 — AES 키와 무관하게 고정이라 키가 달라도 그대로 쓴다.
HASHES=(
    '$2a$10$9Vfw6SMeojcOZfrUbH4QkeraxgsbuB1yY7op38kNjby2SZXXKdltm'
    '$2a$10$WQY3iym9t9aZj1.WtytT8.AnDB9NZwR5BU4Q9RqLoZzcHZoc3QwKe'
    '$2a$10$NkbUPahdMzt/wsyEKIcf1e0vW.lESgWkESqK7JePHASUWVbfJVXQm'
)
CREATED_AT='2026-08-25 03:06:08'

# 1) 계정 생성 (없으면) — 이 배포의 AES 키로 처음부터 올바르게 암호화해서 넣는다
CREATE_SQL=""
for i in 0 1 2; do
    EMAIL_ENC=$(enc "${MAILS[$i]}")
    PHONE_ENC=$(enc "${PHONES[$i]}")
    CREATE_SQL+="INSERT INTO app_user (email, password, user_name, phone, role, login_fail_count, use_yn, created_at)
SELECT '${EMAIL_ENC}', '${HASHES[$i]}', '${NAMES[$i]}', '${PHONE_ENC}', 'ROLE_USER', 0, true, TIMESTAMP '${CREATED_AT}'
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE user_name = '${NAMES[$i]}');
"
done

# 2) 재암호화 (이미 계정이 있던 경우 — 예: 다른 AES 키로 만들어진 뒤 키를 바꾼 경우)
REENCRYPT_SQL=""
for i in 0 1 2; do
    REENCRYPT_SQL+="UPDATE app_user SET email = '$(enc "${MAILS[$i]}")', phone = '$(enc "${PHONES[$i]}")' WHERE user_name = '${NAMES[$i]}';
"
done

GRANT_ADMIN_SQL=""
if [ "$GRANT_ADMIN" = true ]; then
    GRANT_ADMIN_SQL="UPDATE app_user SET role = 'ROLE_ADMIN' WHERE user_name = '관리자 (데모)';
"
fi

# 3) 데모 콘텐츠 — AES 와 무관한 평문 데이터라 db/init/legacy/13-seed-demo-data.sql 원문 그대로다.
#    작은따옴표 대신 $wm$ 달러 인용을 쓴다 — 가이드·회의록 본문에 작은따옴표(아포스트로피)가
#    섞여 있어도 안전하게 넣기 위함(원본 SQL 의 방식을 그대로 유지). 이 블록은 AES 키와
#    무관하므로 재암호화가 필요 없고, 통째로 따옴표 처리된 heredoc 이라 bash 변수 치환이
#    일어나지 않는다(내용 중 $wm$ 를 bash 가 변수로 오인하지 않도록 하는 것이 핵심).
read -r -d '' CONTENT_SQL <<'EOSQL' || true
-- 채팅 이력 — 실제 Gemini 응답 원문
WITH new_room AS (
    INSERT INTO chat_room (user_seq, title, use_yn, created_at)
    SELECT (SELECT user_seq FROM app_user WHERE user_name = $wm$관리자 (데모)$wm$), $wm$RAG에 대해서 설명해줘$wm$, true, TIMESTAMP '2026-08-05 01:41:55'
    WHERE NOT EXISTS (SELECT 1 FROM chat_room WHERE user_seq = (SELECT user_seq FROM app_user WHERE user_name = $wm$관리자 (데모)$wm$) AND title = $wm$RAG에 대해서 설명해줘$wm$)
    RETURNING room_seq
)
INSERT INTO chat_message (room_seq, role, content, model_name, created_at)
SELECT new_room.room_seq, v.role, v.content, v.model_name, v.created_at
FROM new_room, (VALUES
        ($wm$user$wm$, $wm$RAG에 대해서 설명해줘$wm$, NULL, TIMESTAMP '2026-08-05 01:41:55'),
        ($wm$assistant$wm$, $wm$**RAG (Retrieval-Augmented Generation, 검색 증강 생성)**는 거대언어모델(LLM)의 정보 한계와 환각(Hallucination) 현상을 극복하기 위한 기법입니다.

외부 지식 데이터베이스에서 질문과 연관된 근거 문서를 먼저 **검색(Retrieval)**한 뒤, 이를 LLM의 프롬프트에 **합성(Augmentation)**하여 근거 있는 정확한 답변을 **생성(Generation)**합니다.

---

### 🔄 RAG의 4단계 파이프라인 동작 원리

```
[ 1. 문서 수집 & 청크 분할 ] ➔ [ 2. 텍스트 임베딩 & Vector DB 저장 ]
                                           │
[ 4. 프롬프트 합성 & LLM 답변 ] ◄── [ 3. 사용자 질문 유사도 검색 ]
```

1. **청크 분할 (Chunking)**
   * 긴 마크다운, PDF 등의 문서를 500~1000자 단위의 의미 있는 조각(Chunk)으로 자릅니다.
   * 문맥 끊김을 방지하기 위해 일정 부분 중복 영역(Overlap)을 둡니다.
2. **임베딩 및 저장 (Embedding & Storage)**
   * 임베딩 모델(예: `gemini-embedding-001`)을 사용해 텍스트 청크를 고차원 수치 벡터(예: 768차원)로 변환한 뒤 Vector DB(예: PostgreSQL `pgvector`)에 저장합니다.
3. **유사도 검색 (Similarity Search)**
   * 사용자 질문도 동일한 임베딩 모델로 벡터화한 뒤, Vector DB에서 **코사인 유사도(Cosine Similarity)** 등이 높은 상위(Top-K) 관련 청크를 검색합니다.
4. **프롬프트 합성 및 답변 생성 (Augment & Generate)**
   * 검색된 근거 청크들을 LLM 시스템 프롬프트의 "참고 자료"로 주입하여 환각을 방지하고 출처와 함께 정확한 응답을 생성합니다.

---

### 💡 RAG vs 파인튜닝(Fine-tuning) 비교

| 구분 | RAG (검색 증강 생성) | 파인튜닝 (Fine-tuning) |
| :--- | :--- | :--- |
| **방식** | 모델 가중치는 유지하고 외부 지식을 검색해 주입 | 추가 데이터로 모델의 가중치 자체를 재학습 |
| **지식 갱신** | 문서 교체만으로 즉시 반영 | 데이터 재학습 필요 (비용 및 시간 발생) |
| **강점** | 정확한 출처 제시 가능, 자주 바뀌는 문서에 유용 | 특정 말투, 출력 형식, 특수 작업 능력 내재화 |
| **주요 용도** | 사내 문서 검색, FAQ, 최신 정보 기반 Q&A | 고정된 형식 출력, 도메인 전용 말투 학습 |

> 💡 **실무 팁**: 실무에서는 형식이나 작업 숙련도는 파인튜닝으로 잡고, 최신 정보 및 지식 검색은 RAG를 활용하는 방식으로 두 기법을 병행하기도 합니다.

---

### 🛠️ RAG 최적화 전략
* **유사도 임계값(Threshold) 설정**: 유사도가 기준치(예: 0.7) 미만인 저품질 청크는 검색 결과에서 제외하여 환각을 방지합니다.
* **의미 기반 검색**: 키워드 완전 일치가 아닌 의미 공간 상의 거리를 계산하므로, "휴가"라는 단어로 "연차" 관련 규정을 찾아내는 등 유연한 검색이 가능합니다.$wm$, $wm$gemini-flash-latest$wm$, TIMESTAMP '2026-08-05 01:42:03')
    ) AS v(role, content, model_name, created_at);

WITH new_room AS (
    INSERT INTO chat_room (user_seq, title, use_yn, created_at)
    SELECT (SELECT user_seq FROM app_user WHERE user_name = $wm$관리자 (데모)$wm$), $wm$Spring Boot Redis 캐싱 가이드$wm$, true, TIMESTAMP '2026-07-30 22:09:49'
    WHERE NOT EXISTS (SELECT 1 FROM chat_room WHERE user_seq = (SELECT user_seq FROM app_user WHERE user_name = $wm$관리자 (데모)$wm$) AND title = $wm$Spring Boot Redis 캐싱 가이드$wm$)
    RETURNING room_seq
)
INSERT INTO chat_message (room_seq, role, content, model_name, created_at)
SELECT new_room.room_seq, v.role, v.content, v.model_name, v.created_at
FROM new_room, (VALUES
        ($wm$user$wm$, $wm$Spring Boot에서 Redis 캐싱 적용 가이드를 정리해줘.$wm$, $wm$gemini-2.5-flash$wm$, TIMESTAMP '2026-07-30 22:09:49'),
        ($wm$assistant$wm$, $wm$Redis 캐싱 적용 가이드입니다.

```java
@Cacheable(value = "guides", key = "#guideSeq")
public GuideVo getGuide(Long guideSeq) {
    return guideRepository.findById(guideSeq);
}
```

- **Cache-Aside 패턴**: 데이터 요청 시 Redis 확인 후 DB 조회
- **TTL 주기**: 자주 조회되는 가이드 및 세션 부하 절감$wm$, $wm$gemini-2.5-flash$wm$, TIMESTAMP '2026-07-30 22:09:50')
    ) AS v(role, content, model_name, created_at);

-- 영수증 인식 이력 — 실제 멀티모달 OCR 결과
-- image_path 는 설정값(app.upload.receipt-dir) 기준 상대경로로 정규화한다.
-- 업로드 원본 이미지는 저장소에 포함하지 않으므로(uploads/ 는 git 미추적) 파일 자체는 없지만,
-- 이력 목록 화면은 이미지를 표시하지 않아 캡처 재현에는 문제가 없다.
INSERT INTO receipt (user_seq, image_path, pay_amount, biz_no, pay_date, card_name, biz_no_valid, select_type, raw_json, created_at)
SELECT (SELECT user_seq FROM app_user WHERE user_name = $wm$관리자 (데모)$wm$), $wm$uploads/receipt/6c8e6ab5-8043-4ba4-a518-ab5d2747f116.png$wm$, 48100, $wm$2208162517$wm$, $wm$20260715$wm$, $wm$롯데법인카드$wm$, true, $wm$AUTO$wm$, $wm$[{"bizNo":"2208162517","payDate":"20260715","cardName":"롯데법인카드","payAmount":48100}]$wm$::jsonb, TIMESTAMP '2026-07-22 09:05:27'
WHERE NOT EXISTS (SELECT 1 FROM receipt WHERE user_seq = (SELECT user_seq FROM app_user WHERE user_name = $wm$관리자 (데모)$wm$) AND biz_no = $wm$2208162517$wm$ AND pay_date = $wm$20260715$wm$ AND pay_amount = 48100);
INSERT INTO receipt (user_seq, image_path, pay_amount, biz_no, pay_date, card_name, biz_no_valid, select_type, raw_json, created_at)
SELECT (SELECT user_seq FROM app_user WHERE user_name = $wm$관리자 (데모)$wm$), $wm$uploads/receipt/71d06fd4-731f-41de-9143-a6686588c373.jpg$wm$, 22000, $wm$6727600528$wm$, $wm$20260713$wm$, $wm$롯데법인카드$wm$, true, $wm$AUTO$wm$, $wm$[{"bizNo":"6727600528","payDate":"20260713","cardName":"롯데법인카드","payAmount":22000}]$wm$::jsonb, TIMESTAMP '2026-07-29 06:08:54'
WHERE NOT EXISTS (SELECT 1 FROM receipt WHERE user_seq = (SELECT user_seq FROM app_user WHERE user_name = $wm$관리자 (데모)$wm$) AND biz_no = $wm$6727600528$wm$ AND pay_date = $wm$20260713$wm$ AND pay_amount = 22000);
INSERT INTO receipt (user_seq, image_path, pay_amount, biz_no, pay_date, card_name, biz_no_valid, select_type, raw_json, created_at)
SELECT (SELECT user_seq FROM app_user WHERE user_name = $wm$관리자 (데모)$wm$), $wm$uploads/receipt/ebf1d7eb-8002-482a-a5c9-24ff4d35269d.png$wm$, 22000, $wm$6831602314$wm$, $wm$20260401$wm$, $wm$롯데 아멕스 블루 비즈니스 카드$wm$, true, $wm$MANUAL$wm$, $wm$[{"bizNo":"6831602314","payDate":"20260401","cardName":"롯데 아멕스 블루 비즈니스 카드","payAmount":22000}]$wm$::jsonb, TIMESTAMP '2026-07-30 02:00:10'
WHERE NOT EXISTS (SELECT 1 FROM receipt WHERE user_seq = (SELECT user_seq FROM app_user WHERE user_name = $wm$관리자 (데모)$wm$) AND biz_no = $wm$6831602314$wm$ AND pay_date = $wm$20260401$wm$ AND pay_amount = 22000);

-- 회의록 이력 — 실제 STT 전사문과 AI 3단 요약
INSERT INTO voice_record (user_seq, title, stt_text, summary_md, audio_file_name, origin_file_name, file_size, content_type, created_at)
SELECT (SELECT user_seq FROM app_user WHERE user_name = $wm$관리자 (데모)$wm$), $wm$Workmate v3 아키텍처 및 3-tier BFF 설계 회의$wm$, $wm$오늘 회의에서는 Workmate v3의 Vue3 SPA 전환과 얇은 BFF 계층 설계에 대해 논의하겠습니다. 백엔드는 Spring Boot 3와 Spring AI를 기반으로 구축되며, 프론트엔드는 shadcn-vue와 Tailwind CSS v4를 사용하여 단독 SPA로 동적 렌더링됩니다. 브라우저는 8080포트의 WEB BFF만 바라보며, DB 직접 접근은 금지되고 백엔드 WAS(:8081)로 프록시 호출합니다.$wm$, $wm$### 1. 주요 안건
- Vue3 SPA 전환 및 얇은 BFF 계층 설계 정립
- Spring AI 및 pgvector 기반 RAG 구조 도입

### 2. 결정 사항
- Session 기반 인증 (Spring Security, httpOnly 쿠키)
- 3-tier 보안 경계 유지 (브라우저 → 8080 BFF → 8081 WAS)

### 3. 향후 일정
- 프론트엔드 모듈화 구조 적용 및 통합 테스트 진행$wm$, $wm$meeting-test.wav$wm$, $wm$meeting-test.wav$wm$, 1923330, $wm$audio/wav$wm$, TIMESTAMP '2026-07-31 07:09:50'
WHERE NOT EXISTS (SELECT 1 FROM voice_record WHERE user_seq = (SELECT user_seq FROM app_user WHERE user_name = $wm$관리자 (데모)$wm$) AND title = $wm$Workmate v3 아키텍처 및 3-tier BFF 설계 회의$wm$);
INSERT INTO voice_record (user_seq, title, stt_text, summary_md, audio_file_name, origin_file_name, file_size, content_type, created_at)
SELECT (SELECT user_seq FROM app_user WHERE user_name = $wm$관리자 (데모)$wm$), $wm$Sprint 15 주간 기술 스크럼 및 RAG 성능 평가$wm$, $wm$Sprint 15 주간 회의입니다. 금주 진행된 RAG 파이프라인 성능 평가 결과 코사인 유사도 검색 정확도가 92%로 향상되었습니다. 영수증 OCR 자동 인식 모듈과 음성 회의록 STT 파이프라인 통합 작업도 성공적으로 마무리되었습니다.$wm$, $wm$### 1. 성과 공유
- RAG 검색 정확도 92% 달성
- 영수증 OCR 및 음성 STT 파이프라인 구축 완료

### 2. 논리적 이슈
- 대용량 임베딩 쿼터 모니터링 체계 도입 필요$wm$, $wm$weekly_scrum_20260729.mp3$wm$, $wm$weekly_scrum_20260729.mp3$wm$, 1420000, $wm$audio/mp3$wm$, TIMESTAMP '2026-07-31 07:09:50'
WHERE NOT EXISTS (SELECT 1 FROM voice_record WHERE user_seq = (SELECT user_seq FROM app_user WHERE user_name = $wm$관리자 (데모)$wm$) AND title = $wm$Sprint 15 주간 기술 스크럼 및 RAG 성능 평가$wm$);

-- 관리자 감사 로그 (append-only)
INSERT INTO admin_audit_log (admin_user_seq, target_user_seq, action, created_at)
SELECT (SELECT user_seq FROM app_user WHERE user_name = $wm$관리자 (데모)$wm$), (SELECT user_seq FROM app_user WHERE user_name = $wm$홍길동 (데모)$wm$), 'UNLOCK', CURRENT_TIMESTAMP - interval '2 day'
WHERE NOT EXISTS (SELECT 1 FROM admin_audit_log WHERE admin_user_seq = (SELECT user_seq FROM app_user WHERE user_name = $wm$관리자 (데모)$wm$));
INSERT INTO admin_audit_log (admin_user_seq, target_user_seq, action, created_at)
SELECT (SELECT user_seq FROM app_user WHERE user_name = $wm$관리자 (데모)$wm$), (SELECT user_seq FROM app_user WHERE user_name = $wm$김서연 (데모)$wm$), 'RESET_PASSWORD', CURRENT_TIMESTAMP - interval '1 day'
WHERE (SELECT count(*) FROM admin_audit_log WHERE admin_user_seq = (SELECT user_seq FROM app_user WHERE user_name = $wm$관리자 (데모)$wm$)) < 2;
INSERT INTO admin_audit_log (admin_user_seq, target_user_seq, action, created_at)
SELECT (SELECT user_seq FROM app_user WHERE user_name = $wm$관리자 (데모)$wm$), (SELECT user_seq FROM app_user WHERE user_name = $wm$홍길동 (데모)$wm$), 'RESET_PASSWORD', CURRENT_TIMESTAMP - interval '3 hour'
WHERE (SELECT count(*) FROM admin_audit_log WHERE admin_user_seq = (SELECT user_seq FROM app_user WHERE user_name = $wm$관리자 (데모)$wm$)) < 3;
EOSQL

echo "[bootstrap-demo-data] '$DB_CONTAINER' 에 계정을 만든다 (AES 키: $ENV_FILE)"
printf '%s\n' "$CREATE_SQL" | docker exec -i "$DB_CONTAINER" psql -v ON_ERROR_STOP=1 -U "$PG_USER" -d "$PG_DB"

echo "[bootstrap-demo-data] email·phone 을 현재 키로 재암호화한다"
printf '%s\n' "$REENCRYPT_SQL" | docker exec -i "$DB_CONTAINER" psql -v ON_ERROR_STOP=1 -U "$PG_USER" -d "$PG_DB"

echo "[bootstrap-demo-data] 데모 콘텐츠(채팅·영수증·회의록·감사로그)를 채운다"
printf '%s\n' "$CONTENT_SQL" | docker exec -i "$DB_CONTAINER" psql -v ON_ERROR_STOP=1 -U "$PG_USER" -d "$PG_DB"

if [ "$GRANT_ADMIN" = true ]; then
    echo "[bootstrap-demo-data] '관리자 (데모)' 를 ROLE_ADMIN 으로 승격한다"
    printf '%s\n' "$GRANT_ADMIN_SQL" | docker exec -i "$DB_CONTAINER" psql -v ON_ERROR_STOP=1 -U "$PG_USER" -d "$PG_DB"
fi

# 조용한 실패 방지 — 기대한 만큼 계정이 실제로 존재하는지 검증하고, 아니면 비정상 종료한다.
ACCOUNT_COUNT=$(docker exec -i "$DB_CONTAINER" psql -tA -U "$PG_USER" -d "$PG_DB" \
    -c "SELECT count(*) FROM app_user WHERE user_name IN ('관리자 (데모)', '홍길동 (데모)', '김서연 (데모)');")
if [ "$ACCOUNT_COUNT" != "3" ]; then
    echo "[오류] 데모 계정이 3건이어야 하는데 ${ACCOUNT_COUNT}건이다. 위 psql 출력에서 실패한 문장을 확인하라."
    exit 1
fi

echo
echo "[bootstrap-demo-data] 완료. 데모 계정 ${ACCOUNT_COUNT}건 확인됨. 아래 계정으로 로그인된다."
docker exec -i "$DB_CONTAINER" psql -U "$PG_USER" -d "$PG_DB" \
    -c "SELECT user_name, role FROM app_user WHERE user_name IN ('관리자 (데모)', '홍길동 (데모)', '김서연 (데모)') ORDER BY user_seq;"
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
