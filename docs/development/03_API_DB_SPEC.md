# 04. API·DB 상세 설계서 (API & DB Spec)

- **작성일**: 2026-07-14
- **상태**: 사용자 검토 대기
- **상위 문서**: [workmate-design.md](workmate-design.md)
- **관련 문서**: [01 기능 명세서](01_FEATURE_SPEC.md) · [02 화면 설계서](02_SCREEN_DESIGN.md) · [05 공통 요구사항](05_COMMON_REQUIREMENTS.md)

WAS REST API의 요청/응답 규격과 DB 테이블 정의를 확정한다.
아키텍처 원칙(3-tier·프록시·JPA만)과 코딩 표준은 `docs/setup/02_DEVELOPMENT_GUIDE_BACKEND.md`를 따른다.

---

## 1. API 공통 규격

### 1.1 공통 응답 포맷 (`ApiResponse<T>`)

모든 WAS JSON 응답은 아래 포맷 (SSE 스트리밍 제외 — §3.2):

```json
{ "success": true,  "message": null,            "result": { ... } }
{ "success": false, "message": "실패 사유 문구", "result": null }
```

### 1.2 상태 코드 매핑 (GlobalExceptionHandler)

| 상황                                     | 코드 | 비고                                                                                          |
| ---------------------------------------- | ---- | --------------------------------------------------------------------------------------------- |
| 정상                                     | 200  |                                                                                               |
| 입력값 오류 (`IllegalArgumentException`) | 400  |                                                                                               |
| 미인증                                   | 401  | WEB 세션 만료 → 로그인 리다이렉트                                                             |
| 권한 없음 (타인 데이터·비관리자)         | 403  | **타인 영수증 접근도 403으로 확정** (01 F3.3 열린 결정 — 존재 여부는 message에 노출하지 않음) |
| 비즈니스 규칙 위반 (`BusinessException`) | 409  | 예: 계정 잠금, 이메일 중복                                                                    |
| 요청 제한 초과                           | 429  | F2-11                                                                                         |
| 서버 오류                                | 500  | 상세는 로그만, message는 일반 문구                                                            |
| WAS 연결 불가 (WEB 프록시 발행)          | 502  | 동일 `ApiResponse` 포맷                                                                       |

### 1.3 URL·메서드 규칙

- 접두사 `/api/v1/`. **GET/POST만 사용** — 수정 `POST .../{id}/update`, 삭제 `POST .../{id}/delete` (03 가이드 §5.7).
- WEB 프록시는 **동일 경로 중계**: 브라우저 `GET /api/v1/receipts` → WEB(8080) → WAS(8081) 같은 경로. 상태코드·본문 무가공 통과.

### 1.4 사용자 식별 (userSeq 전달 방식 — 확정)

- 브라우저는 userSeq를 **절대 보내지 않는다** (쿼리 파라미터 `?userSeq=` 폐지 — 01 부록 B 기술 부채 ① 해소).
- WEB이 세션에서 로그인 사용자의 userSeq를 꺼내 WAS 호출 시 **`X-User-Seq` 헤더**로 주입한다.
- WAS는 내부망 전용(8081 외부 미노출)이므로 이 헤더를 신뢰한다. 관리자 API는 `X-User-Role` 헤더도 함께 주입·검증.

### 1.5 서버 측 입력 검증 (05 문서 F9 공통 원칙)

- **소유권 검증 (F9-08)**: `{roomSeq}`·`{receiptSeq}`·`{guideSeq}` 등 모든 경로 id는 `X-User-Seq` 소유 자원인지 서비스 계층에서 검증 — 불일치 시 403 (존재 여부 미노출).
- **정규화는 WAS 진입 시점**: 이메일 소문자(F9-01)·사업자번호 숫자만(F9-02)·결제일/금액 형식(F9-03)·전화번호 숫자만(F9-09). 클라이언트 검증과 무관하게 항상 수행.
- 문자열 길이 상한(F9-10)·`month=YYYYMM` 형식(F9-11) 위반은 400.

## 2. 엔드포인트 목록 (전체)

| #   | 메서드·경로                                                                    | 설명                                             | 근거     | 단계 |
| --- | ------------------------------------------------------------------------------ | ------------------------------------------------ | -------- | ---- |
| A1  | `POST /api/v1/auth/signup`                                                     | 회원가입                                         | F1-01    | 1    |
| A2  | `POST /api/v1/auth/login`                                                      | 자격 검증 (세션 발급은 WEB 담당)                 | F1-05    | 1    |
| C1  | `GET /api/v1/chat/rooms`                                                       | 내 채팅방 목록                                   | F2-03    | 1    |
| C2  | `GET /api/v1/chat/rooms/{roomSeq}/messages`                                    | 방 대화 이력                                     | F2-08    | 1    |
| C3  | `POST /api/v1/chat/stream`                                                     | 메시지 전송 + SSE 스트리밍 응답 (방 없으면 생성) | F2-02~06 | 1    |
| C4  | `POST /api/v1/chat/rooms/{roomSeq}/delete`                                     | 방 논리 삭제                                     | F2-04    | 1    |
| R1  | `POST /api/v1/receipts/analyze`                                                | 이미지 분석 + **이력 자동 저장**                 | F3-08    | 2    |
| R2  | `POST /api/v1/receipts/{receiptSeq}/update`                                    | 추출값 수정 / 결제 건 선택 확정                  | F3-08a·b | 2    |
| R3  | `POST /api/v1/receipts`                                                        | 수동 등록 (OCR 실패 폴백 경로 전용)              | F3.3     | 2    |
| R4  | `GET /api/v1/receipts?month=YYYYMM`                                            | 월별 이력 목록                                   | F3-10    | 2    |
| R5  | `GET /api/v1/receipts/{receiptSeq}/image`                                      | 원본 이미지 (본인만)                             | F3-14    | 2    |
| R6  | `GET /api/v1/receipts/csv?month=YYYYMM`                                        | 월별 CSV (확정 건만, UTF-8 BOM)                  | F3-12    | 2    |
| G1  | `GET /api/v1/guides`                                                           | 목록 (본인+공개)                                 | F4-01·08 | 3    |
| G2  | `GET /api/v1/guides/{guideSeq}`                                                | 상세                                             | F4-01    | 3    |
| G3  | `POST /api/v1/guides`                                                          | 등록 (+임베딩)                                   | F4-02    | 3    |
| G4  | `POST /api/v1/guides/{guideSeq}/update`                                        | 수정 (+재임베딩)                                 | F4-02    | 3    |
| G5  | `POST /api/v1/guides/{guideSeq}/delete`                                        | 삭제 (+청크 삭제)                                | F4-04    | 3    |
| M1  | `GET /api/v1/admin/users?keyword=&page=`                                       | 사용자 목록·검색                                 | F6-01    | 1    |
| M2  | `POST /api/v1/admin/users/{userSeq}/unlock`                                    | 잠금 해제 (+감사 기록)                           | F6-02    | 1    |
| M3  | `POST /api/v1/admin/users/{userSeq}/reset-password`                            | 비번 초기화 (+감사 기록)                         | F6-03    | 1    |
| M4  | `GET /api/v1/admin/audit-logs?page=&size=`                                     | 감사 로그 조회 (행위자·대상 이름 조인, 최신순)   | F8       | 5    |
| K1  | `GET /api/common/codes/{groupCode}`                                            | 공통코드 조회 (사용중 코드만, 공개)              | F7-04    | 4    |
| K2  | `GET/POST/PUT/DELETE /api/v1/admin/common-codes/groups[/{group}]`              | 공통코드 그룹 관리(관리자)                       | F7       | 5    |
| K3  | `GET/POST/PUT/DELETE /api/v1/admin/common-codes/groups/{group}/codes[/{code}]` | 공통코드 관리(관리자)                            | F7       | 5    |
| H1  | `GET /api/v1/health`                                                           | 헬스체크 (기구현)                                | —        | —    |

> Tool Calling(F5)은 별도 엔드포인트가 아니라 C3 스트리밍 내부에서 Spring AI `@Tool`로 동작.
>
> K2·K3(공통코드 관리 화면)는 원래 "`db/init` SQL로만 관리" 설계였으나, 운영 중 코드 관리 편의를 위해 관리자 CRUD로 확장했다. 그룹 삭제는 하위 코드가 없을 때만 허용하고, `use_yn`으로 삭제 대신 비활성도 가능하다.

## 3. 주요 API 상세

### 3.1 인증

**A2 로그인 검증** — `POST /api/v1/auth/login`

```json
// 요청
{ "email": "user@example.com", "password": "평문(HTTPS 전제)" }
// 성공 result
{ "userSeq": 1, "userName": "김태경", "role": "ROLE_USER" }
// 실패: 401 (자격 불일치 — 어느 쪽 오류인지 미노출) / 409 (잠금, message에 남은 분)
```

- BCrypt 대조·실패 횟수 증가·잠금 판정은 **WAS**가 수행. 세션 생성·중복 로그인 관리는 **WEB**(Spring Security + 세션 레지스트리).
- **이메일 정규화 (F1-01a)**: A1 가입·A2 로그인 모두 WAS 진입 시점에 `trim` + 소문자 변환 후 처리한다. 클라이언트가 대문자를 막더라도 서버가 다시 정규화한다 — 암호화 저장 특성상 정규화 없이는 UK가 `User@x.com`/`user@x.com` 중복을 잡지 못한다.

### 3.2 채팅 스트리밍 — `POST /api/v1/chat/stream`

```json
// 요청
{
    "roomSeq": null,
    "message": "지난달 영수증 총액?",
    "ragMode": true,
    "modelCode": "gemini-2.5-flash"
}
// roomSeq null이면 방 생성 (F2-02), modelCode는 공통코드 AI_MODEL 값 (F7-03)
// modelCode는 AI_MODEL 그룹 use_yn=true 코드만 허용 — 목록 외 값 400 (F9-04 화이트리스트)
```

응답은 `ApiResponse`가 아닌 **`text/event-stream`** (WEB은 무버퍼 relay — F2-06):

```
event: meta     data: {"roomSeq": 12, "title": "지난달 영수증 총액?"}   ← 방 신규 생성 시 1회
event: token    data: {"delta": "6월"}                                ← 글자 단위 반복
event: source   data: {"guideSeq": 3, "title": "경비처리 가이드"}       ← RAG 출처 (F4-07)
event: done     data: {"messageSeq": 45, "modelName": "gemini-2.5-flash"}
event: error    data: {"message": "응답이 중단되었습니다"}              ← 실패 시 (수신분 보존)
```

### 3.3 영수증 분석 — `POST /api/v1/receipts/analyze` (multipart)

요청: `file` (이미지). 응답 3분기:

```json
// ① 영수증 아님 (F3-02a) — 이력 미저장
{ "success": true, "result": { "isReceipt": false } }

// ② AUTO (롯데법인카드 1건 — 저장 완료·확정)
{ "success": true, "result": {
    "isReceipt": true, "receiptSeq": 27, "selectType": "AUTO", "confirmed": true,
    "payAmount": 48600, "bizNo": "1234567890", "payDate": "20260701",
    "cardName": "롯데법인카드", "bizNoValid": true,
    "payments": [ {"cardName": "롯데법인카드", "amount": 48600, "payDate": "20260701"} ] } }

// ③ MANUAL (0건/복수 건 — 미확정 저장, 사용자 선택 대기)
{ "success": true, "result": {
    "isReceipt": true, "receiptSeq": 28, "selectType": "MANUAL", "confirmed": false,
    "payAmount": null, "bizNo": "9876543210", "payDate": null,
    "payments": [ {"cardName": "신한카드", ...}, {"cardName": "롯데법인카드", ...} ] } }
// OCR 실패: 500 아님 — { "success": false, "message": "분석에 실패했습니다..." } + 프런트가 수동 폼 폴백
```

**R2 수정/확정** — `POST /api/v1/receipts/{receiptSeq}/update`

```json
{
    "payAmount": 48600,
    "bizNo": "1234567890",
    "payDate": "20260701",
    "cardName": "롯데법인카드"
}
// 미확정 건이면 이 호출로 confirmed=true 전환 (F3-08b). bizNo 변경 시 체크섬 재검증
// 입력 정규화: bizNo 숫자만(F9-02) · payDate YYYYMMDD 실존 날짜 + payAmount 양수(F9-03) — 위반 400
```

- R1 업로드 파일은 확장자 + **매직 바이트** 검증 (F9-07). R6 CSV는 `=` `+` `-` `@` 선행 셀 이스케이프 (F9-06 수식 인젝션 차단).

### 3.4 공통코드 — `GET /api/common/codes/{groupCode}`

```json
{
    "success": true,
    "result": [
        { "code": "gemini-2.5-flash", "codeName": "Gemini 2.5 Flash" },
        { "code": "gemini-2.5-pro", "codeName": "Gemini 2.5 Pro" }
    ]
}
// 존재하지 않는 그룹: 빈 배열 (F7.3). 서버 캐시 (F7-05)
```

### 3.5 관리자 — M1 목록 응답의 개인정보

- 이메일·전화번호는 **WAS가 마스킹해서 반환** (`k**@g***.com`) — 화면·프록시에 원문이 흐르지 않게 (F6-01).

## 4. DB 테이블 정의서

네이밍: 소문자 snake_case·단수형·제약 `테이블명_컬럼명_제약`. 스키마는 `db/init/*.sql`로만 관리 (`ddl-auto: validate`).

### 4.1 admin_user (마일스톤 1 — 신규)

| 컬럼             | 타입            | 제약                         | 설명                                                                                                   |
| ---------------- | --------------- | ---------------------------- | ------------------------------------------------------------------------------------------------------ |
| user_seq         | bigint identity | PK                           |                                                                                                        |
| email            | varchar(512)    | NOT NULL, UK                 | **AES-256 암호문** (검색 위해 결정적 암호화 — v1 방식 이식). 암호화 전 **소문자 정규화 필수** (F1-01a) |
| password         | varchar(60)     | NOT NULL                     | BCrypt 해시                                                                                            |
| user_name        | varchar(50)     | NOT NULL                     |                                                                                                        |
| phone            | varchar(512)    |                              | AES-256 암호문                                                                                         |
| role             | varchar(20)     | NOT NULL DEFAULT 'ROLE_USER' | ROLE_USER / ROLE_ADMIN                                                                                 |
| login_fail_count | integer         | NOT NULL DEFAULT 0           | F1-06                                                                                                  |
| locked_at        | timestamp       |                              | 잠금 시각 (해제 판정: +1시간)                                                                          |
| use_yn           | boolean         | NOT NULL DEFAULT true        |                                                                                                        |
| created_at       | timestamp       | NOT NULL DEFAULT now         |                                                                                                        |

### 4.2 chat_room / chat_message (마일스톤 1 — 신규)

```
chat_room    : room_seq PK · user_seq NOT NULL · title varchar(100) · use_yn · created_at
               idx_chat_room_user (user_seq, created_at DESC)
chat_message : message_seq PK · room_seq NOT NULL · role varchar(10)('user'|'assistant')
               · content text · model_name varchar(50) · created_at
               idx_chat_message_room (room_seq, created_at)
```

### 4.3 receipt (기존 테이블 **변경** — 마일스톤 2)

자동 저장·미확정 흐름(F3-08·08b) 반영. 기존 `02-schema.sql`과의 차이:

| 컬럼                           | 변경                                    | 사유                                                           |
| ------------------------------ | --------------------------------------- | -------------------------------------------------------------- |
| pay_amount / biz_no / pay_date | `NOT NULL` → **NULL 허용**              | MANUAL 미확정 시점엔 값 없음                                   |
| biz_no_valid                   | `NOT NULL` → NULL 허용                  | biz_no 없으면 판정 불가                                        |
| **confirm_yn**                 | **신규** boolean NOT NULL DEFAULT false | 확정 여부 — CSV·Tool 집계는 `confirm_yn=true`만 (F3-12, F5-01) |

> 기존 개발 데이터는 초기화 가능하므로 `02-schema.sql` 직접 수정 + 볼륨 재생성(`down -v`)으로 적용한다 (마이그레이션 스크립트 불필요 — 아직 운영 데이터 없음).

### 4.4 guide / vector_store — 기존 유지 (변경 없음)

### 4.5 common_code_group / common_code (마일스톤 4 — 신규, F7)

```sql
common_code_group : group_code varchar(30) PK · group_name varchar(100) NOT NULL
                    · description varchar(255) · use_yn boolean NOT NULL DEFAULT true
common_code       : group_code + code varchar(50) 복합 PK (common_code_pk)
                    · code_name varchar(100) NOT NULL · sort_order integer NOT NULL DEFAULT 0
                    · use_yn boolean NOT NULL DEFAULT true
                    · FK common_code_group_code_fk → common_code_group(group_code)
-- 초기 데이터: ('AI_MODEL','AI 답변 모델') + ('AI_MODEL','gemini-2.5-flash','Gemini 2.5 Flash',1) 등
```

### 4.6 admin_audit_log (마일스톤 1 — 신규, F8 감사 로그 DB 저장 확정)

| 컬럼            | 타입            | 제약                 | 설명                        |
| --------------- | --------------- | -------------------- | --------------------------- |
| audit_seq       | bigint identity | PK                   |                             |
| admin_user_seq  | bigint          | NOT NULL             | 행위자                      |
| target_user_seq | bigint          | NOT NULL             | 대상                        |
| action          | varchar(30)     | NOT NULL             | 'UNLOCK' / 'RESET_PASSWORD' |
| created_at      | timestamp       | NOT NULL DEFAULT now |                             |

- INSERT만 하는 불변(append-only) 테이블 — UPDATE/DELETE 하지 않는다 (감사 무결성).
- M2·M3 처리와 **같은 트랜잭션**에서 기록 (기록 실패 시 행위도 롤백).

## 5. 테이블 ↔ 마일스톤 요약

| 마일스톤 | DB 작업                                                                         |
| -------- | ------------------------------------------------------------------------------- |
| 1        | `admin_user`·`chat_room`·`chat_message`·`admin_audit_log` 신규 (03-schema 추가) |
| 2        | `receipt` 컬럼 변경 (§4.3)                                                      |
| 3        | 없음 (guide·vector_store 기존)                                                  |
| 4        | `common_code_group`·`common_code` 신규 + AI_MODEL 초기 데이터                   |
