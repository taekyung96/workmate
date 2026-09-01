# 백엔드(Spring Boot) 개발 표준 가이드라인 (02_DEVELOPMENT_GUIDE_BACKEND.md)

> **문서 목적**: 본 문서는 WEB/WAS 레이어의 Java 코드 작성 시 준수해야 할 아키텍처, 네이밍, 로깅 및 보안에 대한 **정적(Static) 원칙**을 정의합니다.
> **AI 지시사항**: AI는 새로운 클래스, 메서드, 쿼리를 작성할 때 반드시 이 문서의 규칙을 최우선으로 적용해야 합니다.
>
> **규칙 2계층 구분** (인수인계자용):
>
> - **★ 불변 표준 (변경 금지)**: §3 네이밍, §4 로깅, §5 보안·암호화, 패키지 계층 순서(controller→service(+impl)→dao→vo).
> - **⚠️ 프로젝트 관례 (조정 가능)**: §7 ApiResponse 포맷 세부, §8 Thymeleaf Fragment 규칙.
>
> 이 프로젝트는 원본 스킬 가이드에서 **DB 접근 방식 "JPA만"** 을 선택했으므로 MyBatis 혼용 규칙은 제거되었다.

## 1. 아키텍처 및 레이어 역할 표준 (3-tier)

본 프로젝트는 라우팅을 담당하는 **WEB 레이어**와 비즈니스 로직 및 DB 통신을 담당하는 **WAS 레이어**로 분리된 3계층 아키텍처를 따릅니다.

- **WEB 레이어 (`workmate-web`, :8080)**:
    - 역할: 사용자 요청 접수, Thymeleaf HTML 화면 라우팅, WAS API 프록시 호출.
    - 제약: **데이터베이스에 직접 접근할 수 없으며**, 반드시 `RestClient`(`global/config/RestClientConfig`의 `wasRestClient` 빈)를 통해 WAS 레이어와 통신해야 합니다.
    - 화면의 fetch 는 WEB(8080)의 동일 경로 프록시 컨트롤러를 호출하고, 프록시가 WAS 로 중계한다 (브라우저가 8081을 직접 보지 않는다).
- **WAS 레이어 (`workmate-was`, :8081)**:
    - 역할: REST API 제공, 핵심 비즈니스 로직, 영속성(DB) 관리, Spring AI(Gemini·pgvector) 연동.
    - 제약: 화면(HTML)을 직접 반환하지 않고, 오직 JSON 형태의 데이터만 반환합니다.

## 2. 영속성(DB) 관리 규격 — JPA만

- 모든 CRUD 는 **Spring Data JPA**(`@Entity` + `~Repository`)로 처리한다.
- 스키마는 Flyway 마이그레이션(`workmate-was/src/main/resources/db/migration`)으로만 관리하며 `ddl-auto: validate` 를 유지한다 (Hibernate 임의 변경 금지).
- JPA 로 표현이 어려운 특수 연산(예: `vector_store` 의 jsonb 조건 삭제)만 `JdbcTemplate` 으로 보조한다 — 이때도 SQL 은 서비스 구현체 내부에 두고 로그를 남긴다.

## 3. 네이밍 규칙 (Naming Conventions)

### 3.1 Java 코드 네이밍

- **패키지**: 특수문자 없이 전체 소문자. (예: `com.workmate.was.receipt.service`)
- **클래스**: 파스칼 케이스 (PascalCase).
    - WEB 컨트롤러: `~Controller` (예: `ReceiptController` — 화면 라우팅·프록시)
    - WAS API 컨트롤러: `~ApiController` (예: `ReceiptApiController`) — "Api가 붙으면 REST"가 전 프로젝트 공통 규칙
    - 서비스 및 데이터 객체: `~Service`, `~ServiceImpl`, `~Vo`/`~Request`/`~Response`(DTO), `~Entity`, `~Repository`
    - global 공통 클래스: 설정 `~Config`, 프로퍼티 바인딩 `~Properties`, 예외 처리 `~Handler`, 인증 부품 `~Provider`/`~Filter`, 기동 훅 `~Runner`
- **메서드/변수**: 카멜 케이스 (camelCase)
- **상수**: SNAKE_CASE 대문자

### 3.2 관계형 데이터베이스 (RDBMS) 공통 네이밍

> DB 종류와 무관하게 **모든 데이터베이스 요소는 소문자 snake_case** 를 절대 표준으로 사용합니다.

- **테이블/컬럼**: 소문자 snake_case, 테이블명은 단수형 (`receipt`, `guide` — `receipts` 아님)
- **제약조건**: `테이블명_컬럼명_제약조건` — PK `~_pk` / FK `~_fk` / Unique `~_uk` / Index `idx_~`
    - (예: `receipt_receipt_seq_pk`, `idx_vector_store_embedding`)

## 4. 로깅(Logging) 코딩 룰

- **금지**: `System.out.println()` 사용 엄금 (테스트 코드 포함).
- **표준**: Lombok `@Slf4j` + `log` 객체.
- **문자열 결합 금지**: `+` 대신 `{}` 치환자 사용.
- **예외 추적**: `log.error(메시지, e)` 로 반드시 예외 객체를 마지막 파라미터로 전달.
- 예외의 공통 로깅·응답 변환은 `global/exception/GlobalExceptionHandler` 가 담당한다 — 컨트롤러마다 try-catch 로 로그를 찍지 않는다.

## 5. 공통 보안 및 암호화 규격

- **비밀번호**: `BCryptPasswordEncoder` 단방향 암호화 필수 (로그인 도메인 도입 시).
- **민감 개인정보 (이메일, 연락처 등)**: `AES-256` 양방향 암호화 (`@Convert` 컨버터).
- **접근 제어**: Spring Security 6.x (도입 시) — 정적 자원(`/css/**`, `/js/**`, `/common-ui/**`)과 로그인 페이지는 `permitAll()`, 비즈니스 영역은 `authenticated()`.
- **비밀값**: 코드/yml 평문 금지 — `.env`(환경변수)와 git 미추적 `application-local.yml` 로만 관리.

## 6. 디렉토리 및 패키지 구조 표준

새로운 도메인(기능) 개발 시 아래 구조를 반드시 준수합니다.

    workmate-web/src/main/
     ├── java/com/workmate/web/
     │    ├── [도메인명]/ (예: receipt)
     │    │    ├── controller/          # Thymeleaf 뷰 반환 + WAS 프록시 Controller
     │    │    ├── service/             # WAS API 통신 인터페이스
     │    │    │    └── impl/           # WAS API 통신 구현체 (wasRestClient 사용)
     │    │    └── vo/                  # View 계층용 데이터 전송 객체 (필요 시)
     │    └── global/
     │         └── config/              # RestClientConfig, WasProperties, (도입 시) SecurityConfig
     └── resources/
          ├── static/
          │    ├── css/                 # common.css(전 페이지 공통) + {페이지명}.css(페이지 전용)
          │    ├── js/                  # Fragment 전용 JS (03 가이드 §5.6)
          │    └── common-ui/           # workmate-vue 에서 빌드된 UMD 모듈 (빌드 산출물, 커밋 대상)
          └── templates/
               ├── fragments/           # 재사용 레이아웃 프래그먼트 (2개 이상 페이지 공유 시)
               └── *.html               # 페이지 템플릿

    workmate-was/src/main/
     ├── java/com/workmate/was/
     │    ├── [도메인명]/ (예: receipt, guide)
     │    │    ├── controller/          # REST API Controller (~ApiController)
     │    │    ├── service/             # 비즈니스 로직 (인터페이스 없이 단일 클래스면 service/ 직접 배치 허용)
     │    │    ├── domain/              # Entity + Repository (JPA)
     │    │    └── dto/                 # 요청/응답 DTO
     │    └── global/
     │         ├── config/              # 공통 설정 클래스
     │         ├── response/            # ApiResponse<T> 공통 응답 래퍼 (§7)
     │         └── exception/           # GlobalExceptionHandler + BusinessException (§7)
     └── resources/
          └── application*.yml          # 공통 + 프로파일(local/dev/prod)

> 참고: 원본 스킬 표준은 `dao/`+`vo/` 구조이나, 이 프로젝트는 JPA 중심이라 `domain/`(Entity·Repository)+`dto/` 구조를 관례로 채택했다 (기존 receipt·guide 도메인과 통일할 것).

## 7. API 공통 응답 규격 및 예외 처리 (WAS 레이어)

- **공통 응답 객체**: 모든 WAS API 응답은 `com.workmate.was.global.response.ApiResponse<T>` (`success`/`message`/`result`) 로 반환한다.
- **글로벌 예외 처리**: 컨트롤러 개별 try-catch 금지. `global/exception/GlobalExceptionHandler` (`@RestControllerAdvice`) 가 모든 예외를 공통 포맷으로 변환한다.
    - `BusinessException` → 409 (비즈니스 규칙 위반)
    - `IllegalArgumentException` → 400 (입력값 오류)
    - 그 외 → 500 (상세 메시지는 로그에만)
- WEB 프록시는 WAS 의 상태코드·본문을 가공 없이 통과시키고, WAS 연결 불가 시에만 502 + 동일 포맷 JSON 을 반환한다.

## 8. Thymeleaf 레이아웃 분리(Fragment) 규칙

- 2개 이상의 페이지에서 반복되는 UI 블록(사이드바/헤더/푸터/공통 모달)은 `templates/fragments/` 아래 `th:fragment` 로 분리하고 `th:replace` 로 포함한다 (`th:insert` 지양).
- **판단 기준**: 한 페이지에만 쓰이는 UI는 분리하지 않는다. (현재는 단일 페이지라 Fragment 없음 — 가이드 화면 추가 시 헤더부터 분리할 것)
- Fragment 파일에는 `<script>` 블록을 두지 않는다 — 인터랙션 JS 는 `static/js/{fragment명}.js` 전용 파일로 분리한다 (03 가이드 §5.6).
- Fragment 내 조건 렌더링은 `th:if`, 모델 접근은 `${...}` + Safe Navigation(`?.`) 을 사용한다.
