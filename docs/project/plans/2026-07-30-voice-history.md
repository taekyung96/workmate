# 회의록 이력 · 오디오 보관 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 분석 후 사라지던 회의록을 이력에서 다시 열어보고, 올린 오디오를 재생하고, 필요 없으면 삭제할 수 있게 한다.

**Architecture:** WAS 가 오디오를 디스크(`uploads/voice`)에 보관하고 `Range` 지원 스트리밍으로 내보낸다. WEB 은 JSON 은 기존 `jsonPassthrough`, 오디오는 `RestClient.exchange()` 로 스트림을 그대로 복사한다. 프론트는 로컬 탭을 라우터 이동형으로 바꿔 관리자·영수증·회의록 세 화면의 탭을 한 벌로 통일한다.

**Tech Stack:** Spring Boot 3.5 / JPA(Hibernate 6.6) / PostgreSQL 17 · Vue 3.5 `<script setup>` / Pinia 3 / vue-router 5 / Tailwind v4 + shadcn-vue

**선행 스펙:** [docs/project/specs/F8-1_VOICE_HISTORY_SPEC.md](../specs/F8-1_VOICE_HISTORY_SPEC.md)

## Global Constraints

- **Java 패키지** 전체 소문자 / **클래스** PascalCase(`~ApiController`·`~Service`·`~ServiceImpl`·`~Vo`·`~Repository`) / **메서드·변수** camelCase / **상수** SNAKE_CASE
- **DB** 소문자 snake_case, 테이블명 단수형. 제약조건 `테이블_컬럼_제약`
- **로깅** `System.out.println` 금지. `@Slf4j` + `log`, `{}` 치환자, 예외 시 `log.error(msg, e)`
- **WAS 응답** `global/response/ApiResponse` 공통. 예외는 `GlobalExceptionHandler` — **컨트롤러 개별 try-catch 금지**
- **스키마** `db/init/*.sql` 로만 관리 (`ddl-auto: validate`) — 엔티티와 SQL 이 정확히 일치해야 기동된다
- **Vue** `<script setup>` Composition API. Options API 금지. 컴포넌트 파일 PascalCase, composable `use` 접두사
- **들여쓰기** 스페이스 4칸
- **커밋 메시지** 변경 내용 요약만. **`Co-Authored-By: Claude` / `Generated with Claude Code` 등 AI 서명·트레일러 금지**
- **모듈 경계** 모듈 간 내부 import 금지. 공통은 `common/` 으로만 공유
- **프론트 테스트 현황** `vitest`·`@vue/test-utils`·`jsdom` 은 설치되어 있으나 **테스트 파일이 하나도 없다.** 이 계획은 순수 로직(Task 9 활성 탭 판정)에만 vitest 를 도입하고, 화면은 `npm run type-check` + 수동 확인으로 검증한다

**검증 명령**

| 대상 | 명령 |
| :--- | :--- |
| WAS 테스트 | `./gradlew :workmate-was:test` |
| WAS 특정 테스트 | `./gradlew :workmate-was:test --tests "com.workmate.was.voice.*"` |
| 프론트 타입 | `cd workmate-vue; npm run type-check` |
| 프론트 테스트 | `cd workmate-vue; npx vitest run` |
| 프론트 포맷 | `cd workmate-vue; npm run format` |

---

## Task 1: 가이드 등록 기능 제거

목적 축소 결정에 따라 `to-guide` 를 전 계층에서 걷어낸다. 다른 작업이 이 코드를 건드리기 전에 먼저 지운다.

**Files:**
- Modify: `workmate-was/src/main/java/com/workmate/was/voice/controller/VoiceApiController.java:44-57`
- Modify: `workmate-was/src/main/java/com/workmate/was/voice/service/VoiceService.java`
- Modify: `workmate-was/src/main/java/com/workmate/was/voice/service/impl/VoiceServiceImpl.java:3-5,30,36,39,81-102`
- Modify: `workmate-web/src/main/java/com/workmate/web/voice/controller/VoiceController.java:44-53`
- Modify: `workmate-web/src/main/java/com/workmate/web/voice/service/VoiceService.java`
- Modify: `workmate-web/src/main/java/com/workmate/web/voice/service/impl/VoiceServiceImpl.java:58-65`
- Modify: `workmate-vue/src/modules/voice/api/voice.api.ts:23-27`
- Modify: `workmate-vue/src/modules/voice/composables/useVoiceAnalyze.ts:15-17,32-44,50,56-58`
- Modify: `workmate-vue/src/modules/voice/views/VoicePage.vue:8-17,26-27,195-214`
- Modify: `docs/project/specs/F8-1_VOICE_MEETING_SUMMARY_SPEC.md:30`

**Interfaces:**
- Consumes: 없음 (첫 작업)
- Produces: `VoiceServiceImpl` 생성자가 3개 인자로 축소 — `VoiceServiceImpl(VoiceTranscriber, VoiceRecordRepository, ChatClient.Builder)`. `GuideService` 의존성 제거

- [ ] **Step 1: WAS 컨트롤러에서 엔드포인트 삭제**

`VoiceApiController.java` 에서 `convertToGuide` 메서드(44-57행)와 이제 쓰이지 않는 import(`PathVariable`)를 삭제한다. `analyze` 메서드만 남는다.

- [ ] **Step 2: WAS 서비스 인터페이스·구현에서 삭제**

`VoiceService.java` 에서 `convertToGuide` 선언을 삭제한다. `VoiceServiceImpl.java` 에서:
- import 3개 삭제: `com.workmate.was.guide.service.GuideService`, `com.workmate.was.guide.vo.GuideResponseVo`, `com.workmate.was.guide.vo.GuideSaveRequestVo`
- 필드 `guideService` 삭제
- 생성자를 아래로 교체

```java
    public VoiceServiceImpl(VoiceTranscriber transcriber,
                            VoiceRecordRepository voiceRecordRepository,
                            ChatClient.Builder chatClientBuilder) {
        this.transcriber = transcriber;
        this.voiceRecordRepository = voiceRecordRepository;
        this.chatClient = chatClientBuilder.build();
    }
```

- `convertToGuide` 메서드(81-102행) 전체 삭제

- [ ] **Step 3: WEB 프록시에서 삭제**

`VoiceController.java` 에서 `convertToGuide` 메서드(44-53행)와 `PathVariable` import 삭제. `VoiceService.java`(WEB) 에서 선언 삭제. `VoiceServiceImpl.java`(WEB) 에서 메서드(58-65행) 삭제.

- [ ] **Step 4: 프론트에서 삭제**

`voice.api.ts` — `convertToGuide` 메서드(23-27행) 삭제.

`useVoiceAnalyze.ts` — `registering`·`registeredGuideSeq` ref(15-17행), `convertToGuide` 함수(32-44행), `reset` 안의 `registeredGuideSeq.value = null`(50행), 반환 객체의 해당 4개 키를 삭제한다. 결과는 아래와 같다.

```ts
    return {
        result,
        loading,
        error,
        analyze,
        reset,
    }
```

`VoicePage.vue` — `BookPlus`·`CheckCircle2` import 와 `RouterLink` import 삭제(더 이상 쓰지 않음), 구조분해에서 `registering`·`registeredGuideSeq`·`convertToGuide` 제거, 우측 패널 헤더의 `RouterLink`/`Button` 블록(195-214행)을 삭제해 제목만 남긴다.

```html
                    <div class="flex items-center justify-between border-b px-4 py-2.5">
                        <span class="text-sm font-semibold">AI 요약 리포트</span>
                    </div>
```

- [ ] **Step 5: 선행 스펙에 폐기 표시**

`F8-1_VOICE_MEETING_SUMMARY_SPEC.md` 의 F8-1-6 행(30행)을 아래로 교체한다.

```markdown
| ~~**F8-1-6**~~ | ~~**사내 지식/채팅 공유**~~ | **폐기(2026-07-30)** — 이 화면은 회의록을 AI로 정리·보관하는 도구로 목적을 축소했다. 가이드(RAG)·채팅 공유는 범위에서 제외. [F8-1_VOICE_HISTORY_SPEC.md](F8-1_VOICE_HISTORY_SPEC.md) 참고 |
```

- [ ] **Step 6: 컴파일·타입 확인**

Run: `./gradlew :workmate-was:compileJava :workmate-web:compileJava`
Expected: BUILD SUCCESSFUL (`GuideService` 참조가 모두 사라져 컴파일 통과)

Run: `cd workmate-vue; npm run type-check`
Expected: 에러 없음

- [ ] **Step 7: 커밋**

```bash
git add workmate-was workmate-web workmate-vue docs
git commit -m "refactor(voice): 회의록 → 가이드 등록 기능 제거

- 이 화면의 목적을 회의록 AI 정리·보관으로 축소
- WAS·WEB·프론트 전 계층에서 to-guide 경로 삭제
- 선행 스펙의 F8-1-6 항목 폐기 표시"
```

---

## Task 2: DB 스키마 · 엔티티 · VO 확장

오디오 파일 정보를 담을 컬럼 4개를 추가하고 엔티티·VO 를 맞춘다.

**Files:**
- Create: `db/init/12-voice-history.sql`
- Modify: `workmate-was/src/main/java/com/workmate/was/voice/vo/VoiceRecord.java`
- Modify: `workmate-was/src/main/java/com/workmate/was/voice/vo/VoiceAnalysisResultVo.java`
- Create: `workmate-was/src/main/java/com/workmate/was/voice/vo/VoiceRecordSummaryVo.java`
- Modify: `workmate-vue/src/modules/voice/types.ts`

**Interfaces:**
- Consumes: Task 1 의 축소된 `VoiceServiceImpl` 생성자
- Produces:
  - `VoiceRecord` 신규 getter: `getAudioFileName()`·`getOriginFileName()`·`getFileSize()`·`getContentType()`, 빌더 필드 `audioFileName`·`originFileName`·`fileSize`·`contentType`
  - `VoiceAnalysisResultVo` 신규 getter: `getOriginFileName()`·`getFileSize()`·`isHasAudio()`
  - `VoiceRecordSummaryVo(VoiceRecord record)` 생성자
  - TS `VoiceRecordSummary` 인터페이스

- [ ] **Step 1: 스키마 SQL 작성**

Create `db/init/12-voice-history.sql`:

```sql
-- =============================================================
-- F8-1 확장: 회의록 이력 · 오디오 보관
-- MVP 는 프라이버시 이유로 오디오를 저장하지 않았으나, 이력에서 "올린 파일이 무엇이었는지"
-- 확인·재생할 수 있어야 한다는 요구에 따라 오디오 원본을 보관하도록 방침을 변경했다.
-- 기존 볼륨 환경에 수동 적용:
--   docker exec -i workmate-db psql -U workmate -d workmate_db < db/init/12-voice-history.sql
-- =============================================================

-- 기존 행(MVP 기간 분석분)은 오디오가 없으므로 전부 nullable 이다.
ALTER TABLE voice_record ADD COLUMN IF NOT EXISTS audio_file_name  varchar(200);  -- 서버 저장 파일명(UUID.ext), 경로는 설정값
ALTER TABLE voice_record ADD COLUMN IF NOT EXISTS origin_file_name varchar(255);  -- 사용자가 올린 원본 파일명
ALTER TABLE voice_record ADD COLUMN IF NOT EXISTS file_size        bigint;        -- 파일 크기(바이트)
ALTER TABLE voice_record ADD COLUMN IF NOT EXISTS content_type     varchar(100);  -- 재생 응답의 Content-Type

-- 목록 조회(사용자별 최신순)는 기존 idx_voice_record_user 가 이미 커버한다.
```

- [ ] **Step 2: 로컬 DB 에 적용**

Run (WSL 도커 사용 시):
```bash
docker exec -i workmate-db psql -U workmate -d workmate_db < db/init/12-voice-history.sql
```
Expected: `ALTER TABLE` 4회 출력

확인: `docker exec -i workmate-db psql -U workmate -d workmate_db -c "\d voice_record"` 로 컬럼 4개가 보이는지.

- [ ] **Step 3: 엔티티에 필드 추가**

`VoiceRecord.java` 의 `summaryMd` 필드 다음에 삽입한다.

```java
    /** 서버에 저장된 오디오 파일명 (UUID.확장자). 저장 경로는 설정값이라 파일명만 남긴다 */
    @Column(name = "audio_file_name", length = 200)
    private String audioFileName;

    /** 사용자가 업로드한 원본 파일명 — 이력 목록에 표시 */
    @Column(name = "origin_file_name", length = 255)
    private String originFileName;

    /** 오디오 파일 크기 (바이트) */
    @Column(name = "file_size")
    private Long fileSize;

    /** 오디오 MIME 타입 — 재생 응답의 Content-Type 으로 사용 */
    @Column(name = "content_type", length = 100)
    private String contentType;
```

- [ ] **Step 4: 응답 VO 확장**

`VoiceAnalysisResultVo.java` 를 아래로 교체한다.

```java
package com.workmate.was.voice.vo;

import java.time.LocalDateTime;
import lombok.Getter;

/** 음성 회의록 분석·상세 응답 VO (F8-1). 전사 원문 + 마크다운 요약 + 오디오 파일 정보. */
@Getter
public class VoiceAnalysisResultVo {

    private final Long recordSeq;
    private final String title;
    private final String sttText;
    private final String summaryMd;
    private final String originFileName;
    private final Long fileSize;
    /** 재생 가능한 오디오 보유 여부 — 파일명 노출 없이 화면이 플레이어 표시 여부를 결정한다 */
    private final boolean hasAudio;
    private final LocalDateTime createdAt;

    public VoiceAnalysisResultVo(VoiceRecord record) {
        this.recordSeq = record.getRecordSeq();
        this.title = record.getTitle();
        this.sttText = record.getSttText();
        this.summaryMd = record.getSummaryMd();
        this.originFileName = record.getOriginFileName();
        this.fileSize = record.getFileSize();
        this.hasAudio = record.getAudioFileName() != null;
        this.createdAt = record.getCreatedAt();
    }
}
```

- [ ] **Step 5: 목록 VO 신설**

Create `workmate-was/src/main/java/com/workmate/was/voice/vo/VoiceRecordSummaryVo.java`:

```java
package com.workmate.was.voice.vo;

import java.time.LocalDateTime;
import lombok.Getter;

/**
 * 회의록 이력 목록 항목 VO (F8-1 확장).
 * 전사 원문·요약 본문은 담지 않는다 — 목록 응답이 수백 KB 로 커지는 것을 막는다.
 */
@Getter
public class VoiceRecordSummaryVo {

    private final Long recordSeq;
    private final String title;
    private final String originFileName;
    private final Long fileSize;
    private final boolean hasAudio;
    private final LocalDateTime createdAt;

    public VoiceRecordSummaryVo(VoiceRecord record) {
        this.recordSeq = record.getRecordSeq();
        this.title = record.getTitle();
        this.originFileName = record.getOriginFileName();
        this.fileSize = record.getFileSize();
        this.hasAudio = record.getAudioFileName() != null;
        this.createdAt = record.getCreatedAt();
    }
}
```

- [ ] **Step 6: 프론트 타입 확장**

`workmate-vue/src/modules/voice/types.ts` 를 아래로 교체한다.

```ts
/** 음성 회의록 분석·상세 결과 (WAS VoiceAnalysisResultVo와 대응) */
export interface VoiceAnalysisResult {
    recordSeq: number
    title: string
    /** STT 전사 원문 */
    sttText: string
    /** AI 구조화 요약 (마크다운) */
    summaryMd: string
    /** 사용자가 올린 원본 파일명 (오디오 미보유 시 null) */
    originFileName: string | null
    /** 파일 크기(바이트, 오디오 미보유 시 null) */
    fileSize: number | null
    /** 재생 가능한 오디오 보유 여부 */
    hasAudio: boolean
    createdAt: string
}

/** 회의록 이력 목록 항목 (WAS VoiceRecordSummaryVo와 대응) */
export interface VoiceRecordSummary {
    recordSeq: number
    title: string
    originFileName: string | null
    fileSize: number | null
    hasAudio: boolean
    createdAt: string
}
```

- [ ] **Step 7: 기동 검증 (ddl-auto: validate 통과 확인)**

Run: `./gradlew :workmate-was:bootRun`
Expected: 기동 성공. `Schema-validation` 에러가 없어야 한다. 에러가 나면 SQL 컬럼명·타입과 엔티티 `@Column` 이 어긋난 것이므로 맞춘다. 확인 후 종료(Ctrl+C).

> `bootRun` 을 쓰는 이유: IDE 실행(`bin/main`)은 `-parameters` 컴파일 옵션이 없어 별개 오류가 날 수 있다. Gradle 은 `build.gradle:12-15` 에서 그 옵션을 붙인다.

- [ ] **Step 8: 커밋**

```bash
git add db/init/12-voice-history.sql workmate-was workmate-vue
git commit -m "feat(voice): 회의록 오디오 파일 정보 컬럼·VO 추가

- voice_record 에 audio_file_name·origin_file_name·file_size·content_type 추가
- 목록 전용 VoiceRecordSummaryVo 신설(전사문·요약 제외로 응답 경량화)
- 상세·분석 응답에 파일 정보와 hasAudio 파생값 노출"
```

---

## Task 3: VoiceAudioStore — 오디오 파일 저장소

파일 저장·조회·삭제를 한 클래스로 묶는다. 서비스에서 분리하면 임시 디렉토리로 단위 테스트가 가능하고, 경로 조립 규칙이 한곳에 모인다.

**Files:**
- Create: `workmate-was/src/main/java/com/workmate/was/voice/service/VoiceAudioStore.java`
- Create: `workmate-was/src/test/java/com/workmate/was/voice/service/VoiceAudioStoreTest.java`
- Modify: `workmate-was/src/main/resources/application.yml`

**Interfaces:**
- Consumes: Task 2 의 엔티티 필드
- Produces:
  - `String store(MultipartFile file)` — 저장 후 생성된 파일명(`{UUID}.{ext}`) 반환
  - `Optional<Resource> load(String fileName)` — 파일이 없으면 `Optional.empty()`
  - `boolean delete(String fileName)` — 지웠으면 `true`, 파일이 없으면 `false`
  - `static String extensionOf(String originalFilename)` — `.m4a` 형태 또는 빈 문자열

- [ ] **Step 1: 실패하는 테스트 작성**

Create `workmate-was/src/test/java/com/workmate/was/voice/service/VoiceAudioStoreTest.java`:

```java
package com.workmate.was.voice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VoiceAudioStoreTest {

    @TempDir
    Path tempDir;

    private VoiceAudioStore store() {
        return new VoiceAudioStore(tempDir.toString());
    }

    @Test
    @DisplayName("원본 파일명에서 확장자를 소문자로 추출한다")
    void extensionOf_extractsLowercase() {
        assertThat(VoiceAudioStore.extensionOf("회의록.M4A")).isEqualTo(".m4a");
        assertThat(VoiceAudioStore.extensionOf("a.b.webm")).isEqualTo(".webm");
    }

    @Test
    @DisplayName("확장자가 없거나 파일명이 없으면 빈 문자열을 반환한다")
    void extensionOf_noExtension_returnsEmpty() {
        assertThat(VoiceAudioStore.extensionOf("확장자없음")).isEmpty();
        assertThat(VoiceAudioStore.extensionOf(null)).isEmpty();
    }

    @Test
    @DisplayName("저장한 파일을 파일명으로 다시 읽고 삭제할 수 있다")
    void store_load_delete_roundTrip() throws IOException {
        VoiceAudioStore store = store();
        MockMultipartFile file = new MockMultipartFile(
                "file", "회의.m4a", "audio/mp4", "audio-bytes".getBytes());

        String saved = store.store(file);

        assertThat(saved).endsWith(".m4a");
        Optional<Resource> loaded = store.load(saved);
        assertThat(loaded).isPresent();
        assertThat(loaded.get().contentLength()).isEqualTo("audio-bytes".length());

        assertThat(store.delete(saved)).isTrue();
        assertThat(store.load(saved)).isEmpty();
    }

    @Test
    @DisplayName("없는 파일 조회는 빈 Optional, 없는 파일 삭제는 false 를 반환한다")
    void load_delete_missingFile() {
        VoiceAudioStore store = store();
        assertThat(store.load("없는파일.m4a")).isEmpty();
        assertThat(store.delete("없는파일.m4a")).isFalse();
    }

    @Test
    @DisplayName("파일명에 경로 구분자나 상위 경로가 섞이면 거부한다 (경로 탈출 차단)")
    void load_pathTraversal_rejected() {
        VoiceAudioStore store = store();
        assertThatThrownBy(() -> store.load("../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.delete("sub/dir.m4a"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :workmate-was:test --tests "com.workmate.was.voice.service.VoiceAudioStoreTest"`
Expected: 컴파일 실패 — `VoiceAudioStore` 클래스가 없음

- [ ] **Step 3: VoiceAudioStore 구현**

Create `workmate-was/src/main/java/com/workmate/was/voice/service/VoiceAudioStore.java`:

```java
package com.workmate.was.voice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * 회의 오디오 파일 저장소 (F8-1 확장).
 *
 * <p>DB 에는 파일명만 남기고 저장 루트는 설정값(app.upload.voice-dir)으로 둔다.
 * 영수증(ReceiptServiceImpl)은 절대경로를 DB 에 넣어 PC 이식·배포 시 경로가 깨지는데,
 * 그 방식을 반복하지 않기 위한 분리다.</p>
 */
@Slf4j
@Component
public class VoiceAudioStore {

    private final Path rootDir;

    public VoiceAudioStore(@Value("${app.upload.voice-dir}") String voiceDir) {
        this.rootDir = Paths.get(voiceDir).toAbsolutePath().normalize();
    }

    /**
     * 업로드된 오디오를 저장하고 생성된 파일명을 반환한다.
     *
     * @param file 업로드 오디오
     * @return 저장된 파일명 ({UUID}.확장자)
     */
    public String store(MultipartFile file) {
        try {
            Files.createDirectories(rootDir);
            String fileName = UUID.randomUUID() + extensionOf(file.getOriginalFilename());
            Path target = rootDir.resolve(fileName);
            file.transferTo(target.toFile());
            log.info("회의 오디오 저장 완료 - 파일: {}, 크기: {} bytes", fileName, file.getSize());
            return fileName;
        } catch (IOException e) {
            log.error("회의 오디오 저장 실패 - 원본: {}", file.getOriginalFilename(), e);
            throw new IllegalStateException("오디오 파일을 저장하지 못했습니다.", e);
        }
    }

    /**
     * 파일명으로 오디오 리소스를 찾는다.
     *
     * @param fileName 저장된 파일명
     * @return 리소스. 파일이 없으면 빈 Optional
     */
    public Optional<Resource> load(String fileName) {
        Path target = resolveSafely(fileName);
        if (!Files.isReadable(target)) {
            log.warn("회의 오디오 파일 없음 - 파일: {}", fileName);
            return Optional.empty();
        }
        return Optional.of(new FileSystemResource(target));
    }

    /**
     * 오디오 파일을 삭제한다.
     *
     * @param fileName 저장된 파일명
     * @return 실제로 삭제했으면 true, 파일이 없었으면 false
     */
    public boolean delete(String fileName) {
        Path target = resolveSafely(fileName);
        try {
            boolean deleted = Files.deleteIfExists(target);
            if (!deleted) {
                log.warn("삭제할 회의 오디오 파일이 이미 없음 - 파일: {}", fileName);
            }
            return deleted;
        } catch (IOException e) {
            log.error("회의 오디오 삭제 실패 - 파일: {}", fileName, e);
            throw new IllegalStateException("오디오 파일을 삭제하지 못했습니다.", e);
        }
    }

    /**
     * 원본 파일명에서 확장자를 소문자로 뽑는다.
     *
     * @param originalFilename 업로드 원본 파일명 (null 허용)
     * @return {@code .m4a} 형태. 확장자가 없으면 빈 문자열
     */
    static String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dot).toLowerCase(Locale.ROOT);
    }

    /**
     * 파일명을 저장 루트 기준으로 안전하게 해석한다.
     * 경로 구분자·상위 경로가 섞인 입력은 저장 루트를 벗어날 수 있어 거부한다.
     *
     * @param fileName 저장된 파일명 (경로 없이 순수 파일명)
     * @return 해석된 절대 경로
     */
    private Path resolveSafely(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("오디오 파일명이 비어 있습니다.");
        }
        Path resolved = rootDir.resolve(fileName).normalize();
        if (!resolved.getParent().equals(rootDir)) {
            log.warn("허용되지 않는 오디오 파일 경로 접근 - 입력: {}", fileName);
            throw new IllegalArgumentException("잘못된 오디오 파일명입니다.");
        }
        return resolved;
    }
}
```

- [ ] **Step 4: 설정값 추가**

`workmate-was/src/main/resources/application.yml` 의 `app:` 블록에 아래를 추가한다 (`crypto:` 다음).

```yaml
    # 업로드 파일 보관 위치 — 회의 오디오(F8-1). DB 에는 파일명만 저장하고 루트는 이 설정값을 쓴다
    # (상대경로는 WAS 실행 디렉토리 기준. 배포 시 절대경로로 덮어쓴다)
    upload:
        voice-dir: uploads/voice
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew :workmate-was:test --tests "com.workmate.was.voice.service.VoiceAudioStoreTest"`
Expected: 5개 테스트 PASS

- [ ] **Step 6: 업로드 디렉토리 git 제외**

루트 `.gitignore` 에 아래가 없으면 추가한다.

```
uploads/
```

- [ ] **Step 7: 커밋**

```bash
git add workmate-was .gitignore
git commit -m "feat(voice): 회의 오디오 파일 저장소(VoiceAudioStore) 추가

- 저장 루트를 app.upload.voice-dir 설정값으로 분리(DB 에는 파일명만)
- 경로 구분자·상위 경로가 섞인 파일명 접근 차단
- @TempDir 기반 단위 테스트 5건"
```

---

## Task 4: 분석 시 오디오 저장 연결

`analyze` 가 전사·요약만 하고 파일을 버리던 흐름에 저장 단계를 넣는다.

**Files:**
- Modify: `workmate-was/src/main/java/com/workmate/was/voice/service/impl/VoiceServiceImpl.java`
- Create: `workmate-was/src/test/java/com/workmate/was/voice/service/impl/VoiceServiceImplTest.java`

**Interfaces:**
- Consumes: `VoiceAudioStore.store(MultipartFile)` (Task 3), `VoiceRecord` 빌더 신규 필드 (Task 2)
- Produces: `VoiceServiceImpl(VoiceTranscriber, VoiceRecordRepository, VoiceAudioStore, ChatClient.Builder)` — 4개 인자 생성자

- [ ] **Step 1: 실패하는 테스트 작성**

Create `workmate-was/src/test/java/com/workmate/was/voice/service/impl/VoiceServiceImplTest.java`:

```java
package com.workmate.was.voice.service.impl;

import com.workmate.was.voice.dao.VoiceRecordRepository;
import com.workmate.was.voice.service.VoiceAudioStore;
import com.workmate.was.voice.service.VoiceTranscriber;
import com.workmate.was.voice.vo.VoiceRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VoiceServiceImplTest {

    private final VoiceTranscriber transcriber = mock(VoiceTranscriber.class);
    private final VoiceRecordRepository repository = mock(VoiceRecordRepository.class);
    private final VoiceAudioStore audioStore = mock(VoiceAudioStore.class);

    /** ChatClient 는 요약 호출에만 쓰이므로 빌더가 mock ChatClient 를 내주도록만 세팅한다 */
    private VoiceServiceImpl service() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(mock(ChatClient.class));
        return new VoiceServiceImpl(transcriber, repository, audioStore, builder);
    }

    @Test
    @DisplayName("파일이 비어 있으면 분석을 거부한다")
    void analyze_emptyFile_throws() {
        VoiceServiceImpl service = service();
        MultipartFile empty = new MockMultipartFile("file", "a.m4a", "audio/mp4", new byte[0]);

        assertThat(catchIllegalArgument(() -> service.analyze(1L, "제목", empty)))
                .contains("오디오 파일을 첨부해주세요");
    }

    @Test
    @DisplayName("저장 시 오디오 파일명·원본명·크기·타입이 함께 기록된다")
    void analyze_persistsAudioMetadata() {
        VoiceServiceImpl service = service();
        MockMultipartFile file = new MockMultipartFile(
                "file", "2026 회의.m4a", "audio/mp4", "bytes".getBytes());
        when(transcriber.transcribe(any(), any())).thenReturn("전사된 텍스트");
        when(audioStore.store(file)).thenReturn("uuid-1.m4a");
        when(repository.save(any(VoiceRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        service.analyze(7L, "아키텍처 회의", file);

        ArgumentCaptor<VoiceRecord> captor = ArgumentCaptor.forClass(VoiceRecord.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        VoiceRecord saved = captor.getValue();
        assertThat(saved.getUserSeq()).isEqualTo(7L);
        assertThat(saved.getTitle()).isEqualTo("아키텍처 회의");
        assertThat(saved.getAudioFileName()).isEqualTo("uuid-1.m4a");
        assertThat(saved.getOriginFileName()).isEqualTo("2026 회의.m4a");
        assertThat(saved.getFileSize()).isEqualTo("bytes".length());
        assertThat(saved.getContentType()).isEqualTo("audio/mp4");
    }

    /** 예외 메시지만 꺼내는 보조 — assertThatThrownBy 체인을 짧게 쓰기 위함 */
    private String catchIllegalArgument(Runnable action) {
        try {
            action.run();
            return "";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }
}
```

> 요약 단계는 mock `ChatClient` 가 `prompt()` 에서 null 을 돌려주므로, 두 번째 테스트는 요약 호출 전까지의 저장 인자를 검증하지 못할 수 있다. Step 3 의 구현은 `summarize` 가 null 을 빈 문자열로 바꾸도록 이미 되어 있으나(`VoiceServiceImpl.java:111`), mock 체인이 NPE 를 던지면 Step 3 에서 `summarize` 를 protected 로 열어 테스트에서 오버라이드한다.

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :workmate-was:test --tests "com.workmate.was.voice.service.impl.VoiceServiceImplTest"`
Expected: 컴파일 실패 — 4개 인자 생성자가 없음

- [ ] **Step 3: 서비스 수정**

`VoiceServiceImpl.java` 에서:

import 추가: `com.workmate.was.voice.service.VoiceAudioStore`

필드·생성자를 아래로 교체한다.

```java
    private final VoiceTranscriber transcriber;
    private final VoiceRecordRepository voiceRecordRepository;
    private final VoiceAudioStore audioStore;
    private final ChatClient chatClient;

    public VoiceServiceImpl(VoiceTranscriber transcriber,
                            VoiceRecordRepository voiceRecordRepository,
                            VoiceAudioStore audioStore,
                            ChatClient.Builder chatClientBuilder) {
        this.transcriber = transcriber;
        this.voiceRecordRepository = voiceRecordRepository;
        this.audioStore = audioStore;
        this.chatClient = chatClientBuilder.build();
    }
```

`analyze` 의 3) 저장 블록을 아래로 교체한다.

```java
        // 3) 오디오 원본 저장 — 이력에서 다시 재생할 수 있어야 하므로 파일을 보관한다
        String audioFileName = audioStore.store(file);

        // 4) 회의록 저장 (DB 에는 파일명만, 저장 루트는 설정값)
        VoiceRecord saved = voiceRecordRepository.save(VoiceRecord.builder()
                .userSeq(userSeq)
                .title(resolveTitle(title))
                .sttText(sttText)
                .summaryMd(summaryMd)
                .audioFileName(audioFileName)
                .originFileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .build());
```

클래스 javadoc 의 흐름 설명도 `(3) 오디오 저장 → (4) 이력 저장` 으로 갱신한다.

`summarize` 메서드 선언을 `private` 에서 `protected` 로 바꾼다 (테스트에서 오버라이드 가능하도록).

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :workmate-was:test --tests "com.workmate.was.voice.service.impl.VoiceServiceImplTest"`
Expected: 2개 테스트 PASS. `summarize` 에서 NPE 가 나면 테스트의 `service()` 를 아래로 바꿔 요약을 우회한다.

```java
    private VoiceServiceImpl service() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(mock(ChatClient.class));
        return new VoiceServiceImpl(transcriber, repository, audioStore, builder) {
            @Override
            protected String summarize(String sttText) {
                return "### 📌 핵심 요약\n- 요약";
            }
        };
    }
```

- [ ] **Step 5: 커밋**

```bash
git add workmate-was
git commit -m "feat(voice): 분석 시 오디오 원본 저장

- VoiceAudioStore 연결, 파일명·원본명·크기·MIME 타입을 이력에 기록
- 저장 인자 검증 단위 테스트 추가"
```

---

## Task 5: 이력 목록 · 상세 API

**Files:**
- Modify: `workmate-was/src/main/java/com/workmate/was/voice/dao/VoiceRecordRepository.java`
- Modify: `workmate-was/src/main/java/com/workmate/was/voice/service/VoiceService.java`
- Modify: `workmate-was/src/main/java/com/workmate/was/voice/service/impl/VoiceServiceImpl.java`
- Modify: `workmate-was/src/main/java/com/workmate/was/voice/controller/VoiceApiController.java`
- Modify: `workmate-was/src/test/java/com/workmate/was/voice/service/impl/VoiceServiceImplTest.java`

**Interfaces:**
- Consumes: `VoiceRecordSummaryVo(VoiceRecord)`·`VoiceAnalysisResultVo(VoiceRecord)` (Task 2)
- Produces:
  - `List<VoiceRecord> findByUserSeqOrderByCreatedAtDesc(Long userSeq)`
  - `List<VoiceRecordSummaryVo> getHistory(Long userSeq)`
  - `VoiceAnalysisResultVo getRecord(Long userSeq, Long recordSeq)`
  - `private VoiceRecord findOwnedRecord(Long userSeq, Long recordSeq)` — Task 6·7 이 재사용

- [ ] **Step 1: 실패하는 테스트 추가**

`VoiceServiceImplTest.java` 에 아래 테스트와 import 를 추가한다.

```java
    @Test
    @DisplayName("이력 목록은 사용자 소유분만 최신순으로 요약 VO 로 반환한다")
    void getHistory_returnsSummaries() {
        VoiceServiceImpl service = service();
        when(repository.findByUserSeqOrderByCreatedAtDesc(7L)).thenReturn(java.util.List.of(
                VoiceRecord.builder().userSeq(7L).title("회의A").sttText("s").summaryMd("m")
                        .audioFileName("uuid-a.m4a").originFileName("a.m4a").fileSize(100L).build(),
                VoiceRecord.builder().userSeq(7L).title("회의B").sttText("s").summaryMd("m").build()));

        var history = service.getHistory(7L);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getTitle()).isEqualTo("회의A");
        assertThat(history.get(0).isHasAudio()).isTrue();
        assertThat(history.get(1).isHasAudio()).isFalse();
    }

    @Test
    @DisplayName("타인의 회의록 상세 조회는 거부된다")
    void getRecord_otherUser_throws() {
        VoiceServiceImpl service = service();
        when(repository.findById(5L)).thenReturn(java.util.Optional.of(
                VoiceRecord.builder().userSeq(99L).title("남의 회의").sttText("s").summaryMd("m").build()));

        assertThat(catchIllegalArgument(() -> service.getRecord(7L, 5L)))
                .contains("본인의 회의록만");
    }

    @Test
    @DisplayName("존재하지 않는 회의록 조회는 거부된다")
    void getRecord_notFound_throws() {
        VoiceServiceImpl service = service();
        when(repository.findById(404L)).thenReturn(java.util.Optional.empty());

        assertThat(catchIllegalArgument(() -> service.getRecord(7L, 404L)))
                .contains("존재하지 않는 회의록");
    }
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :workmate-was:test --tests "com.workmate.was.voice.service.impl.VoiceServiceImplTest"`
Expected: 컴파일 실패 — `getHistory`·`getRecord` 없음

- [ ] **Step 3: 리포지토리에 조회 메서드 추가**

`VoiceRecordRepository.java` 를 아래로 교체한다.

```java
package com.workmate.was.voice.dao;

import com.workmate.was.voice.vo.VoiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 음성 회의록 엔티티 JPA 리포지토리 (F8-1).
 */
public interface VoiceRecordRepository extends JpaRepository<VoiceRecord, Long> {

    /**
     * 사용자의 회의록을 최신순으로 조회한다 (idx_voice_record_user 인덱스 사용).
     *
     * @param userSeq 사용자 식별자
     * @return 최신순 회의록 목록
     */
    List<VoiceRecord> findByUserSeqOrderByCreatedAtDesc(Long userSeq);
}
```

- [ ] **Step 4: 서비스 인터페이스·구현 확장**

`VoiceService.java` 에 선언을 추가한다.

```java
    /**
     * 사용자의 회의록 이력을 최신순으로 조회한다.
     *
     * @param userSeq 사용자 식별자
     * @return 목록 요약 VO (전사문·요약 본문 제외)
     */
    List<VoiceRecordSummaryVo> getHistory(Long userSeq);

    /**
     * 회의록 상세를 조회한다 (본인 소유만).
     *
     * @param userSeq   사용자 식별자
     * @param recordSeq 회의록 식별자
     * @return 전사문·요약 전문을 포함한 상세
     */
    VoiceAnalysisResultVo getRecord(Long userSeq, Long recordSeq);
```

`VoiceServiceImpl.java` 에 구현과 소유권 검증 공통 메서드를 추가한다.

```java
    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<VoiceRecordSummaryVo> getHistory(Long userSeq) {
        return voiceRecordRepository.findByUserSeqOrderByCreatedAtDesc(userSeq).stream()
                .map(VoiceRecordSummaryVo::new)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public VoiceAnalysisResultVo getRecord(Long userSeq, Long recordSeq) {
        return new VoiceAnalysisResultVo(findOwnedRecord(userSeq, recordSeq));
    }

    /**
     * 본인 소유의 회의록을 찾는다. 없거나 타인 소유면 예외.
     * 상세·오디오·삭제가 공유하는 검증 지점이다.
     *
     * @param userSeq   요청자 식별자
     * @param recordSeq 회의록 식별자
     * @return 검증된 회의록
     */
    private VoiceRecord findOwnedRecord(Long userSeq, Long recordSeq) {
        VoiceRecord record = voiceRecordRepository.findById(recordSeq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회의록입니다."));
        if (!record.getUserSeq().equals(userSeq)) {
            log.warn("회의록 접근 거부 - userSeq: {}, recordSeq: {}", userSeq, recordSeq);
            throw new IllegalArgumentException("본인의 회의록만 조회할 수 있습니다.");
        }
        return record;
    }
```

import 추가: `java.util.List`, `com.workmate.was.voice.vo.VoiceRecordSummaryVo`

- [ ] **Step 5: 컨트롤러에 엔드포인트 추가**

`VoiceApiController.java` 에 추가한다 (import: `GetMapping`, `PathVariable`, `List`, `VoiceRecordSummaryVo`).

```java
    /**
     * 내 회의록 이력을 최신순으로 조회한다.
     *
     * @param userSeq 인증 세션에서 WEB 인터셉터가 주입한 사용자 식별자
     * @return 목록 요약 (전사문·요약 본문 제외)
     */
    @GetMapping
    public ApiResponse<List<VoiceRecordSummaryVo>> history(
            @RequestHeader("X-User-Seq") Long userSeq) {
        log.info("회의록 이력 조회 API 호출 - userSeq: {}", userSeq);
        return ApiResponse.success(voiceService.getHistory(userSeq));
    }

    /**
     * 회의록 상세를 조회한다 (본인 소유만).
     *
     * @param userSeq   인증 세션에서 WEB 인터셉터가 주입한 사용자 식별자
     * @param recordSeq 회의록 식별자
     * @return 전사문·요약 전문
     */
    @GetMapping("/{recordSeq}")
    public ApiResponse<VoiceAnalysisResultVo> record(
            @RequestHeader("X-User-Seq") Long userSeq,
            @PathVariable Long recordSeq) {
        log.info("회의록 상세 조회 API 호출 - userSeq: {}, recordSeq: {}", userSeq, recordSeq);
        return ApiResponse.success(voiceService.getRecord(userSeq, recordSeq));
    }
```

- [ ] **Step 6: 테스트 통과 확인**

Run: `./gradlew :workmate-was:test --tests "com.workmate.was.voice.service.impl.VoiceServiceImplTest"`
Expected: 5개 테스트 PASS

- [ ] **Step 7: 커밋**

```bash
git add workmate-was
git commit -m "feat(voice): 회의록 이력 목록·상세 조회 API

- GET /api/v1/voice (목록), GET /api/v1/voice/{recordSeq} (상세)
- 소유권 검증을 findOwnedRecord 로 공통화
- 목록·소유권·미존재 케이스 단위 테스트 추가"
```

---

## Task 6: 오디오 스트리밍 API

`Range` 요청을 처리해 재생 중 구간 이동이 되게 한다.

**Files:**
- Create: `workmate-was/src/main/java/com/workmate/was/voice/vo/VoiceAudioVo.java`
- Modify: `workmate-was/src/main/java/com/workmate/was/voice/service/VoiceService.java`
- Modify: `workmate-was/src/main/java/com/workmate/was/voice/service/impl/VoiceServiceImpl.java`
- Modify: `workmate-was/src/main/java/com/workmate/was/voice/controller/VoiceApiController.java`
- Modify: `workmate-was/src/test/java/com/workmate/was/voice/service/impl/VoiceServiceImplTest.java`

**Interfaces:**
- Consumes: `findOwnedRecord` (Task 5), `VoiceAudioStore.load(String)` (Task 3)
- Produces: `VoiceAudioVo getAudio(Long userSeq, Long recordSeq)` — `getResource()`·`getContentType()` 보유

- [ ] **Step 1: 실패하는 테스트 추가**

`VoiceServiceImplTest.java` 에 추가한다.

```java
    @Test
    @DisplayName("오디오가 없는 회의록의 재생 요청은 거부된다")
    void getAudio_noAudio_throws() {
        VoiceServiceImpl service = service();
        when(repository.findById(5L)).thenReturn(java.util.Optional.of(
                VoiceRecord.builder().userSeq(7L).title("옛 회의").sttText("s").summaryMd("m").build()));

        assertThat(catchIllegalArgument(() -> service.getAudio(7L, 5L)))
                .contains("오디오가 없습니다");
    }

    @Test
    @DisplayName("파일이 사라진 회의록의 재생 요청은 거부된다")
    void getAudio_fileMissing_throws() {
        VoiceServiceImpl service = service();
        when(repository.findById(5L)).thenReturn(java.util.Optional.of(
                VoiceRecord.builder().userSeq(7L).title("회의").sttText("s").summaryMd("m")
                        .audioFileName("gone.m4a").contentType("audio/mp4").build()));
        when(audioStore.load("gone.m4a")).thenReturn(java.util.Optional.empty());

        assertThat(catchIllegalArgument(() -> service.getAudio(7L, 5L)))
                .contains("오디오 파일을 찾을 수 없습니다");
    }
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :workmate-was:test --tests "com.workmate.was.voice.service.impl.VoiceServiceImplTest"`
Expected: 컴파일 실패 — `getAudio` 없음

- [ ] **Step 3: VoiceAudioVo 생성**

Create `workmate-was/src/main/java/com/workmate/was/voice/vo/VoiceAudioVo.java`:

```java
package com.workmate.was.voice.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.io.Resource;

/** 회의 오디오 스트리밍 응답 재료 (F8-1 확장). 리소스와 재생에 필요한 메타를 함께 넘긴다. */
@Getter
@AllArgsConstructor
public class VoiceAudioVo {

    /** 오디오 파일 리소스 */
    private final Resource resource;

    /** 응답 Content-Type — 저장 시 기록한 MIME 타입 */
    private final String contentType;

    /** 사용자가 올린 원본 파일명 (다운로드 파일명으로 사용) */
    private final String originFileName;
}
```

- [ ] **Step 4: 서비스에 getAudio 구현**

`VoiceService.java` 에 선언 추가.

```java
    /**
     * 회의록의 오디오 리소스를 가져온다 (본인 소유만).
     *
     * @param userSeq   사용자 식별자
     * @param recordSeq 회의록 식별자
     * @return 리소스 + Content-Type + 원본 파일명
     */
    VoiceAudioVo getAudio(Long userSeq, Long recordSeq);
```

`VoiceServiceImpl.java` 에 구현 추가 (import: `VoiceAudioVo`, `org.springframework.core.io.Resource`).

```java
    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public VoiceAudioVo getAudio(Long userSeq, Long recordSeq) {
        VoiceRecord record = findOwnedRecord(userSeq, recordSeq);
        if (record.getAudioFileName() == null) {
            throw new IllegalArgumentException("이 회의록에는 저장된 오디오가 없습니다.");
        }
        Resource resource = audioStore.load(record.getAudioFileName())
                .orElseThrow(() -> new IllegalArgumentException("오디오 파일을 찾을 수 없습니다."));
        // Content-Type 이 비어 있던 과거 업로드는 범용 바이너리로 내려 브라우저가 판단하게 한다
        String contentType = record.getContentType() != null
                ? record.getContentType() : "application/octet-stream";
        return new VoiceAudioVo(resource, contentType, record.getOriginFileName());
    }
```

- [ ] **Step 5: 컨트롤러에 Range 지원 엔드포인트 추가**

`VoiceApiController.java` 에 추가한다.

```java
    /**
     * 회의록 오디오를 스트리밍한다 (본인 소유만).
     * Range 요청이 오면 206 Partial Content 로 해당 구간만 내려 재생 중 구간 이동을 지원한다.
     *
     * @param userSeq   인증 세션에서 WEB 인터셉터가 주입한 사용자 식별자
     * @param recordSeq 회의록 식별자
     * @param headers   요청 헤더 (Range 확인용)
     * @return 전체(200) 또는 요청 구간(206) 오디오
     * @throws IOException 리소스 길이 조회 실패 시
     */
    @GetMapping("/{recordSeq}/audio")
    public ResponseEntity<ResourceRegion> audio(
            @RequestHeader("X-User-Seq") Long userSeq,
            @PathVariable Long recordSeq,
            @RequestHeader HttpHeaders headers) throws IOException {
        VoiceAudioVo audio = voiceService.getAudio(userSeq, recordSeq);
        Resource resource = audio.getResource();
        long total = resource.contentLength();

        List<HttpRange> ranges = headers.getRange();
        boolean partial = !ranges.isEmpty();
        ResourceRegion region = partial
                ? ranges.get(0).toResourceRegion(resource)
                : new ResourceRegion(resource, 0, total);

        return ResponseEntity.status(partial ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK)
                .contentType(MediaType.parseMediaType(audio.getContentType()))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(region);
    }
```

import 추가: `org.springframework.core.io.Resource`, `org.springframework.core.io.support.ResourceRegion`, `org.springframework.http.HttpHeaders`, `org.springframework.http.HttpRange`, `org.springframework.http.HttpStatus`, `org.springframework.http.MediaType`, `org.springframework.http.ResponseEntity`, `com.workmate.was.voice.vo.VoiceAudioVo`, `java.io.IOException`

- [ ] **Step 6: 테스트 통과 확인**

Run: `./gradlew :workmate-was:test --tests "com.workmate.was.voice.service.impl.VoiceServiceImplTest"`
Expected: 7개 테스트 PASS

- [ ] **Step 7: 커밋**

```bash
git add workmate-was
git commit -m "feat(voice): 회의록 오디오 스트리밍 API (Range 지원)

- GET /api/v1/voice/{recordSeq}/audio, ResourceRegion 으로 206 처리
- 오디오 미보유·파일 유실 케이스를 명확한 메시지로 거부"
```

---

## Task 7: 삭제 API

**Files:**
- Modify: `workmate-was/src/main/java/com/workmate/was/voice/service/VoiceService.java`
- Modify: `workmate-was/src/main/java/com/workmate/was/voice/service/impl/VoiceServiceImpl.java`
- Modify: `workmate-was/src/main/java/com/workmate/was/voice/controller/VoiceApiController.java`
- Modify: `workmate-was/src/test/java/com/workmate/was/voice/service/impl/VoiceServiceImplTest.java`

**Interfaces:**
- Consumes: `findOwnedRecord` (Task 5), `VoiceAudioStore.delete(String)` (Task 3)
- Produces: `void deleteRecord(Long userSeq, Long recordSeq)`

- [ ] **Step 1: 실패하는 테스트 추가**

`VoiceServiceImplTest.java` 에 추가한다.

```java
    @Test
    @DisplayName("삭제 시 DB 행과 오디오 파일을 함께 지운다")
    void deleteRecord_removesRowAndFile() {
        VoiceServiceImpl service = service();
        VoiceRecord record = VoiceRecord.builder().userSeq(7L).title("회의").sttText("s")
                .summaryMd("m").audioFileName("uuid-x.m4a").build();
        when(repository.findById(5L)).thenReturn(java.util.Optional.of(record));
        when(audioStore.delete("uuid-x.m4a")).thenReturn(true);

        service.deleteRecord(7L, 5L);

        org.mockito.Mockito.verify(audioStore).delete("uuid-x.m4a");
        org.mockito.Mockito.verify(repository).delete(record);
    }

    @Test
    @DisplayName("오디오가 없는 과거 회의록도 예외 없이 삭제된다")
    void deleteRecord_noAudio_deletesRowOnly() {
        VoiceServiceImpl service = service();
        VoiceRecord record = VoiceRecord.builder().userSeq(7L).title("옛 회의").sttText("s")
                .summaryMd("m").build();
        when(repository.findById(5L)).thenReturn(java.util.Optional.of(record));

        service.deleteRecord(7L, 5L);

        org.mockito.Mockito.verify(audioStore, org.mockito.Mockito.never()).delete(any());
        org.mockito.Mockito.verify(repository).delete(record);
    }

    @Test
    @DisplayName("타인의 회의록 삭제는 거부된다")
    void deleteRecord_otherUser_throws() {
        VoiceServiceImpl service = service();
        when(repository.findById(5L)).thenReturn(java.util.Optional.of(
                VoiceRecord.builder().userSeq(99L).title("남의 회의").sttText("s").summaryMd("m").build()));

        assertThat(catchIllegalArgument(() -> service.deleteRecord(7L, 5L)))
                .contains("본인의 회의록만");
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).delete(any());
    }
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :workmate-was:test --tests "com.workmate.was.voice.service.impl.VoiceServiceImplTest"`
Expected: 컴파일 실패 — `deleteRecord` 없음

- [ ] **Step 3: 서비스 구현**

`VoiceService.java` 에 선언 추가.

```java
    /**
     * 회의록을 삭제한다 (본인 소유만). DB 행과 오디오 파일을 함께 지운다.
     *
     * @param userSeq   사용자 식별자
     * @param recordSeq 회의록 식별자
     */
    void deleteRecord(Long userSeq, Long recordSeq);
```

`VoiceServiceImpl.java` 에 구현 추가.

```java
    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteRecord(Long userSeq, Long recordSeq) {
        VoiceRecord record = findOwnedRecord(userSeq, recordSeq);
        // 파일이 이미 없어도(수동 삭제·유실) DB 행 삭제는 진행한다 — store 가 경고 로그만 남긴다
        if (record.getAudioFileName() != null) {
            audioStore.delete(record.getAudioFileName());
        }
        voiceRecordRepository.delete(record);
        log.info("회의록 삭제 완료 - userSeq: {}, recordSeq: {}", userSeq, recordSeq);
    }
```

- [ ] **Step 4: 컨트롤러에 엔드포인트 추가**

`VoiceApiController.java` 에 추가한다 (프로젝트 관례상 `POST /delete`).

```java
    /**
     * 회의록을 삭제한다 (본인 소유만). DB 행과 오디오 파일을 함께 지운다.
     *
     * @param userSeq   인증 세션에서 WEB 인터셉터가 주입한 사용자 식별자
     * @param recordSeq 회의록 식별자
     * @return 성공 응답
     */
    @PostMapping("/{recordSeq}/delete")
    public ApiResponse<Void> delete(
            @RequestHeader("X-User-Seq") Long userSeq,
            @PathVariable Long recordSeq) {
        log.info("회의록 삭제 API 호출 - userSeq: {}, recordSeq: {}", userSeq, recordSeq);
        voiceService.deleteRecord(userSeq, recordSeq);
        return ApiResponse.success();
    }
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew :workmate-was:test --tests "com.workmate.was.voice.service.impl.VoiceServiceImplTest"`
Expected: 10개 테스트 PASS

- [ ] **Step 6: WAS 전체 테스트 확인**

Run: `./gradlew :workmate-was:test`
Expected: BUILD SUCCESSFUL (기존 테스트 회귀 없음)

- [ ] **Step 7: 커밋**

```bash
git add workmate-was
git commit -m "feat(voice): 회의록 삭제 API

- POST /api/v1/voice/{recordSeq}/delete — DB 행 + 오디오 파일 함께 삭제
- 오디오 없는 과거 행·타인 소유 케이스 단위 테스트 추가"
```

---

## Task 8: WEB 프록시

**Files:**
- Modify: `workmate-web/src/main/java/com/workmate/web/voice/service/VoiceService.java`
- Modify: `workmate-web/src/main/java/com/workmate/web/voice/service/impl/VoiceServiceImpl.java`
- Modify: `workmate-web/src/main/java/com/workmate/web/voice/controller/VoiceController.java`

**Interfaces:**
- Consumes: WAS 엔드포인트 4개 (Task 5·6·7)
- Produces:
  - `ResponseEntity<String> history()`·`getRecord(Long)`·`delete(Long)`
  - `void relayAudio(Long recordSeq, String rangeHeader, HttpServletResponse response)`

- [ ] **Step 1: 서비스 인터페이스 확장**

`workmate-web/.../voice/service/VoiceService.java` 에 선언을 추가한다.

```java
    /** 내 회의록 이력 조회 중계 */
    ResponseEntity<String> history();

    /** 회의록 상세 조회 중계 */
    ResponseEntity<String> getRecord(Long recordSeq);

    /** 회의록 삭제 중계 */
    ResponseEntity<String> delete(Long recordSeq);

    /**
     * 회의록 오디오를 WAS 에서 받아 브라우저 응답으로 그대로 흘려보낸다.
     *
     * @param recordSeq   회의록 식별자
     * @param rangeHeader 브라우저가 보낸 Range 헤더 (없으면 null)
     * @param response    서블릿 응답 — 상태코드·헤더·본문을 직접 쓴다
     */
    void relayAudio(Long recordSeq, String rangeHeader, HttpServletResponse response);
```

import: `jakarta.servlet.http.HttpServletResponse`

- [ ] **Step 2: JSON 3개 중계 구현**

`workmate-web/.../voice/service/impl/VoiceServiceImpl.java` 에 추가한다.

```java
    @Override
    public ResponseEntity<String> history() {
        return wasRestClient.get()
                .uri("/api/v1/voice")
                .retrieve()
                .toEntity(String.class);
    }

    @Override
    public ResponseEntity<String> getRecord(Long recordSeq) {
        return wasRestClient.get()
                .uri("/api/v1/voice/{recordSeq}", recordSeq)
                .retrieve()
                .toEntity(String.class);
    }

    @Override
    public ResponseEntity<String> delete(Long recordSeq) {
        log.info("회의록 삭제 프록시 요청. recordSeq: {}", recordSeq);
        return wasRestClient.post()
                .uri("/api/v1/voice/{recordSeq}/delete", recordSeq)
                .retrieve()
                .toEntity(String.class);
    }
```

- [ ] **Step 3: 오디오 스트림 중계 구현**

같은 파일에 추가한다.

```java
    /**
     * {@inheritDoc}
     *
     * <p>WebClient 대신 RestClient.exchange() 를 쓴다. wasWebClient 는 X-User-Seq 자동 주입이
     * 없고(WebClientConfig 주석 참고) 리액티브 스트림 소비가 서블릿 스택에서 번거롭기 때문이다.
     * exchange 콜백 안에서 곧바로 복사하므로 파일 전체가 메모리에 올라가지 않는다.</p>
     */
    @Override
    public void relayAudio(Long recordSeq, String rangeHeader, HttpServletResponse response) {
        wasRestClient.get()
                .uri("/api/v1/voice/{recordSeq}/audio", recordSeq)
                .headers(headers -> {
                    if (rangeHeader != null && !rangeHeader.isBlank()) {
                        headers.set(HttpHeaders.RANGE, rangeHeader);
                    }
                })
                .exchange((request, wasResponse) -> {
                    response.setStatus(wasResponse.getStatusCode().value());
                    copyHeader(wasResponse, response, HttpHeaders.CONTENT_TYPE);
                    copyHeader(wasResponse, response, HttpHeaders.CONTENT_LENGTH);
                    copyHeader(wasResponse, response, HttpHeaders.ACCEPT_RANGES);
                    copyHeader(wasResponse, response, HttpHeaders.CONTENT_RANGE);
                    StreamUtils.copy(wasResponse.getBody(), response.getOutputStream());
                    return null;
                });
        // exchange 는 상태 핸들러를 적용하지 않으므로 4xx·5xx 도 예외 없이 그대로 중계된다.
        // 응답 스트림은 콜백 안에서 전부 소비하며, exchange 가 반환 시 자동으로 닫는다.
    }

    /** WAS 응답 헤더 하나를 브라우저 응답으로 옮긴다 (없으면 아무것도 하지 않음). */
    private void copyHeader(org.springframework.http.client.ClientHttpResponse wasResponse,
                            HttpServletResponse response, String name) {
        String value = wasResponse.getHeaders().getFirst(name);
        if (value != null) {
            response.setHeader(name, value);
        }
    }
```

import 추가: `org.springframework.http.HttpHeaders`, `org.springframework.util.StreamUtils`, `jakarta.servlet.http.HttpServletResponse`

- [ ] **Step 4: 컨트롤러에 엔드포인트 추가**

`workmate-web/.../voice/controller/VoiceController.java` 에 추가한다.

```java
    /**
     * 내 회의록 이력 조회 중계.
     *
     * @return WAS 목록 응답 JSON
     */
    @GetMapping
    public ResponseEntity<String> history() {
        return jsonPassthrough(voiceService.history());
    }

    /**
     * 회의록 상세 조회 중계.
     *
     * @param recordSeq 회의록 식별자
     * @return WAS 상세 응답 JSON
     */
    @GetMapping("/{recordSeq}")
    public ResponseEntity<String> record(@PathVariable Long recordSeq) {
        return jsonPassthrough(voiceService.getRecord(recordSeq));
    }

    /**
     * 회의록 삭제 중계.
     *
     * @param recordSeq 회의록 식별자
     * @return WAS 응답 JSON
     */
    @PostMapping("/{recordSeq}/delete")
    public ResponseEntity<String> delete(@PathVariable Long recordSeq) {
        return jsonPassthrough(voiceService.delete(recordSeq));
    }

    /**
     * 회의록 오디오 스트리밍 중계. Range 헤더를 그대로 전달해 구간 재생을 지원한다.
     *
     * @param recordSeq   회의록 식별자
     * @param rangeHeader 브라우저 Range 헤더 (선택)
     * @param response    서블릿 응답 — 서비스가 직접 기록한다
     */
    @GetMapping("/{recordSeq}/audio")
    public void audio(
            @PathVariable Long recordSeq,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader,
            HttpServletResponse response) {
        voiceService.relayAudio(recordSeq, rangeHeader, response);
    }
```

import 추가: `org.springframework.web.bind.annotation.GetMapping`, `org.springframework.web.bind.annotation.PathVariable`, `org.springframework.web.bind.annotation.RequestHeader`, `org.springframework.http.HttpHeaders`, `jakarta.servlet.http.HttpServletResponse`

- [ ] **Step 5: 컴파일 확인**

Run: `./gradlew :workmate-web:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 수동 통합 확인**

WAS·WEB 을 모두 띄운다.

```bash
./gradlew :workmate-was:bootRun
./gradlew :workmate-web:bootRun
```

브라우저에서 로그인 후 개발자도구 콘솔에서 확인한다.

```js
await (await fetch('/api/v1/voice', { credentials: 'include' })).json()
```
Expected: `{success: true, message: "success", result: [...]}`

이미 분석된 회의록이 있으면 `result` 에 항목이 보이고, MVP 기간 데이터는 `hasAudio: false` 로 나온다.

- [ ] **Step 7: 커밋**

```bash
git add workmate-web
git commit -m "feat(voice): 회의록 이력·상세·삭제·오디오 WEB 프록시

- JSON 3개는 기존 jsonPassthrough 방식 유지
- 오디오는 RestClient.exchange 로 스트림 복사 + Range 헤더 전달"
```

---

## Task 9: 공통 탭 컴포넌트 PageTabs

관리자·영수증·회의록이 공유할 밑줄형 라우터 탭. 활성 판정 로직만 vitest 로 검증한다.

**Files:**
- Create: `workmate-vue/src/common/components/PageTabs.vue`
- Create: `workmate-vue/src/common/components/pageTabs.ts`
- Create: `workmate-vue/src/common/components/pageTabs.spec.ts`
- Modify: `workmate-vue/src/modules/admin/views/AdminUsersPage.vue`
- Modify: `workmate-vue/src/modules/admin/views/AdminAuditLogPage.vue`
- Modify: `workmate-vue/src/modules/admin/views/AdminCommonCodesPage.vue`
- Delete: `workmate-vue/src/modules/admin/components/AdminTabs.vue`

**Interfaces:**
- Consumes: 없음
- Produces:
  - `PageTab` 타입: `{ name: string; label: string; match?: string[] }`
  - `isTabActive(tab: PageTab, currentRouteName: string): boolean`
  - `PageTabs.vue` props: `tabs: PageTab[]`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `workmate-vue/src/common/components/pageTabs.spec.ts`:

```ts
import { describe, it, expect } from 'vitest'
import { isTabActive, type PageTab } from './pageTabs'

describe('isTabActive', () => {
    it('라우트 이름이 탭 이름과 같으면 활성이다', () => {
        const tab: PageTab = { name: 'voice', label: '분석' }
        expect(isTabActive(tab, 'voice')).toBe(true)
        expect(isTabActive(tab, 'voice-history')).toBe(false)
    })

    it('match 에 포함된 라우트 이름도 활성으로 본다 (상세 화면에서 목록 탭 유지)', () => {
        const tab: PageTab = {
            name: 'voice-history',
            label: '이력',
            match: ['voice-history', 'voice-record'],
        }
        expect(isTabActive(tab, 'voice-record')).toBe(true)
        expect(isTabActive(tab, 'voice-history')).toBe(true)
        expect(isTabActive(tab, 'voice')).toBe(false)
    })

    it('라우트 이름이 없으면(초기 렌더) 어떤 탭도 활성이 아니다', () => {
        const tab: PageTab = { name: 'voice', label: '분석' }
        expect(isTabActive(tab, '')).toBe(false)
    })
})
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd workmate-vue; npx vitest run src/common/components/pageTabs.spec.ts`
Expected: FAIL — `./pageTabs` 모듈을 찾을 수 없음

- [ ] **Step 3: 타입·판정 로직 구현**

Create `workmate-vue/src/common/components/pageTabs.ts`:

```ts
/** 공통 페이지 탭 정의 (라우터 이동형) */
export interface PageTab {
    /** 이동할 라우트 name */
    name: string
    /** 탭에 표시할 라벨 */
    label: string
    /**
     * 이 탭을 활성으로 표시할 라우트 name 목록.
     * 하위 화면(예: 이력 상세)에서도 목록 탭이 활성으로 보이게 할 때 쓴다.
     * 미지정 시 name 자체로만 판정한다.
     */
    match?: string[]
}

/**
 * 현재 라우트가 해당 탭에 속하는지 판정한다.
 *
 * @param tab              탭 정의
 * @param currentRouteName 현재 라우트 name (없으면 빈 문자열)
 * @returns 활성 여부
 */
export function isTabActive(tab: PageTab, currentRouteName: string): boolean {
    if (!currentRouteName) return false
    const names = tab.match ?? [tab.name]
    return names.includes(currentRouteName)
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd workmate-vue; npx vitest run src/common/components/pageTabs.spec.ts`
Expected: 3개 테스트 PASS

- [ ] **Step 5: 컴포넌트 작성**

Create `workmate-vue/src/common/components/PageTabs.vue`:

```vue
<script setup lang="ts">
/**
 * 화면 상단 공통 탭 네비게이션 (밑줄형, 라우터 이동).
 * 관리자·영수증·회의록이 같은 모양·동작을 공유한다.
 * 탭 전환이 실제 라우트 이동이므로 새로고침·뒤로가기·링크 공유가 모두 동작한다.
 */
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { isTabActive, type PageTab } from './pageTabs'

const props = defineProps<{ tabs: PageTab[] }>()

const route = useRoute()
const currentName = computed(() => (route.name ? String(route.name) : ''))
</script>

<template>
    <nav class="mb-6 flex gap-1 border-b">
        <RouterLink
            v-for="tab in props.tabs"
            :key="tab.name"
            :to="{ name: tab.name }"
            class="-mb-px border-b-2 px-4 py-2 text-sm font-medium"
            :class="
                isTabActive(tab, currentName)
                    ? 'border-primary text-foreground'
                    : 'border-transparent text-muted-foreground hover:text-foreground'
            "
        >
            {{ tab.label }}
        </RouterLink>
    </nav>
</template>
```

- [ ] **Step 6: 관리자 화면을 공통 컴포넌트로 교체**

세 화면(`AdminUsersPage.vue`·`AdminAuditLogPage.vue`·`AdminCommonCodesPage.vue`)에서 `AdminTabs` import 를 아래로 바꾸고,

```ts
import PageTabs from '@/common/components/PageTabs.vue'

// 관리자 하위 화면 탭 — 사이드바는 관리자 진입점 하나만 유지한다
const adminTabs = [
    { name: 'admin-users', label: '사용자 관리' },
    { name: 'admin-audit-logs', label: '감사 로그' },
    { name: 'admin-common-codes', label: '공통코드' },
]
```

템플릿의 `<AdminTabs />` 를 `<PageTabs :tabs="adminTabs" />` 로 바꾼다.

그다음 `workmate-vue/src/modules/admin/components/AdminTabs.vue` 를 삭제한다.

- [ ] **Step 7: 타입·동작 확인**

Run: `cd workmate-vue; npm run type-check`
Expected: 에러 없음

Run: `cd workmate-vue; npm run dev` → 관리자 화면 3개를 오가며 탭 활성 표시와 이동이 이전과 같은지 확인.

- [ ] **Step 8: 커밋**

```bash
git add workmate-vue
git commit -m "feat(common): 공통 페이지 탭 컴포넌트(PageTabs) 추가

- 밑줄형 라우터 이동 탭을 common/components 로 공통화
- match 옵션으로 하위 화면에서도 상위 탭 활성 유지
- 활성 판정 로직 vitest 테스트 3건 (프로젝트 첫 프론트 테스트)
- AdminTabs 를 PageTabs 로 교체 후 삭제"
```

---

## Task 10: voice 라우트 · store · 분석 화면 분리

**Files:**
- Modify: `workmate-vue/src/modules/voice/routes.ts`
- Create: `workmate-vue/src/modules/voice/stores/voice.store.ts`
- Create: `workmate-vue/src/modules/voice/views/VoiceAnalyzePage.vue`
- Create: `workmate-vue/src/modules/voice/components/VoiceResultPanel.vue`
- Modify: `workmate-vue/src/modules/voice/api/voice.api.ts`
- Delete: `workmate-vue/src/modules/voice/views/VoicePage.vue`
- Delete: `workmate-vue/src/modules/voice/composables/useVoiceAnalyze.ts`

**Interfaces:**
- Consumes: `PageTab` (Task 9), `VoiceAnalysisResult` (Task 2)
- Produces:
  - `useVoiceStore()` — `result`·`loading`·`error`·`analyze(file, title)`·`reset()`
  - `voiceTabs: PageTab[]` (voice 모듈에서 공유)
  - `VoiceResultPanel` props: `record: VoiceAnalysisResult`
  - `voiceApi.history()`·`voiceApi.getRecord(seq)`·`voiceApi.remove(seq)`·`voiceApi.audioUrl(seq)`

- [ ] **Step 1: API 확장**

`voice.api.ts` 에 추가한다 (import 에 `VoiceRecordSummary` 추가).

```ts
    /** 내 회의록 이력 (최신순) */
    async history(): Promise<VoiceRecordSummary[]> {
        const { data } = await client.get<ApiResponse<VoiceRecordSummary[]>>('/v1/voice')
        return data.result
    },

    /** 회의록 상세 (전사문·요약 전문) */
    async getRecord(recordSeq: number): Promise<VoiceAnalysisResult> {
        const { data } = await client.get<ApiResponse<VoiceAnalysisResult>>(`/v1/voice/${recordSeq}`)
        return data.result
    },

    /** 회의록 삭제 (DB 행 + 오디오 파일) */
    async remove(recordSeq: number): Promise<void> {
        await client.post<ApiResponse<void>>(`/v1/voice/${recordSeq}/delete`)
    },

    /**
     * 오디오 스트리밍 URL. <audio src> 에 직접 넣어 브라우저가 Range 요청을 하게 한다
     * (axios 로 받아 Blob 으로 만들면 구간 이동이 안 되므로 URL 을 그대로 쓴다).
     */
    audioUrl(recordSeq: number): string {
        return `/api/v1/voice/${recordSeq}/audio`
    },
```

- [ ] **Step 2: store 작성**

Create `workmate-vue/src/modules/voice/stores/voice.store.ts`:

```ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { extractErrorMessage } from '@/common/utils/error'
import { voiceApi } from '../api/voice.api'
import type { VoiceAnalysisResult } from '../types'

/**
 * 회의록 분석 상태 (F8-1).
 * 탭이 라우터 이동형이라 화면을 벗어나면 컴포넌트가 파괴된다.
 * 수십 초 걸리는 분석 결과를 잃지 않도록 상태를 store 에 둔다.
 */
export const useVoiceStore = defineStore('voice', () => {
    const result = ref<VoiceAnalysisResult | null>(null)
    const loading = ref(false)
    const error = ref('')

    /** 오디오 분석 실행 */
    async function analyze(file: File, title: string): Promise<void> {
        loading.value = true
        error.value = ''
        try {
            result.value = await voiceApi.analyze(file, title)
        } catch (e) {
            error.value = extractErrorMessage(e, '회의록 분석에 실패했습니다.')
        } finally {
            loading.value = false
        }
    }

    /** 결과·에러 초기화 (새로 분석하기) */
    function reset(): void {
        result.value = null
        error.value = ''
    }

    return { result, loading, error, analyze, reset }
})
```

- [ ] **Step 3: 라우트 확장**

`workmate-vue/src/modules/voice/routes.ts` 를 아래로 교체한다.

```ts
import type { RouteRecordRaw } from 'vue-router'
import type { PageTab } from '@/common/components/pageTabs'

/**
 * voice 모듈 라우트 (인증 필요 — 전역 가드가 보호). F8-1.
 * 분석·이력·상세를 각각 라우트로 두어 새로고침·뒤로가기·링크 공유가 동작한다.
 */
export const voiceRoutes: RouteRecordRaw[] = [
    {
        path: '/voice',
        name: 'voice',
        component: () => import('./views/VoiceAnalyzePage.vue'),
    },
    {
        path: '/voice/history',
        name: 'voice-history',
        component: () => import('./views/VoiceHistoryPage.vue'),
    },
    {
        path: '/voice/history/:recordSeq',
        name: 'voice-record',
        component: () => import('./views/VoiceRecordPage.vue'),
    },
]

/** 회의록 화면 공통 탭 — 상세에서도 '이력' 탭이 활성으로 보이게 match 를 준다 */
export const voiceTabs: PageTab[] = [
    { name: 'voice', label: '분석' },
    { name: 'voice-history', label: '이력', match: ['voice-history', 'voice-record'] },
]
```

- [ ] **Step 4: 결과 패널 추출**

Create `workmate-vue/src/modules/voice/components/VoiceResultPanel.vue`:

```vue
<script setup lang="ts">
/**
 * 회의록 결과 2분할 패널 (좌: STT 원문 / 우: AI 요약).
 * 분석 직후 화면과 이력 상세가 이 컴포넌트를 공유하므로,
 * TXT 다운로드가 과거 회의록에서도 그대로 동작한다.
 */
import { computed } from 'vue'
import { Download } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { renderMarkdown } from '@/common/utils/markdown'
import { useMarkdownCopy } from '@/common/composables/useMarkdownCopy'
import type { VoiceAnalysisResult } from '../types'

const props = defineProps<{ record: VoiceAnalysisResult }>()
const { onMarkdownClick } = useMarkdownCopy()

const summaryHtml = computed(() => renderMarkdown(props.record.summaryMd))

/** 전사문 + 요약을 .txt 로 다운로드 */
function downloadTxt(): void {
    const r = props.record
    const content =
        `# ${r.title}\n\n` +
        `===== AI 요약 =====\n${r.summaryMd}\n\n` +
        `===== STT 전사 원문 =====\n${r.sttText}\n`
    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${r.title}.txt`
    a.click()
    URL.revokeObjectURL(url)
}
</script>

<template>
    <div class="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <!-- 좌: STT 원문 -->
        <div class="rounded-lg border">
            <div class="flex items-center justify-between border-b px-4 py-2.5">
                <span class="text-sm font-semibold">STT 전사 원문</span>
                <Button size="sm" variant="outline" @click="downloadTxt">
                    <Download class="mr-1.5 size-4" />
                    TXT 다운로드
                </Button>
            </div>
            <div
                class="slim-scroll max-h-[60vh] overflow-y-auto whitespace-pre-wrap px-4 py-3 text-sm leading-relaxed"
            >
                {{ props.record.sttText }}
            </div>
        </div>

        <!-- 우: AI 구조화 요약 -->
        <div class="rounded-lg border">
            <div class="flex items-center justify-between border-b px-4 py-2.5">
                <span class="text-sm font-semibold">AI 요약 리포트</span>
            </div>
            <div
                class="markdown-body markdown-doc slim-scroll max-h-[60vh] overflow-y-auto px-4 py-3"
                v-html="summaryHtml"
                @click="onMarkdownClick"
            />
        </div>
    </div>
</template>
```

- [ ] **Step 5: 분석 화면 작성**

Create `workmate-vue/src/modules/voice/views/VoiceAnalyzePage.vue` — 기존 `VoicePage.vue` 의 업로드 UI 를 옮기고, 상태를 store 에서 받고, 결과 부분을 `VoiceResultPanel` 로 대체한다.

```vue
<script setup lang="ts">
/**
 * 회의록 [분석] 화면 (/voice, F8-1).
 * 오디오 업로드 → Gemini 전사(STT) + 3단 구조화 요약 → 결과 2분할 표시.
 * 분석 상태는 store 에 있어 이력 탭을 다녀와도 유지된다.
 */
import { ref } from 'vue'
import { FileAudio, Mic, RefreshCw, Upload } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { Input } from '@/common/components/ui/input'
import { Spinner } from '@/common/components/ui/spinner'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import PageTabs from '@/common/components/PageTabs.vue'
import { useVoiceStore } from '../stores/voice.store'
import { voiceTabs } from '../routes'
import VoiceResultPanel from '../components/VoiceResultPanel.vue'

const store = useVoiceStore()

const title = ref('')
const selectedFile = ref<File | null>(null)
const dragOver = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const localError = ref('')

/** 최대 업로드 크기 (WAS max-file-size 와 맞춤) */
const MAX_SIZE = 25 * 1024 * 1024

/** 파일 선택/드롭 공통 검증 처리 */
function acceptFile(file: File | undefined): void {
    localError.value = ''
    if (!file) return
    if (!file.type.startsWith('audio/')) {
        localError.value = '오디오 파일만 업로드할 수 있습니다.'
        return
    }
    if (file.size > MAX_SIZE) {
        localError.value = '파일이 너무 큽니다. 최대 25MB까지 가능합니다.'
        return
    }
    selectedFile.value = file
}

function onDrop(e: DragEvent): void {
    dragOver.value = false
    acceptFile(e.dataTransfer?.files?.[0])
}

function onPick(e: Event): void {
    acceptFile((e.target as HTMLInputElement).files?.[0])
}

/** 분석 실행 */
async function onAnalyze(): Promise<void> {
    if (!selectedFile.value || store.loading) return
    await store.analyze(selectedFile.value, title.value)
}

/** 새로 분석하기 — 입력·결과 초기화 */
function onReset(): void {
    store.reset()
    selectedFile.value = null
    title.value = ''
    localError.value = ''
}

/** 사람이 읽기 좋은 파일 크기 표기 */
function formatSize(bytes: number): string {
    return bytes < 1024 * 1024
        ? `${(bytes / 1024).toFixed(0)} KB`
        : `${(bytes / 1024 / 1024).toFixed(1)} MB`
}
</script>

<template>
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-5xl px-6 py-8">
            <div class="mb-6 flex items-center gap-2">
                <Mic class="size-6" />
                <h1 class="text-2xl font-semibold">회의록 요약</h1>
            </div>

            <PageTabs :tabs="voiceTabs" />

            <!-- 업로드 섹션 -->
            <div class="rounded-lg border p-6">
                <label class="mb-1.5 block text-sm font-medium">회의 제목 (선택)</label>
                <Input
                    v-model="title"
                    placeholder="예: 2026-07-30 아키텍처 회의"
                    class="mb-4 max-w-md"
                />

                <div
                    class="flex flex-col items-center justify-center rounded-lg border-2 border-dashed px-6 py-10 text-center transition-colors"
                    :class="dragOver ? 'border-primary bg-primary/5' : 'border-border'"
                    @dragover.prevent="dragOver = true"
                    @dragleave.prevent="dragOver = false"
                    @drop.prevent="onDrop"
                    @click="fileInput?.click()"
                    role="button"
                >
                    <input
                        ref="fileInput"
                        type="file"
                        accept="audio/*"
                        class="hidden"
                        @change="onPick"
                    />
                    <template v-if="selectedFile">
                        <FileAudio class="mb-2 size-8 text-primary" />
                        <p class="font-medium">{{ selectedFile.name }}</p>
                        <p class="text-sm text-muted-foreground">
                            {{ formatSize(selectedFile.size) }} · 클릭해서 다른 파일 선택
                        </p>
                    </template>
                    <template v-else>
                        <Upload class="mb-2 size-8 text-muted-foreground" />
                        <p class="font-medium">오디오 파일을 여기로 끌어다 놓거나 클릭해서 선택</p>
                        <p class="text-sm text-muted-foreground">
                            mp3 · wav · m4a · webm (최대 25MB)
                        </p>
                    </template>
                </div>

                <Alert v-if="localError || store.error" variant="destructive" class="mt-4">
                    <AlertDescription>{{ localError || store.error }}</AlertDescription>
                </Alert>

                <div class="mt-4 flex items-center gap-2">
                    <Button :disabled="!selectedFile || store.loading" @click="onAnalyze">
                        <Spinner v-if="store.loading" class="mr-2 size-4" />
                        {{ store.loading ? 'AI가 분석 중…' : 'AI 회의록 요약하기' }}
                    </Button>
                    <Button v-if="store.result || selectedFile" variant="outline" @click="onReset">
                        <RefreshCw class="mr-2 size-4" />
                        새로 분석
                    </Button>
                </div>
                <p v-if="store.loading" class="mt-2 text-xs text-muted-foreground">
                    오디오 길이에 따라 수십 초 걸릴 수 있습니다.
                </p>
            </div>

            <VoiceResultPanel v-if="store.result" :record="store.result" class="mt-6" />
        </div>
    </div>
</template>
```

- [ ] **Step 6: 옛 파일 삭제**

`workmate-vue/src/modules/voice/views/VoicePage.vue` 와 `workmate-vue/src/modules/voice/composables/useVoiceAnalyze.ts` 를 삭제한다.

- [ ] **Step 7: 타입 확인**

Run: `cd workmate-vue; npm run type-check`
Expected: `VoiceHistoryPage.vue`·`VoiceRecordPage.vue` 가 아직 없어 라우트에서 에러가 난다 — Task 11·12 에서 생성하므로, 여기서는 두 파일을 최소 형태로 먼저 만들어 통과시킨다.

```vue
<script setup lang="ts">
// Task 11·12 에서 구현
</script>

<template>
    <div />
</template>
```

이 스텁을 `views/VoiceHistoryPage.vue`·`views/VoiceRecordPage.vue` 로 만든 뒤 다시 `npm run type-check` 를 실행해 에러가 없어야 한다.

- [ ] **Step 8: 커밋**

```bash
git add workmate-vue
git commit -m "refactor(voice): 라우터형 탭 구조로 전환 + 분석 상태 store 화

- /voice·/voice/history·/voice/history/:recordSeq 라우트 분리
- 분석 상태를 voice.store 로 옮겨 탭 이동 중에도 결과 유지
- 결과 2분할 뷰를 VoiceResultPanel 로 추출(분석·상세 공유)"
```

---

## Task 11: 이력 목록 화면

**Files:**
- Create: `workmate-vue/src/modules/voice/composables/useVoiceHistory.ts`
- Modify: `workmate-vue/src/modules/voice/views/VoiceHistoryPage.vue` (Task 10 스텁 대체)

**Interfaces:**
- Consumes: `voiceApi.history()`·`voiceApi.remove()` (Task 10), `voiceTabs` (Task 10)
- Produces: `useVoiceHistory()` — `records`·`loading`·`error`·`load()`·`remove(seq)`

- [ ] **Step 1: composable 작성**

Create `workmate-vue/src/modules/voice/composables/useVoiceHistory.ts`:

```ts
import { ref } from 'vue'
import { extractErrorMessage } from '@/common/utils/error'
import { voiceApi } from '../api/voice.api'
import type { VoiceRecordSummary } from '../types'

/**
 * 회의록 이력 화면 로직 (≈ Service).
 * 목록 조회와 삭제를 담당한다.
 *
 * @returns 이력 상태와 액션들
 */
export function useVoiceHistory() {
    const records = ref<VoiceRecordSummary[]>([])
    const loading = ref(false)
    const error = ref('')

    /** 이력 목록 로드 (최신순) */
    async function load(): Promise<void> {
        loading.value = true
        error.value = ''
        try {
            records.value = await voiceApi.history()
        } catch (e) {
            error.value = extractErrorMessage(e, '회의록 이력을 불러오지 못했습니다.')
        } finally {
            loading.value = false
        }
    }

    /** 회의록 삭제 후 목록에서 제거 */
    async function remove(recordSeq: number): Promise<void> {
        error.value = ''
        try {
            await voiceApi.remove(recordSeq)
            records.value = records.value.filter((r) => r.recordSeq !== recordSeq)
        } catch (e) {
            error.value = extractErrorMessage(e, '회의록 삭제에 실패했습니다.')
        }
    }

    return { records, loading, error, load, remove }
}
```

- [ ] **Step 2: 목록 화면 작성**

Replace `workmate-vue/src/modules/voice/views/VoiceHistoryPage.vue`:

```vue
<script setup lang="ts">
/**
 * 회의록 [이력] 화면 (/voice/history, F8-1 확장).
 * 등록된 회의록을 최신순으로 보여주고, 행을 누르면 상세로 이동한다.
 * 오디오는 1건당 최대 25MB 라 건별 삭제를 제공한다.
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Mic, Trash2 } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { Spinner } from '@/common/components/ui/spinner'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/common/components/ui/alert-dialog'
import PageTabs from '@/common/components/PageTabs.vue'
import { useVoiceHistory } from '../composables/useVoiceHistory'
import { voiceTabs } from '../routes'

const router = useRouter()
const { records, loading, error, load, remove } = useVoiceHistory()

const deleteOpen = ref(false)
const deleteTarget = ref<number | null>(null)

onMounted(load)

/** 상세로 이동 */
function openRecord(recordSeq: number): void {
    router.push({ name: 'voice-record', params: { recordSeq } })
}

/** 삭제 확인창 열기 (행 클릭으로 상세 진입하는 것과 섞이지 않게 이벤트 전파를 막는다) */
function askDelete(recordSeq: number): void {
    deleteTarget.value = recordSeq
    deleteOpen.value = true
}

/** 삭제 확정 */
async function confirmDelete(): Promise<void> {
    if (deleteTarget.value === null) return
    await remove(deleteTarget.value)
    deleteTarget.value = null
}

/** YYYY.MM.DD 표기 */
function formatDate(iso: string): string {
    const d = new Date(iso)
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    return `${d.getFullYear()}.${mm}.${dd}`
}

/** 사람이 읽기 좋은 파일 크기 표기 */
function formatSize(bytes: number | null): string {
    if (bytes === null) return ''
    return bytes < 1024 * 1024
        ? `${(bytes / 1024).toFixed(0)} KB`
        : `${(bytes / 1024 / 1024).toFixed(1)} MB`
}
</script>

<template>
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-5xl px-6 py-8">
            <div class="mb-6 flex items-center gap-2">
                <Mic class="size-6" />
                <h1 class="text-2xl font-semibold">회의록 요약</h1>
            </div>

            <PageTabs :tabs="voiceTabs" />

            <p class="mb-3 text-sm text-muted-foreground">
                총 <span class="font-medium text-foreground">{{ records.length }}</span>건
            </p>

            <Alert v-if="error" variant="destructive" class="mb-4">
                <AlertDescription>{{ error }}</AlertDescription>
            </Alert>

            <div v-if="loading" class="flex justify-center py-16">
                <Spinner class="size-6" />
            </div>

            <p
                v-else-if="records.length === 0"
                class="py-16 text-center text-sm text-muted-foreground"
            >
                아직 분석한 회의록이 없습니다.
            </p>

            <div v-else class="overflow-x-auto rounded-lg border">
                <table class="w-full text-sm">
                    <thead class="border-b bg-muted/40 text-left text-muted-foreground">
                        <tr>
                            <th class="px-4 py-2.5 font-medium">제목</th>
                            <th class="px-4 py-2.5 font-medium">파일</th>
                            <th class="px-4 py-2.5 font-medium">생성일</th>
                            <th class="px-4 py-2.5 font-medium"></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr
                            v-for="r in records"
                            :key="r.recordSeq"
                            class="cursor-pointer border-b last:border-0 hover:bg-accent/40"
                            @click="openRecord(r.recordSeq)"
                        >
                            <td class="px-4 py-2.5 font-medium">{{ r.title }}</td>
                            <td class="px-4 py-2.5 text-muted-foreground">
                                <template v-if="r.hasAudio">
                                    {{ r.originFileName }} · {{ formatSize(r.fileSize) }}
                                </template>
                                <span v-else>오디오 없음</span>
                            </td>
                            <td class="px-4 py-2.5 tabular-nums">{{ formatDate(r.createdAt) }}</td>
                            <td class="px-4 py-2.5 text-right">
                                <Button
                                    size="sm"
                                    variant="ghost"
                                    aria-label="회의록 삭제"
                                    @click.stop="askDelete(r.recordSeq)"
                                >
                                    <Trash2 class="size-4" />
                                </Button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <AlertDialog v-model:open="deleteOpen">
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>이 회의록을 삭제할까요?</AlertDialogTitle>
                        <AlertDialogDescription>
                            전사문·요약과 저장된 오디오 파일이 함께 삭제되며 되돌릴 수 없습니다.
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>취소</AlertDialogCancel>
                        <AlertDialogAction @click="confirmDelete">삭제</AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </div>
    </div>
</template>
```

- [ ] **Step 3: 타입 확인**

Run: `cd workmate-vue; npm run type-check`
Expected: 에러 없음

- [ ] **Step 4: 수동 확인**

WAS·WEB 기동 후 `/voice` 에서 오디오를 분석하고, `이력` 탭으로 이동해 방금 건이 보이는지 확인한다. 목록에서 파일명·크기·생성일이 맞는지, 삭제 확인창이 뜨고 삭제 후 행이 사라지는지 확인한다. MVP 기간 데이터는 "오디오 없음" 으로 보여야 한다.

- [ ] **Step 5: 커밋**

```bash
git add workmate-vue
git commit -m "feat(voice): 회의록 이력 목록 화면

- 최신순 목록(제목·파일·생성일) + 행 클릭 상세 이동
- AlertDialog 확인 후 건별 삭제
- 오디오 없는 과거 회의록은 '오디오 없음' 표시"
```

---

## Task 12: 상세 화면 · 오디오 플레이어

**Files:**
- Create: `workmate-vue/src/modules/voice/components/VoiceAudioPlayer.vue`
- Modify: `workmate-vue/src/modules/voice/views/VoiceRecordPage.vue` (Task 10 스텁 대체)

**Interfaces:**
- Consumes: `voiceApi.getRecord()`·`voiceApi.audioUrl()` (Task 10), `VoiceResultPanel` (Task 10)
- Produces: `VoiceAudioPlayer` props: `recordSeq: number`·`originFileName: string | null`·`hasAudio: boolean`

- [ ] **Step 1: 오디오 플레이어 컴포넌트**

Create `workmate-vue/src/modules/voice/components/VoiceAudioPlayer.vue`:

```vue
<script setup lang="ts">
/**
 * 회의 오디오 재생기.
 * <audio> 에 스트리밍 URL 을 직접 물려 브라우저가 Range 요청으로 구간 이동하게 한다.
 * MVP 기간에 저장된 회의록은 오디오가 없어 안내 문구만 보여준다.
 */
import { ref } from 'vue'
import { FileAudio } from 'lucide-vue-next'
import { voiceApi } from '../api/voice.api'

const props = defineProps<{
    recordSeq: number
    originFileName: string | null
    hasAudio: boolean
}>()

const failed = ref(false)
</script>

<template>
    <div class="rounded-lg border px-4 py-3">
        <template v-if="props.hasAudio">
            <div class="mb-2 flex items-center gap-1.5 text-sm text-muted-foreground">
                <FileAudio class="size-4" />
                <span>{{ props.originFileName }}</span>
            </div>
            <p v-if="failed" class="text-sm text-destructive">오디오를 재생할 수 없습니다.</p>
            <audio
                v-else
                class="w-full"
                controls
                preload="metadata"
                :src="voiceApi.audioUrl(props.recordSeq)"
                @error="failed = true"
            />
        </template>
        <p v-else class="text-sm text-muted-foreground">
            이 회의록에는 저장된 오디오가 없습니다.
        </p>
    </div>
</template>
```

- [ ] **Step 2: 상세 화면 작성**

Replace `workmate-vue/src/modules/voice/views/VoiceRecordPage.vue`:

```vue
<script setup lang="ts">
/**
 * 회의록 상세 화면 (/voice/history/:recordSeq, F8-1 확장).
 * 오디오 재생 + 분석 화면과 동일한 결과 2분할을 보여준다.
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Mic } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { Spinner } from '@/common/components/ui/spinner'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import PageTabs from '@/common/components/PageTabs.vue'
import { extractErrorMessage } from '@/common/utils/error'
import { voiceApi } from '../api/voice.api'
import { voiceTabs } from '../routes'
import VoiceResultPanel from '../components/VoiceResultPanel.vue'
import VoiceAudioPlayer from '../components/VoiceAudioPlayer.vue'
import type { VoiceAnalysisResult } from '../types'

const route = useRoute()
const router = useRouter()

const record = ref<VoiceAnalysisResult | null>(null)
const loading = ref(false)
const error = ref('')

const recordSeq = computed(() => Number(route.params.recordSeq))

onMounted(async () => {
    loading.value = true
    try {
        record.value = await voiceApi.getRecord(recordSeq.value)
    } catch (e) {
        error.value = extractErrorMessage(e, '회의록을 불러오지 못했습니다.')
    } finally {
        loading.value = false
    }
})
</script>

<template>
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-5xl px-6 py-8">
            <div class="mb-6 flex items-center gap-2">
                <Mic class="size-6" />
                <h1 class="text-2xl font-semibold">회의록 요약</h1>
            </div>

            <PageTabs :tabs="voiceTabs" />

            <Button variant="ghost" size="sm" class="mb-4" @click="router.back()">
                <ArrowLeft class="mr-1.5 size-4" />
                이력으로
            </Button>

            <Alert v-if="error" variant="destructive">
                <AlertDescription>{{ error }}</AlertDescription>
            </Alert>

            <div v-if="loading" class="flex justify-center py-16">
                <Spinner class="size-6" />
            </div>

            <template v-else-if="record">
                <h2 class="mb-3 text-lg font-semibold">{{ record.title }}</h2>

                <VoiceAudioPlayer
                    class="mb-4"
                    :record-seq="record.recordSeq"
                    :origin-file-name="record.originFileName"
                    :has-audio="record.hasAudio"
                />

                <VoiceResultPanel :record="record" />
            </template>
        </div>
    </div>
</template>
```

- [ ] **Step 3: 타입 확인**

Run: `cd workmate-vue; npm run type-check`
Expected: 에러 없음

- [ ] **Step 4: 수동 확인 (핵심 — Range 동작)**

이력에서 회의록을 열고 확인한다.

1. 오디오가 재생되는지
2. **재생 바 중간을 클릭해 구간 이동이 되는지** (Range 가 동작하지 않으면 여기서 실패한다)
3. 개발자도구 Network 에서 `audio` 요청의 상태코드가 **206** 이고 `Content-Range` 헤더가 있는지
4. 브라우저 주소창에 상세 URL 을 직접 입력해도 화면이 열리는지 (SPA 딥링크 fallback)
5. 새로고침 후에도 "이력" 탭이 활성으로 보이는지 (`match` 옵션)
6. MVP 기간 회의록은 "저장된 오디오가 없습니다" 가 보이는지

- [ ] **Step 5: 커밋**

```bash
git add workmate-vue
git commit -m "feat(voice): 회의록 상세 화면 + 오디오 재생

- 상세에서 오디오 재생(Range 구간 이동) + 결과 2분할 표시
- 오디오 미보유·재생 실패 안내 처리"
```

---

## Task 13: 영수증 라우터형 전환

세 화면의 탭 동작을 한 벌로 맞춘다. UI·로직은 그대로 옮기고 껍데기만 바꾼다.

**Files:**
- Modify: `workmate-vue/src/modules/receipt/routes.ts`
- Create: `workmate-vue/src/modules/receipt/stores/receipt.store.ts`
- Create: `workmate-vue/src/modules/receipt/views/ReceiptAnalyzePage.vue`
- Create: `workmate-vue/src/modules/receipt/views/ReceiptHistoryPage.vue`
- Delete: `workmate-vue/src/modules/receipt/views/ReceiptPage.vue`
- Delete: `workmate-vue/src/modules/receipt/components/ReceiptAnalyzeTab.vue`
- Delete: `workmate-vue/src/modules/receipt/components/ReceiptHistoryTab.vue`

**Interfaces:**
- Consumes: `PageTab`·`PageTabs` (Task 9)
- Produces: `receiptTabs: PageTab[]`, `useReceiptStore()`

- [ ] **Step 1: 기존 분석 탭 상태를 store 로 옮기기**

Create `workmate-vue/src/modules/receipt/stores/receipt.store.ts` — `useReceiptAnalyze.ts` 의 내용을 `defineStore` 로 감싼 것이다. 반환 키를 그대로 유지해 화면 쪽 구조분해를 바꾸지 않는다.

```ts
import { defineStore } from 'pinia'
import { computed, reactive, ref } from 'vue'
import { receiptApi } from '../api/receipt.api'
import { extractErrorMessage } from '@/common/utils/error'
import type { ReceiptAnalysis, ReceiptSaveRequest } from '../types'

/** 업로드 허용 형식·크기 (설계 F3-01) */
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp']
const MAX_BYTES = 10 * 1024 * 1024 // 10MB

/**
 * 영수증 분석 상태.
 * 탭이 라우터 이동형이라 화면을 벗어나면 컴포넌트가 파괴된다.
 * OCR 결과와 사용자가 확인·수정 중인 값을 잃지 않도록 store 에 둔다.
 */
export const useReceiptStore = defineStore('receipt', () => {
    const file = ref<File | null>(null)
    const previewUrl = ref<string | null>(null)
    const analyzing = ref(false)
    const saving = ref(false)
    const saved = ref(false)
    const error = ref<string | null>(null)
    const analysis = ref<ReceiptAnalysis | null>(null)

    // 사용자가 확인·수정하는 최종 값 (분석 결과로 초기화)
    const form = reactive({
        payAmount: null as number | null,
        bizNo: '',
        payDate: '',
        cardName: '',
    })

    /** 저장 가능 조건 — 금액 0 이상, 사업자번호 10자리, 결제일 8자리 (WAS 검증과 동일) */
    const canSave = computed(
        () =>
            !saving.value &&
            form.payAmount !== null &&
            form.payAmount >= 0 &&
            /^\d{10}$/.test(form.bizNo) &&
            /^\d{8}$/.test(form.payDate),
    )

    /** 파일 선택/드롭 처리 — 형식·크기 선검증 후 미리보기 준비 */
    function selectFile(picked: File): void {
        if (!ALLOWED_TYPES.includes(picked.type)) {
            error.value = 'jpg / png / webp 형식만 업로드할 수 있습니다.'
            return
        }
        if (picked.size > MAX_BYTES) {
            error.value = '이미지 크기는 10MB 이하여야 합니다.'
            return
        }
        revokePreview()
        error.value = null
        analysis.value = null
        saved.value = false
        file.value = picked
        previewUrl.value = URL.createObjectURL(picked)
    }

    /** 선택된 이미지를 분석하고 편집 폼을 채운다 */
    async function analyze(): Promise<void> {
        if (!file.value || analyzing.value) return
        analyzing.value = true
        error.value = null
        try {
            const result = await receiptApi.analyze(file.value)
            analysis.value = result
            form.payAmount = result.payAmount
            form.bizNo = result.bizNo ?? ''
            form.payDate = result.payDate ?? ''
            form.cardName = result.cardName ?? ''
        } catch (e) {
            error.value = extractErrorMessage(e, '영수증 분석에 실패했습니다.')
        } finally {
            analyzing.value = false
        }
    }

    /** 확인·수정한 값을 최종 저장 */
    async function save(): Promise<boolean> {
        if (!analysis.value || !canSave.value) return false
        saving.value = true
        error.value = null
        try {
            const payload: ReceiptSaveRequest = {
                imagePath: analysis.value.imagePath,
                payAmount: form.payAmount!,
                bizNo: form.bizNo,
                payDate: form.payDate,
                cardName: form.cardName || null,
                selectType: analysis.value.selectType,
                rawJson: analysis.value.rawJson,
            }
            await receiptApi.save(payload)
            saved.value = true
            return true
        } catch (e) {
            error.value = extractErrorMessage(e, '영수증 저장에 실패했습니다.')
            return false
        } finally {
            saving.value = false
        }
    }

    /** 처음 상태로 초기화 (다른 영수증 분석) */
    function reset(): void {
        revokePreview()
        file.value = null
        analysis.value = null
        saved.value = false
        error.value = null
        form.payAmount = null
        form.bizNo = ''
        form.payDate = ''
        form.cardName = ''
    }

    /** objectURL 메모리 해제 */
    function revokePreview(): void {
        if (previewUrl.value) {
            URL.revokeObjectURL(previewUrl.value)
            previewUrl.value = null
        }
    }

    return {
        file,
        previewUrl,
        analyzing,
        saving,
        saved,
        error,
        analysis,
        form,
        canSave,
        selectFile,
        analyze,
        save,
        reset,
    }
})
```

옮긴 뒤 `workmate-vue/src/modules/receipt/composables/useReceiptAnalyze.ts` 를 삭제한다.

- [ ] **Step 2: 라우트 확장**

`workmate-vue/src/modules/receipt/routes.ts` 를 아래로 교체한다.

```ts
import type { RouteRecordRaw } from 'vue-router'
import type { PageTab } from '@/common/components/pageTabs'

/**
 * receipt 모듈 라우트 (인증 필요 — 전역 가드가 보호).
 * 분석·이력을 각각 라우트로 두어 새로고침·뒤로가기·링크 공유가 동작한다.
 */
export const receiptRoutes: RouteRecordRaw[] = [
    {
        path: '/receipt',
        name: 'receipt',
        component: () => import('./views/ReceiptAnalyzePage.vue'),
    },
    {
        path: '/receipt/history',
        name: 'receipt-history',
        component: () => import('./views/ReceiptHistoryPage.vue'),
    },
]

/** 영수증 화면 공통 탭 */
export const receiptTabs: PageTab[] = [
    { name: 'receipt', label: '분석' },
    { name: 'receipt-history', label: '이력' },
]
```

- [ ] **Step 3: 분석 화면 생성**

Create `workmate-vue/src/modules/receipt/views/ReceiptAnalyzePage.vue` — `ReceiptAnalyzeTab.vue` 의 템플릿을 그대로 쓰고, ① `ReceiptPage.vue` 의 바깥 래퍼·제목을 흡수, ② `PageTabs` 추가, ③ `emit('saved')` 를 라우터 이동으로 교체, ④ 상태를 store 에서 받는다.

```vue
<script setup lang="ts">
/**
 * 영수증 [분석] 화면 (/receipt) — 업로드 → (미리보기) → 분석 → 결과 확인 폼 → 저장.
 * 상태·동작은 receipt store 가 보유하고, 여기선 화면 전환만 담당한다.
 * 저장에 성공하면 방금 등록한 건을 바로 보도록 이력 화면으로 이동한다.
 */
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { CheckCircle2, RotateCcw } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { Badge } from '@/common/components/ui/badge'
import { Spinner } from '@/common/components/ui/spinner'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import PageTabs from '@/common/components/PageTabs.vue'
import { useReceiptStore } from '../stores/receipt.store'
import { receiptTabs } from '../routes'
import ReceiptUpload from '../components/ReceiptUpload.vue'
import CopyField from '../components/CopyField.vue'

const router = useRouter()

const {
    previewUrl,
    analyzing,
    saving,
    saved,
    error,
    analysis,
    form,
    canSave,
    selectFile,
    analyze,
    save,
    reset,
} = useReceiptStore()

// 금액(number|null)을 CopyField(string)와 잇는 양방향 브리지 — 숫자만 남겨 저장값에 반영
const amountStr = computed<string>({
    get: () => (form.payAmount === null ? '' : String(form.payAmount)),
    set: (v) => {
        const digits = v.replace(/[^\d]/g, '')
        form.payAmount = digits === '' ? null : Number(digits)
    },
})

// 사업자번호 체크섬 실패 여부 (분석 결과 기준)
const bizNoInvalid = computed(() => analysis.value?.bizNoValid === false)

async function onSave(): Promise<void> {
    const ok = await save()
    if (ok) router.push({ name: 'receipt-history' })
}
</script>

<template>
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-4xl px-6 py-8">
            <h1 class="mb-6 text-2xl font-semibold">영수증</h1>

            <PageTabs :tabs="receiptTabs" />

            <div class="flex flex-col gap-4">
                <Alert v-if="error" variant="destructive">
                    <AlertDescription>{{ error }}</AlertDescription>
                </Alert>

                <!-- 1) 업로드 전 -->
                <ReceiptUpload v-if="!previewUrl" @select="selectFile" />

                <!-- 2) 이미지 선택됨 · 분석 결과 -->
                <div v-else class="flex flex-col gap-4 md:flex-row md:items-start">
                    <!-- 원본 이미지 미리보기 -->
                    <div class="md:w-2/5">
                        <img
                            :src="previewUrl"
                            alt="영수증 미리보기"
                            class="w-full rounded-lg border object-contain"
                        />
                    </div>

                    <!-- 우측: 분석 전이면 버튼, 분석 후면 확인 폼 -->
                    <div class="flex flex-col gap-4 md:flex-1">
                        <!-- 분석 전 -->
                        <template v-if="!analysis">
                            <div class="flex gap-2">
                                <Button :disabled="analyzing" @click="analyze">
                                    <Spinner v-if="analyzing" class="mr-2 size-4" />
                                    {{ analyzing ? '분석 중…' : '분석하기' }}
                                </Button>
                                <Button variant="outline" :disabled="analyzing" @click="reset">
                                    다시 선택
                                </Button>
                            </div>
                            <p class="text-sm text-muted-foreground">
                                분석하면 결과가 이력에 자동 저장되며, 아래에서 값을 확인·수정할 수
                                있습니다.
                            </p>
                        </template>

                        <!-- 분석 후: 확인 폼 -->
                        <template v-else>
                            <div class="flex items-center gap-2">
                                <Badge v-if="analysis.selectType === 'AUTO'" variant="default">
                                    ✅ 카드 자동 선택
                                </Badge>
                                <Badge v-else variant="secondary">✋ 수동 선택 필요</Badge>
                                <span
                                    v-if="analysis.cardName"
                                    class="text-sm text-muted-foreground"
                                >
                                    {{ analysis.cardName }}
                                </span>
                            </div>

                            <CopyField
                                v-model="amountStr"
                                label="금액"
                                placeholder="숫자만"
                                numeric
                            />
                            <CopyField
                                v-model="form.bizNo"
                                label="사업자등록번호"
                                placeholder="하이픈 없는 10자리"
                                numeric
                            >
                                <template #hint>
                                    <p v-if="bizNoInvalid" class="text-xs text-destructive">
                                        ⚠ 체크섬 검증 실패 — 번호를 확인해 주세요.
                                    </p>
                                </template>
                            </CopyField>
                            <CopyField
                                v-model="form.payDate"
                                label="결제일"
                                placeholder="YYYYMMDD"
                                numeric
                            />
                            <CopyField
                                v-model="form.cardName"
                                label="카드사"
                                placeholder="예: 롯데법인카드"
                            />

                            <div class="flex items-center gap-2">
                                <Button :disabled="!canSave" @click="onSave">
                                    <Spinner v-if="saving" class="mr-2 size-4" />
                                    {{ saving ? '저장 중…' : '저장' }}
                                </Button>
                                <Button variant="outline" @click="reset">
                                    <RotateCcw class="mr-2 size-4" />
                                    다른 영수증
                                </Button>
                                <span
                                    v-if="saved"
                                    class="flex items-center gap-1 text-sm text-green-600"
                                >
                                    <CheckCircle2 class="size-4" /> 저장됨
                                </span>
                            </div>
                        </template>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
```

> **주의: store 를 구조분해할 때** Pinia setup store 의 반환값을 위처럼 그대로 구조분해하면 `ref` 는 반응성을 잃는다. `useReceiptStore()` 는 반환된 `ref` 를 자동 unwrap 하므로, 위 코드처럼 `previewUrl` 을 `.value` 없이 쓰되 **구조분해 대신 `const store = useReceiptStore()` 후 `store.previewUrl` 로 접근**하거나 `storeToRefs` 를 써야 한다. 이 화면은 값을 읽고 쓰는 곳이 많으므로 아래처럼 바꾼다.
>
> ```ts
> import { storeToRefs } from 'pinia'
>
> const store = useReceiptStore()
> const { previewUrl, analyzing, saving, saved, error, analysis, form, canSave } =
>     storeToRefs(store)
> const { selectFile, analyze, save, reset } = store
> ```
>
> `storeToRefs` 로 꺼낸 상태는 템플릿에서 그대로 쓰고, `<script>` 안에서는 `analysis.value` 처럼 접근한다. `form` 은 `reactive` 객체라 `storeToRefs` 결과에서 `form.value.payAmount` 가 되므로, `amountStr` 브리지와 템플릿의 `form.bizNo` 를 `form.value.bizNo` / `form.bizNo` 로 맞춰 `npm run type-check` 가 통과하는 형태로 정리한다.

- [ ] **Step 3-2: 이력 화면 생성**

Create `workmate-vue/src/modules/receipt/views/ReceiptHistoryPage.vue` — `ReceiptHistoryTab.vue` 의 `<script setup>` 과 표 마크업을 그대로 옮기고, 래퍼·제목·`PageTabs` 만 감싼다. `useReceiptHistory` 컴포저블은 변경 없이 계속 쓴다(이력은 라우트 진입 시마다 새로 조회하는 게 맞으므로 store 로 올리지 않는다).

```vue
<script setup lang="ts">
/**
 * 영수증 [이력] 화면 (/receipt/history) — 등록 목록(최신순) + CSV 다운로드.
 * 목록은 결제일·금액·사업자번호·검증상태를 보여준다.
 */
import { onMounted } from 'vue'
import { Download } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { Badge } from '@/common/components/ui/badge'
import { Spinner } from '@/common/components/ui/spinner'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import PageTabs from '@/common/components/PageTabs.vue'
import { useReceiptHistory } from '../composables/useReceiptHistory'
import { receiptTabs } from '../routes'

const { receipts, loading, error, load, downloadCsv } = useReceiptHistory()

onMounted(load)

/** YYYYMMDD → YYYY.MM.DD */
function formatDate(yyyymmdd: string): string {
    if (!/^\d{8}$/.test(yyyymmdd)) return yyyymmdd
    return `${yyyymmdd.slice(0, 4)}.${yyyymmdd.slice(4, 6)}.${yyyymmdd.slice(6, 8)}`
}

/** 10자리 사업자번호 → 123-45-67890 */
function formatBizNo(bizNo: string): string {
    if (!/^\d{10}$/.test(bizNo)) return bizNo
    return `${bizNo.slice(0, 3)}-${bizNo.slice(3, 5)}-${bizNo.slice(5)}`
}

/** 금액 천단위 콤마 */
function formatAmount(amount: number): string {
    return amount.toLocaleString('ko-KR')
}
</script>

<template>
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-4xl px-6 py-8">
            <h1 class="mb-6 text-2xl font-semibold">영수증</h1>

            <PageTabs :tabs="receiptTabs" />

            <div class="flex flex-col gap-4">
                <div class="flex items-center justify-between">
                    <p class="text-sm text-muted-foreground">
                        총 <span class="font-medium text-foreground">{{ receipts.length }}</span
                        >건
                    </p>
                    <Button
                        variant="outline"
                        size="sm"
                        :disabled="receipts.length === 0"
                        @click="downloadCsv"
                    >
                        <Download class="mr-2 size-4" />
                        CSV 다운로드
                    </Button>
                </div>

                <Alert v-if="error" variant="destructive">
                    <AlertDescription>{{ error }}</AlertDescription>
                </Alert>

                <div v-if="loading" class="flex justify-center py-16">
                    <Spinner class="size-6" />
                </div>

                <p
                    v-else-if="receipts.length === 0"
                    class="py-16 text-center text-sm text-muted-foreground"
                >
                    아직 분석한 영수증이 없습니다.
                </p>

                <div v-else class="overflow-x-auto rounded-lg border">
                    <table class="w-full text-sm">
                        <thead class="border-b bg-muted/40 text-left text-muted-foreground">
                            <tr>
                                <th class="px-4 py-2.5 font-medium">결제일</th>
                                <th class="px-4 py-2.5 text-right font-medium">금액</th>
                                <th class="px-4 py-2.5 font-medium">사업자번호</th>
                                <th class="px-4 py-2.5 font-medium">카드사</th>
                                <th class="px-4 py-2.5 font-medium">검증</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr
                                v-for="r in receipts"
                                :key="r.receiptSeq"
                                class="border-b last:border-0 hover:bg-accent/40"
                            >
                                <td class="px-4 py-2.5">{{ formatDate(r.payDate) }}</td>
                                <td class="px-4 py-2.5 text-right tabular-nums">
                                    {{ formatAmount(r.payAmount) }}원
                                </td>
                                <td class="px-4 py-2.5 tabular-nums">
                                    {{ formatBizNo(r.bizNo) }}
                                </td>
                                <td class="px-4 py-2.5">{{ r.cardName || '—' }}</td>
                                <td class="px-4 py-2.5">
                                    <Badge v-if="r.bizNoValid" variant="secondary">✅ 정상</Badge>
                                    <Badge v-else variant="destructive">⚠ 확인필요</Badge>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</template>
```

- [ ] **Step 4: 옛 파일 삭제**

`views/ReceiptPage.vue`·`components/ReceiptAnalyzeTab.vue`·`components/ReceiptHistoryTab.vue` 를 삭제한다.

- [ ] **Step 5: 타입 확인**

Run: `cd workmate-vue; npm run type-check`
Expected: 에러 없음

- [ ] **Step 6: 수동 확인**

`/receipt` 에서 영수증을 분석·저장하면 `/receipt/history` 로 이동하는지, 탭이 회의록·관리자와 같은 모양인지, 분석 중 이력 탭을 다녀와도 진행 상태가 유지되는지, 사이드바의 영수증 항목이 하위 라우트에서도 활성으로 남는지(`AppSidebar.vue:92` 가 `route.path.startsWith('/receipt')` 로 판정하므로 유지되어야 한다) 확인한다.

- [ ] **Step 7: 커밋**

```bash
git add workmate-vue
git commit -m "refactor(receipt): 라우터형 탭 구조로 전환

- /receipt·/receipt/history 라우트 분리, PageTabs 적용
- 분석 상태를 receipt.store 로 옮겨 탭 이동 중에도 유지
- 탭 컴포넌트를 views 로 승격하고 탭 컨테이너 페이지 삭제"
```

---

## Task 14: 문서 갱신

**Files:**
- Modify: `docs/project/specs/F8-1_VOICE_MEETING_SUMMARY_SPEC.md`
- Modify: `docs/development/03_API_DB_SPEC.md`

**Interfaces:**
- Consumes: Task 1~13 의 최종 구현
- Produces: 없음 (문서)

- [ ] **Step 1: 선행 스펙의 프라이버시 방침 갱신**

`F8-1_VOICE_MEETING_SUMMARY_SPEC.md` 의 0번 노트에서 "프라이버시" 항목(10행)을 아래로 교체한다.

```markdown
> - **프라이버시**: ~~오디오 원본은 저장하지 않고 전사문·요약만 DB에 남긴다.~~ → **2026-07-30 변경**: 이력에서 "올린 파일이 무엇이었는지" 확인·재생해야 한다는 요구에 따라 **오디오 원본을 보관**하도록 방침을 바꿨다. 저장 위치는 `app.upload.voice-dir`(기본 `uploads/voice`), DB(`voice_record`)에는 파일명·원본명·크기·MIME 타입만 남긴다. 이력에서 건별 삭제 시 파일도 함께 지운다. 자세한 내용은 [F8-1_VOICE_HISTORY_SPEC.md](F8-1_VOICE_HISTORY_SPEC.md) 참고.
```

같은 노트의 "스코프(1단계)" 항목에서 `3단계: 이력·가이드/채팅 공유 → 미구현` 을 `3단계: 이력 → 구현 완료(2026-07-30). 가이드/채팅 공유 → 폐기` 로 고친다.

- [ ] **Step 2: API·DB 스펙에 엔드포인트·컬럼 반영**

`docs/development/03_API_DB_SPEC.md` 에서 voice 관련 절을 찾아 아래 4개를 추가하고, 제거된 `POST /api/v1/voice/{recordSeq}/to-guide` 를 삭제한다. voice 절이 없으면 문서 형식에 맞춰 새 절을 만든다.

| 메서드 | 경로 | 설명 |
| :--- | :--- | :--- |
| `GET` | `/api/v1/voice` | 내 회의록 이력 (최신순, 본문 제외) |
| `GET` | `/api/v1/voice/{recordSeq}` | 회의록 상세 (전사문·요약 전문) |
| `GET` | `/api/v1/voice/{recordSeq}/audio` | 오디오 스트리밍 (Range → 206) |
| `POST` | `/api/v1/voice/{recordSeq}/delete` | 회의록 삭제 (DB 행 + 오디오 파일) |

`voice_record` 테이블 정의에 컬럼 4개(`audio_file_name`·`origin_file_name`·`file_size`·`content_type`)를 추가한다.

- [ ] **Step 3: 전체 검증**

Run: `./gradlew :workmate-was:test`
Expected: BUILD SUCCESSFUL

Run: `cd workmate-vue; npx vitest run`
Expected: 3개 테스트 PASS

Run: `cd workmate-vue; npm run type-check`
Expected: 에러 없음

Run: `cd workmate-vue; npm run format`
Expected: 포맷 적용 완료

- [ ] **Step 4: 커밋**

```bash
git add docs workmate-vue
git commit -m "docs(voice): 회의록 이력·오디오 보관 반영

- 선행 스펙의 오디오 미저장 방침을 보관으로 변경 기록
- API·DB 스펙에 이력·상세·오디오·삭제 엔드포인트와 신규 컬럼 반영"
```

---

## 완료 기준

| # | 확인 항목 |
| :-- | :--- |
| 1 | `/voice` 에서 분석 → `이력` 탭에 방금 건이 파일명·크기와 함께 보인다 |
| 2 | 이력에서 회의록을 열면 오디오가 재생되고 **재생 바 구간 이동이 된다**(Network 에 206) |
| 3 | 상세 URL 을 새 탭에 붙여넣어도 화면이 열리고 "이력" 탭이 활성이다 |
| 4 | 삭제하면 목록에서 사라지고 `uploads/voice` 의 파일도 없어진다 |
| 5 | MVP 기간 회의록은 "오디오 없음"·"저장된 오디오가 없습니다" 로 표시되고 조회·삭제가 정상이다 |
| 6 | 관리자·영수증·회의록 세 화면의 탭 모양·동작이 같다 |
| 7 | 회의록 화면에 "가이드로 등록" 이 존재하지 않는다 |
| 8 | `./gradlew :workmate-was:test` 와 `npx vitest run` 이 모두 통과한다 |
