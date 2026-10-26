# API 명세

WAS(:8081) REST API 목록이다. 컨트롤러 코드에서 뽑았다. 테이블 구조는 [README 의 ERD](../../README.md#db-스키마)와 Flyway 마이그레이션(`workmate-was/src/main/resources/db/migration`)이 원본이다.

## 공통 규칙

**경로와 중계.** 브라우저는 대부분 WEB(:8080)의 같은 경로를 부르고, WEB 이 세션을 확인한 뒤 WAS 로 그대로 넘긴다. 상태 코드와 본문은 가공하지 않는다. 경로가 다른 곳은 둘이다 — 인증은 WEB 이 `/api/auth/*` 로 받고(로그인·로그아웃·세션 확인은 WEB 이 직접 처리), 도우미는 WEB `/api/assistant/stream` 이 WAS `/api/v1/assistant/stream` 으로 넘긴다.

**사용자 식별.** 브라우저는 사용자 번호를 보내지 않는다. WEB 이 세션에서 꺼내 `X-User-Seq`·`X-User-Role` 헤더로 붙인다. WAS 는 내부망에만 있어서 이 헤더를 믿는다([ADR-0003](../project/adr/0003-guide-authz-in-was.md)).

**응답 포맷.** SSE 스트리밍과 파일 응답(CSV·오디오)을 빼면 모두 `ApiResponse` 로 감싼다.

```json
{ "success": true,  "message": null,       "result": { } }
{ "success": false, "message": "실패 사유", "result": null }
```

**상태 코드.** 예외는 `GlobalExceptionHandler` 한 곳에서 바꾼다.

| 코드 | 언제                                                                |
| ---- | ------------------------------------------------------------------- |
| 400  | 입력값 오류, 남의 자원 접근(`IllegalArgumentException`)             |
| 401  | 로그인 실패(`AuthenticationFailedException`), WEB 세션 없음         |
| 403  | 관리자 API 를 일반 사용자가 호출 — WEB 보안 설정이 막는다           |
| 404  | 없는 경로                                                           |
| 409  | 비즈니스 규칙 위반(`BusinessException`) — 계정 잠금, 이메일 중복 등 |
| 429  | 채팅 요청 한도 초과(`RateLimitExceededException`)                   |
| 500  | 그 밖의 서버 오류. 상세는 로그에만 남기고 메시지는 일반 문구        |

## 엔드포인트

### 인증

브라우저가 부르는 WEB 경로와, WEB 이 호출하는 WAS 경로가 다르다.

| WEB (브라우저) | WAS | 설명 |
| --- | --- | --- |
| `POST /api/auth/login` | `POST /api/v1/auth/login` | WEB 의 Spring Security 가 받아 WAS 에 자격 증명 확인을 맡기고, 통과하면 세션을 만든다 |
| `POST /api/auth/logout` | — | 세션 종료. WEB 이 직접 처리한다 |
| `GET /api/auth/me` | — | 현재 로그인 사용자. 세션이 없으면 401 |
| `POST /api/auth/signup` | `POST /api/v1/auth/signup` | 회원가입 — 관리자만 |
| (소셜 로그인 콜백) | `POST /api/v1/auth/social-login` | 토큰 교환과 프로필 조회를 WEB 이 끝낸 뒤 호출한다 |

### 채팅 · 도우미

| 메서드 | 경로                                    | 설명                                                                                      |
| ------ | --------------------------------------- | ----------------------------------------------------------------------------------------- |
| GET    | `/api/v1/chat/rooms`                    | 내 채팅방 목록                                                                            |
| GET    | `/api/v1/chat/rooms/{roomSeq}/messages` | 대화 이력                                                                                 |
| POST   | `/api/v1/chat/rooms/{roomSeq}/delete`   | 채팅방 논리 삭제                                                                          |
| POST   | `/api/v1/chat/stream`                   | 메시지 전송 + 답변 SSE 스트리밍. 방이 없으면 만든다. 출처는 토큰과 별개의 이벤트로 보낸다 |
| POST   | `/api/v1/assistant/stream`              | 페이지 도우미 SSE. 방과 이력을 저장하지 않는다                                            |

### 사내 가이드

| 메서드 | 경로                               | 설명                                                                    |
| ------ | ---------------------------------- | ----------------------------------------------------------------------- |
| GET    | `/api/v1/guides`                   | 목록(본인 + 공개). `keyword` · `page` · `size`, 페이지 값이 없으면 전체 |
| GET    | `/api/v1/guides/{guideSeq}`        | 상세                                                                    |
| POST   | `/api/v1/guides`                   | 등록 + 임베딩                                                           |
| POST   | `/api/v1/guides/{guideSeq}/update` | 수정 — 본인 또는 관리자. 본문이 그대로면 재임베딩하지 않는다            |
| POST   | `/api/v1/guides/{guideSeq}/delete` | 삭제 + 청크 삭제 — 본인 또는 관리자                                     |

### 영수증

| 메서드 | 경로                       | 설명                                                                               |
| ------ | -------------------------- | ---------------------------------------------------------------------------------- |
| POST   | `/api/v1/receipts/analyze` | 이미지(`file`) 분석 — 추출 후보와 사업자번호 검증 결과를 돌려준다. 저장하지 않는다 |
| POST   | `/api/v1/receipts`         | 사용자가 확인·수정한 값을 저장                                                     |
| GET    | `/api/v1/receipts`         | 내 영수증 목록(최신순). `page` · `size`                                            |
| GET    | `/api/v1/receipts/csv`     | CSV 내보내기(UTF-8 BOM)                                                            |

### 회의록

| 메서드 | 경로                               | 설명                                                                    |
| ------ | ---------------------------------- | ----------------------------------------------------------------------- |
| POST   | `/api/v1/voice/analyze`            | 오디오(`file`, 선택 `title`) 전사 + 요약. 결과와 오디오를 함께 저장한다 |
| GET    | `/api/v1/voice`                    | 내 회의록 목록(최신순)                                                  |
| GET    | `/api/v1/voice/{recordSeq}`        | 상세(전사문 · 요약 전문)                                                |
| GET    | `/api/v1/voice/{recordSeq}/audio`  | 오디오 스트리밍. `Range` 요청이면 206 으로 구간만 보낸다                |
| POST   | `/api/v1/voice/{recordSeq}/title`  | 제목 수정                                                               |
| POST   | `/api/v1/voice/{recordSeq}/delete` | 삭제 — DB 행과 오디오 파일을 함께 지운다                                |

### 사용량

| 메서드 | 경로                          | 설명                                                              |
| ------ | ----------------------------- | ----------------------------------------------------------------- |
| GET    | `/api/v1/usage/me`            | 내 사용량 — 합계 · 기능별 · 일별. `from` · `to`(없으면 최근 30일) |
| GET    | `/api/v1/admin/usage/summary` | 전체 사용량 요약(관리자)                                          |
| GET    | `/api/v1/admin/usage/by-user` | 사용자별 집계(관리자). 토큰 합계 내림차순, `page` · `size`        |

### 관리자

| 메서드 | 경로                                           | 설명                                                          |
| ------ | ---------------------------------------------- | ------------------------------------------------------------- |
| GET    | `/api/v1/admin/users`                          | 사용자 목록 · 검색. `keyword` · `page` · `size`               |
| POST   | `/api/v1/admin/users/{userSeq}/unlock`         | 잠금 해제 + 감사 기록                                         |
| POST   | `/api/v1/admin/users/{userSeq}/reset-password` | 비밀번호 초기화 + 감사 기록. 임시 비밀번호를 한 번만 돌려준다 |
| GET    | `/api/v1/admin/audit-logs`                     | 감사 로그(최신순). `page` · `size`                            |

### 공통코드

| 메서드       | 경로                                                         | 설명                                                                           |
| ------------ | ------------------------------------------------------------ | ------------------------------------------------------------------------------ |
| GET          | `/api/common/codes/{groupCode}`                              | 사용 중인 코드 조회. 없는 그룹은 빈 배열. `AI_MODEL` 그룹이 채팅 모델 목록이다 |
| GET · POST   | `/api/v1/admin/common-codes/groups`                          | 그룹 목록 · 등록(관리자)                                                       |
| PUT · DELETE | `/api/v1/admin/common-codes/groups/{groupCode}`              | 그룹 수정 · 삭제(관리자)                                                       |
| GET · POST   | `/api/v1/admin/common-codes/groups/{groupCode}/codes`        | 코드 목록 · 등록(관리자)                                                       |
| PUT · DELETE | `/api/v1/admin/common-codes/groups/{groupCode}/codes/{code}` | 코드 수정 · 삭제(관리자)                                                       |

### 기타

| 메서드 | 경로             | 설명     |
| ------ | ---------------- | -------- |
| GET    | `/api/v1/health` | 헬스체크 |

수정·삭제는 대부분 `POST .../update`·`POST .../delete` 로 통일했다. 공통코드 관리만 나중에 추가하면서 PUT·DELETE 를 썼다.
