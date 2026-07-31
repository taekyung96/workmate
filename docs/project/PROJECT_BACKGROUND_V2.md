# Workmate 설계 문서

- **상태**: 승인 완료 — 재설계 세션에서 상세 설계 문서 5종으로 구체화됨
- **상세 설계 문서** (개발 기준 — 본 문서는 개요, 충돌 시 상세 문서가 우선):
    - [기능 명세서](FEATURE_SPEC.md) — F1~F7 기능별 요구사항·예외·완료 조건
    - [화면 설계서](../design/01_SCREEN_DESIGN.md) — 7개 화면 와이어프레임·이동 흐름
    - [디자인 시스템](../design/00_DESIGN_SYSTEM.md) — 컬러 토큰·타이포·컴포넌트 톤
    - [API·DB 설계서](../development/03_API_DB_SPEC.md) — 엔드포인트 스키마·테이블 정의
    - [공통 요구사항](../development/05_COMMON_REQUIREMENTS.md) — F8 로깅·F9 서버 측 입력 검증
- **목적**: 이직 포트폴리오 대표작. Spring AI 기반 업무 비서 — 스트리밍 채팅 · 영수증 자동 인식 · 문서 RAG
- **목표 기간**: 2~3개월 (평일 저녁 + 주말)

---

## 1. 배경

chat-space(v1)의 전면 재설계 후속작. v1에서 얻은 것과 버릴 것:

**계승**: WEB/WAS 분리 아키텍처, Thymeleaf SSR + Vue 3 UMD, JPA+MyBatis 혼합, 계층별 예외 처리, AES-256 투명 암호화, 세션 인증, `spring-vue-scaffold` 스킬 컨벤션.

**v1의 문제 → v2의 교정**:

| v1 문제                        | v2 교정                                          |
| ------------------------------ | ------------------------------------------------ |
| git 저장소 3개 분리 + zip 백업 | 루트 단일 저장소(모노레포) + Gradle 멀티프로젝트 |
| Gemini API를 직접 HTTP 호출    | Spring AI 추상화 (모델 교체 = 설정 변경)         |
| AI 응답이 한 번에 출력         | SSE 스트리밍                                     |
| 루트에 가이드 문서 난립        | `docs/` 폴더로 일원화                            |
| STS 2개 실행 (메모리 부족)     | 단일 워크스페이스 + Boot Dashboard 동시 실행     |

**영수증 기능의 실사용 동기**: 회사 ERP에 영수증 경비를 등록할 때 금액·사업자번호·결제일을 수기 입력하는 불편을 해결한다. 법인카드(롯데법인카드) 분할 결제 시 해당 카드 결제분만 입력하는 업무 규칙이 존재한다.

## 2. 아키텍처

```
Browser (Thymeleaf 페이지 + Vue 3 UMD 컴포넌트)
   │  HTTP (세션 쿠키) + SSE (스트리밍)
workmate-web (8080) — Thymeleaf SSR · Spring Security 세션 인증 · WAS 프록시 + SSE 스트림 중계
   │  REST + 스트리밍 relay (내부망)
workmate-was (8081) — 비즈니스 로직 · JPA/MyBatis · Spring AI (Gemini 2.5 Flash)
   │  JDBC
PostgreSQL 17 + pgvector (업무 데이터 + 벡터 임베딩 단일 DB)
```

**핵심 결정**:

1. **WEB/WAS 분리 유지** — 실무 경험(Tomcat WEB/WAS 분리 구성)과 일관된 서사. WEB은 세션 인증·렌더링·프록시 전담, WAS는 stateless.
2. **SSE 스트리밍 중계** — AI 응답이 `WAS(Flux) → WEB(무버퍼 relay) → Browser(SSE)` 두 홉을 실시간 통과. WEB이 WebClient로 WAS 스트림을 구독해 받은 조각을 즉시 흘려보낸다(버퍼링 금지).
3. **pgvector** — RAG 벡터 저장소를 별도 DB 없이 PostgreSQL 확장으로. 운영 대상 최소화, Spring AI PgVectorStore 공식 지원.
4. **모노레포 + Gradle 멀티프로젝트** — 루트 `settings.gradle`에 `include 'workmate-web', 'workmate-was'`. STS 워크스페이스 하나에 루트 임포트, Boot Dashboard에서 두 내장톰캣 동시 실행. workmate-vue는 일반 프로젝트로 추가(빌드는 npm).

**저장소 구조**:

```
workmate/
├── settings.gradle          # include 'workmate-web', 'workmate-was'
├── workmate-web/                # WEB 레이어 (8080)
├── workmate-was/                # WAS 레이어 (8081)
├── workmate-vue/                # Vue 3 UMD 공통 컴포넌트 (Vite)
├── docs/
│   ├── DESIGN_GUIDE.md      # 디자인 원칙 (1단계 산출)
│   ├── ARCHITECTURE.md      # 아키텍처 결정 기록
│   ├── DEV_LOG.md           # 문제 → 해결 기록 (면접 대비 핵심)
│   └── superpowers/specs/   # 설계 문서 (본 문서)
├── docker-compose.yml
├── .env.example             # 키 이름만 있는 템플릿 (커밋 대상)
└── README.md
```

## 3. 기능 명세

### 3.1 AI 채팅 (1단계)

- 채팅방 생성·조회·삭제(논리), 대화 이력 저장
- Spring AI `ChatClient` 기반 Gemini 2.5 Flash 연동 (`spring-ai-starter-model-google-genai`)
- 스트리밍: `chatClient.prompt().stream()` → SSE로 글자 단위 실시간 표시, 마크다운 렌더링
- `chat_message.model_name` 컬럼으로 답변 생성 모델 기록 (4단계 멀티모델 대비)

### 3.2 영수증 처리 (2단계) — 실사용 기능

**흐름**: 이미지 업로드 → [분석하기] → Vision 추출 + 도메인 규칙 → **이력 자동 저장**(별도 저장 버튼 없음) → 결과 표시(수정 시 UPDATE) → 이력/내보내기. 롯데카드 0건/복수 건은 미확정 저장 후 사용자 선택으로 확정 (상세: 01 문서 F3)

- **추출 스키마**: 결제 건 배열 `[{카드사명, 금액, 결제일}, ...]` — 영수증에 결제 수단이 여러 건일 수 있음
- **ERP 입력 필드**: 금액, 사업자번호, 결제일 (3개 필드)
- **카드 선택 규칙**: 카드사명에 "롯데법인카드" 포함 건을 자동 선택(`select_type=AUTO`). 해당 건이 없거나 복수이면 자동 추측하지 않고 사용자 선택 폼 표시(`select_type=MANUAL`)
- **사업자번호 체크섬 검증**: 한국 사업자등록번호 검증 알고리즘 적용. 실패 시 저장은 허용하되 경고 표시(`biz_no_valid=false`)
- **AI 제안 → 규칙 검증 → 사람 확인** 3단계: 추출값은 수정 가능한 폼으로 제시, 바로 저장하지 않음
- **원본 보관**: AI 추출 전체 JSON을 `raw_json`에 저장 (규칙 변경 시 재처리, 오인식 디버깅 근거)
- **이력**: 목록 + 건별 필드 복사 버튼 + 월별 CSV 다운로드 (ERP 입력 보조)

### 3.3 가이드 문서 + RAG (3단계)

- v1 가이드 문서 기능 이식 (마크다운 작성·공개/비공개)
- 문서 저장 시 청크 분할 → 임베딩 → pgvector 저장
- 채팅에서 RAG 모드: 질문 → 유사 청크 검색 → 근거 포함 답변 + **출처 문서 표시** (항상)

### 3.4 Tool Calling + 멀티모델 (4단계)

- `@Tool` 등록: 영수증 이력 조회(예: "지난달 영수증 총액?"), 가이드 문서 검색 등
- 멀티모델: Spring AI 설정 기반 모델 스위치. 같은 질문 모델별 비교 화면(선택 구현)

### 3.5 v1 이식 기능

- 회원/인증: 로그인, 회원가입, 계정 잠금(5회/1시간), 중복 로그인 방지
- 관리자: 사용자 목록·검색·잠금 해제·비밀번호 초기화 (ROLE_ADMIN)
- 좋아요/댓글은 v2 범위에서 제외 (v1에 이미 구현 서사 존재)

## 4. 모듈 구조 (workmate-was)

```
com.workmate.was/
├── auth/          # 인증·계정잠금 (v1 이식)
├── admin/         # 사용자 관리 (v1 이식)
├── chat/          # 채팅방 CRUD + AI 스트리밍
├── receipt/       # 영수증 — OCR 추출·검증·이력·내보내기
├── guide/         # 가이드 문서 (3단계)
├── rag/           # 임베딩·검색 (3단계)
├── ai/            # Spring AI 공통 — ChatClient 설정, 시스템 프롬프트, Tool 정의
└── global/        # 공통 응답·예외·AES-256·보안 설정 (v1 이식)
```

## 5. DB 스키마 (핵심)

네이밍: 소문자 snake_case, 테이블명 단수형, 제약조건 `테이블명_컬럼명_제약` 형식.

```sql
-- v1 이식: admin_user (이메일·전화번호 AES-256, 계정잠금 필드)

chat_room     (room_seq PK, user_seq FK, title, use_yn, created_at)
chat_message  (message_seq PK, room_seq FK, role,      -- 'user' | 'assistant'
               content, model_name, created_at)

receipt       (receipt_seq PK, user_seq FK, image_path,
               pay_amount, biz_no, pay_date,            -- ERP 입력 3필드 (미확정 시 NULL)
               card_name, biz_no_valid,
               select_type,                             -- 'AUTO' | 'MANUAL'
               confirm_yn,                              -- 확정 여부 (분석=자동저장 흐름)
               raw_json,                                -- AI 추출 원본 (jsonb)
               created_at)

-- 3단계
guide         (v1 구조 이식)
vector_store  (Spring AI PgVectorStore 표준 스키마)

-- 공통·감사 (2026-07-14 추가 결정)
common_code_group / common_code   -- 공통코드 (첫 그룹: AI_MODEL, 4단계)
admin_audit_log                   -- 관리자 행위 감사 로그 (append-only, 1단계)
```

> 컬럼·제약 상세는 [API·DB 설계서](../development/03_API_DB_SPEC.md) 가 기준.

## 6. 에러 처리

**v1 계승**: `BusinessException`(비즈니스 규칙 위반) / `IllegalArgumentException`(입력 오류) 분리 + `GlobalExceptionHandler` HTTP 상태 매핑.

**AI 특화 3종**:

| 실패                            | 처리                                                               |
| ------------------------------- | ------------------------------------------------------------------ |
| Gemini API 실패 (타임아웃·쿼터) | 재시도 1회 → 실패 시 사용자 안내, 로그에 원인 구분                 |
| 스트리밍 중간 끊김              | SSE error 이벤트 → 수신분 보존 + "응답 중단" 표시 + 재시도 버튼    |
| OCR 추출/파싱 실패              | 예외 대신 **빈 수동 입력 폼 폴백** — 이미지 보관, 업무 연속성 보장 |

## 7. 테스트 전략

결정적 영역과 비결정적 영역 분리:

- **결정적(주력, TDD)**: 사업자번호 체크섬(순수 함수), 롯데법인카드 선택 규칙(롯데 1건 AUTO / 없음 MANUAL / 복수 건 MANUAL), AI 응답 JSON 파싱·폴백, 서비스 계층(ChatClient 모킹)
- **비결정적(최소)**: 실제 Gemini 호출 스모크 테스트 1~2개, 별도 태그로 CI 제외
- 도구: JUnit 5 + Mockito + AssertJ (v1 동일), superpowers TDD 워크플로우 적용

## 8. 보안

- **v1 계승**: BCrypt, AES-256(이메일·전화번호), 세션 인증, 중복 로그인 방지, 계정 잠금, ROLE 기반 접근 제어
- **API 키**: `.env` 파일 관리. `.env`는 `.gitignore`, `.env.example`만 커밋. docker-compose가 `.env` 참조
- **프롬프트 인젝션**: RAG 문서 내 악성 지시 대비 시스템 프롬프트 우선 구성 + 출처 표시로 사용자 검증 가능
- **영수증 접근 제어**: 본인 데이터만 조회. 이미지 직접 URL 차단, 인증 API로만 다운로드
- **사용량 제한**: 사용자별 분당 AI 요청 제한 (쿼터 보호·악용 방지)

## 9. 디자인 가이드 방침

`docs/DESIGN_GUIDE.md`를 1단계에서 작성하고 이후 모든 페이지가 이를 따른다.

- **구조(UX) 확정**: 사이드바 통합형 (Claude.ai식) — 좌측 사이드바에 채팅 목록 + 영수증·가이드·관리자 메뉴 통합 (02 문서 §0)
- **시각 토큰 확정**: 포인트 컬러 **딥 블루 `#2563EB`** + 쿨 그레이 뉴트럴, 라이트 테마 전용(토큰 경유로 다크 확장 여지). 상세는 디자인 시스템 문서
- **폼·리스트 참고**: Mobbin 핀테크 패턴 (영수증 확인 폼, 이력 목록)
- 반응형: Flexbox/Grid 기반, 모바일 대응
- 픽셀 단위 복제 금지 (구조·톤 참고만)

## 10. 마일스톤

| 단계 | 기간  | 내용                                                              | 완료 기준                            |
| ---- | ----- | ----------------------------------------------------------------- | ------------------------------------ |
| 1    | 2~3주 | 스캐폴드 + 인증 + Spring AI 채팅 + SSE 스트리밍 + DESIGN_GUIDE.md | 로그인 → 질문 → 실시간 스트리밍 응답 |
| 2    | 2주   | 영수증 추출·검증·폼·이력·CSV                                      | 회사 영수증 실사용 시작              |
| 3    | 3~4주 | 가이드 이식 + RAG(pgvector)                                       | 내 문서 근거 답변 + 출처 표시        |
| 4    | 2~3주 | Tool Calling + 멀티모델                                           | "지난달 영수증 총액?" AI 조회 응답   |
| 5    | 1주   | Docker Compose + README + 시연 시나리오                           | `docker compose up` + GitHub 공개    |

각 단계는 독립적으로 데모 가능한 상태로 마감한다 (걸어다니는 뼈대 전략 — 중단 시에도 포트폴리오 가치 보존).

## 11. 범위 제외 (YAGNI)

- 좋아요/댓글 (v1 서사로 충분)
- 클라우드 라이브 배포 (Docker Compose까지만)
- 소셜 로그인, 알림, 다국어
- 영수증 외 문서 OCR (명함, 계약서 등)
