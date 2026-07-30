# [F8-1 확장] 회의록 이력 · 오디오 보관 설계서 🎙️🗂️

> **작성일**: 2026-07-30
> **선행 문서**: [F8-1_VOICE_MEETING_SUMMARY_SPEC.md](F8-1_VOICE_MEETING_SUMMARY_SPEC.md)
> **성격**: F8-1 MVP 이후 3단계(이력) 구현 설계 + 방향 정정

---

## 0. 배경 — 왜 지금 이력인가

F8-1 MVP는 **쓰기만 되고 읽기가 없는 상태**로 끝났다.

| 확인 지점 | 상태 |
| :--- | :--- |
| `db/init/09-voice-schema.sql` | `voice_record` 테이블 존재 |
| `VoiceServiceImpl.java:70-75` | 분석할 때마다 DB 저장 **되고 있음** |
| `VoiceRecordRepository.java` | 빈 인터페이스 — 조회 메서드 0개 |
| `VoiceApiController.java` | 엔드포인트 2개(`analyze`·`to-guide`) — 목록 조회 없음 |
| `VoicePage.vue:5` | 주석에 `(실시간 녹음·이력·공유는 다음 단계)` |

즉 분석마다 DB에 행이 쌓이지만 사용자는 그것을 **영원히 볼 수 없다.** 새로고침하면 `recordSeq`도 잃는다.

선행 스펙 5.2절에 `GET /api/v1/voice/history` 가 이미 명세되어 있었으나 구현되지 않았고, 그 위에 얹는 응용 기능(가이드 등록)이 먼저 구현되어 **순서가 뒤바뀐 상태**였다.

## 1. 방향 정정 — 이 페이지의 목적 축소

**이 화면은 "회의 녹음을 AI로 간략하게 정리하고 다시 확인하는 도구"다.** 사내 지식베이스(guide)·RAG 와는 무관하다.

따라서 직전 커밋(`a9a0b57`)으로 들어간 **회의록 → 가이드 등록(F8-1-6)을 전부 제거한다.**

| 제거 대상 | 위치 |
| :--- | :--- |
| WAS 엔드포인트 | `VoiceApiController.java:51-57` (`POST /{recordSeq}/to-guide`) |
| WAS 서비스 | `VoiceServiceImpl.java:84-102` (`convertToGuide`) + `GuideService` 의존성 |
| WEB 프록시 | `VoiceController.java:50-53`, `web/.../voice/service/impl/VoiceServiceImpl.java:58-65` |
| 프론트 API | `voice.api.ts:24-27` |
| 프론트 상태 | `useVoiceAnalyze.ts:15-17, 32-44` (`registering`·`registeredGuideSeq`) |
| 프론트 UI | `VoicePage.vue:195-214` ("가이드로 등록" 버튼 / "가이드에서 보기" 링크) |
| 스펙 항목 | 선행 스펙 2절 **F8-1-6** — 폐기로 표시 |

> 제거된 코드는 커밋 `a9a0b57` 에 남아 있어 필요 시 복원 가능하다.

## 2. 확정된 결정 사항

| # | 결정 | 근거 |
| :-- | :--- | :--- |
| 1 | **오디오 원본을 보관하고 재생 가능하게 한다** | "올린 파일이 어떤 건지 확인" 요구. 선행 스펙의 "오디오 미저장" 프라이버시 방침을 **의도적으로 뒤집는 결정**이며, 해당 문서 0번 노트도 함께 갱신한다 |
| 2 | **오디오 서빙은 디스크 보관 + `Range` 스트리밍 중계** | 재생 중 구간 이동(시킹)이 오디오의 기본 기대치. `byte[]` 통째 중계는 25MB가 메모리에 올라가고 시킹이 불안정. DB `bytea` 는 pgvector DB를 무겁게 함 |
| 3 | **탭은 라우터 이동형으로 통일** | URL이 상태를 가져 새로고침·뒤로가기·링크 공유가 동작. 관리자 탭과 메커니즘까지 일치 |
| 4 | **영수증도 함께 라우터형으로 전환** | 세 화면(관리자·영수증·회의록)의 모양·동작·URL 규칙을 한 벌로 맞춘다 |
| 5 | **이력에서 건별 삭제** | 1건당 최대 25MB 오디오가 쌓이므로 사용자에게 통제권 필요. 스케줄러 없이 구현 가능 |
| 6 | **파일 경로는 설정값 + 파일명만 DB 저장** | `ReceiptServiceImpl.java:93` 은 절대경로를 DB에 넣어 PC 이식·배포 시 전부 깨진다. 이 실수를 반복하지 않는다 |

## 3. DB 스키마 (`db/init/12-voice-history.sql`)

`voice_record` 에 컬럼 4개를 추가한다. `ddl-auto: validate` 이므로 엔티티와 정확히 일치해야 한다.

```sql
ALTER TABLE voice_record ADD COLUMN IF NOT EXISTS audio_file_name  varchar(200);
ALTER TABLE voice_record ADD COLUMN IF NOT EXISTS origin_file_name varchar(255);
ALTER TABLE voice_record ADD COLUMN IF NOT EXISTS file_size        bigint;
ALTER TABLE voice_record ADD COLUMN IF NOT EXISTS content_type     varchar(100);
```

| 컬럼 | 타입 | 용도 |
| :--- | :--- | :--- |
| `audio_file_name` | `varchar(200)` | 서버 저장 이름 (`{UUID}.m4a`). **파일명만** — 경로는 설정값 |
| `origin_file_name` | `varchar(255)` | 사용자가 올린 원본 이름. 목록에 표시 |
| `file_size` | `bigint` | 바이트. 목록에 `8.2MB` 로 표기 |
| `content_type` | `varchar(100)` | 재생 응답의 `Content-Type` 에 사용 |

**전부 nullable.** MVP 기간에 쌓인 기존 행은 오디오가 없으므로, 이력에서 재생 컨트롤 대신 "오디오 없음"을 표시한다.

기존 인덱스 `idx_voice_record_user(user_seq, created_at DESC)` 가 이미 목록 조회(사용자별 최신순)를 커버하므로 추가 인덱스는 없다.

## 4. 백엔드 설계

### 4.1 WAS 엔드포인트

삭제는 프로젝트 관례(`GuideServiceImpl.java:54`·`ChatServiceImpl.java:55`)를 따라 `POST /delete` 를 쓴다.

| 메서드 | 경로 | 응답 | 비고 |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/voice` | `List<VoiceRecordSummaryVo>` | 전사문·요약 본문 **제외** (목록 응답 경량화) |
| `GET` | `/api/v1/voice/{recordSeq}` | `VoiceAnalysisResultVo` | 전문 포함. 분석 응답과 동일 형태 |
| `GET` | `/api/v1/voice/{recordSeq}/audio` | `ResponseEntity<Resource>` | `Range` 지원. `Content-Type` 은 저장된 값 |
| `POST` | `/api/v1/voice/{recordSeq}/delete` | `ApiResponse<Void>` | DB 행 + 오디오 파일 함께 삭제 |

**신규** `VoiceRecordSummaryVo` 필드: `recordSeq` · `title` · `originFileName` · `fileSize` · `hasAudio` · `createdAt`

**기존 `VoiceAnalysisResultVo` 확장** — 현재 필드(`recordSeq`·`title`·`sttText`·`summaryMd`·`createdAt`)만으로는 상세 화면이 오디오 플레이어를 그릴 수 없다. `originFileName` · `fileSize` · `hasAudio` 를 추가한다. 이 VO 는 분석 응답에도 쓰이므로, 분석 직후 화면에서도 방금 올린 오디오를 바로 재생할 수 있게 된다.

`hasAudio` 는 컬럼이 아니라 **파생값**이다. VO 생성 시 `audioFileName != null` 로 계산한다. 프론트에 파일 실체 존재 여부를 노출하지 않기 위해 `audioFileName` 자체는 응답에 담지 않는다(재생은 `recordSeq` 로만 요청).

프론트 `modules/voice/types.ts` 의 `VoiceAnalysisResult` 인터페이스도 같은 3개 필드를 추가하고, `VoiceRecordSummary` 인터페이스를 신설한다.

### 4.2 소유권 검증

`VoiceServiceImpl.java:86-91` 의 검증 로직을 private 공통 메서드로 추출해 조회·상세·오디오·삭제 4곳이 공유한다. 타인 회의록 접근은 거부하고 `log.warn` 을 남긴다.

### 4.3 파일 저장

- 저장 루트: `app.upload.voice-dir` (기본 `uploads/voice`) — `application.yml` 에 추가
- 파일명: `{UUID}.{원본확장자}`
- DB: **파일명만** 저장. 조회 시 `설정 루트 + 파일명` 으로 조합
- 삭제: DB 행 삭제와 함께 파일도 삭제. 파일이 이미 없으면 경고 로그만 남기고 DB 삭제는 진행

### 4.4 WEB 프록시

| 대상 | 방식 |
| :--- | :--- |
| 목록·상세·삭제 | 기존 `wasRestClient` + `jsonPassthrough` 그대로 |
| 오디오 | `wasWebClient` 로 무버퍼 relay + `Range` 요청 헤더 전달, 응답의 `Content-Type`·`Content-Length`·`Accept-Ranges`·`Content-Range` 유지 |

`wasWebClient` 는 채팅 SSE 중계(`ChatServiceImpl.java:30`)가 이미 쓰는 빈이므로 **새 의존성이 필요 없다.**

## 5. 프론트엔드 설계

### 5.1 라우트 (평면 구조 — `admin/routes.ts` 관례)

| 경로 | name | 화면 |
| :--- | :--- | :--- |
| `/voice` | `voice` | 회의록 분석 |
| `/voice/history` | `voice-history` | 이력 목록 |
| `/voice/history/:recordSeq` | `voice-record` | 회의록 상세 |
| `/receipt` | `receipt` | 영수증 분석 |
| `/receipt/history` | `receipt-history` | 영수증 이력 |

### 5.2 공통 탭 컴포넌트 `common/components/PageTabs.vue`

- 모양: **밑줄형** — `AdminTabs.vue:20-34` 의 Tailwind 클래스를 그대로 계승
- 동작: `RouterLink` 기반 라우터 이동 (단일 모드)
- props: `tabs: { name: string; label: string; match?: string[] }[]`
- `match` 는 그 탭을 활성으로 표시할 라우트 name 목록. 상세 화면(`voice-record`)에서도 "이력" 탭이 활성으로 보이게 한다. 미지정 시 `name` 자체로만 판정
- `AdminTabs.vue` 는 이 컴포넌트로 교체(삭제)

### 5.3 상태 (Pinia)

라우터 이동 시 컴포넌트가 파괴되므로, 진행 중인 분석 결과를 잃지 않도록 분석 상태를 store 로 올린다. 위치는 기존 관례(`modules/*/stores/*.store.ts`)를 따른다.

- `modules/voice/stores/voice.store.ts` — 분석 진행/결과/에러
- `modules/receipt/stores/receipt.store.ts` — 영수증 분석 진행/결과/확인 단계 값

### 5.4 컴포넌트 구조

```
modules/voice/
  views/
    VoiceAnalyzePage.vue      기존 VoicePage 의 업로드·분석 UI 이동
    VoiceHistoryPage.vue      신규 — 목록 표 + 삭제
    VoiceRecordPage.vue       신규 — 상세
  components/
    VoiceResultPanel.vue      신규 — 결과 2분할 뷰 추출 (분석/상세 공유)
    VoiceAudioPlayer.vue      신규 — <audio> + 오디오 없을 때 안내
  stores/voice.store.ts       신규
  composables/
    useVoiceAnalyze.ts        store 사용으로 수정
    useVoiceHistory.ts        신규 — 목록·상세·삭제
```

`VoicePage.vue` 는 `VoiceAnalyzePage.vue` 로 대체되어 삭제된다.

**`VoiceResultPanel.vue` 추출이 핵심.** 분석 직후 화면과 과거 상세가 같은 컴포넌트를 쓰므로, TXT 다운로드가 모든 과거 회의록에서 자동으로 동작한다.

### 5.4.1 영수증 모듈 전환

라우터형 전환에 맞춰 기존 탭 컨테이너 구조를 라우트 단위 화면으로 바꾼다. 내용(UI·로직)은 그대로 옮기고 껍데기만 교체한다.

```
modules/receipt/
  views/
    ReceiptAnalyzePage.vue    ReceiptAnalyzeTab.vue 내용 이동 (신규)
    ReceiptHistoryPage.vue    ReceiptHistoryTab.vue 내용 이동 (신규)
  components/
    ReceiptAnalyzeTab.vue     → 삭제 (views 로 승격)
    ReceiptHistoryTab.vue     → 삭제 (views 로 승격)
    ReceiptUpload.vue         유지
    CopyField.vue             유지
  stores/receipt.store.ts     신규 — 분석 진행/결과/확인 단계 값
  views/ReceiptPage.vue       → 삭제 (탭 컨테이너 역할 소멸)
```

기존 `ReceiptAnalyzeTab.vue` 의 `@saved` 이벤트(저장 성공 시 이력 탭으로 전환)는 **`router.push({ name: 'receipt-history' })` 로 대체**한다.

### 5.5 이력 목록 표시

| 열 | 내용 |
| :--- | :--- |
| 제목 | `title`. 클릭 시 상세로 이동 |
| 파일 | `originFileName` · `fileSize` (`8.2MB`). 오디오 없으면 `—` |
| 생성일 | `createdAt` (`2026.07.30`) |
| 삭제 | 삭제 버튼 — `common/composables/useDialog` 확인창 |

빈 목록일 때 "아직 분석한 회의록이 없습니다." 를 표시한다(`ReceiptHistoryTab.vue:62-67` 패턴).

영수증 이력과 달리 **페이징·CSV 다운로드는 넣지 않는다.** 회의록은 건수가 적고 표 형태 내보내기의 의미가 없다.

## 6. 에러 처리

기존 경로를 그대로 사용한다.

- WAS: `global/exception/GlobalExceptionHandler` (컨트롤러 개별 try-catch 금지)
- 프론트: `extractErrorMessage` + `Alert` 컴포넌트
- 오디오 재생 실패: `<audio>` 의 `error` 이벤트를 잡아 "오디오를 재생할 수 없습니다." 표시

## 7. 테스트

| 대상 | 검증 |
| :--- | :--- |
| 소유권 | 타인 `userSeq` 의 회의록 상세·오디오·삭제 요청이 모두 거부되는지 |
| 삭제 | DB 행 삭제와 함께 `uploads/voice` 의 파일이 실제로 사라지는지 |
| 파일 부재 | 오디오 파일이 없는 상태로 삭제 요청 시 예외 없이 DB 행만 지워지는지 |
| 레거시 행 | `audio_file_name` 이 `NULL` 인 기존 행의 목록·상세 조회가 정상 동작하는지 |

`build.gradle:50-56` 이 테스트 JVM 에 AES 더미 키를 주입하므로 컨텍스트 로딩 설정은 추가 작업이 없다.

## 8. 범위에서 제외

- 브라우저 실시간 녹음(F8-1-1) — 별도 단계
- 회의록 → 채팅 공유(F8-1-6 의 나머지 절반) — 목적 축소로 폐기
- 오디오 자동 만료(N일 후 삭제) — 건별 삭제로 충분
- 이력 페이징·검색 — 건수가 늘면 그때 추가
