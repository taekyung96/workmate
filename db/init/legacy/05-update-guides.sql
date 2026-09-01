-- =============================================================
-- 가이드 문서 데이터 고도화 SQL (db/init/05-update-guides.sql)
-- 부실했던 가이드 문서 본문(content)을 실무급 마크다운 문서로 대폭 강화
-- =============================================================

-- 1. Docker와 Kubernetes의 차이 (guide_seq = 24)
UPDATE guide SET 
    title = 'Docker와 Kubernetes의 실무 비교 및 오케스트레이션 가이드',
    content = '# 🐳 Docker와 Kubernetes의 실무 비교 및 오케스트레이션 가이드

> **한 줄 요약**: Docker는 단일 호스트에서 애플리케이션을 컨테이너로 격리·실행하는 도구이고, Kubernetes(K8s)는 수십~수천 개의 컨테이너를 복수의 서버 군(Cluster)에서 자동 배치·확장·복구하는 오케스트레이션 플랫폼입니다.

---

## 1. 📦 Docker (도커) 핵심 개념 및 사용법

Docker는 애플리케이션과 그에 필요한 라이브러리, 환경 설정을 하나의 **이미지(Image)**로 패키징하여 "내 PC에서는 되는데 서버에서는 안 된다"는 환경 이격 문제를 해결합니다.

### 주요 명령어 예시
```bash
# Docker 이미지 빌드
docker build -t workmate-was:v3 .

# 컨테이너 실행 (8081 포트 포워딩 및 환경변수 주입)
docker run -d -p 8081:8081 --name workmate-was -e SPRING_PROFILES_ACTIVE=prod workmate-was:v3

# 실행 중인 컨테이너 상태 및 로그 확인
docker ps
docker logs -f workmate-was
```

---

## 2. ☸️ Kubernetes (쿠버네티스, K8s) 핵심 개념

Kubernetes는 컨테이너화된 애플리케이션의 **배포, 스케일링, 장애 복구(Self-Healing), 로드밸런싱**을 자동화합니다.

### 핵심 구성요소 (Objects)
1. **Pod (파드)**: K8s에서 배포 가능한 가장 작은 단위 (하나 이상의 컨테이너가 묶인 집합).
2. **Deployment (디플레이먼트)**: Pod의 개수를 유지하고 롤링 업데이트(무중단 배포)를 관리.
3. **Service (서비스)**: Pod 집합에 대한 단일 IP 및 로드밸런싱 엔드포인트 제공.

### K8s Deployment 선언 예시 (`deployment.yaml`)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: workmate-was-deployment
spec:
  replicas: 3 # 3개의 Pod 인스턴스 자동 유지
  selector:
    matchLabels:
      app: workmate-was
  template:
    metadata:
      labels:
        app: workmate-was
    spec:
      containers:
      - name: was
        image: workmate-was:v3
        ports:
        - containerPort: 8081
```

---

## 3. ⚖️ 실무 도입 판단 기준

- **Docker (또는 Docker Compose)**: 단일 서버, 개발 환경, MSA 초기 단계, 배치 작업에 적합.
- **Kubernetes**: 멀티 노드 서버 환경, 무중단 배포(Rolling Update) 필수인 서비스, 트래픽 폭주에 따른 Auto-scaling(HPA)이 필요한 엔터프라이즈 환경.',
    updated_at = CURRENT_TIMESTAMP
WHERE guide_seq = 24;

-- 2. RAG - 검색 증강 생성 (guide_seq = 32)
UPDATE guide SET 
    title = 'RAG (Retrieval-Augmented Generation) 시스템 아키텍처 및 파이프라인 가이드',
    content = '# 📚 RAG (Retrieval-Augmented Generation) 시스템 아키텍처 및 파이프라인 가이드

> **한 줄 요약**: RAG는 거대언어모델(LLM)이 가진 정보의 한계와 환각(Hallucination)을 극복하기 위해, 내부 지식 데이터베이스(Vector DB)에서 연관된 근거 문서를 먼저 검색한 뒤 이를 LLM 프롬프트에 합성하여 근거 있는 정확한 답변을 생성하는 기법입니다.

---

## 1. 🔄 RAG 4단계 파이프라인 동작 원리

```
[ 1. 문서 수집 & 청크 분할 ] ➔ [ 2. 텍스트 임베딩 & Vector DB 저장 ]
                                           │
[ 4. 프롬프트 합성 & LLM 답변 ] ◄── [ 3. 사용자 질문 유사도 검색 (pgvector) ]
```

### 1단계: 청크 분할 (Chunking)
긴 마크다운이나 PDF 문서를 500~1000자 단위의 의미 있는 조각(Chunk)으로 자릅니다. Overlap(중복 영역)을 두어 문맥 끊김을 방지합니다.

### 2단계: 임베딩 (Embedding)
Gemini `gemini-embedding-001` 모델을 사용하여 텍스트 청크를 768차원의 수치 벡터로 변환하여 PostgreSQL `pgvector`에 저장합니다.

### 3단계: 유사도 검색 (Similarity Search)
사용자 질문을 동일한 모델로 임베딩한 후, Vector DB에서 **코사인 유사도(Cosine Similarity)**가 높거나 L2 거리(Distance)가 가까운 Top-K(예: 상위 4개) 청크를 빠르게 검색합니다.

```sql
-- pgvector 코사인 유사도 검색 SQL 예시
SELECT content, metadata, 1 - (embedding <=> :queryVector) AS similarity
FROM vector_store
ORDER BY embedding <=> :queryVector ASC
LIMIT 4;
```

### 4단계: 프롬프트 합성 및 답변 생성 (Augment & Generate)
검색된 근거 청크들을 LLM 시스템 프롬프트의 "참고 자료" 블록으로 주입하여 환각을 방지하고 출처(Citation)와 함께 스트리밍 응답을 생성합니다.

---

## 2. 💡 RAG 최적화 및 튜닝 전략

- **유사도 임계값(Threshold) 설정**: 유사도가 0.7 미만인 저품질 청크는 검색 결과에서 제외하여 환각 방지.
- **하이브리드 검색(Hybrid Search)**: 키워드 검색(BM25)과 시맨틱 벡터 검색을 결합하여 고유명사 및 모델명 검색 성능 보완.',
    updated_at = CURRENT_TIMESTAMP
WHERE guide_seq = 32;

-- 3. 벡터 데이터베이스와 pgvector (guide_seq = 36)
UPDATE guide SET 
    title = 'PostgreSQL pgvector 기반 벡터 데이터베이스 구축 및 HNSW 인덱스 가이드',
    content = '# ⚡ PostgreSQL pgvector 기반 벡터 데이터베이스 구축 및 HNSW 인덱스 가이드

> **한 줄 요약**: `pgvector`는 PostgreSQL 데이터베이스 내에서 고차원 임베딩 벡터의 저장, 인덱싱 및 유사도 검색(코사인 유사도, L2 거리, 내적)을 네이티브로 지원하는 강력한 오픈소스 익스텐션입니다.

---

## 1. 🛠️ pgvector 테이블 스키마 및 인덱스 설계

Spring AI 및 백엔드와의 완벽한 연동을 위한 PostgreSQL `pgvector` 테이블 설계 표준입니다.

```sql
-- 1. pgvector 익스텐션 활성화
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. vector_store 테이블 생성 (Gemini 768차원 임베딩 대응)
CREATE TABLE IF NOT EXISTS vector_store (
    id        uuid NOT NULL PRIMARY KEY,   -- 청크 UUID
    content   text NOT NULL,              -- 텍스트 청크 본문
    metadata  jsonb,                      -- 출처 가이드 번호, 제목 등 메타데이터
    embedding vector(768)                 -- 768차원 임베딩 벡터
);

-- 3. HNSW (Hierarchical Navigable Small World) 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_vector_store_embedding
    ON vector_store USING hnsw (embedding vector_cosine_ops);
```

---

## 2. 🚀 HNSW 인덱스 vs IVFFlat 인덱스 비교

- **HNSW (강력 추천 ⭐)**: 
  - 그래프 기반 탐색 구조로 고속 알고리즘 제공.
  - 데이터가 적을 때나 많을 때 모두 높은 정밀도(Recall)와 빠른 검색 속도 유지.
- **IVFFlat**:
  - 클러스터 중심점 기반 분할. 인덱스 생성 시 기존 데이터가 충분히 쌓여 있어야 효과적임.

---

## 3. 🔍 Spring AI 연동 Java 설정 예시

```java
@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(768)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .build();
    }
}
```',
    updated_at = CURRENT_TIMESTAMP
WHERE guide_seq = 36;

-- 4. AI 에이전트와 툴 콜링 (guide_seq = 39)
UPDATE guide SET 
    title = 'Spring AI 기반 Tool Calling (Function Calling) 및 AI 에이전트 개발 가이드',
    content = '# 🛠️ Spring AI 기반 Tool Calling (Function Calling) 및 AI 에이전트 개발 가이드

> **한 줄 요약**: Tool Calling(도구 호출)은 LLM이 정적인 텍스트 생성에 그치지 않고, 필요시 사용자의 데이터베이스 조회, 날씨 API, 계산기 등의 외부 도구를 능동적으로 판단하고 파라미터를 추출하여 호출하는 지능형 연동 기술입니다.

---

## 1. ⚙️ Tool Calling 동작 흐름 (ReAct 패턴)

```
[ 사용자 질문 ] ("지난달 내 법인카드 영수증 총액 얼마야?")
       │
[ 1. Gemini LLM 의도 파악 ] ➔ "fetchReceiptSummary(last_month)" 도구 호출 결정
       │
[ 2. 백엔드 @Tool 메서드 실행 ] ➔ DB 쿼리 집계 (결과: 350,000원)
       │
[ 3. LLM 결과 재합성 ] ➔ "지난달 법인카드 사용 총액은 350,000원(3건)입니다."
```

---

## 2. 💻 Spring AI `@Tool` 구현 코드 예시

```java
@Component
public class ReceiptTools {

    @Autowired
    private ReceiptRepository receiptRepository;

    @Tool(description = "사용자의 특정 월 영수증 결제 건수 및 총액 합계를 조회한다.")
    public ReceiptSummaryResponse getReceiptSummary(
            @ToolParam(description = "조회 대상 연월 (YYYYMM)") String yearMonth,
            @ToolParam(description = "사용자 식별자") Long userSeq) {
        
        // 실제 데이터베이스 집계 쿼리 실행
        return receiptRepository.findSummaryByMonth(userSeq, yearMonth);
    }
}
```

---

## 3. 🔒 보안 및 가드레일 수칙

1. **사용자 격리**: Tool 호출 시 반드시 현재 세션 로그인 사용자(`userSeq`)의 데이터만 조회되도록 파라미터 샌드박싱 적용.
2. **읽기 전용 제한**: 조회용 Tool(`SELECT`) 위주로 구성하여 AI가 임의로 DB 데이터를 수정/삭제하지 못하도록 조율.',
    updated_at = CURRENT_TIMESTAMP
WHERE guide_seq = 39;
