# RAG 검색 품질 평가 하네스 — 설계 문서

- **작성일**: 2026-07-29
- **상태**: 설계 확정 (구현 계획 수립 전)
- **목적**: `GuideRetriever` 기반 RAG 검색 품질을 **수치로 측정**하고, `topK`·`threshold`를 스윕해 최적값을 찾은 **튜닝 흔적**을 리포트로 남긴다. (포트폴리오 목표: "RAG를 연결했다"에서 "RAG를 측정·튜닝했다"로)
- **범위**: 검색 품질(retrieval)만. 답변 품질(LLM-judge)·리랭킹은 대상 아님(YAGNI, 다음 단계).

---

## 1. 배경과 결정

### 왜 검색 품질만 먼저인가

RAG 평가는 ① 검색 품질("맞는 청크를 찾아오나") ② 답변 품질("근거로 답을 잘하나")로 나뉜다.
검색 품질은 **임베딩만 호출** → 저렴·빠름·재현 가능하고, 답변 품질은 LLM 생성+심판이 필요해 비싸고 비결정적이다.
가성비·신뢰성 우선으로 **검색 품질을 먼저** 측정한다.

### 왜 실제 데이터인가 (Testcontainers·픽스처 폐기)

초기엔 격리를 위해 Testcontainers + 고정 픽스처를 고려했으나 재검토 결과:

- `vectorStore.similaritySearch`는 vector_store 테이블 **전체**를 검색한다. 픽스처를 섞으면 오염되지만, **픽스처를 안 쓰고 실제 코퍼스만 대상으로 하면 오염 문제 자체가 사라진다.**
- 실제 코퍼스 전체 검색 = **프로덕션과 동일 조건** → 가장 현실적인 평가.
- similaritySearch는 **읽기 전용** → DB 변경 0, cleanup 불필요 → 더 안전.
- 구현량 대폭 감소(시드로더·corpus.json·Testcontainers 삭제).

**트레이드오프(수용)**: 시간이 지나 DB가 바뀌면 옛 리포트를 똑같이 재현하기 어렵다. → 리포트에 **실행일·가이드 개수**를 박아 "이 시점 코퍼스"를 명시하는 것으로 실용적으로 해결. 단, **한 번의 스윕 실행 안에서는 코퍼스가 고정**이라 설정 간 비교(=튜닝 표)는 유효하다.

**전제**: dev DB에 가이드 15개 이상 존재(AI 개발자 기술면접 주제 — 도커 vs 쿠버네티스, LangChain, LLM, RAG 등). 주제가 뚜렷이 갈려 골든셋 정답 라벨이 깨끗하다.

---

## 2. 구성 요소

### 2.1 골든셋 `queries.json` (test resources, 유일한 데이터 파일)

```json
[
    {
        "question": "도커랑 쿠버네티스 차이가 뭐야?",
        "expectedTitles": ["Docker vs Kubernetes"]
    },
    { "question": "RAG가 뭔지 설명해줘", "expectedTitles": ["RAG란 무엇인가"] }
]
```

- **정답을 guideSeq가 아니라 title로 매핑** → 환경 독립적(어느 DB든 동작). `GuideSourceChunk`에 title이 있어 반환 청크와 직접 대조.
- 실제 가이드당 1~~2문항, **15~~25문항** 목표.
- 작성 절차: 실제 가이드 목록을 **읽기 전용 조회**해 제목을 확인한 뒤 문항 작성.

### 2.2 러너 (`@Tag("rag-eval")` JUnit `@SpringBootTest`)

- dev DB에 붙어 **실제 임베딩 + 실제 vector_store로 읽기 전용 검색**.
- 각 질문을 **실제 `GuideRetriever.retrieve(userSeq, question)`** 로 검색 → 반환 청크의 title을 `expectedTitles`와 대조.
- **프로덕션 코드 무변경**: topK·threshold 스윕은 `GuideRetriever`의 `@Value` 필드를 반복마다 `ReflectionTestUtils.setField`로 주입.
- 접근 필터(공개/본인) 대응: 가이드가 공개(isPublic=true)라는 전제에서 임의 userSeq로 호출. (전제가 깨지면 소유자 seq를 넘기도록 러너에서 조정)

### 2.3 메트릭

| 지표          | 정의                                     | 의미                              |
| ------------- | ---------------------------------------- | --------------------------------- |
| **Hit@K**     | 정답 title이 topK 결과 안에 든 질문 비율 | 검색이 정답을 건지는가            |
| **MRR**       | 첫 정답의 역순위(1/rank) 평균            | 정답을 얼마나 **위쪽**에 올리는가 |
| **Miss rate** | threshold 때문에 0건 반환된 질문 비율    | 임계값이 너무 높은가              |

**청크 vs 가이드 기준 (모호함 제거)**: `retrieve()`는 **청크** 리스트를 유사도 순으로 반환하며, 한 가이드가 여러 청크로 쪼개져 있어 topK 안에 같은 title이 중복될 수 있다. 메트릭은 **반환된 청크의 title 시퀀스**로 계산한다:

- **Hit@K** = topK개 청크의 title 집합에 `expectedTitles` 중 하나라도 포함되면 hit.
- **MRR** = 위에서부터 훑어 **title이 일치하는 첫 청크의 위치**(1-based)로 `1/rank`. (같은 title이 뒤에 또 나와도 첫 위치만 사용)

이 정의는 프로덕션 검색 동작(청크 단위 반환)을 그대로 반영한다.

### 2.4 스윕 & 리포트

- 격자: `topK ∈ {2, 4, 6, 8}` × `threshold ∈ {0.3, 0.4, 0.5, 0.6}` (총 16조합).
- 산출물: `docs/development/rag-eval/REPORT-YYYY-MM-DD.md`
    - 조합별 Hit@K·MRR·Miss rate **표**
    - **코퍼스 메타**(가이드 개수·실행일·문항 수)
    - 최적값 선택 근거 **한 문단** (예: "threshold 0.4→0.5에서 Miss가 5%→18%로 뛰고 Hit@4는 불변 → 0.4 유지")
- 콘솔에도 표 출력.

### 2.5 실행

- 전용 Gradle 태스크 `./gradlew ragEval` (tag `rag-eval` 포함 실행).
- 평상시 `./gradlew test`에서는 **제외**(비용·API키 필요).
- 요구 환경: `GEMINI_API_KEY`. **Docker 불필요**(실제 dev DB 사용).

---

## 3. 컴포넌트 경계 (단일 책임)

| 단위                    | 책임                 | 입력 → 출력                               |
| ----------------------- | -------------------- | ----------------------------------------- |
| `queries.json`          | 골든셋 데이터        | —                                         |
| `GoldenSetLoader`       | JSON 로드·역직렬화   | 파일 → `List<EvalQuery>`                  |
| `RetrievalMetrics`      | 지표 계산(순수 함수) | (반환 titles, 기대 titles) → Hit/MRR/Miss |
| `RagEvalRunner`(테스트) | 스윕 오케스트레이션  | 골든셋 × 격자 → 결과 표                   |
| `EvalReportWriter`      | 마크다운 리포트 생성 | 결과 표 → `REPORT-*.md`                   |

- `RetrievalMetrics`는 DB·Spring 의존 없는 **순수 함수** → 일반 단위 테스트로 검증 가능(결정적).
- 나머지는 러너가 조립.

---

## 4. 범위 밖 (YAGNI)

- 답변 품질(LLM-judge), 근거 충실도 평가
- 리랭킹, 하이브리드 검색(BM25+벡터)
- 다국어/오탈자 강건성 평가
- CI 자동 실행(비용 때문에 수동 트리거 유지)

---

## 5. 완료 조건

- [ ] `queries.json` 15문항 이상, 실제 가이드 제목 기준으로 라벨링
- [ ] `RetrievalMetrics` 순수 함수 + 단위 테스트(결정적)
- [ ] `./gradlew ragEval` 실행 시 16조합 표가 콘솔+마크다운으로 출력
- [ ] `REPORT-*.md`에 코퍼스 메타 + 최적값 선택 근거 한 문단 포함
- [ ] 프로덕션 코드(`GuideRetriever` 등) 무변경 확인
