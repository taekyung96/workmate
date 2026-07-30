# RAG VectorStore 임베딩 쿼터 이슈 분석 & 로컬 임베딩 전환 가이드 📚

## 1. 개요 (Overview)

Workmate v3 시스템은 PostgreSQL `pgvector`와 Spring AI를 활용하여 사내 업무 가이드 문서를 수치 벡터(768차원)로 변환한 뒤 `vector_store` 테이블에 저장하고, 사용자 질문 시 코사인 유사도 기반 RAG(Retrieval-Augmented Generation, 검색 증강 생성) 검색을 제공합니다.

본 문서는 RAG 문서 임베딩 적재 과정에서 발생한 Google Gemini Embedding API **429 (Too Many Requests)** 쿼터 제한 이슈의 근본 원인을 분석하고, 백엔드 아키텍처상의 개선 대안 및 **로컬 임베딩 AI 모델(Local Embedding)** 도입 가이드를 정리합니다.

---

## 2. 쿼터 초과 (429 Error) 근본 원인 분석

```text
WARN ... SpringAiRetryAutoConfiguration : Retry error. Exception: 429 . You exceeded your current quota...
model: gemini-embedding-001. Please retry in 38.86s.
```

> ⚠️ 사용 모델은 `application.yml`에 설정된 **`gemini-embedding-001`(출력 768차원 고정)** 이다. (`text-embedding-004`는 genai v1beta `embedContent`에서 404가 나서 폐기됨)

### 2.1 Google Gemini Free Tier 한도 스펙

Google AI Studio에서 제공하는 무료 티어(`Free Tier`) 키는 RPM(분당 요청)·RPD(일일 요청)·TPM(분당 토큰)의 하드 쿼터 제한(Hard Limits)을 가집니다.

- **RPM (Requests Per Minute)**: 분당 요청 수 제한 — 임베딩 무료 티어는 **낮게 잡혀 있어** 짧은 시간 연속 호출 시 쉽게 429가 발생한다. (로그의 `Please retry in 38.86s`가 그 증거)
- **RPD (Requests Per Day)**: 하루 총 요청 제한(약 1,000건 수준) — 일일 한도 초과 시 리셋될 때까지 락.
- **TPM (Tokens Per Minute)**: 분당 토큰 수 제한.

> 정확한 수치는 모델·시점에 따라 바뀌므로 [Google AI 공식 rate limits 문서](https://ai.google.dev/gemini-api/docs/rate-limits)를 확인할 것. (핵심: 임베딩 무료 티어 RPM은 낮아 대량 연사에 취약하다는 점)

### 2.2 쿼터 스파이크(Quota Spike) 유발 경로

이 프로젝트에는 **앱 시동 시 전체 가이드를 자동 임베딩하는 러너(CommandLineRunner·ApplicationRunner 등)가 존재하지 않는다.** 앱에서의 임베딩은 가이드 등록/수정 API를 통해 **1건씩만** 일어난다. 실제 429가 발생한 경로는 다음과 같다:

1. **테스트 기반 대량 시딩/평가 반복**: `RagEvalRunner`(RAG 검색 품질 평가)·`GuideCorpusSeeder`(코퍼스 대량 시딩)를 짧은 간격으로 반복 실행하면서 수십 건의 임베딩을 연사 → 무료 티어 RPM을 순식간에 고갈. (근본 원인은 "시동"이 아니라 "개발 중 대량 반복 임베딩")
2. **수정(update) 시 선삭제-후적재 패턴**: `GuideServiceImpl.updateGuide`는 `deleteEmbeddings()` → `saveEmbeddings()` 순으로 동작한다. 단, 둘 다 **같은 `@Transactional` 안**이라 적재가 429로 실패하면 삭제도 **함께 롤백**되어 데이터가 유지된다. 반면 **트랜잭션 밖(별도 테스트 tearDown 등)에서 `vector_store`를 직접 `DELETE`한 뒤 재적재가 실패하면** 0건으로 비는 사고가 날 수 있다. (실제로 과거 파괴적 테스트가 `vector_store`를 통째로 비운 사례가 있었고, 현재는 해당 테스트를 격리·수정 완료)

---

### 3. 해결 대안 비교 (Gemini API vs Local Embedding)

| 비교 항목          | ☁️ Google Gemini Embedding API               | 💻 Local Transformer Embedding (ONNX) |
| :----------------- | :------------------------------------------- | :------------------------------------ |
| **호출 방식**      | Google AI Studio HTTP REST API               | 내 서버 CPU/GPU 내부 직접 계산        |
| **쿼터 제한**      | **Free Tier RPM 낮음 → 대량 연사 시 429 락** | **무제한 (0% 쿼터 제약)** ⭐          |
| **처리 속도**      | 네트워크 Latency (약 100~300ms/건)           | **초고속 (약 0.001초/건)** ⭐         |
| **비용 및 안정성** | 일일 한도 초과 시 RAG 폴백 위험              | 100% 무상 / 완전 오프라인 작동        |
| **벡터 차원**      | 768 차원 (`gemini-embedding-001`)            | 384 차원 / 768 차원 (선택 가능)       |

---

## 4. 실질적 해결 가이드 (3가지 대안)

### 대안 1. 개발자 전용 JUnit 유틸리티 테스트 활용 (권장)

운영 백엔드 코드(`main`)를 더럽히지 않고, 개발 환경에서 필요할 때 실행 한 번으로 각 문서 사이에 1.5초 딜레이(+ 실패 시 5초 대기 3회 재시도)를 두고 안전하게 적재하는 방법입니다.

- **클래스 위치**: [GuideEmbeddingSyncTest.java](file:///C:/ClaudeCode/workmate-v3-ws/workmate-v3/workmate-was/src/test/java/com/workmate/was/guide/GuideEmbeddingSyncTest.java)
- **실행 방법**: IDE에서 `syncUnembeddedGuides()` 테스트 메서드 옆 초록색 `[▶]` 버튼 클릭.

```java
@SpringBootTest
@ActiveProfiles("local")
class GuideEmbeddingSyncTest {

    @Autowired private GuideRepository guideRepository;
    @Autowired private VectorStore vectorStore;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("DB guide 테이블의 누락된 가이드 문서를 탐색하여 vector_store에 1.5초 간격으로 안전하게 임베딩 적재한다")
    void syncUnembeddedGuides() {
        List<Guide> guides = guideRepository.findAll();
        TokenTextSplitter splitter = new TokenTextSplitter();

        for (Guide guide : guides) {
            // 이미 vector_store에 존재하는 guideSeq이면 스킵
            String checkSql = "SELECT COUNT(*) FROM vector_store WHERE (metadata->>'guideSeq')::bigint = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, guide.getGuideSeq());
            if (count != null && count > 0) continue;

            // 429 방지: 최대 3회 재시도 + 성공 시 1.5초, 실패 시 5초 대기
            boolean success = false;
            int retries = 0;
            while (!success && retries < 3) {
                try {
                    Document doc = new Document(guide.getContent(), Map.of(
                            "guideSeq", guide.getGuideSeq(),
                            "userSeq", guide.getUserSeq(),
                            "title", guide.getTitle(),
                            "isPublic", guide.getIsPublic()));
                    vectorStore.add(splitter.split(List.of(doc)));
                    success = true;
                    Thread.sleep(1500); // 성공 후 1.5초 슬립으로 RPM 보호
                } catch (Exception e) {
                    retries++;
                    log.warn("가이드 [Seq: {}] 임베딩 재시도 ({}/3) - {}", guide.getGuideSeq(), retries, e.getMessage());
                    try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                }
            }
        }
    }
}
```

> 임베딩 시 메타데이터에 `guideSeq`·`userSeq`·`title`·`isPublic`을 함께 넣는다. `userSeq`·`isPublic`은 RAG 검색 시 "본인 문서 + 공개 문서"만 거르는 필터(F4-08)에 쓰이므로 반드시 포함해야 한다.

### 대안 2. 로컬 임베딩 AI 모델(Spring AI ONNX)로 전환

Spring AI가 제공하는 로컬 Transformer(ONNX) 임베딩을 추가하면 외부 API 키 없이 완전 무료로 임베딩을 구동할 수 있습니다.

```groovy
// build.gradle (workmate-was)
implementation 'org.springframework.ai:spring-ai-transformers-embedding'
```

> ⚠️ **아티팩트 좌표·버전 확인 필수**: 정확한 좌표는 이 프로젝트가 쓰는 Spring AI 버전(1.1.x)에 맞춰 확인해야 한다(버전에 따라 `spring-ai-transformers`·스타터명이 다를 수 있음). 또한 **로컬 모델은 출력 차원이 다를 수 있어**(예: MiniLM 384차원), 현재 `vector_store`·설정이 **768차원 고정**이므로 차원을 768로 맞추거나 스키마·설정을 함께 바꿔야 한다.

### 대안 3. 신규 Google API 키 교체

다른 Google 계정으로 [Google AI Studio](https://aistudio.google.com/)에서 새 API 키를 생성한 후 `application-local.yml`의 키 값을 교체하면 쿼터 한도가 즉시 초기화됩니다.

---

## 5. 결론 및 결언

1. **실제 서비스 운영 중**: 가이드 등록/수정 API(`GuideServiceImpl`)를 사용할 때는 1건씩 처리되므로 Google Gemini API 쿼터에 전혀 문제가 없습니다.
2. **SQL 직접 인서트 시**: 운영 WAS 코드를 더럽히지 않고 `GuideEmbeddingSyncTest.java` JUnit 유틸리티를 활용하거나 로컬 임베딩 모델로 전환하는 것이 가장 안전하고 모범적인 아키텍처 솔루션입니다.
