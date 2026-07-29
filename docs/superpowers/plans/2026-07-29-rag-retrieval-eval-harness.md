# RAG 검색 품질 평가 하네스 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `GuideRetriever`의 실제 검색을 대상으로 Hit@K·MRR·Miss rate를 측정하고, `topK×threshold`를 스윕해 최적값 튜닝 흔적을 마크다운 리포트로 남기는 평가 하네스를 만든다.

**Architecture:** 순수 함수(메트릭·리포트 렌더)와 통합 러너(@SpringBootTest)를 분리한다. 순수 함수는 결정적 단위 테스트로 검증하고, 러너는 실제 dev DB·실제 임베딩으로 읽기 전용 검색해 스윕을 오케스트레이션한다. 프로덕션 코드는 건드리지 않는다(스윕은 ReflectionTestUtils로 필드 주입).

**Tech Stack:** Java 17, Spring Boot 3.5, Spring AI pgvector, JUnit5(@Tag), Jackson, AssertJ, Gradle.

**설계 문서:** `docs/superpowers/specs/2026-07-29-rag-retrieval-eval-harness-design.md`

---

## 파일 구조

모든 코드는 **테스트 소스셋**(`workmate-was/src/test`)에 둔다 — 평가는 프로덕션 산출물이 아니다. 패키지 `com.workmate.was.rageval`.

| 파일                                                           | 책임                                            |
| -------------------------------------------------------------- | ----------------------------------------------- |
| `src/test/java/com/workmate/was/rageval/EvalQuery.java`        | 골든셋 한 문항 (record)                         |
| `src/test/java/com/workmate/was/rageval/GoldenSetLoader.java`  | `queries.json` → `List<EvalQuery>`              |
| `src/test/java/com/workmate/was/rageval/RetrievalMetrics.java` | Hit@K·MRR·Miss rate 계산 (순수 함수)            |
| `src/test/java/com/workmate/was/rageval/EvalReportWriter.java` | 결과 → 마크다운 렌더 + 파일 쓰기                |
| `src/test/java/com/workmate/was/rageval/RagEvalRunner.java`    | 스윕 오케스트레이션 (@SpringBootTest @Tag)      |
| `src/test/resources/rageval/queries.json`                      | 실제 골든셋 (15문항+)                           |
| `src/test/resources/rageval/sample-queries.json`               | 로더 테스트용 소형 픽스처                       |
| `workmate-was/build.gradle`                                    | `ragEval` 태스크 추가 + 기본 test에서 태그 제외 |
| `docs/development/rag-eval/REPORT-*.md`                        | 실행 산출물 (러너가 생성)                       |

---

## Task 1: 골든셋 레코드 + 로더

**Files:**

- Create: `workmate-was/src/test/java/com/workmate/was/rageval/EvalQuery.java`
- Create: `workmate-was/src/test/java/com/workmate/was/rageval/GoldenSetLoader.java`
- Create: `workmate-was/src/test/resources/rageval/sample-queries.json`
- Test: `workmate-was/src/test/java/com/workmate/was/rageval/GoldenSetLoaderTest.java`

- [ ] **Step 1: 로더 테스트용 픽스처 작성**

Create `workmate-was/src/test/resources/rageval/sample-queries.json`:

```json
[
    {
        "question": "도커랑 쿠버네티스 차이가 뭐야?",
        "expectedTitles": ["Docker vs Kubernetes"]
    },
    {
        "question": "RAG가 뭔지 설명해줘",
        "expectedTitles": ["RAG란 무엇인가", "RAG 개요"]
    }
]
```

- [ ] **Step 2: EvalQuery 레코드 작성**

```java
package com.workmate.was.rageval;

import java.util.List;

/**
 * 평가 골든셋의 한 문항.
 *
 * @param question       사용자 질문
 * @param expectedTitles 정답으로 인정하는 가이드 제목들(하나라도 검색되면 hit)
 */
public record EvalQuery(String question, List<String> expectedTitles) {
}
```

- [ ] **Step 3: 실패하는 로더 테스트 작성**

```java
package com.workmate.was.rageval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoldenSetLoaderTest {

    @Test
    @DisplayName("classpath JSON 을 EvalQuery 리스트로 로드한다")
    void loads_json() {
        List<EvalQuery> queries = new GoldenSetLoader().load("rageval/sample-queries.json");

        assertThat(queries).hasSize(2);
        assertThat(queries.get(0).question()).isEqualTo("도커랑 쿠버네티스 차이가 뭐야?");
        assertThat(queries.get(0).expectedTitles()).containsExactly("Docker vs Kubernetes");
        assertThat(queries.get(1).expectedTitles()).hasSize(2);
    }

    @Test
    @DisplayName("리소스가 없으면 IllegalArgumentException")
    void missing_resource_throws() {
        assertThatThrownBy(() -> new GoldenSetLoader().load("rageval/none.json"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 4: 테스트 실행 → 실패 확인**

Run: `./gradlew :workmate-was:test --tests "com.workmate.was.rageval.GoldenSetLoaderTest"`
Expected: FAIL — `GoldenSetLoader` 클래스 없음(컴파일 에러)

- [ ] **Step 5: GoldenSetLoader 구현**

```java
package com.workmate.was.rageval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * classpath 의 골든셋 JSON 을 로드한다.
 */
public class GoldenSetLoader {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * @param classpathResource 예: "rageval/queries.json"
     * @return 파싱된 문항 리스트
     */
    public List<EvalQuery> load(String classpathResource) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalArgumentException("골든셋 리소스를 찾을 수 없습니다: " + classpathResource);
            }
            return mapper.readValue(in, new TypeReference<List<EvalQuery>>() {});
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
```

- [ ] **Step 6: 테스트 실행 → 통과 확인**

Run: `./gradlew :workmate-was:test --tests "com.workmate.was.rageval.GoldenSetLoaderTest"`
Expected: PASS (2 tests)

- [ ] **Step 7: 커밋**

```bash
git add workmate-was/src/test/java/com/workmate/was/rageval/EvalQuery.java \
        workmate-was/src/test/java/com/workmate/was/rageval/GoldenSetLoader.java \
        workmate-was/src/test/java/com/workmate/was/rageval/GoldenSetLoaderTest.java \
        workmate-was/src/test/resources/rageval/sample-queries.json
git commit -m "test(rag-eval): 골든셋 레코드 + JSON 로더"
```

---

## Task 2: 검색 메트릭 (순수 함수)

**Files:**

- Create: `workmate-was/src/test/java/com/workmate/was/rageval/RetrievalMetrics.java`
- Test: `workmate-was/src/test/java/com/workmate/was/rageval/RetrievalMetricsTest.java`

- [ ] **Step 1: 실패하는 메트릭 테스트 작성**

```java
package com.workmate.was.rageval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RetrievalMetricsTest {

    @Test
    @DisplayName("Hit·MRR·Miss 를 문항 집합에 대해 집계한다")
    void computes_aggregate() {
        // 1) 1위에 정답 → hit, rr=1.0
        var c1 = new RetrievalMetrics.EvalCase(List.of("RAG란 무엇인가", "Docker vs Kubernetes"), Set.of("RAG란 무엇인가"));
        // 2) 2위에 정답 → hit, rr=0.5
        var c2 = new RetrievalMetrics.EvalCase(List.of("Docker vs Kubernetes", "RAG란 무엇인가"), Set.of("RAG란 무엇인가"));
        // 3) 빈 결과 → miss, hit 아님, rr=0
        var c3 = new RetrievalMetrics.EvalCase(List.of(), Set.of("LangChain 개요"));

        RetrievalMetrics.ComboMetrics m = RetrievalMetrics.compute(List.of(c1, c2, c3));

        assertThat(m.total()).isEqualTo(3);
        assertThat(m.hitRate()).isCloseTo(2.0 / 3, within(1e-9));  // 2/3 hit
        assertThat(m.mrr()).isCloseTo((1.0 + 0.5 + 0.0) / 3, within(1e-9));
        assertThat(m.missRate()).isCloseTo(1.0 / 3, within(1e-9));
    }

    @Test
    @DisplayName("빈 입력은 0 으로 방어한다")
    void empty_input_is_zero() {
        RetrievalMetrics.ComboMetrics m = RetrievalMetrics.compute(List.of());
        assertThat(m.total()).isZero();
        assertThat(m.hitRate()).isZero();
    }
}
```

- [ ] **Step 2: 테스트 실행 → 실패 확인**

Run: `./gradlew :workmate-was:test --tests "com.workmate.was.rageval.RetrievalMetricsTest"`
Expected: FAIL — `RetrievalMetrics` 없음(컴파일 에러)

- [ ] **Step 3: RetrievalMetrics 구현**

```java
package com.workmate.was.rageval;

import java.util.List;
import java.util.Set;

/**
 * 검색 품질 메트릭 계산 (순수 함수 — DB·Spring 의존 없음).
 * 메트릭은 retrieve() 가 이미 topK 로 잘라 반환한 **청크 title 시퀀스**로 계산한다.
 */
public final class RetrievalMetrics {

    private RetrievalMetrics() {
    }

    /**
     * 한 문항의 평가 입력.
     *
     * @param returnedTitles 검색 반환 청크의 title(유사도 순, 중복 가능)
     * @param expectedTitles 정답 title 집합
     */
    public record EvalCase(List<String> returnedTitles, Set<String> expectedTitles) {
    }

    /** 한 (topK,threshold) 조합의 집계 지표 */
    public record ComboMetrics(double hitRate, double mrr, double missRate, int total) {
    }

    /**
     * 문항들을 집계해 Hit@K·MRR·Miss rate 를 낸다.
     *
     * @param cases 문항별 반환/정답
     * @return 집계 지표 (빈 입력이면 전부 0)
     */
    public static ComboMetrics compute(List<EvalCase> cases) {
        if (cases.isEmpty()) {
            return new ComboMetrics(0, 0, 0, 0);
        }
        int n = cases.size();
        double hits = 0, rrSum = 0, misses = 0;
        for (EvalCase c : cases) {
            if (c.returnedTitles().isEmpty()) {
                misses++;
            }
            // 위에서부터 훑어 정답과 일치하는 첫 청크의 위치로 역순위(1/rank) 계산
            for (int i = 0; i < c.returnedTitles().size(); i++) {
                if (c.expectedTitles().contains(c.returnedTitles().get(i))) {
                    rrSum += 1.0 / (i + 1);
                    hits++;
                    break;
                }
            }
        }
        return new ComboMetrics(hits / n, rrSum / n, misses / n, n);
    }
}
```

- [ ] **Step 4: 테스트 실행 → 통과 확인**

Run: `./gradlew :workmate-was:test --tests "com.workmate.was.rageval.RetrievalMetricsTest"`
Expected: PASS (2 tests)

- [ ] **Step 5: 커밋**

```bash
git add workmate-was/src/test/java/com/workmate/was/rageval/RetrievalMetrics.java \
        workmate-was/src/test/java/com/workmate/was/rageval/RetrievalMetricsTest.java
git commit -m "test(rag-eval): 검색 메트릭(Hit@K·MRR·Miss) 순수 함수"
```

---

## Task 3: 리포트 렌더러

**Files:**

- Create: `workmate-was/src/test/java/com/workmate/was/rageval/EvalReportWriter.java`
- Test: `workmate-was/src/test/java/com/workmate/was/rageval/EvalReportWriterTest.java`

- [ ] **Step 1: 실패하는 렌더 테스트 작성**

```java
package com.workmate.was.rageval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvalReportWriterTest {

    @Test
    @DisplayName("스윕 결과를 마크다운 표로 렌더한다")
    void renders_markdown_table() {
        var metrics = new RetrievalMetrics.ComboMetrics(0.8, 0.65, 0.1, 20);
        var result = new EvalReportWriter.SweepResult(4, 0.4, metrics);
        var meta = new EvalReportWriter.CorpusMeta(18, 20, LocalDate.of(2026, 7, 29));

        String md = new EvalReportWriter().render(List.of(result), meta);

        assertThat(md).contains("가이드 개수: 18");
        assertThat(md).contains("평가 문항 수: 20");
        assertThat(md).contains("| topK | threshold | Hit@K | MRR | Miss rate |");
        // 4, 0.40, 80.0%, 0.650, 10.0%
        assertThat(md).contains("| 4 | 0.40 | 80.0% | 0.650 | 10.0% |");
    }
}
```

- [ ] **Step 2: 테스트 실행 → 실패 확인**

Run: `./gradlew :workmate-was:test --tests "com.workmate.was.rageval.EvalReportWriterTest"`
Expected: FAIL — `EvalReportWriter` 없음(컴파일 에러)

- [ ] **Step 3: EvalReportWriter 구현**

```java
package com.workmate.was.rageval;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * 스윕 결과를 마크다운 리포트로 렌더하고 파일로 저장한다.
 * render(순수)와 write(IO)를 분리해 렌더는 결정적으로 테스트 가능하게 한다.
 */
public class EvalReportWriter {

    /** 한 (topK,threshold) 조합의 결과 행 */
    public record SweepResult(int topK, double threshold, RetrievalMetrics.ComboMetrics metrics) {
    }

    /** 리포트 상단 코퍼스 메타 */
    public record CorpusMeta(int guideCount, int queryCount, LocalDate runDate) {
    }

    /**
     * 마크다운 문자열 생성 (파일 IO 없음).
     */
    public String render(List<SweepResult> results, CorpusMeta meta) {
        StringBuilder sb = new StringBuilder();
        sb.append("# RAG 검색 품질 평가 리포트\n\n");
        sb.append("- 실행일: ").append(meta.runDate()).append("\n");
        sb.append("- 가이드 개수: ").append(meta.guideCount()).append("\n");
        sb.append("- 평가 문항 수: ").append(meta.queryCount()).append("\n\n");
        sb.append("| topK | threshold | Hit@K | MRR | Miss rate |\n");
        sb.append("| ---: | ---: | ---: | ---: | ---: |\n");
        for (SweepResult r : results) {
            // Locale.ROOT 로 소수점(.) 고정 — 지역설정이 콤마여도 표가 깨지지 않게
            sb.append(String.format(Locale.ROOT, "| %d | %.2f | %.1f%% | %.3f | %.1f%% |\n",
                    r.topK(), r.threshold(),
                    r.metrics().hitRate() * 100, r.metrics().mrr(), r.metrics().missRate() * 100));
        }
        return sb.toString();
    }

    /**
     * 렌더된 마크다운을 `dir/REPORT-<date>.md` 로 저장한다.
     *
     * @return 저장된 파일 경로
     */
    public Path write(String markdown, Path dir, LocalDate date) throws IOException {
        Files.createDirectories(dir);
        Path file = dir.resolve("REPORT-" + date + ".md");
        Files.writeString(file, markdown);
        return file;
    }
}
```

- [ ] **Step 4: 테스트 실행 → 통과 확인**

Run: `./gradlew :workmate-was:test --tests "com.workmate.was.rageval.EvalReportWriterTest"`
Expected: PASS (1 test)

- [ ] **Step 5: 커밋**

```bash
git add workmate-was/src/test/java/com/workmate/was/rageval/EvalReportWriter.java \
        workmate-was/src/test/java/com/workmate/was/rageval/EvalReportWriterTest.java
git commit -m "test(rag-eval): 마크다운 리포트 렌더러"
```

---

## Task 4: 실제 골든셋 작성 (실제 가이드 기준)

**Files:**

- Create: `workmate-was/src/test/resources/rageval/queries.json`

- [ ] **Step 1: 실제 가이드 제목 목록 확인 (읽기 전용)**

서버가 떠 있는 상태에서 관리자 로그인 후 브라우저 또는 아래로 제목을 확인한다. 앱을 통해 확인이 어렵다면, 러너를 먼저 만든 뒤(Task 5) 콘솔 로그로 제목을 찍어도 된다. 가장 간단한 확인: 실행 중인 dev DB 에 읽기 전용으로 접속해

```sql
SELECT guide_seq, title FROM guide ORDER BY guide_seq;
```

를 실행해 제목을 파악한다. (개인정보 없음 — 제목만)

- [ ] **Step 2: queries.json 작성 (15문항 이상)**

Step 1 에서 확인한 **실제 제목**으로 `expectedTitles` 를 채운다. 각 가이드당 1~2문항, 자연스러운 구어체 질문으로. 스키마 예:

```json
[
    {
        "question": "도커랑 쿠버네티스 차이가 뭐야?",
        "expectedTitles": ["<실제 제목>"]
    },
    { "question": "쿠버네티스는 언제 써?", "expectedTitles": ["<실제 제목>"] },
    {
        "question": "RAG가 뭔지 쉽게 설명해줘",
        "expectedTitles": ["<실제 제목>"]
    },
    {
        "question": "LangChain은 어디에 쓰는 거야?",
        "expectedTitles": ["<실제 제목>"]
    },
    { "question": "LLM이 뭐야?", "expectedTitles": ["<실제 제목>"] }
]
```

> ⚠️ `expectedTitles` 값은 DB 의 `title` 과 **정확히 일치**해야 한다(공백·대소문자 포함). 메트릭이 문자열 완전일치로 대조하기 때문.

- [ ] **Step 3: 로더로 파싱 되는지 스모크 확인**

임시 테스트를 추가하지 말고, 다음 한 줄 검증을 `GoldenSetLoaderTest` 에 추가한다:

```java
    @Test
    @DisplayName("실제 골든셋도 파싱되고 15문항 이상이다")
    void real_golden_set_parses() {
        List<EvalQuery> queries = new GoldenSetLoader().load("rageval/queries.json");
        assertThat(queries).hasSizeGreaterThanOrEqualTo(15);
        assertThat(queries).allSatisfy(q -> {
            assertThat(q.question()).isNotBlank();
            assertThat(q.expectedTitles()).isNotEmpty();
        });
    }
```

- [ ] **Step 4: 테스트 실행 → 통과 확인**

Run: `./gradlew :workmate-was:test --tests "com.workmate.was.rageval.GoldenSetLoaderTest"`
Expected: PASS (3 tests)

- [ ] **Step 5: 커밋**

```bash
git add workmate-was/src/test/resources/rageval/queries.json \
        workmate-was/src/test/java/com/workmate/was/rageval/GoldenSetLoaderTest.java
git commit -m "test(rag-eval): 실제 가이드 기준 골든셋 15문항"
```

---

## Task 5: 통합 러너 + Gradle 태스크

**Files:**

- Create: `workmate-was/src/test/java/com/workmate/was/rageval/RagEvalRunner.java`
- Modify: `workmate-was/build.gradle`

- [ ] **Step 1: 기본 test 에서 rag-eval 태그 제외 + ragEval 태스크 추가**

`workmate-was/build.gradle` 의 `tasks.named('test')` 블록을 아래로 교체하고, 그 아래 `ragEval` 태스크를 추가한다:

```groovy
tasks.named('test') {
    useJUnitPlatform {
        // 평가 하네스는 비용·API키·DB 가 필요하므로 평상시 test 에서 제외
        excludeTags 'rag-eval'
    }
    environment 'AES_SECRET_KEY', 'MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY='
    environment 'AES_SECRET_IV', 'MDEyMzQ1Njc4OWFiY2RlZg=='
}

// RAG 검색 품질 평가 — 실제 dev DB + 실제 임베딩으로 수동 실행한다.
// 요구: GEMINI_API_KEY + 실행 중인 dev DB(둘 다 .env / OS 환경변수에서 주입).
tasks.register('ragEval', Test) {
    group = 'verification'
    description = 'RAG 검색 품질 평가(@Tag rag-eval) 실행 후 리포트 생성'
    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath
    useJUnitPlatform {
        includeTags 'rag-eval'
    }
    environment 'AES_SECRET_KEY', 'MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY='
    environment 'AES_SECRET_IV', 'MDEyMzQ1Njc4OWFiY2RlZg=='
    // 리포트 저장 위치를 저장소 루트 기준 절대경로로 넘긴다(테스트 작업 디렉토리 의존 제거)
    systemProperty 'ragEval.reportDir', "${rootProject.projectDir}/docs/development/rag-eval"
    // 콘솔에 표를 보기 위해 표준출력 표시
    testLogging { showStandardStreams = true }
    // 코퍼스가 바뀌면 결과도 바뀌므로 항상 재실행
    outputs.upToDateWhen { false }
}
```

- [ ] **Step 2: 러너 작성**

```java
package com.workmate.was.rageval;

import com.workmate.was.guide.dao.GuideRepository;
import com.workmate.was.guide.service.GuideRetriever;
import com.workmate.was.guide.vo.Guide;
import com.workmate.was.guide.vo.GuideSourceChunk;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RAG 검색 품질 평가 러너 — 실제 dev DB·실제 임베딩으로 읽기 전용 검색을 수행한다.
 * topK×threshold 를 스윕하며 GuideRetriever 의 실제 코드 경로를 그대로 호출하고,
 * 결과를 마크다운 리포트로 남긴다. 스윕은 ReflectionTestUtils 로 필드만 주입해
 * 프로덕션 코드를 바꾸지 않는다.
 *
 * 실행: ./gradlew :workmate-was:ragEval  (GEMINI_API_KEY + dev DB 필요)
 */
@Tag("rag-eval")
@SpringBootTest
class RagEvalRunner {

    /** 스윕 격자 — 상위 K 개 */
    private static final List<Integer> TOP_KS = List.of(2, 4, 6, 8);
    /** 스윕 격자 — 최소 유사도 임계값 */
    private static final List<Double> THRESHOLDS = List.of(0.3, 0.4, 0.5, 0.6);

    @Autowired
    private GuideRetriever guideRetriever;
    @Autowired
    private GuideRepository guideRepository;

    @Test
    @DisplayName("검색 품질 스윕 실행 후 리포트를 남긴다")
    void run() throws Exception {
        List<Guide> guides = guideRepository.findAll();
        assertThat(guides).as("평가하려면 dev DB 에 가이드가 있어야 한다").isNotEmpty();
        // 접근 필터(공개/본인) 통과를 위해 가이드 소유자 seq 로 검색한다(본인 문서는 무조건 접근 가능)
        Long ownerSeq = guides.get(0).getUserSeq();

        List<EvalQuery> queries = new GoldenSetLoader().load("rageval/queries.json");
        assertThat(queries).as("골든셋이 비어 있으면 안 된다").isNotEmpty();

        List<EvalReportWriter.SweepResult> results = new ArrayList<>();
        for (int topK : TOP_KS) {
            for (double threshold : THRESHOLDS) {
                // 프로덕션 코드 무변경 — @Value 필드만 반복마다 주입
                ReflectionTestUtils.setField(guideRetriever, "topK", topK);
                ReflectionTestUtils.setField(guideRetriever, "threshold", threshold);

                List<RetrievalMetrics.EvalCase> cases = new ArrayList<>();
                for (EvalQuery q : queries) {
                    List<String> titles = guideRetriever.retrieve(ownerSeq, q.question()).stream()
                            .map(GuideSourceChunk::title)
                            .toList();
                    cases.add(new RetrievalMetrics.EvalCase(titles, new HashSet<>(q.expectedTitles())));
                }
                results.add(new EvalReportWriter.SweepResult(
                        topK, threshold, RetrievalMetrics.compute(cases)));
            }
        }

        EvalReportWriter writer = new EvalReportWriter();
        EvalReportWriter.CorpusMeta meta =
                new EvalReportWriter.CorpusMeta(guides.size(), queries.size(), LocalDate.now());
        String markdown = writer.render(results, meta);
        System.out.println(markdown);

        String reportDir = System.getProperty("ragEval.reportDir", "../docs/development/rag-eval");
        Path file = writer.write(markdown, Path.of(reportDir), LocalDate.now());

        assertThat(Files.exists(file)).isTrue();
        // 최소 한 조합에서는 정답을 건져야 골든셋·검색이 정상 연결된 것
        assertThat(results).anyMatch(r -> r.metrics().hitRate() > 0);
    }
}
```

- [ ] **Step 3: 평가 실행 (실제 DB + GEMINI_API_KEY 필요)**

dev DB(docker-compose 등)가 떠 있고 `.env` 에 `GEMINI_API_KEY` 가 있는 상태에서:

Run: `./gradlew :workmate-was:ragEval`
Expected:

- 콘솔에 16행짜리 마크다운 표 출력
- BUILD SUCCESSFUL
- `docs/development/rag-eval/REPORT-<오늘날짜>.md` 생성

> 실패 시 점검: `assertThat(guides).isNotEmpty()` 실패 → DB 미연결 또는 가이드 없음. `hitRate>0` 없음 → `expectedTitles` 가 실제 `title` 과 불일치(공백·표기 확인). 컨텍스트 로딩 실패 → GEMINI_API_KEY 미주입(.env 확인).

- [ ] **Step 4: 리포트에 튜닝 근거 한 문단 추가**

생성된 `REPORT-*.md` 하단에 표를 해석한 **한 문단**을 손으로 덧붙인다. 예:

```markdown
## 해석 및 선택

threshold 0.4→0.5 구간에서 Miss rate 가 5%→18% 로 뛰지만 Hit@4 는 거의 불변이므로,
현재 코퍼스에서는 threshold 0.4·topK 4 가 재현율과 노이즈의 균형점이다.
(운영 기본값과 일치 — 데이터로 뒷받침됨)
```

실제 수치에 맞춰 문장을 조정한다.

- [ ] **Step 5: 커밋**

```bash
git add workmate-was/build.gradle \
        workmate-was/src/test/java/com/workmate/was/rageval/RagEvalRunner.java \
        docs/development/rag-eval/REPORT-*.md
git commit -m "test(rag-eval): 스윕 러너 + ragEval 태스크 + 첫 평가 리포트"
```

---

## Task 6: 최종 검증

- [ ] **Step 1: 기본 test 가 평가 태그를 제외하는지 확인**

Run: `./gradlew :workmate-was:test`
Expected: BUILD SUCCESSFUL, `RagEvalRunner` 는 실행되지 않음(로그에 rag-eval 관련 실행 없음). 순수 함수 테스트(GoldenSetLoaderTest·RetrievalMetricsTest·EvalReportWriterTest)는 정상 실행·통과.

- [ ] **Step 2: 문서 링크 정합성 확인**

설계 문서(`docs/superpowers/specs/2026-07-29-rag-retrieval-eval-harness-design.md`)의 완료 조건 체크리스트가 모두 충족됐는지 대조한다. 미충족 항목이 있으면 해당 태스크로 돌아간다.

---

## Self-Review 결과 (계획 작성자 확인)

- **스펙 커버리지**: 골든셋(§2.1)→T1·T4, 러너(§2.2)→T5, 메트릭(§2.3)→T2, 스윕·리포트(§2.4)→T3·T5, 실행/Gradle(§2.5)→T5, 컴포넌트 경계(§3)→파일구조 표, 완료조건(§5)→T6. 누락 없음.
- **플레이스홀더**: `queries.json` 의 `<실제 제목>` 은 의도된 데이터 자리(코드 아님) — T4에서 실제 제목으로 채우는 절차 명시. 그 외 TODO/TBD 없음.
- **타입 일관성**: `EvalQuery(question, expectedTitles)`, `RetrievalMetrics.EvalCase/ComboMetrics`, `EvalReportWriter.SweepResult/CorpusMeta`, `GuideSourceChunk::title`, `GuideRetriever.retrieve(Long,String)` — 태스크 간 시그니처 일치 확인.
