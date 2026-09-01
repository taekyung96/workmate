-- =============================================================
-- V2 — 참조 데이터 시드 (guide · vector_store · common_code · common_code_group)
--
-- `pg_dump --data-only --no-owner --inserts -t guide -t vector_store -t common_code
-- -t common_code_group` 결과에 각 INSERT 문마다 `ON CONFLICT DO NOTHING` 을 붙였다.
-- (D3 설계 결정) 이미 이 데이터를 db/init/*.sql 로 가지고 있는 기존 DB 에
-- baseline-version=1 로 재생시켜도 중복 INSERT 로 실패하지 않게 하기 위함이다.
-- `\restrict`/`\unrestrict` 줄은 V1 과 같은 이유로 제거했다.
--
-- 데모 계정(13-seed-demo-data.sql)은 여기 포함하지 않는다 — 개발용 AES 키로 암호화된
-- 값이라 다른 배포에서 복호화가 안 된다(D4). 데모 계정은 scripts/bootstrap-demo-login.sh 가 맡는다.
--
-- ⚠️ 적용된 뒤에는 이 파일을 손으로 고치지 말 것 — 체크섬 불일치로 기동이 깨진다.
-- =============================================================
--
-- PostgreSQL database dump
--


-- Dumped from database version 17.10 (Debian 17.10-1.pgdg12+1)
-- Dumped by pg_dump version 17.10 (Debian 17.10-1.pgdg12+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: common_code_group; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.common_code_group VALUES ('AI_MODEL', 'AI 답변 모델', NULL, true) ON CONFLICT DO NOTHING;



--
-- Data for Name: common_code; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.common_code VALUES ('AI_MODEL', 'gemini-flash-latest', 'Gemini Flash (latest)', 1, true) ON CONFLICT DO NOTHING;

INSERT INTO public.common_code VALUES ('AI_MODEL', 'gemini-pro-latest', 'Gemini Pro (latest)', 2, true) ON CONFLICT DO NOTHING;



--
-- Data for Name: guide; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.guide VALUES (24, 1, 'Docker와 Kubernetes의 실무 비교 및 오케스트레이션 가이드', '# 🐳 Docker와 Kubernetes의 실무 비교 및 오케스트레이션 가이드

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
- **Kubernetes**: 멀티 노드 서버 환경, 무중단 배포(Rolling Update) 필수인 서비스, 트래픽 폭주에 따른 Auto-scaling(HPA)이 필요한 엔터프라이즈 환경.', true, '2026-07-29 17:06:19.802423', '2026-07-30 01:42:13.176502') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (25, 1, '[DevOps] GitHub Actions & Docker 기반 CI/CD 파이프라인 구축 가이드', '# 🚀 GitHub Actions & Docker 기반 CI/CD 파이프라인 구축 가이드

> **한 줄 요약**: CI(지속적 통합)는 코드 변경 사항의 자동 빌드 및 테스트를, CD(지속적 배포)는 검증된 코드의 서버 자동 배포를 담당하여 소프트웨어 전달 속도와 안정성을 극대화합니다.

---

## 1. ⚙️ CI/CD 파이프라인 4단계 구성

```
[ Developer Push ] ➔ [ 1. CI 빌드 & 자동 테스트 ] ➔ [ 2. Docker 이미지 빌드 및 레지스트리 Push ]
                                                                      │
[ 4. Health Check 및 완료 ] ◄── [ 3. 운영 서버 SSH / K8s 자동 배포 ]
```

---

## 2. 📝 GitHub Actions 워크플로우 예시 (`.github/workflows/deploy.yml`)

```yaml
name: Workmate CI/CD Pipeline

on:
  push:
    branches: [ main ]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - name: 소스코드 체크아웃
        uses: actions/checkout@v4

      - name: JDK 17 세팅
        uses: actions/setup-java@v4
        with:
          java-version: ''17''
          distribution: ''temurin''

      - name: Gradle 빌드 및 테스트
        run: ./gradlew build --no-daemon

      - name: Docker Hub 로그인 및 이미지 빌드/Push
        uses: docker/build-push-action@v5
        with:
          push: true
          tags: ${{ secrets.DOCKERHUB_USERNAME }}/workmate-was:latest

      - name: 운영 서버 SSH 원격 배포
        uses: appleboy/ssh-action@v1.0.0
        with:
          host: ${{ secrets.SERVER_IP }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SSH_PRIVATE_KEY }}
          script: |
            docker pull ${{ secrets.DOCKERHUB_USERNAME }}/workmate-was:latest
            docker compose up -d db was web
```', true, '2026-07-29 17:06:21.593167', '2026-07-30 01:46:18.273856') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (26, 1, '[API 아키텍처] REST API vs GraphQL 실무 비교 및 설계 가이드', '# 🌐 REST API vs GraphQL 실무 비교 및 설계 가이드

> **한 줄 요약**: REST는 리소스 기반의 표준 HTTP 메서드(GET, POST, PUT, DELETE)를 활용하는 명확한 웹 표준 아키텍처이며, GraphQL은 클라이언트가 필요한 데이터 구조를 단일 엔드포인트에서 쿼리로 직접 지정해 가져오는 쿼리 언어입니다.

---

## 1. 📊 핵심 비교 및 특징

| 구분 | REST API | GraphQL |
| :--- | :--- | :--- |
| **엔드포인트** | `/api/users`, `/api/orders` 등 복수 엔드포인트 | `/graphql` 단일 엔드포인트 (POST) |
| **데이터 오버패칭** | 불필요한 필드까지 고정 응답 (Over-fetching) | 필요한 필드만 선택 응답 (No Over-fetching) |
| **언더패칭** | 연관 데이터 조회를 위해 N번 호출 (Under-fetching) | 단 1번의 쿼리로 연관 데이터 통합 수신 |
| **캐싱** | HTTP 표준 캐싱 (ETag, Cache-Control) 용이 | HTTP 캐싱 복잡 (클라이언트 캐시 라이브러리 사용) |

---

## 2. 💻 GraphQL 쿼리 예시

```graphql
# 클라이언트가 필요한 사용자 이름과 영수증 결제 금액만 요청
query GetUserReceipts {
  user(id: "3") {
    name
    receipts {
      payAmount
      payDate
    }
  }
}
```', true, '2026-07-29 17:06:22.2976', '2026-07-30 01:46:18.278147') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (27, 1, '[Database] RDBMS (PostgreSQL) vs NoSQL (MongoDB/Redis) 선택 가이드', '# 🗄️ RDBMS (PostgreSQL) vs NoSQL 선택 및 데이터 모델링 가이드

> **한 줄 요약**: RDBMS는 엄격한 스키마와 ACID 트랜잭션으로 금융/결제/회원 데이터의 신뢰성을 보장하며, NoSQL은 유연한 문서/키-값 구조로 대용량 트래픽의 고속 읽기/쓰기에 최적화되어 있습니다.

---

## 1. ⚖️ 기술별 적합한 유즈케이스

- **PostgreSQL (RDBMS)**:
  - 회원/권한 관리, 결제/영수증 정산, RAG 벡터 검색(pgvector) 등 데이터 일관성이 중요한 핵심 비즈니스 로직.
- **Redis (NoSQL Key-Value)**:
  - 세션 저장소, 실시간 랭킹, 캐싱, 쿼터 제한(Rate Limiting) 카운터.
- **MongoDB (NoSQL Document)**:
  - 스키마 변형이 잦은 비구조화 로그, 이벤트 이력, 실시간 텍스트 데이터 보관.

---

## 2. 🔒 PostgreSQL ACID 트랜잭션 보장
```sql
BEGIN;
  UPDATE account SET balance = balance - 10000 WHERE user_seq = 1;
  UPDATE account SET balance = balance + 10000 WHERE user_seq = 2;
COMMIT;
```', true, '2026-07-29 17:06:22.98372', '2026-07-30 01:46:18.282414') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (28, 1, '[Performance] Redis 캐싱 전략 및 Spring Boot Caching 가이드', '# ⚡ Redis 캐싱 전략 및 Spring Boot Caching 가이드

> **한 줄 요약**: Redis는 인메모리(In-Memory) 기반의 초고속 데이터 구조 저장소로, DB 조회를 줄여 응답 속도를 10배 이상 향상시키는 캐시(Cache) 레이어로 활용됩니다.

---

## 1. 🧠 대표적인 캐시 패턴

1. **Look-Aside (Cache-Aside) 패턴 (가장 보편적 ⭐)**:
   - 앱이 먼저 Redis 조회를 시도 ➔ 데이터가 있으면 반환(Cache Hit) ➔ 없으면 DB 조회(Cache Miss) 후 Redis에 저장.
2. **Write-Through 패턴**:
   - DB와 Redis에 동시에 데이터를 기록하여 항상 최신 데이터 유지.

---

## 2. 💻 Spring Boot `@Cacheable` 적용 예시

```java
@Service
public class CommonCodeService {

    // 공통코드 목록 조회 결과는 변경 빈도가 낮으므로 1시간 캐싱
    @Cacheable(value = "commonCodes", key = "#groupCode")
    public List<CommonCodeVo> getCodes(String groupCode) {
        return commonCodeRepository.findByGroupCode(groupCode);
    }
}
```', true, '2026-07-29 17:06:23.665228', '2026-07-30 01:46:18.290631') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (51, 3, '[Coding Standard] Full-Stack (Spring Boot & Vue 3) 사내 코딩 컨벤션 & 모범 규정', '# [Coding Standard] Full-Stack (Spring Boot & Vue 3) 사내 코딩 컨벤션 & 모범 규정 📜

## 1. 개요
Workmate v3 프로젝트의 백엔드(Spring Boot) 및 프론트엔드(Vue 3 + TypeScript) 코드 품질 향상, 가독성 유지, 그리고 유지보수성을 극대화하기 위한 풀스택 표준 코딩 컨벤션을 정의합니다.

---

## 2. Spring Boot 백엔드 컨벤션

### ① 레이어별 역할 분리 (Layered Architecture)
- **Controller**: HTTP 요청 파라미터 검증(`@Valid`), 응답 포맷Wraping만 담당하며 **비즈니스 로직 작성 금지**.
- **Service**: 트랜잭션(`@Transactional`) 관리 및 핵심 비즈니스 로직 전담.
- **Repository/DAO**: DB CRUD 쿼리 전담.

### ② DTO vs Entity 엄격 분리
- **Entity**: 데이터베이스 테이블과 1:1 매핑되는 객체로, **클라이언트 응답(JSON)이나 API 요청 파라미터로 직접 노출하는 것을 엄격히 금지**합니다.
- **DTO**: API 요청/응답 전용 객체로 `record` 키워드나 `@Builder` 패턴을 사용하여 생성합니다.

```java
// ✅ DTO (API 응답 전용 record 객체)
public record ReceiptResponseDto(
    Long receiptSeq,
    String storeName,
    Integer amount,
    LocalDateTime createdAt
) {}
```

---

## 3. Vue 3 + TypeScript 프론트엔드 컨벤션

### ① `<script setup lang="ts">` 모범 구조
Vue 3 컴포넌트는 아래의 정해진 순서대로 코드 섹션을 구성합니다.

```vue
<script setup lang="ts">
// 1. Vue / Vue Router / Pinia 임포트
import { ref, computed, onMounted } from ''vue''
import { useRoute } from ''vue-router''

// 2. Lucide 아이콘 및 UI 컴포넌트 임포트
import { Button } from ''@/common/components/ui/button''

// 3. Props / Emits 정의
interface Props {
    title: string
}
const props = defineProps<Props>()

// 4. Reactive State & Store
const count = ref<number>(0)

// 5. Computed Properties
const doubleCount = computed(() => count.value * 2)

// 6. Methods
function increment() {
    count.value++
}
</script>
```

### ② Pinia 상태 관리 가이드 (`Composition API Store`)
```typescript
import { defineStore } from ''pinia''
import { ref } from ''vue''

export const useUserStore = defineStore(''user'', () => {
    const userName = ref<string>('''')

    function setUserName(name: string) {
        userName.value = name
    }

    return { userName, setUserName }
})
```

---

## 4. 예외 처리 & PII 보안 규정
1. **GlobalExceptionHandler**: 백엔드의 모든 비즈니스 예외는 `@RestControllerAdvice`에서 공통 `ErrorResponse` 포맷으로 래핑하여 500 에러 노출을 방지합니다.
2. **PII(개인식별정보) 암호화**: 사용자 이메일 및 전화번호는 DB 저장 시 **AES-256 결정적 암호화**를 반드시 적용합니다.', true, '2026-07-30 05:45:41.33664', '2026-07-30 05:45:41.33664') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (29, 1, '[Architecture] Event-Driven 메시지 큐 (Kafka vs RabbitMQ) 비교 가이드', '# 📩 Event-Driven 메시지 큐 (Kafka vs RabbitMQ) 비교 가이드

> **한 줄 요약**: 메시지 큐(Message Queue)는 시스템 간 결합도를 낮추고 비동기 처리 및 대용량 트래픽 버퍼링을 제공하는 이벤트 기반 아키텍처의 핵심 부품입니다.

---

## 1. 📊 Apache Kafka vs RabbitMQ 비교

- **Apache Kafka (분산 스트리밍 디스크 파일 기반)**:
  - 대용량 로그 수집, 대규모 이벤트 스트리밍, 메시지 영속성 보장 (소비 후에도 메시지 보존).
- **RabbitMQ (AMQP 프로토콜 메모리 기반)**:
  - 복잡한 라우팅 규칙(Exchange), 미세한 우선순위 큐, 메시지 즉시 소비 및 삭제 처리.

---

## 2. 💻 Kafka Producer/Consumer 예시

```java
// Spring Kafka Producer 예시
@Service
public class EventProducer {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendAuditLog(String eventJson) {
        kafkaTemplate.send("audit-log-topic", eventJson);
    }
}
```', true, '2026-07-29 17:06:24.354607', '2026-07-30 01:46:18.301065') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (30, 1, 'LLM이란 무엇인가', '## LLM이란 무엇인가

**LLM(대규모 언어 모델)** 은 방대한 텍스트로 학습해 "다음에 올 토큰(단어 조각)"을 확률적으로 예측하는 신경망이다. 이 단순한 목표를 대규모로 학습하면 번역·요약·코딩·추론 같은 능력이 창발한다.

핵심은 **트랜스포머(Transformer)** 구조와 **셀프 어텐션(self-attention)** 이다. 어텐션은 문장 안 단어들이 서로 얼마나 관련 있는지 가중치로 계산해, 긴 문맥의 의존 관계를 병렬로 처리한다.

용어
- 파라미터: 모델이 학습한 가중치 수(규모의 척도)
- 사전학습→미세조정: 일반 학습 후 특정 작업에 맞춤
- 생성은 확률적이라 같은 질문에도 답이 달라질 수 있다

대표 모델: GPT, Claude, Gemini, Llama.', true, '2026-07-29 17:06:25.051112', '2026-07-29 17:06:25.051112') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (31, 1, '임베딩과 벡터 검색', '## 임베딩과 벡터 검색

**임베딩(embedding)** 은 텍스트·이미지 등을 의미를 담은 고차원 숫자 벡터로 바꾼 것이다. 의미가 비슷한 것끼리 벡터 공간에서 가까이 위치한다.

**벡터 검색** 은 질의도 임베딩으로 바꿔, 저장된 벡터들과의 **거리(코사인 유사도 등)** 를 계산해 가장 가까운 것을 찾는다. 키워드 완전일치가 아니라 **의미 기반** 검색이라, "휴가"로 "연차"를 찾을 수 있다.

핵심 개념
- 코사인 유사도: 두 벡터의 방향이 얼마나 같은지(1에 가까울수록 유사)
- ANN(근사 최근접 이웃): HNSW 같은 인덱스로 대규모에서도 빠르게 검색
- 차원 수: 모델이 정한 벡터 길이(예: 768)

RAG의 검색 단계가 바로 이 벡터 검색이다.', true, '2026-07-29 17:06:25.715624', '2026-07-29 17:06:25.715624') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (32, 1, '[AI/RAG] RAG (Retrieval-Augmented Generation) 파이프라인 실무 구축 가이드', '# [AI/RAG] RAG (Retrieval-Augmented Generation) 파이프라인 실무 구축 가이드 🔍

## 1. RAG 개요 및 아키텍처
RAG(검색 증강 생성)는 최신 사내 전용 문서나 보안 텍스트를 파인튜닝 없이 LLM 답변의 참고 근거(Context)로 주입하는 기술입니다.

```mermaid
graph LR
    A[사용자 질문] --> B[Embedding Model]
    B --> C[(pgvector DB)]
    C -->|유사도 상위 K개 문서| D[LLM Prompt 주입]
    D --> E[최종 근거 기반 답변]
```

---

## 2. Spring AI 기반 RAG 검색어 처리 예시

```java
@Component
@RequiredArgsConstructor
public class GuideRetriever {
    private final VectorStore vectorStore;

    public List<Document> searchAccessibleDocs(String query, Long userSeq) {
        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(4)
                .similarityThreshold(0.4) // 최소 코사인 유사도 0.4 이상만 검색
                .build());
    }
}
```

---

## 3. RAG 성능 향상을 위한 실무 팁
1. **Chunk Size 최적화**: 500~1,000 토큰 단위로 문단을 분할하고 Overlap(100 토큰)을 두어 문맥 단절을 방지하세요.
2. **Hybrid Search**: 키워드 검색(BM25)과 코사인 유사도 벡터 검색을 조합하면 고유명사 검색률이 급증합니다.', true, '2026-07-29 17:06:26.384502', '2026-07-30 01:42:13.183501') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (33, 1, '[AI/Prompt] 실무 프롬프트 엔지니어링 & Few-Shot 패턴 가이드', '# [AI/Prompt] 실무 프롬프트 엔지니어링 & Few-Shot 패턴 가이드 🤖

## 1. 개요
프롬프트 엔지니어링(Prompt Engineering)은 LLM(대형 언어 모델)으로부터 원하는 형식과 높은 품질의 답변을 유도하기 위한 프롬프트 구성 및 설계 기술입니다.

---

## 2. 핵심 프롬프트 패턴 & 실제 적용 예시

### ① 역할 지정 패턴 (Persona Pattern)
AI에게 명확한 전문가 페르소나를 부여하여 답변의 전문성과 톤앤매너를 고정합니다.
```text
[System Prompt]
당신은 10년 차 수석 Java/Spring Boot 아키텍트입니다. 
코드 리뷰 요청에 대해 1) 보안 취약점, 2) N+1 성능 문제, 3) 가독성 관점에서 친절하게 지적하고 개선된 코드를 제시하세요.
```

### ② Few-Shot 패턴 (예시 기반 학습)
입출력 예시(Shot)를 2~3개 제공하여 모델이 출력 포맷과 스타일을 완벽하게 학습하도록 합니다.
```text
다음 자연어 요구사항을 JSON 포맷으로 변환하세요.

[Input]: 홍길동 30세 개발자
[Output]: {"name": "홍길동", "age": 30, "role": "개발자"}

[Input]: 이순신 45세 장군
[Output]: {"name": "이순신", "age": 45, "role": "장군"}

[Input]: 강감찬 38세 디자이너
[Output]:
```

### ③ Chain-of-Thought (CoT, 단계별 생각 유도)
"단계별로 생각해보자(Let''s think step by step)" 구문을 통해 복잡한 추론 및 수학 문제의 환각을 방지합니다.
```text
Q: 사과가 5개 있었는데 2개를 먹고, 3개를 새로 산 뒤 절반을 친구에게 주었습니다. 남은 사과는 몇 개인가요?
A: 단계별로 계산해봅시다.
1) 처음 사과: 5개
2) 2개 먹음: 5 - 2 = 3개
3) 3개 새로 사옴: 3 + 3 = 6개
4) 절반을 친구에게 줌: 6 / 2 = 3개
최종 남은 사과는 3개입니다.
```

---

## 3. 실무 주의사항 (Hallucination 방지)
- 모르는 내용이 나오면 솔직히 "정보가 부족합니다"라고 답하도록 제약 조건을 명시하세요.
- 출력 형식(JSON, Markdown, CSV 등)을 엄격하게 문맥 하단에 배치하세요.', true, '2026-07-29 17:06:27.055246', '2026-07-29 17:06:27.055246') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (34, 1, '파인튜닝과 RAG 비교', '## 파인튜닝과 RAG 비교

둘 다 LLM을 특정 도메인에 맞추는 방법이지만 접근이 다르다.

**파인튜닝(fine-tuning)** 은 추가 데이터로 모델 가중치를 재학습해 **말투·형식·특정 작업 능력**을 내재화한다. 지식이 모델 안에 박히지만, 갱신하려면 다시 학습해야 하고 비용·시간이 든다.

**RAG** 는 가중치를 건드리지 않고 **외부 지식을 검색해 주입**한다. 지식 갱신이 문서 교체만으로 즉시 되고 출처를 댈 수 있다.

선택 기준
- 자주 바뀌는 사실·사내 문서 → RAG
- 고정된 말투·출력 형식·특수 작업 숙련 → 파인튜닝
- 실무에선 **둘을 병행**하기도 한다(형식은 파인튜닝, 지식은 RAG).', true, '2026-07-29 17:06:27.703988', '2026-07-29 17:06:27.704955') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (35, 1, 'LangChain이란', '## LangChain이란

**LangChain** 은 LLM 기반 애플리케이션을 조립하기 위한 프레임워크다. 모델 호출, 프롬프트 템플릿, 외부 도구, 메모리, 벡터 저장소 등을 표준 인터페이스로 연결해준다.

핵심 구성요소
- **체인(Chain)**: 여러 단계를 순차로 연결(프롬프트→모델→파서)
- **리트리버(Retriever)**: 벡터 DB에서 문서 검색(RAG 구성)
- **에이전트(Agent)**: 모델이 어떤 도구를 쓸지 스스로 결정
- **메모리**: 대화 맥락 유지

장점: RAG·에이전트 같은 패턴을 빠르게 프로토타이핑. 단점: 추상화가 두꺼워 디버깅이 어려울 수 있어, 규모가 커지면 직접 제어를 선호하기도 한다. (유사: LlamaIndex, Spring AI)', true, '2026-07-29 17:06:28.390412', '2026-07-29 17:06:28.390412') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (36, 1, 'PostgreSQL pgvector 기반 벡터 데이터베이스 구축 및 HNSW 인덱스 가이드', '# ⚡ PostgreSQL pgvector 기반 벡터 데이터베이스 구축 및 HNSW 인덱스 가이드

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
```', true, '2026-07-29 17:06:29.128694', '2026-07-30 01:42:13.188742') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (37, 1, '토큰과 컨텍스트 윈도우, 비용', '## 토큰과 컨텍스트 윈도우, 비용

**토큰(token)** 은 LLM이 텍스트를 처리하는 단위로, 대략 단어의 조각이다(영어 1토큰≈4자). 모델은 입력·출력을 토큰 단위로 센다.

**컨텍스트 윈도우** 는 모델이 한 번에 볼 수 있는 최대 토큰 수다. 이를 넘으면 앞부분이 잘리거나 요약이 필요하다. RAG로 넣는 근거 문서도 이 한도 안에 들어가야 한다.

**비용** 은 보통 입력·출력 토큰 수에 비례한다. 그래서 실무에선
- 불필요한 맥락 제거, 프롬프트 압축
- 검색 근거는 관련 top-k만
- 캐싱(프롬프트 캐시)으로 반복 비용 절감

토큰·지연·비용을 함께 관측(observability)하는 것이 운영 LLM의 핵심 역량이다.', true, '2026-07-29 17:06:29.800094', '2026-07-29 17:06:29.800094') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (38, 1, '할루시네이션과 LLM 평가', '## 할루시네이션과 LLM 평가

**할루시네이션(환각)** 은 LLM이 그럴듯하지만 틀린 내용을 지어내는 현상이다. 모델이 "확률적으로 그럴듯한" 문장을 생성할 뿐 사실을 검증하지 않기 때문에 발생한다.

완화책: RAG로 근거 제공, "모르면 모른다" 지시, 출처 인용 강제, 낮은 temperature.

**LLM 평가** 는 출력 품질을 수치로 재는 것이다.
- 검색(RAG) 평가: Hit@K, MRR, Recall처럼 정답 문서를 잘 찾는지
- 답변 평가: 정답셋 대비 정확도, 또는 **LLM-as-judge**(다른 LLM이 채점)
- 근거 충실도: 답이 제공된 근거에 실제로 기반했는지

평가셋(golden set)을 만들어 지표를 추적하면, 프롬프트·검색 파라미터 튜닝의 효과를 객관적으로 비교할 수 있다.', true, '2026-07-29 17:06:30.515469', '2026-07-29 17:06:30.515469') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (39, 1, 'Spring AI 기반 Tool Calling (Function Calling) 및 AI 에이전트 개발 가이드', '# 🛠️ Spring AI 기반 Tool Calling (Function Calling) 및 AI 에이전트 개발 가이드

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
2. **읽기 전용 제한**: 조회용 Tool(`SELECT`) 위주로 구성하여 AI가 임의로 DB 데이터를 수정/삭제하지 못하도록 조율.', true, '2026-07-29 17:06:31.176159', '2026-07-30 01:42:13.193863') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (40, 1, 'MLOps 개요', '## MLOps 개요

**MLOps** 는 머신러닝 모델을 안정적으로 개발·배포·운영하기 위한 실천과 자동화의 총칭이다. DevOps를 ML 특성(데이터·모델·실험)에 맞게 확장한 것이다.

일반 소프트웨어와 다른 점: 코드뿐 아니라 **데이터와 모델도 버전 관리**해야 하고, 배포 후 **데이터 드리프트(입력 분포 변화)** 로 성능이 저절로 떨어질 수 있다.

핵심 요소
- 데이터/모델 버전 관리, 실험 추적(MLflow 등)
- 재현 가능한 학습 파이프라인
- 모델 배포(서빙)와 A/B 테스트
- 모니터링: 성능·드리프트·지연·비용 관측
- 재학습 트리거

LLM 시대엔 프롬프트·RAG 평가·토큰 비용 관측을 포함해 **LLMOps** 라고도 부른다.', true, '2026-07-29 17:06:31.869133', '2026-07-29 17:06:31.869133') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (43, 1, '[Linux] 실무 필수 리눅스(Linux) 시스템 & 서버 관리 명령어 가이드', '# 🐧 실무 필수 리눅스(Linux) 시스템 & 서버 관리 명령어 가이드

> **한 줄 요약**: 서버 운영, 로그 분석, 프로세스 모니터링 및 네트워크 상태를 파악하기 위한 엔지니어 필수 리눅스 CLI 명령어 집합입니다.

---

## 1. 🔍 파일 검색 및 텍스트 파싱 (find, grep, awk)

```bash
# 특정 디렉토리에서 .log 파일 중 최근 7일 이내 변경된 파일 검색
find /var/log/ -name "*.log" -mtime -7

# 로그 파일에서 ERROR 문구 추출 및 특정 키워드 카운트
grep -rn "ERROR" /var/log/was/ | wc -l

# p6spy 쿼리 실행 로그 중 수행시간(took)이 100ms 이상인 라인 추출 (awk)
awk ''$6 > 100 {print $0}'' /var/log/was/p6spy.log
```

---

## 2. 📊 프로세스 및 포트 점검 (ps, netstat, lsof, top)

```bash
# 8081 포트를 사용 중인 프로세스 PID 확인
lsof -i :8081
netstat -tulpn | grep 8081

# 자바(WAS) 프로세스 메모리/CPU 점유율 모니터링
ps aux | grep java
top -p $(pgrep -d, java)
```

---

## 3. ⚙️ systemctl 데몬 서비스 및 systemd 로그 관리 (journalctl)

```bash
# Docker 서비스 상태 확인 및 재시작
sudo systemctl status docker
sudo systemctl restart docker

# 특정 서비스의 실시간 로그 감시 (journalctl)
sudo journalctl -u docker -f -n 100
```

---

## 4. 🔒 권한 관리 (chmod, chown)

```bash
# 특정 디렉토리 및 하위 파일 소유권 변경
sudo chown -R ubuntu:ubuntu /app/workmate/

# 실행 권한 부여 (755)
chmod +x ./gradlew
', true, '2026-07-30 01:46:08.127036', '2026-07-30 01:46:08.127036') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (44, 1, '[Git] 실무 필수 Git 브랜치 전략 & 고급 명령어 가이드', '# 🌿 실무 필수 Git 브랜치 전략 & 고급 명령어 가이드

> **한 줄 요약**: 소스 코드 버전 관리, 충돌 해결, 커밋 이력 정돈 및 고급 이력 복구를 위한 Git 명령과 브랜치 협업 전략입니다.

---

## 1. 🔀 고급 브랜치 조작 (Rebase, Cherry-pick, Stash)

```bash
# 임시 작업 저장 (작업 중 급한 핫픽스 처리 시)
git stash save "work in progress"
git stash pop

# 특정 타 브랜치의 특정 커밋 단건만 내 브랜치로 가져오기 (Cherry-pick)
git cherry-pick a1b2c3d4

# 커밋 이력을 깔끔한 일자선으로 정돈 (Rebase)
git checkout feature/auth
git rebase main
```

---

## 2. 🚨 실수 복구 및 되돌리기 (Reset, Reflog)

```bash
# 커밋만 취소하고 작업 파일은 유지 (Soft Reset)
git reset --soft HEAD~1

# 완전히 이전 커밋 상태로 되돌리기 (Hard Reset)
git reset --hard HEAD~1

# 실수로 지운 커밋/브랜치까지 포함한 전체 히스토리 확인 및 복구 (Reflog ⭐)
git reflog
git reset --hard HEAD@{3}
```

---

## 3. 📝 커스텀 메시지 규칙
- `feat`: 신규 기능 추가
- `fix`: 버그 수정
- `docs`: 문서 수정
- `refactor`: 코드 리팩토링 (기능 변경 없음)
- `test`: 테스트 코드 추가
', true, '2026-07-30 01:46:08.133479', '2026-07-30 01:46:08.133479') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (47, 3, '[Frontend/Vue3] Vue 3 Composition API & Pinia & TypeScript 실무 모범 가이드', '# [Frontend/Vue3] Vue 3 Composition API & Pinia & TypeScript 실무 모범 가이드 🎨

## 1. 개요
Workmate v3 프론트엔드는 Vue 3, Composition API (`<script setup>`), Pinia, TypeScript를 기반으로 구축되었습니다. 

---

## 2. 실무 컴포넌트 작성 표준 코드 예시

```vue
<script setup lang="ts">
import { ref, computed, onMounted } from ''vue''
import { useChatStore } from ''@/modules/chat/stores/chat.store''

// 1. Props & Emits 타입 정의
interface Props {
    roomSeq: number
    title?: string
}
const props = withDefaults(defineProps<Props>(), {
    title: ''새 대화''
})

const emit = defineEmits<{
    (e: ''select'', roomSeq: number): void
}>()

// 2. Store & Reactive State
const chatStore = useChatStore()
const isLoading = ref<boolean>(false)

// 3. Computed Property
const isSelected = computed(() => chatStore.currentRoomSeq === props.roomSeq)

// 4. Method
function handleClick() {
    emit(''select'', props.roomSeq)
}
</script>

<template>
    <div 
        class="flex items-center p-2 rounded-md hover:bg-accent cursor-pointer"
        :class="{ ''bg-accent font-semibold'': isSelected }"
        @click="handleClick"
    >
        <span>{{ title }}</span>
    </div>
</template>
```

---

## 3. Pinia 스토어 세팅 모범 가이드 (`chat.store.ts`)

```typescript
import { defineStore } from ''pinia''
import { ref } from ''vue''

export const useChatStore = defineStore(''chat'', () => {
    const currentRoomSeq = ref<number | null>(null)
    
    function selectRoom(seq: number) {
        currentRoomSeq.value = seq
    }

    return { currentRoomSeq, selectRoom }
})
```', true, '2026-07-30 05:43:09.914836', '2026-07-30 05:43:09.914836') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (48, 3, '[Backend/Java] Spring Boot 3 & JPA N+1 문제 해결 및 쿼리 최적화 가이드', '# [Backend/Java] Spring Boot 3 & JPA N+1 문제 해결 및 쿼리 최적화 가이드 ⚡

## 1. N+1 문제란?
하위 엔티티를 조회할 때, 1번의 쿼리 실행 후 연관된 N개의 엔티티를 조회하기 위해 추가 쿼리가 N번 더 실행되는 성능 저하 현상입니다.

---

## 2. 해결 예시 1: Fetch Join (JPQL)

```java
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    
    // N+1 문제 발생 ❌
    // List<ChatRoom> findByUserSeq(Long userSeq);

    // Fetch Join 적용으로 1번의 JOIN 쿼리로 해결 ⭕
    @Query("SELECT r FROM ChatRoom r JOIN FETCH r.messages WHERE r.userSeq = :userSeq")
    List<ChatRoom> findByUserSeqWithMessages(@Param("userSeq") Long userSeq);
}
```

---

## 3. 해결 예시 2: @EntityGraph 활용

```java
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    
    @EntityGraph(attributePaths = {"user"})
    List<Receipt> findByUserSeqOrderByCreatedAtDesc(Long userSeq);
}
```

---

## 4. DTO 직접 조회 (Projection)
필요한 컬럼만 SELECT 하여 조회 성능을 높이고 메모리 낭비를 줄입니다.
```java
@Query("SELECT new com.workmate.was.chat.vo.ChatRoomDto(r.roomSeq, r.title, r.createdAt) FROM ChatRoom r WHERE r.userSeq = :userSeq")
List<ChatRoomDto> findRoomDtosByUserSeq(@Param("userSeq") Long userSeq);
```', true, '2026-07-30 05:43:09.923864', '2026-07-30 05:43:09.923864') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (49, 3, '[DevOps/Docker] Docker & Docker Compose 멀티 스테이지 빌드 실무 가이드', '# [DevOps/Docker] Docker & Docker Compose 멀티 스테이지 빌드 실무 가이드 🐳

## 1. 개요
멀티 스테이지 빌드(Multi-stage Build)를 사용하면 빌드 환경과 실행 환경을 분리하여 Docker 이미지 용량을 1GB에서 100MB 이하로 줄일 수 있습니다.

---

## 2. Spring Boot WAS 멀티 스테이지 Dockerfile 예시

```dockerfile
# 1단계: Gradle 빌드 스테이지
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY . .
RUN ./gradlew :workmate-was:bootJar --no-daemon

# 2단계: 경량 JRE 실행 스테이지
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/workmate-was/build/libs/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=local", "app.jar"]
```

---

## 3. docker-compose.yml 셋업 예시 (PostgreSQL pgvector)

```yaml
version: ''3.8''
services:
  db:
    image: pgvector/pgvector:pg17
    container_name: workmate-db
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: workmate_db
      POSTGRES_USER: workmate
      POSTGRES_PASSWORD: 1234
    volumes:
      - ./db/data:/var/lib/postgresql/data
      - ./db/init:/docker-entrypoint-initdb.d
```', true, '2026-07-30 05:43:09.933265', '2026-07-30 05:43:09.933265') ON CONFLICT DO NOTHING;

INSERT INTO public.guide VALUES (50, 3, '[Architecture] Spring Boot & Vue 3 표준 뼈대 구축 및 호환성 세팅 가이드', '# [Architecture] Spring Boot & Vue 3 표준 뼈대 구축 및 호환성 세팅 가이드 🏛️

## 1. 개요
본 가이드는 Workmate v3 및 사내 웹 애플리케이션의 **Spring Boot 3.x 백엔드**와 **Vue 3 프론트엔드** 간의 표준 기술 스택 호환성, 계층 아키텍처(2-tier vs 3-tier), 그리고 Docker 기반 데이터베이스 환경 구축 기준을 정의합니다.

---

## 2. 기술 스택 버전 호환성 매트릭스 (Compatibility Matrix)

| 구분 | 버전 기준 | 비고 / 호환성 가이드 |
| :--- | :--- | :--- |
| **Java JDK** | **JDK 17 LTS** 또는 **JDK 21 LTS** | Spring Boot 3.x 가동을 위한 최소 필수 요구사항 |
| **Spring Boot** | **3.5.x (또는 3.2+)** | `jakarta.*` 패키지 네임스페이스 사용 필수 |
| **Build Tool** | **Gradle 8.13+** (또는 Maven 3.9+) | Boot 3.x 호환 최소 Gradle 버전: 7.5+ |
| **Database** | **PostgreSQL 17 + pgvector** | 차세대 HNSW 벡터 검색 지원 (Port: 5432) |
| **Node.js** | **Node 20 LTS** | Vite 5/6 기반 프론트엔드 번들링 최소 버전 |
| **Frontend** | **Vue 3.5+ (Composition API)** | TypeScript, Pinia 2.x, Vue Router 4.x |

---

## 3. 계층 아키텍처 선택 기준 (2-Tier vs 3-Tier)

### ① 2-Tier 단일 서버 구조 (Workmate v3 방식 - 권장)
- **구조**: 단일 Spring Boot 프로젝트 내에 REST API Controller + Service + Repository를 두고, Vue 3 SPA 프론트엔드와 REST(JSON) 통신.
- **장점**: 개발 생산성이 매우 높고 배포 단위가 단순하며, WAS와 SPA간 CORS 설정이 명확함.

### ② 3-Tier WEB/WAS 분리 구조 (사내 정산/보안 강화 시스템)
- **구조**: WEB 레이어(Nginx/BFF 정적 서빙 및 API Gateway) ↔ WAS 레이어(비즈니스 로직 및 DB 전용) 분리.
- **장점**: 외부 망에는 WEB 서버만 노출하고 WAS와 DB는 사내 망에 숨겨 보안 극대화.

---

## 4. Docker Compose 데이터베이스 환경 구축 예시

```yaml
version: ''3.8''
services:
  workmate-db:
    image: pgvector/pgvector:pg17
    container_name: workmate-db
    restart: always
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: workmate_db
      POSTGRES_USER: workmate
      POSTGRES_PASSWORD: "1234"
    volumes:
      - ./db/data:/var/lib/postgresql/data
      - ./db/init:/docker-entrypoint-initdb.d
```', true, '2026-07-30 05:45:41.332445', '2026-07-30 05:45:41.332445') ON CONFLICT DO NOTHING;



--
-- Data for Name: vector_store; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.vector_store VALUES ('030af867-f4b8-4507-adb0-77d19008bd1d', '# 🛠️ Spring AI 기반 Tool Calling (Function Calling) 및 AI 에이전트 개발 가이드

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
2. **읽기 전용 제한**: 조회용 Tool(`SELECT`) 위주로 구성하여 AI가 임의로 DB 데이터를 수정/삭제하지 못하도록 조율.', '{"title": "Spring AI 기반 Tool Calling (Function Calling) 및 AI 에이전트 개발 가이드", "userSeq": 1, "guideSeq": 39, "isPublic": true, "chunk_index": 0, "total_chunks": 1, "parent_document_id": "0d3736c2-0104-4056-b147-0003d9a752db"}', '[-0.030599438,0.009119882,0.017346872,-0.064027645,-0.001528329,-0.005037345,-0.008327846,0.013314732,-0.010820991,0.005539161,-0.028821886,0.0059047355,-0.011540936,0.0027674555,0.12767948,-0.0014275357,0.02695711,0.0026694618,0.0016387997,-0.024192218,-0.00943712,0.01865528,-0.015428997,-0.0030982317,-0.00031935447,-0.0029761042,0.004381526,0.004917456,0.039713733,0.0055270363,0.0024994214,0.0015924393,0.014204934,-1.3094584e-07,0.00090528774,0.022827756,0.025323216,-0.025758576,-0.011223408,0.025439564,0.009980715,-0.009089707,-0.01936766,0.011782354,-0.008546484,-0.0065199705,-0.004888311,-0.01969787,-0.01584272,0.048463926,0.0110137705,0.011022495,0.020863209,-0.16604604,-0.006464587,-0.00065708393,-0.031170081,0.010941013,-0.00035400488,-0.011452406,0.011386435,0.024682889,-0.016513996,-0.0012185802,-0.008014862,-0.016053723,0.021180045,0.007659274,-0.017465675,-0.038117282,-0.003971955,-0.022582712,-0.014852452,-0.0163728,0.021855742,-0.032101665,-0.005054375,-0.009239207,0.0047100293,0.002674331,0.018279713,-0.04827029,0.0005812941,0.003733317,0.0074987966,-0.029316239,0.013005764,0.0076296865,0.0029129416,0.028378008,0.023973808,0.017924493,0.009130932,0.0031470836,-0.004008657,-0.013620482,-0.016275857,-1.8457533e-05,0.003861443,-0.019024594,-0.014351179,0.010624763,-0.0029942887,0.028006349,0.021194683,0.013575307,0.015922869,0.0024490731,-0.026260195,-0.0041540656,0.003976242,-0.031194007,0.00015232498,0.009221532,-0.0146550955,-0.13109453,0.0068018297,0.007337708,0.030427625,-0.0019617847,-0.010811697,0.021075018,-0.00049794844,0.012871198,0.020866059,-0.029782198,0.006383842,0.000992939,0.019973477,0.0054095616,-0.015932975,-0.020628702,0.0030822977,-0.010698836,-0.0073459405,0.025981015,-0.024509184,0.00049791817,-0.008095651,-0.020466268,-0.0069518816,0.024335114,-0.010158154,-0.008266532,-0.016311808,-0.032261044,-0.03279329,-0.0018339421,-0.0051301843,-0.03038894,0.014002331,-0.025441539,-0.019662108,0.016433943,0.0014123722,-0.02545413,-0.01093269,-0.0045722555,0.003830044,0.016778441,-0.05158733,0.0021866527,0.00034669682,0.014131626,0.015215277,-0.011794418,0.011164014,-0.0030501517,0.0010613254,0.029328229,-0.0046988083,0.009600931,0.010969444,-0.008464927,-0.018830936,0.012237355,0.0036431965,-0.016938066,-0.0038635232,-0.008261943,-0.0067972383,0.025569849,-0.0053678174,-0.008277915,0.00965757,0.0020807495,0.00157724,0.0028675483,0.0133936815,-0.017398307,-0.0084273545,0.016050788,0.009858317,-0.022415433,0.002576301,-0.051778987,-0.010500963,-0.011232074,-0.014143095,0.032281492,0.0010151481,0.009283468,-0.003241861,-0.00682787,-0.0033433584,0.0002986184,0.015752636,-0.026418865,0.014071588,-0.0014531956,-0.030790083,0.00032358218,0.019507369,-0.018999418,-0.018472016,-0.010417857,0.0017937172,-0.00913404,0.013520206,-0.008922987,0.032320954,0.0090965,0.008731366,0.026611662,0.0064409254,-0.015900562,-0.012016436,-0.020837676,-0.011245974,-0.018739268,0.0015334421,-0.00038371122,0.01441141,0.010127539,0.029627,-0.0037707782,-0.018849587,0.013115485,-0.0030510304,0.0072794254,-0.011759811,0.004722346,0.00070188113,0.022111401,0.018941427,-0.021610573,0.0012500959,0.0032542876,-0.0017231907,-0.0044280696,-0.002894689,-0.00074777985,0.02098868,0.012093643,-0.02677481,-0.033005442,0.0028881736,0.0008358138,-0.026537085,0.04362608,-0.006248688,0.0051322314,-0.033956885,-0.00078148296,-0.015406822,-0.007952609,-0.0019786693,0.00972616,-0.013887045,0.00028217572,0.02845975,0.031632066,-0.0148760425,-0.01918798,0.016504722,-0.0012395252,-0.031594843,0.051502995,0.0021295918,-0.022710951,0.006279416,0.0005753814,0.008418815,0.00982283,-0.010663907,0.018483827,-0.010282352,-0.005603778,0.028838636,-0.00899647,0.013179453,0.025144776,0.0002851379,-0.008495344,0.00539676,-0.032070093,-0.017784495,0.01115108,0.030000607,0.008210752,0.005958476,0.009236566,0.023663703,0.0420826,-0.020904858,-0.0060169757,0.004799997,0.0233381,0.00015943372,-0.011792625,-0.0139577715,-0.00318533,0.014150441,-0.005545189,-0.007444034,0.0078742,0.0022595583,-0.018986978,0.02399511,-0.02839125,-0.023514124,-0.027171401,0.0098396,0.0075721294,-0.0068727694,-0.023454027,-0.01794126,0.015579586,0.012032274,-2.9308323e-05,-0.0005917967,0.002414983,0.024683602,-0.014286229,0.01993069,0.0082186805,-0.0042447047,-0.008563338,-0.020405931,0.03279755,-0.0058096405,0.0005076434,-0.005851368,-0.0028503384,-0.034074165,0.026366979,0.0470108,0.013346056,-0.019268995,-0.01824755,-0.008230391,0.0012640184,0.0075775646,-0.012400589,-0.043026853,0.032883406,-0.008919792,-0.006302914,0.018167771,0.011427557,-0.027714988,0.028480971,-0.008130731,0.011704383,0.031341158,-0.0017668576,0.016976817,0.038808852,-0.015155119,0.024301857,-0.019575818,0.019242555,-0.0142167,-0.004811541,-0.025886202,0.016630558,-0.028145492,0.013670837,-0.008985756,-0.010831901,-0.011408423,0.018271597,-0.040313434,0.0023021481,-0.021845708,0.0054105115,0.014273872,0.013682766,-0.00070753804,0.0122937085,-0.0064915456,0.014588767,-0.006646865,0.012205865,0.024553578,-0.016254641,-0.03164528,0.010121411,-0.02897133,0.00445485,-0.011959464,0.0035430482,-0.019550806,0.011296703,0.005236487,-0.004488688,-0.0028011277,0.025029005,-0.021035075,-0.028728008,-0.017662473,0.007436597,-0.0033038845,0.009870133,0.018115371,0.01843121,-0.009604402,0.006468716,0.023687586,-0.03957521,0.016648961,0.03255495,8.226e-05,-0.0021777558,0.0014731935,-0.011547184,-0.018129414,0.006736878,-0.011456044,-0.0039879414,-0.029720012,0.007858757,0.0011552342,-0.024025302,-0.021606676,0.0058367266,0.0032225286,-0.021088831,-0.023382388,-0.008525725,0.036906354,0.0032755106,0.0059097507,0.012528026,0.022459313,-0.0073355464,0.0018022223,-0.012468843,-0.026887923,-0.011136335,0.014388107,-0.0042026457,0.033081535,-0.02088966,-0.0041518304,0.0060411324,0.012967969,0.0030271574,-0.0010943556,0.018269897,0.003390277,-0.0052370266,-0.0010780254,-0.0014419962,-0.020321326,-0.008018608,0.009383783,0.007970269,-0.033062283,0.04717828,0.010693525,0.0067547555,0.014529372,0.0030043768,0.007084352,-0.013194245,-0.017945854,-0.0005329648,-0.0051762206,0.012122213,-0.020056443,-0.009938197,-0.00816923,-0.006325654,-0.0085243685,0.0026424075,0.0077984575,-0.025958052,0.002153628,-0.010026874,0.02233586,0.007796679,0.0076744994,0.0053508463,-0.029220415,-0.009102516,0.013643428,-0.00932863,0.007239226,-0.02213285,-0.0024005694,-0.009362071,-0.021756891,-0.01649657,0.0088320365,-0.020054135,-0.022624379,-0.01678921,0.02462548,-0.014987105,0.011834938,-0.0011583475,-0.007865457,-0.026813058,0.01102987,8.030657e-05,0.014212771,-0.0040424694,0.018777266,-0.014980482,0.018372301,-0.010894531,-0.04297799,0.018174179,-0.016914507,0.009182304,0.031211456,-0.006840155,0.0075317444,0.011672402,-0.0028842224,0.021287961,-0.016445704,-0.005928962,-0.0051834327,-0.0050690835,0.01631382,0.004717633,-0.0138329975,-0.0016135388,-0.014678799,-0.015213172,-0.003018089,0.012132317,0.018051794,-0.09278513,0.00020331403,-0.008204625,0.0020554007,-0.018175682,-0.009389191,-0.012423366,-0.022150561,-0.009689677,0.026473433,0.0024142636,-0.017444158,0.032321595,0.014211725,-0.015799161,0.0013404584,-0.016317746,-0.036949832,0.021564912,-0.041132826,0.015396968,-0.021421999,0.005014145,0.005796762,-0.014412033,0.01431552,0.0074484674,0.005105469,0.00016010235,0.00057433965,-0.010104284,-0.016521905,-0.0014306857,-0.0069594975,0.016781773,0.008832391,0.023104807,-0.045111284,0.022601051,0.016697213,-0.026273549,-0.01281559,0.008783743,-0.0248613,0.001877748,0.0022304715,-0.001226965,-0.026391968,0.03895932,0.032330554,-0.05053288,-0.0027633142,-0.0007970615,-0.019653652,-0.014671201,0.013549197,-0.022010038,0.021170387,0.0023572424,0.015310016,-0.001554528,0.011288513,-0.0029665895,0.028846933,-0.012471643,0.014058348,0.020317413,0.010102963,-0.01851922,0.024591897,-0.0017372346,-0.017706372,-0.009745514,0.032391097,-0.003974098,-0.0075216372,-0.005772002,0.045558844,-0.0023113557,0.007251097,-0.02917704,-0.0064619286,-0.10878425,0.015842156,-0.01588251,0.0031828906,-0.0012128539,0.0079274075,0.022069165,-0.008160473,-0.0069653345,-0.0475442,0.016114661,0.0018353382,-0.007713253,-0.036548935,0.033720363,-0.018012604,-0.0051546907,0.0058385697,-0.030667257,-0.010674676,-0.009889021,0.0032516858,0.009203225,0.012206444,0.011966277,0.016706219,0.002532883,0.018599268,-0.025794696,0.0029769503,0.0034574661,-0.13361433,-0.0019703312,0.0022572745,0.008756065,-0.01491121,0.03118517,0.0012441069,0.00077469216,-0.011660382,-0.01478956,0.01071451,-0.0101189865,-0.012269411,-0.01722991,-0.0162269,0.10018322,-0.02653136,0.007308285,0.008027133,-0.002560466,0.021115577,-0.030306606,-0.012943862,0.0007819408,0.017442884,-0.024284586,-0.008626082,-0.016133023,0.011264901,0.023077905,0.02547394,0.016753683,-0.030643173,-0.003590386,0.0068232585,0.031432446,0.013357033,-0.020935625,0.017936768,0.008167936,0.018052725,0.028133731,0.004189952,-0.029158361,-0.009134507,0.00052580336,-0.03340419,0.0037261015,0.012102152,-0.014414434,-0.010884477,-0.091684826,0.009739756,-0.012454746,0.011375938,-0.0037359076,0.0128392875,0.021122858,0.016634053,0.008758767,0.021383554,-0.020821316,-0.02889016,0.019684147,0.0064019426,-0.020304421,0.036836702,0.024450583,0.0067436667,0.013497596,-0.009196351,0.008532296,-0.007125575,0.0047429274,-0.01739907,-0.0059768623,0.011888016,0.006392213,0.014531156,-0.009031001,0.0041215024,0.0020335938,-8.195281e-05,0.00071883434,-0.014539696,0.012661167,0.0004277413,-0.017054195,-0.0031372812,-0.004791975,0.006902222,0.034898534,-0.018239783,0.026673116,0.001522831,0.00032991086,-0.0016738017,0.025732266,-0.020899203,0.0033327609,-0.014234179,-0.014676465,0.026921066,-0.012410054,0.006224372,0.02032463,-0.015787836,0.020709418,-0.0069049546,-0.0004133173]') ON CONFLICT DO NOTHING;

INSERT INTO public.vector_store VALUES ('0a56d1bf-b8df-4167-a363-29ac6f04fa81', '## 프롬프트 엔지니어링

**프롬프트 엔지니어링** 은 LLM이 원하는 출력을 내도록 지시문을 설계하는 기술이다. 같은 모델도 프롬프트에 따라 결과 품질이 크게 달라진다.

대표 기법
- **역할 부여**: "너는 시니어 백엔드 개발자야"로 문맥 고정
- **Few-shot**: 예시 몇 개를 보여줘 형식·기준을 학습
- **CoT(생각의 사슬)**: "단계별로 생각해봐"로 추론 유도
- **출력 형식 지정**: JSON 등 구조 강제
- **제약·근거 요구**: "모르면 모른다고 해", "근거를 함께"

실무 팁: 지시는 구체적으로, 맥락은 앞에, 제약은 명확히. 프롬프트도 버전 관리하고 실패 사례로 개선한다.', '{"title": "프롬프트 엔지니어링", "userSeq": 1, "guideSeq": 33, "isPublic": true, "chunk_index": 0, "total_chunks": 1, "parent_document_id": "edfcdb07-2cef-438e-86d5-ca99674d3c5f"}', '[-0.009385655,0.0067070993,0.020208005,-0.067365006,-0.00215416,-0.010389215,-0.019537866,0.011951452,0.0039735674,0.0021130317,-0.02417484,-0.024688771,0.020333964,-0.01046868,0.13806692,0.013612058,-0.013385152,-0.0062928363,0.028767608,-0.018088616,-0.005696003,0.024414368,-0.020447975,0.007693302,-0.007064299,-0.0099810865,0.010767682,0.009401571,0.027477067,0.015940478,-0.021204056,0.027402261,-0.011077437,0.033044238,-0.0082353065,0.023456505,0.01635604,-0.06286319,0.0049535884,0.023393977,-0.010045535,-0.007439159,-0.023502002,-0.014089291,-0.018819137,0.00781472,0.01137731,-0.0020155527,0.008611392,0.029419122,-0.0015237065,0.026399253,0.0038202712,-0.1651045,-0.014694603,-0.00076876616,-0.01792975,-0.0044031567,0.011653866,-0.00037541435,-0.030460725,0.029591065,-0.0007016551,-0.0058803274,0.006229993,0.006184001,0.022167947,-0.00012616088,0.009207891,-0.010928988,0.002404439,-8.13469e-05,0.0012314038,-0.040740114,-0.001697729,-0.026320923,0.011308747,-0.012226838,0.0009116675,0.0042801043,0.0024570771,-0.0087883305,0.00041839032,-0.0046661133,0.0026414841,-0.00012417222,0.011472415,-0.0177411,0.004698565,0.015270878,0.020307606,-0.001719116,0.012927563,0.011480445,-0.012142303,-0.0009365788,-0.0021060174,0.01971647,0.005815336,-0.03996808,-0.02331046,-0.0036161263,0.014785516,0.009099385,0.011128523,0.016658658,-0.006020384,-0.02398472,-0.02904853,-0.0020337794,0.0005848218,-0.010512664,0.010874998,-0.0037081246,-0.008241177,-0.13146418,-0.008748922,0.0005186561,0.0020417653,-0.0064890278,-0.023714306,-0.003876829,-0.0059624524,0.028535582,0.0063832714,-0.009302664,0.013186852,0.007871155,0.0005130903,-0.008837638,-0.029544372,0.005391363,-0.002833799,0.010700256,-0.000107740765,0.028330026,-0.005548407,-0.008683039,-0.03260991,-0.01723574,-0.01902557,0.0111969495,0.0038040795,-0.0033296212,-0.03097748,-0.012845672,-0.028459482,-0.011079452,0.008095213,-0.010020238,0.0045958036,-0.038938086,-0.02092274,-0.014187693,0.008366288,-0.02576629,0.024570936,0.014336661,0.0140774585,0.0023019365,-0.018678835,-0.0027746055,-0.0018844149,-0.0077299224,0.0018373617,0.039371286,0.006713382,-0.00054758746,-0.033212423,-0.006648964,0.0058338344,0.006673398,0.014289317,-0.030062992,-0.020348873,-0.011238329,0.005161854,-0.015174263,-0.0011426714,0.0054043327,0.046710208,0.02745713,-0.021618225,0.017660143,-0.011806968,0.019310933,-0.014636457,0.030550001,0.043014456,0.03399761,-0.015050376,0.014721297,0.009088828,-0.01181677,0.0072384346,-0.03855926,-0.010127072,-0.017515335,-0.014293496,0.0049672984,0.00811435,0.014477546,0.0040226667,-0.0108539555,0.036834903,-0.005816509,0.00519036,0.0054812278,0.03204926,0.00095866463,-0.011665842,0.018228833,0.027459929,-0.0025285461,-0.013110916,-0.016446197,-0.022098677,0.013000442,0.011628979,0.000906519,0.010821564,-0.002648221,0.004171186,-0.0021904109,-0.019817857,-0.028302522,0.00046373616,-0.0053234003,0.007951805,0.0109847635,0.01601833,-0.0022835773,0.013097287,0.007483409,0.043347966,0.011903051,-0.01865057,0.03566271,-0.0022152814,0.0046633193,-0.033739522,-0.017968176,0.014800705,0.024777964,0.043892857,-0.010203563,-0.008344821,0.008351027,-0.018626465,0.005718022,0.010469609,-0.023255523,-0.020105302,0.0020174198,0.008128495,-0.018610796,-0.023651924,-0.011150137,-0.013006115,0.012049757,-0.03211543,0.00072420895,0.0029273077,-0.028796906,0.012999093,-0.003998054,-0.007488023,0.029004166,-0.025570806,-0.01355422,-0.011236666,0.023529366,-0.012808631,-0.02671539,0.010729109,-0.015875213,-0.047479082,0.052840352,0.0019671915,-0.013882218,-0.005661909,-0.008675441,0.02629775,0.007984142,-0.024541432,0.015452027,0.0023411158,-0.010509561,0.0032955774,-0.0066851894,0.009727134,0.029965876,-0.03361406,-0.008133677,-0.0071281837,-0.014988386,-0.012944914,-0.0069711595,-0.006291427,-0.010865146,0.024909005,-0.00806769,0.005249185,0.046015143,0.02014575,0.012001756,0.019093266,0.017290004,0.0024328805,-0.016784836,-0.009771427,-0.028583668,0.011647269,-0.0065712626,0.001279294,0.0200158,0.041654766,-0.013936875,-0.013936906,0.0015820173,0.012044274,0.0012331485,0.00010462068,-0.000667079,0.008266425,-0.007872216,-0.035713594,0.008396297,0.02810495,-0.00400887,0.010183514,0.022417247,0.0054726982,-0.014302644,0.016513053,0.006410107,-0.0045524556,-0.0023006436,-0.0055483133,0.0030661568,0.008122812,-0.019404877,0.0014556735,0.0030352776,-0.024718016,0.03732321,0.014687074,-0.005770323,0.0008946657,-0.028138317,0.004712539,0.008000078,0.023297368,-0.010436682,-0.031800818,0.019308656,-0.00019301492,-0.012833653,0.027403457,0.014461865,-9.412156e-05,0.048235584,-0.014189205,0.00013559143,0.031999346,-0.034813106,-0.014559866,0.025567735,0.0032302302,0.010125558,0.0017450424,-0.012767999,-0.0192883,-0.0020354055,-0.015720548,0.01903171,-0.035519514,0.005750468,-0.00832394,-0.0029400052,-0.010064677,0.00892819,-0.019859863,0.01282198,-0.022469394,-0.008874056,0.027464656,-0.0031724619,-0.0146823125,0.010444777,-0.010350584,0.015572177,-0.0066533517,0.01604795,-0.010563537,0.003407408,-0.025958339,0.0012063918,-0.008848834,-0.007619496,0.014772432,0.027039008,-0.004693518,-0.0022209785,0.0065816385,0.034500573,-0.009262488,0.0067755766,-0.022857629,-0.001281782,-0.013093181,0.0043532206,0.00838681,0.015584117,0.024634926,0.012632244,0.009000559,0.011832938,0.016132677,-0.022451518,0.008556835,0.040589496,0.0007216586,-0.0169096,-0.012732853,-0.017122116,-0.0009840738,0.0036241775,-0.00051026524,-0.026199326,-0.008733316,-0.0034905116,-0.028068392,-0.0040377313,0.00330094,0.017092442,0.009553967,0.009501806,-0.015796276,-0.0026040936,0.028023368,0.002912063,0.008651783,0.0019180147,-0.009354811,-0.014803788,0.02401461,-0.015374833,-0.0214791,0.03035135,0.03158896,0.00979936,0.0030810088,-0.014365945,0.009040017,-0.009681109,-0.010388583,0.012951199,0.0033208241,-0.016760372,-0.005792383,-0.0061473693,0.008562661,0.031230599,-0.019248543,0.0022787945,-0.0045862366,-0.0067250654,-0.019138949,0.03377969,-0.015671667,0.019202756,0.03350295,0.0031378584,-0.00319262,-0.016245069,-0.0039652083,-0.004476317,0.023009373,0.017075257,-0.0007679173,0.010851018,0.0041932296,-0.015031326,-0.025499638,-0.00323156,-0.027759513,-0.026204664,-0.022961723,-0.00635131,0.01120079,0.0117556695,0.03569424,-0.007010143,-0.015680728,0.009670217,-0.004318486,0.0011844316,0.0022177622,0.010370691,-0.0047418284,-0.013989131,0.007377705,-0.022913678,0.0021466836,-0.037446924,-0.026657613,-0.0047482373,0.019333173,0.003038404,-0.012819339,0.027396679,0.015184304,0.011751534,-0.0043298006,0.008691897,0.033571042,0.0031635875,0.0034578796,-0.03145011,0.010263359,-0.018739147,-0.012761617,0.023416763,-0.026166305,-0.022679834,0.028623633,0.0009781949,0.013171837,0.02044749,-0.0072073517,0.015309009,-0.010091116,-0.015878243,-0.007653987,-0.005021789,0.041613642,0.019613206,-0.0018603201,-0.00493347,-0.0072969426,0.009233127,0.00381596,-0.0054466724,0.0056381193,-0.08354668,-0.01065866,-0.012602817,-0.00043768506,-0.0032088785,-0.00526993,0.010914566,-0.013868362,0.02832333,-0.0031798873,0.040479403,-0.021626046,0.013215033,0.01613181,-0.013812203,-0.012706068,-0.016523499,-0.021670384,0.018400213,-0.03585019,0.01658282,-0.008459413,-0.00219903,-0.005848697,-0.019616751,-0.0031285877,0.016048858,0.010068675,-0.024291035,0.0028996768,-0.02456956,-0.03405991,-0.006910112,0.022148587,0.017541494,-0.01424618,0.034906924,-0.012660692,0.0120964395,-0.027510025,-0.017809989,0.006861638,0.0025775346,-0.029442467,0.002982856,-0.016561357,-0.0112190405,-0.0118023865,0.032289695,0.0006216429,-0.025374431,-0.019831099,-0.006773532,-0.008623757,-0.018019224,0.0039775087,-0.0022253809,0.011247057,-0.0011560785,0.005441718,-0.0050888187,-0.014099692,-0.009635575,0.03634924,-0.011376154,-0.007306985,-0.003300605,0.0065199514,-0.007926456,0.00860606,-0.006327621,-0.016171986,-0.014838559,0.024776584,-0.02791525,-0.0058309687,-0.007014185,0.01197564,0.011007618,0.00821289,-0.014058807,-0.0031701024,-0.07541094,-0.01292376,0.005939142,-0.008107375,0.010872013,-0.020517662,0.007176462,-0.009152617,-0.009252162,-0.03369837,0.00953643,-0.0348415,-0.019388339,-0.03255949,0.025153909,-0.018973285,-0.012747319,-0.015072062,-0.032731686,0.0073744664,0.01569964,0.02344557,0.01568691,0.002478273,-0.014474855,0.015653115,0.008247224,0.017433437,-0.014425779,0.021805136,0.011708057,-0.14173922,-0.008434442,0.00058687857,-0.007048339,0.0025001548,0.0010188379,-0.011987556,0.018836696,-0.0024863724,-0.017246434,0.006905502,-0.05733475,-0.038939428,-0.023481019,-0.017785972,0.09686819,-0.028029177,0.00856844,-0.005471786,0.00454745,0.020960405,-0.019957356,-0.015594774,0.014162868,-0.009816125,-0.02213803,0.005097173,-0.035375502,-0.0052501527,0.028960045,0.025954897,0.021457385,-0.014240669,-0.0052554016,0.022554692,0.01859301,-0.0048396992,-0.002749071,-0.03046125,-0.008501974,-0.001641707,0.023746822,0.0008446162,-0.017785909,-0.021370133,0.0043908395,0.009837511,-0.03967004,0.003936707,-0.010572298,-0.019790938,-0.065456346,-0.0020226398,-0.0052850903,0.037676558,0.04000649,-0.024923593,0.03990449,-0.014176601,0.000460448,0.018827863,-0.011284287,8.4058905e-05,0.026293894,-0.0015878379,-0.0032167798,0.030869242,0.0072590476,0.00027812188,0.032848198,-0.0030010033,0.005937288,0.015616721,0.0014251225,-0.0058444804,-0.014386016,0.020030774,-0.0005485631,0.013521676,-0.0040823612,0.03182665,0.0039717597,0.029693518,0.010355879,0.00028192846,0.029782912,0.03325026,0.0004143249,0.02415148,0.012396361,0.009630225,0.012031614,0.008591217,0.00065640983,-0.0026562628,-0.0029982908,0.013706247,0.019121796,-0.010470952,-0.0215025,-0.026676869,-0.0018037042,0.020247407,-0.038645938,0.0037749559,0.003341535,-0.020847358,0.03029779,0.040080514,0.0012920657]') ON CONFLICT DO NOTHING;

INSERT INTO public.vector_store VALUES ('1378f384-2fa2-4d0d-8e80-3ffd1e86b2d2', '# 🐧 실무 필수 리눅스(Linux) 시스템 & 서버 관리 명령어 가이드

> **한 줄 요약**: 서버 운영, 로그 분석, 프로세스 모니터링 및 네트워크 상태를 파악하기 위한 엔지니어 필수 리눅스 CLI 명령어 집합입니다.

---

## 1. 🔍 파일 검색 및 텍스트 파싱 (find, grep, awk)

```bash
# 특정 디렉토리에서 .log 파일 중 최근 7일 이내 변경된 파일 검색
find /var/log/ -name "*.log" -mtime -7

# 로그 파일에서 ERROR 문구 추출 및 특정 키워드 카운트
grep -rn "ERROR" /var/log/was/ | wc -l

# p6spy 쿼리 실행 로그 중 수행시간(took)이 100ms 이상인 라인 추출 (awk)
awk ''$6 > 100 {print $0}'' /var/log/was/p6spy.log
```

---

## 2. 📊 프로세스 및 포트 점검 (ps, netstat, lsof, top)

```bash
# 8081 포트를 사용 중인 프로세스 PID 확인
lsof -i :8081
netstat -tulpn | grep 8081

# 자바(WAS) 프로세스 메모리/CPU 점유율 모니터링
ps aux | grep java
top -p $(pgrep -d, java)
```

---

## 3. ⚙️ systemctl 데몬 서비스 및 systemd 로그 관리 (journalctl)

```bash
# Docker 서비스 상태 확인 및 재시작
sudo systemctl status docker
sudo systemctl restart docker

# 특정 서비스의 실시간 로그 감시 (journalctl)
sudo journalctl -u docker -f -n 100
```

---

## 4. 🔒 권한 관리 (chmod, chown)

```bash
# 특정 디렉토리 및 하위 파일 소유권 변경
sudo chown -R ubuntu:ubuntu /app/workmate/

# 실행 권한 부여 (755)
chmod +x ./gradlew', '{"title": "[Linux] 실무 필수 리눅스(Linux) 시스템 & 서버 관리 명령어 가이드", "userSeq": 1, "guideSeq": 43, "isPublic": true, "chunk_index": 0, "total_chunks": 1, "parent_document_id": "3f80f8e1-6969-4bd0-b363-b003631ffd92"}', '[-0.022291217,0.01549418,0.010519304,-0.05499391,0.0016010578,0.022571895,-0.02326388,-0.030658776,-0.005306254,0.02245768,0.0037859383,-0.017612588,-0.0075118695,-0.0009447249,0.12193117,0.00044501614,0.00026388033,0.00024558883,0.022306064,-0.028606106,0.0083459895,0.0021907222,-0.0014709454,0.00263946,-0.0040339753,0.0030630007,0.018398194,-0.023797117,0.05541511,0.0035583903,-0.008198066,0.028180322,-0.00211451,0.015467421,-0.020063428,0.014302524,0.028818164,-0.020519076,-0.019871756,0.0015920948,0.0038943638,-0.008706199,0.004223902,-0.011501808,0.00846138,0.015001913,0.0010664869,-0.020257037,-0.0029039187,0.022468839,-0.0056018406,0.0016149778,-0.009417154,-0.1901999,0.016673995,-0.014320874,-0.020673288,-0.0026744343,0.004455847,-0.010388034,-0.036343656,0.045092944,-0.009122819,-0.013305226,-0.019547934,-0.014949156,0.03162715,0.00203921,-0.010600589,-0.017992586,-0.0027584133,0.0151981255,-0.030311482,-0.0074444134,0.005145029,-0.010351153,-0.024899561,0.022904214,0.00460493,0.0005343772,-0.0009378933,-0.020419553,-0.013748947,-0.018937796,0.015769241,-0.0014386631,-0.012277474,-0.002507819,-0.0071556387,0.037198182,0.010189019,-0.015336898,-0.013432084,0.016912222,-0.027311007,0.03191219,-0.009644751,-0.025703423,0.019784946,-0.009123589,-0.011043001,-0.0006616542,0.015253469,0.02471022,0.0076544117,0.017103525,0.026162853,-0.019692399,-0.016656619,-0.00094892527,0.013041478,-0.014821881,0.004388435,-0.0007586451,0.005965779,-0.15225436,-0.019472077,-0.0020497472,0.03177441,-0.02584565,-0.015987137,0.0022465768,-0.010537166,0.009075859,-0.0054113623,-0.02773948,0.007481134,0.005154044,0.01287841,-0.005483006,-0.0035766938,-0.028149126,-0.010419227,0.009239225,0.0017385924,0.043977834,-0.014519671,-0.012159079,-0.0069061564,-0.03664474,-0.0030835005,-0.007621548,0.00017687626,-0.0057040355,-0.03710055,0.00030904816,-0.015254195,-0.00084026426,0.008081876,-0.027518839,0.008246329,-0.03869498,-0.0077210213,0.0120517,0.00013892993,-0.040008746,0.026343005,0.007129741,-0.0028774517,-0.0045526903,-0.018531488,0.024321988,0.0075792195,0.029822625,0.010485665,-0.0031746987,-0.0176123,0.0153463725,-0.013492784,0.024117552,-0.021037374,0.016329361,0.018489044,-0.047448657,-0.016054321,-0.019942293,0.001576908,0.0026555234,-0.012146906,0.009782534,0.019127885,0.007310478,-0.008180145,-0.02571343,-0.015606253,-0.019801453,-0.002630048,-0.01254174,0.036693178,0.010570517,-0.0064072507,0.0010530971,0.00914599,-0.0101154,0.010079617,-0.033442084,-0.008013277,-0.022154393,-0.00027649823,0.015111077,0.038171463,0.004091649,0.009704998,-0.007822761,0.0054654125,0.017102633,-0.0048320196,-0.036341142,0.006959362,0.013464493,-0.0019662385,0.018220903,0.014682125,-0.019838268,-0.0019696732,-0.04821427,0.0015951953,0.020838134,-0.005588053,0.02277051,0.0031935812,0.015462311,-0.0004622411,0.028678646,0.0030592147,-0.024672946,0.0022972063,-0.009760836,-0.014785555,-0.0070590777,0.038422257,-0.016143013,-0.0071786195,0.016077671,0.033678263,-0.008884638,-0.014305004,0.030702356,-0.0037884314,0.028419798,-0.033454612,0.0021144634,-0.019058475,0.016900955,0.023121338,-0.00698984,-0.028393488,-0.0067942343,-0.018244868,0.005430632,0.032792997,-0.005837656,-0.0063281823,-0.012353912,-0.005572335,-0.0006328974,-0.0022927807,-0.012061837,0.013100521,-0.0040994706,0.01404521,-0.0005078414,-0.004811592,-0.015737174,0.022334144,0.020696536,0.019353583,0.019325532,-0.00680296,0.027288139,0.008903707,0.008002095,-0.0196791,-0.029362949,6.595372e-05,0.0011414094,-0.04112126,0.00096956943,0.014503545,-0.032601293,0.021605054,0.0025632218,0.004827867,-0.007202146,-0.008378478,-0.0023834922,0.001443124,0.013082508,0.01332277,-0.0012033513,0.014248764,0.018004538,-0.010145491,-0.019351384,-0.016844608,-0.008630928,-0.0060330136,-0.02580695,0.0068279267,-0.022644017,-0.014452009,-0.009806201,0.007185143,0.052902788,0.024890078,0.03139388,0.0044064578,0.019629814,0.007879403,-0.011103816,-0.0029039525,-0.013083315,0.032181628,0.013253984,-0.0039197253,-0.0213864,0.036562268,0.0009955565,-0.020022038,0.0015440927,-0.016146902,0.0020094696,0.0009462266,-0.009805667,-0.014286968,0.011572044,-0.0006190705,0.038398616,0.02451845,-0.009576162,-0.0032543838,0.008857909,0.018643461,-0.0027212882,-0.007585635,0.01207534,-0.008459146,-0.011776284,0.0072651743,0.018364122,-0.023328194,-0.0037386876,-0.011224463,0.024737066,-0.023802655,0.013250606,0.0074672997,-0.0005183319,0.0015360884,-0.03801048,0.017532293,0.0018591175,-0.0056804596,0.01083474,-0.007169451,-0.0016667864,0.010552808,0.00790861,-0.00062385685,0.017823994,0.00043654855,0.014684783,-0.007595194,0.016086278,0.011496652,-0.0141618075,-0.012462615,0.017439634,-0.009672852,0.01408729,0.0028703823,0.016426682,0.009296945,-0.024082728,-0.03061572,0.03353984,-0.029106272,0.0049489806,0.015866008,-0.002960617,0.006937111,0.019682894,0.0049210875,0.015831854,-0.01903234,-0.012884354,0.0027801613,0.03135223,-0.011844165,0.00955689,-0.02536398,0.02427634,0.0017619858,0.028874498,-0.012791869,0.013275122,-0.053713195,0.019113354,-0.008430135,0.009396152,-0.0034335018,0.0025275755,-0.023239184,-0.0044570128,-0.001512669,-0.001033672,-0.00762984,0.008979079,-0.01476831,-0.02106694,-0.026042761,0.023384092,-0.025719624,0.01557001,0.0075159287,-0.000785322,0.024300512,0.008859863,0.029018639,-0.022561293,0.012724718,0.03015035,-0.004051763,-0.0022349847,0.01693423,0.01705013,-0.015732678,-0.005213452,-0.01744436,-0.020745268,-0.015360622,0.015918085,-0.009934789,-0.025404546,-0.003792118,0.0036214152,-0.020360969,-0.0013805648,0.008817321,0.0060050352,0.036991224,0.017282953,0.0029513158,0.012923862,0.018026637,-0.019202482,0.01149299,0.0074056303,-0.03979532,-0.0091462135,0.0036577093,-0.014534402,0.018211966,-0.00837613,-0.0054631825,0.026670968,-0.01184895,0.015792606,0.0010700952,0.0011918667,-0.005351987,-0.010909726,-0.0047479635,-0.037707906,-0.017449358,-0.022573613,-0.00691076,-0.022243513,-0.00964012,0.009289626,-0.042505413,0.010587005,0.0121236425,-0.013799396,0.0018237301,0.005070087,-0.0034246524,-0.01263397,-0.031274628,0.01798988,0.007583708,0.01422728,0.00010068903,-0.011443733,0.0037542963,0.018128978,-0.018553395,-0.015229543,0.0002379587,0.015235028,-0.0027900992,-0.007118831,0.016522372,0.027779628,-0.035102144,-0.0061230566,-0.020133229,0.013525675,0.006623368,0.015389859,-0.022160495,0.0096365195,-0.01042355,-0.0071795625,-0.0026430239,-0.015617116,0.022426907,-0.0067848456,0.026896892,-0.019006679,-0.03415952,-0.011576914,0.008030895,0.0041471473,-0.0046861153,-0.012609848,-0.012313036,0.01781284,-0.00741826,-0.026207905,0.016677758,-0.029538035,-0.02070407,0.017741969,-0.015740644,0.007801051,0.016739754,0.0071113785,0.001015793,0.009612687,0.013088432,0.0009474782,-0.015063627,0.004830542,0.008991509,-0.012181309,-0.004745378,0.00429482,-0.015789809,0.020532541,0.028674617,-0.035109434,0.020867763,0.025019431,0.0014379722,-0.07726764,-0.032780807,0.015442676,0.007792505,-0.02823499,-0.010453907,-0.004977646,-0.013591679,-0.016074043,0.020614851,-0.012764597,0.009109506,0.033589475,-0.0011121371,-0.036265157,-0.014574645,-0.005515896,-0.015161129,0.015008072,-0.023732863,0.013001724,-0.022198953,0.00028713123,0.010784338,-0.019986587,-0.009243934,0.01388805,0.015642013,-0.0060820463,0.006612213,-0.007984575,-0.0067766462,0.017032664,-0.009139576,0.013785042,-0.0044887303,0.014315408,-0.012173024,0.0047718463,0.0061728507,-0.02200606,-0.0041943965,0.029999677,-0.014581502,0.023963647,0.024419291,-0.007908264,-0.0039277337,-0.010061194,0.00026187932,-0.05400852,-0.008365939,0.0030338538,-0.0033335318,0.009946053,0.0011940275,-0.0015840916,-0.0041282037,-0.028901223,0.0058024344,-0.013455385,0.017008368,-0.0043512695,0.020653443,-0.025551826,-0.012826489,0.008652207,0.015363675,0.01691079,0.00022419721,-0.02377258,0.005481604,0.010793944,0.00706711,-0.044752665,0.017640334,-0.005789115,0.01995138,0.00092360395,0.013357775,0.0046247276,-0.018414645,-0.08035696,-0.0026115975,-0.00871105,-0.007514655,0.0016247048,-0.03123744,-0.008963045,0.00091422943,-0.000591298,0.011682039,-0.002843382,-0.0015819729,0.00078914437,-0.036404364,0.018531729,-0.028557612,-0.00010877351,0.011592992,-0.030147143,-0.011743257,-0.00025946315,-0.011132135,0.001551346,-0.0059960727,-0.008868293,0.019014182,-0.004502872,0.011142558,-0.00404797,-0.014716917,0.006529515,-0.11215598,-0.005125499,-0.0051980205,0.0027989869,0.009987872,-0.0056276745,-0.0035206226,-0.033442922,-0.007124508,-0.010466555,0.041257866,-0.048012603,-0.040106002,-0.006406773,-0.004686526,0.0891754,0.015274459,0.025536472,0.02552954,0.014447059,0.00018022265,-0.026792103,-0.008287405,0.039516732,0.0035412775,-0.026150778,0.008727632,-0.0056605684,-0.004134263,0.028043512,0.013638834,0.017908484,-0.015923755,0.0054404107,-0.0056479517,-0.0105858045,-0.016818717,0.02017518,-0.0019455864,0.023703301,-0.021911947,0.026267966,0.0018225692,-0.023890315,-0.002297781,-0.012681691,-0.0035374688,-0.0020784982,0.012346843,-0.033123873,-0.027160455,-0.05226265,0.008363878,0.014300561,0.0142547935,0.033239707,-0.020025292,-0.011525236,-0.0037560386,-0.0021264933,0.035431296,-0.013398027,-0.006804195,0.0140244085,0.00895944,0.014854693,0.015350118,0.024045968,0.008864833,-0.0043796743,-0.006297426,-0.009082498,-0.013931352,0.01182622,-0.0031499718,-0.011937466,0.020694993,0.016174778,0.0021358358,-0.0065796534,-0.009820133,0.015554158,0.019434832,-0.031616386,0.009528507,-0.0016487541,0.005143124,-0.022192642,-0.010241818,0.01274821,0.0068502673,0.044282567,-0.009904631,-0.013422371,0.040349625,-0.014579484,-0.013799105,0.039098214,0.007003846,0.0121626565,-0.022099819,-0.037279464,0.017928343,-0.026699437,0.0012945674,-0.01765233,0.008069466,0.04142294,0.055883538,-0.014042074]') ON CONFLICT DO NOTHING;

INSERT INTO public.vector_store VALUES ('137b7a13-e13e-4042-8539-979eec221b51', '# 🌿 실무 필수 Git 브랜치 전략 & 고급 명령어 가이드

> **한 줄 요약**: 소스 코드 버전 관리, 충돌 해결, 커밋 이력 정돈 및 고급 이력 복구를 위한 Git 명령과 브랜치 협업 전략입니다.

---

## 1. 🔀 고급 브랜치 조작 (Rebase, Cherry-pick, Stash)

```bash
# 임시 작업 저장 (작업 중 급한 핫픽스 처리 시)
git stash save "work in progress"
git stash pop

# 특정 타 브랜치의 특정 커밋 단건만 내 브랜치로 가져오기 (Cherry-pick)
git cherry-pick a1b2c3d4

# 커밋 이력을 깔끔한 일자선으로 정돈 (Rebase)
git checkout feature/auth
git rebase main
```

---

## 2. 🚨 실수 복구 및 되돌리기 (Reset, Reflog)

```bash
# 커밋만 취소하고 작업 파일은 유지 (Soft Reset)
git reset --soft HEAD~1

# 완전히 이전 커밋 상태로 되돌리기 (Hard Reset)
git reset --hard HEAD~1

# 실수로 지운 커밋/브랜치까지 포함한 전체 히스토리 확인 및 복구 (Reflog ⭐)
git reflog
git reset --hard HEAD@{3}
```

---

## 3. 📝 커스텀 메시지 규칙
- `feat`: 신규 기능 추가
- `fix`: 버그 수정
- `docs`: 문서 수정
- `refactor`: 코드 리팩토링 (기능 변경 없음)
- `test`: 테스트 코드 추가', '{"title": "[Git] 실무 필수 Git 브랜치 전략 & 고급 명령어 가이드", "userSeq": 1, "guideSeq": 46, "isPublic": true, "chunk_index": 0, "total_chunks": 1, "parent_document_id": "8df6110f-2ea6-448a-abf8-38cfccb2a5a3"}', '[-0.029182352,0.019026523,0.0076471516,-0.047578223,0.00096617127,0.024359794,-0.0116622085,-0.015455013,-0.018835438,-0.00043858014,-0.011906915,-0.017010437,0.019567443,-0.007158023,0.14800984,-0.009693939,-0.017992042,0.015046483,0.012009412,0.0019060177,0.005848873,0.015225641,-9.1862246e-05,-0.008440247,-0.010630194,0.0016313558,-0.010800308,-0.010174628,0.03538561,0.003120736,-0.0107831,0.016631411,0.008551177,0.029624624,-0.028164072,0.011660438,0.013686426,-0.009991264,-0.005290502,0.028893355,0.00619615,0.010824725,0.013781361,-0.017256726,0.014161337,-0.0016980928,0.017173305,-0.02578155,0.0060516517,0.04296321,-0.006025779,0.00911575,0.0045282803,-0.18488738,0.013025862,-0.009104421,0.0034874072,-0.008422499,-0.01252168,0.014130711,-0.028506774,-0.008706404,-0.01785211,-0.034858868,0.0044214386,0.00069827586,0.021566115,0.005885873,-0.012998479,-0.025113417,-0.01984101,-0.005705362,-0.039825153,-0.034804825,0.019618677,-0.004011728,0.01324121,0.0032082926,-0.0054282504,0.01323127,0.002150064,-0.031856764,-0.017781507,-0.0033733377,0.017292751,-0.0002708751,-0.01647847,0.004570709,-0.016520575,0.027707022,0.0411323,0.01483957,-0.006731172,-0.007065952,-0.022384888,0.010182982,0.004191012,0.0051890262,0.015711803,-0.0026242854,0.020795874,-0.023636945,0.007358245,0.01369921,0.019201903,0.014730957,-0.0014353975,-0.007440217,-0.00276547,0.0036955606,-0.0004591425,-0.009459041,0.003729312,-0.017029569,0.009405274,-0.12964243,-0.007750054,-0.020192591,0.009015704,-0.009854626,-0.018256972,0.009045769,0.00021309944,0.021217387,0.004435653,-0.03461284,0.02311062,0.024195956,0.011298598,-0.02925271,-0.040150322,-0.0133291455,-0.0018645256,-0.015868714,-0.0030812798,0.023607943,0.005214651,-0.013844318,-0.029520104,-0.048525363,-0.0013943213,0.0027663421,-0.0047476664,-0.005184761,-0.037739526,0.011122171,-0.023995483,-0.019028945,-0.008174248,-0.015879693,0.01960995,-0.024334082,-0.008143461,-0.007502144,-0.013046426,-0.045143075,0.028173264,-0.018720632,0.004654673,0.010914402,-0.035710085,0.031559147,0.024743365,0.014690288,0.025840476,0.01626497,0.0020888625,0.019119587,-0.028212551,0.016535508,-0.016380511,0.027729454,0.02506043,-0.01885422,-0.0086916415,-0.0046621454,0.017235778,-0.0057155,0.018891132,-0.006012559,-0.0016934497,0.016011994,-0.0040557906,0.017695734,-0.031486016,-0.0034780717,-0.0069935024,-0.0052931476,0.02513342,0.0024970486,0.007295468,-0.01396425,-0.0063936566,-0.025111986,0.035361167,-0.04473821,-0.0020764512,-0.012918159,-0.010035155,0.015864728,0.022393195,0.017574042,-0.019694053,-0.006866076,-0.014703627,-0.0059455023,-0.012462181,-0.014574476,0.019673212,-0.0015749558,-0.01164883,0.027443783,0.016106168,-0.04665151,-0.0068038246,-0.032745216,-0.020053705,-0.015269082,-0.011582475,0.00533277,-0.001166509,0.015681861,0.026435925,0.03304714,0.008746824,-0.028912837,0.021998094,-0.041544247,0.00987185,-0.004180489,0.028772732,-0.0013637285,-0.04664034,0.010155053,0.030679204,0.010122822,-0.028032122,0.04488685,9.515208e-05,0.033621047,-0.0116785085,-0.0003335603,-0.018362204,-0.0076694144,0.023945302,-0.015980223,-0.002093384,0.013610885,0.030849313,-0.011408617,0.0001434536,0.0005685751,0.009401944,-0.013661181,0.0027471073,-0.019544184,-0.03091434,-0.03383188,0.007366385,0.0052630445,-0.00350385,-0.002204426,-0.035404302,-0.0048396657,0.008713995,0.00044724453,0.04119227,0.019803962,-0.022741348,0.0025327327,0.001763908,0.026987525,-0.014261326,-0.038131844,-0.014666145,0.0048789643,-0.042390082,0.045441948,-0.0298126,-0.050850533,3.8651197e-05,-0.0033240414,0.007639453,-0.0017322889,0.013754945,0.014677101,0.0051565473,0.00744885,0.0049071363,-0.019495321,-0.010605551,0.025744513,0.016784871,0.011600484,0.0058488417,-0.011677455,-0.008735195,-0.0012987136,0.0030560899,-0.026211424,0.011960467,0.009060265,0.017393157,0.04438063,0.017189946,0.0030902033,-0.001530103,0.034817725,-0.0036299457,-0.007326036,0.0001996597,-0.03359715,0.018817252,-0.018812368,0.013077249,-0.009605704,0.019321391,-0.007719943,-0.026896518,0.010786859,0.0010529591,-0.015797997,-0.0064018606,-0.0008827148,-0.024244184,-0.011745543,-0.01583022,-0.010214501,0.02631137,0.0078105694,0.014995726,0.0035898567,0.0071892664,0.0055483268,0.009066173,0.024102844,-0.00069584395,-0.0040017455,-0.01187951,0.0031272587,0.0032939552,-0.0066247154,-0.03819251,0.0069586444,-0.040628795,0.0067503457,0.004971799,-0.013532631,0.015927017,-0.037794262,0.0076920222,-0.0070030936,-0.0071193688,-0.008221384,-0.027136581,-0.008375806,-0.010480984,0.0027353135,0.0020404716,0.0047269217,-0.020441018,0.005549061,0.016394334,0.01573657,0.01746865,-0.032292295,0.003497905,0.023617638,-0.029459462,0.0012984748,-0.0208596,0.018025944,-0.0031755737,-0.03346159,-0.016900772,0.02421083,0.0063460334,0.009322357,0.0072044334,-0.015186948,-0.000585132,-0.010740191,-0.02895756,0.00086123525,-0.016329437,-0.017796362,0.004463503,0.003489286,0.0056036534,-0.00864946,0.014498852,-0.01130835,0.0042264755,0.022975795,0.0006683636,0.0082548335,-0.029054299,-0.011568103,-0.029799387,-0.0064312452,-0.016374897,0.0034701081,0.016764536,0.0022125484,0.0023412681,0.012816829,0.002065836,0.0026482267,-0.037317816,-0.025008636,0.0046881195,0.015759759,-0.0076262476,0.018066905,0.0037210886,0.036927585,0.014857841,0.007323542,0.007587258,-0.03856594,0.0004980267,0.017167715,0.0054928404,-0.013179451,0.0073359576,0.0083546275,-0.024777558,-0.01804478,-0.00071327394,-0.017090572,-0.021407273,-0.00991084,0.009986942,-0.008402343,0.017884417,0.0014303523,0.00078848033,0.016186055,-0.013024595,0.014174138,0.018304681,0.011540516,0.005015755,-0.014301035,0.009117063,-0.0066557885,0.005345115,-0.008537983,-0.025811968,7.764317e-05,0.0031747539,-0.023469966,0.018615523,-0.0015507054,0.003910098,0.021640187,-0.015777687,0.009449831,0.0060443515,-0.008598009,-0.002188945,-0.018677102,0.015481696,0.0018966482,-0.016644508,-0.02375607,0.006483089,-0.007891701,-0.031392947,0.019775134,0.009530681,0.026181258,0.021341067,0.00088275457,0.031127889,-0.0063587823,-0.017794766,0.0042187977,-0.0053628916,0.0049903295,-0.020541726,-0.007094691,-0.020319542,-0.013738922,0.02131943,0.008574773,0.01969843,-0.02946907,-0.001164334,0.023001187,0.034040883,0.010992442,0.0110987425,0.022126982,-0.03392202,0.011428252,1.7161432e-05,0.018625608,0.04967986,-0.0031028423,0.0036422713,-0.0049449042,0.010310725,0.010765732,-0.0027523374,-0.0016795645,-0.012360728,0.008480335,0.019433063,-0.01030437,0.012483675,-0.0006610232,0.028969718,-0.011671,-0.0120607335,-0.008636226,0.0018832617,0.023211166,0.02918927,-0.00025395295,-0.020569682,0.017607443,-0.005084484,0.022211557,-0.023966955,-0.0025232784,0.011587848,-0.0040243585,0.010437499,0.023572026,-0.02990113,0.021006491,-0.02487003,0.0091791,0.021537708,-0.013288532,0.013993003,0.013022737,-0.02076557,-0.018078785,0.013091987,0.008560074,0.00269422,0.027823484,-0.015270859,-0.09210841,-0.014996496,0.014510671,0.009584169,0.0010939053,-0.011594185,-0.021487165,-0.00585281,0.005800062,0.00619419,0.019203138,0.004684941,0.02269226,0.00916102,-0.026303768,-0.008849296,-0.016385252,-0.017290827,0.011696665,-0.02357965,-0.0005781059,-0.027894903,0.002863182,0.0024957282,-0.009191558,-0.0010160104,0.00980275,-0.012549579,-0.011350312,0.0017185643,-0.0044459677,-0.003971653,0.018899003,0.020244079,0.013448642,-0.0150301345,0.0050448044,-0.014319732,-0.011370336,-0.015597066,-0.006340589,0.017945496,0.0029557669,0.0047577205,0.015096866,0.019116823,-0.0060587553,-0.022310816,0.0026478525,0.009653385,-0.028606107,-0.009162457,-0.002538105,-0.013093205,0.008624161,0.029046306,-0.022136716,0.020051276,-0.02039299,0.027305298,-0.028527465,0.003678344,-0.023705944,0.04716573,0.011013364,0.0030450907,-0.0062171905,-0.0009520486,0.016896311,-0.008853612,-0.028307809,-0.010615364,-0.016210798,0.00983644,-0.03294816,0.024101404,0.001086793,0.009146469,-0.0007844484,0.005947651,0.017021913,-0.0131418845,-0.07776714,-0.0037086552,0.0095118815,0.007427295,0.0039232057,-0.022647696,0.017425923,-0.009051443,0.007245453,-0.012923161,0.0031037452,-0.018726861,0.0013650755,-0.02468957,0.03866828,0.0023636608,0.011161903,0.0055611567,-0.0024018511,-0.0038616038,0.009624221,0.007949149,0.007870143,0.00014952752,0.005256978,0.02644649,-0.010808214,0.014629084,1.5534783e-05,0.007184051,0.0002982179,-0.12399423,-0.009222244,-0.002689604,-0.0015834608,-0.0077833734,0.023441963,0.002525378,-0.0020619307,-0.019724049,-0.013928657,0.0039081555,-0.013223494,-0.018965239,-0.013697185,-0.007837439,0.08566854,0.007623744,-0.0051860614,0.0175492,-0.021621019,-0.005298544,-0.033379942,0.005022403,0.018651627,-0.028663818,-0.009503361,-0.0014494127,-0.0076647657,0.018434744,-0.0017822293,0.0023649281,0.01420244,-0.013253159,-0.007216083,-0.0017031106,-0.0037177792,0.032454524,0.016582096,0.0030268934,0.015524866,0.006108574,0.014603857,0.013878736,-0.024291128,-0.0021183104,0.0030174197,0.0032219132,-0.03668183,0.021624757,-0.018257638,-0.0061162137,-0.077439286,-0.0067670103,-0.011072692,0.014687812,0.02138676,0.002856777,0.014271285,0.00993112,-0.0060398947,0.027232153,0.0146001745,0.009179464,0.044754896,0.0031402614,-0.0047545503,0.026481982,0.014042022,0.0009853466,-0.012805111,0.007895293,-0.027019886,0.0035003931,0.02170102,-0.01711631,0.0011182912,0.0071100593,0.001732265,-0.0068371277,0.0011644799,0.021989718,0.026545253,0.017789679,-0.016962536,-0.005064689,-0.0012846425,0.011988162,0.00687782,0.021163644,-0.000768531,0.011376903,0.049927775,-0.00050741393,-0.00019623,0.015751692,-0.0031219074,0.0148830395,0.037448056,0.004543791,-0.013205646,0.0017782907,0.002604473,0.008197528,-0.016527655,-0.0032337357,-0.015441659,-0.033556778,0.018531397,0.051823728,0.022630338]') ON CONFLICT DO NOTHING;

INSERT INTO public.vector_store VALUES ('165b8933-fcc9-4738-ab7f-0be39d42e8aa', '# 🌿 실무 필수 Git 브랜치 전략 & 고급 명령어 가이드

> **한 줄 요약**: 소스 코드 버전 관리, 충돌 해결, 커밋 이력 정돈 및 고급 이력 복구를 위한 Git 명령과 브랜치 협업 전략입니다.

---

## 1. 🔀 고급 브랜치 조작 (Rebase, Cherry-pick, Stash)

```bash
# 임시 작업 저장 (작업 중 급한 핫픽스 처리 시)
git stash save "work in progress"
git stash pop

# 특정 타 브랜치의 특정 커밋 단건만 내 브랜치로 가져오기 (Cherry-pick)
git cherry-pick a1b2c3d4

# 커밋 이력을 깔끔한 일자선으로 정돈 (Rebase)
git checkout feature/auth
git rebase main
```

---

## 2. 🚨 실수 복구 및 되돌리기 (Reset, Reflog)

```bash
# 커밋만 취소하고 작업 파일은 유지 (Soft Reset)
git reset --soft HEAD~1

# 완전히 이전 커밋 상태로 되돌리기 (Hard Reset)
git reset --hard HEAD~1

# 실수로 지운 커밋/브랜치까지 포함한 전체 히스토리 확인 및 복구 (Reflog ⭐)
git reflog
git reset --hard HEAD@{3}
```

---

## 3. 📝 커스텀 메시지 규칙
- `feat`: 신규 기능 추가
- `fix`: 버그 수정
- `docs`: 문서 수정
- `refactor`: 코드 리팩토링 (기능 변경 없음)
- `test`: 테스트 코드 추가', '{"title": "[Git] 실무 필수 Git 브랜치 전략 & 고급 명령어 가이드", "userSeq": 1, "guideSeq": 44, "isPublic": true, "chunk_index": 0, "total_chunks": 1, "parent_document_id": "65735894-f8ca-455e-8fd0-8845006deab9"}', '[-0.029182352,0.019026523,0.0076471516,-0.047578223,0.00096617127,0.024359794,-0.0116622085,-0.015455013,-0.018835438,-0.00043858014,-0.011906915,-0.017010437,0.019567443,-0.007158023,0.14800984,-0.009693939,-0.017992042,0.015046483,0.012009412,0.0019060177,0.005848873,0.015225641,-9.1862246e-05,-0.008440247,-0.010630194,0.0016313558,-0.010800308,-0.010174628,0.03538561,0.003120736,-0.0107831,0.016631411,0.008551177,0.029624624,-0.028164072,0.011660438,0.013686426,-0.009991264,-0.005290502,0.028893355,0.00619615,0.010824725,0.013781361,-0.017256726,0.014161337,-0.0016980928,0.017173305,-0.02578155,0.0060516517,0.04296321,-0.006025779,0.00911575,0.0045282803,-0.18488738,0.013025862,-0.009104421,0.0034874072,-0.008422499,-0.01252168,0.014130711,-0.028506774,-0.008706404,-0.01785211,-0.034858868,0.0044214386,0.00069827586,0.021566115,0.005885873,-0.012998479,-0.025113417,-0.01984101,-0.005705362,-0.039825153,-0.034804825,0.019618677,-0.004011728,0.01324121,0.0032082926,-0.0054282504,0.01323127,0.002150064,-0.031856764,-0.017781507,-0.0033733377,0.017292751,-0.0002708751,-0.01647847,0.004570709,-0.016520575,0.027707022,0.0411323,0.01483957,-0.006731172,-0.007065952,-0.022384888,0.010182982,0.004191012,0.0051890262,0.015711803,-0.0026242854,0.020795874,-0.023636945,0.007358245,0.01369921,0.019201903,0.014730957,-0.0014353975,-0.007440217,-0.00276547,0.0036955606,-0.0004591425,-0.009459041,0.003729312,-0.017029569,0.009405274,-0.12964243,-0.007750054,-0.020192591,0.009015704,-0.009854626,-0.018256972,0.009045769,0.00021309944,0.021217387,0.004435653,-0.03461284,0.02311062,0.024195956,0.011298598,-0.02925271,-0.040150322,-0.0133291455,-0.0018645256,-0.015868714,-0.0030812798,0.023607943,0.005214651,-0.013844318,-0.029520104,-0.048525363,-0.0013943213,0.0027663421,-0.0047476664,-0.005184761,-0.037739526,0.011122171,-0.023995483,-0.019028945,-0.008174248,-0.015879693,0.01960995,-0.024334082,-0.008143461,-0.007502144,-0.013046426,-0.045143075,0.028173264,-0.018720632,0.004654673,0.010914402,-0.035710085,0.031559147,0.024743365,0.014690288,0.025840476,0.01626497,0.0020888625,0.019119587,-0.028212551,0.016535508,-0.016380511,0.027729454,0.02506043,-0.01885422,-0.0086916415,-0.0046621454,0.017235778,-0.0057155,0.018891132,-0.006012559,-0.0016934497,0.016011994,-0.0040557906,0.017695734,-0.031486016,-0.0034780717,-0.0069935024,-0.0052931476,0.02513342,0.0024970486,0.007295468,-0.01396425,-0.0063936566,-0.025111986,0.035361167,-0.04473821,-0.0020764512,-0.012918159,-0.010035155,0.015864728,0.022393195,0.017574042,-0.019694053,-0.006866076,-0.014703627,-0.0059455023,-0.012462181,-0.014574476,0.019673212,-0.0015749558,-0.01164883,0.027443783,0.016106168,-0.04665151,-0.0068038246,-0.032745216,-0.020053705,-0.015269082,-0.011582475,0.00533277,-0.001166509,0.015681861,0.026435925,0.03304714,0.008746824,-0.028912837,0.021998094,-0.041544247,0.00987185,-0.004180489,0.028772732,-0.0013637285,-0.04664034,0.010155053,0.030679204,0.010122822,-0.028032122,0.04488685,9.515208e-05,0.033621047,-0.0116785085,-0.0003335603,-0.018362204,-0.0076694144,0.023945302,-0.015980223,-0.002093384,0.013610885,0.030849313,-0.011408617,0.0001434536,0.0005685751,0.009401944,-0.013661181,0.0027471073,-0.019544184,-0.03091434,-0.03383188,0.007366385,0.0052630445,-0.00350385,-0.002204426,-0.035404302,-0.0048396657,0.008713995,0.00044724453,0.04119227,0.019803962,-0.022741348,0.0025327327,0.001763908,0.026987525,-0.014261326,-0.038131844,-0.014666145,0.0048789643,-0.042390082,0.045441948,-0.0298126,-0.050850533,3.8651197e-05,-0.0033240414,0.007639453,-0.0017322889,0.013754945,0.014677101,0.0051565473,0.00744885,0.0049071363,-0.019495321,-0.010605551,0.025744513,0.016784871,0.011600484,0.0058488417,-0.011677455,-0.008735195,-0.0012987136,0.0030560899,-0.026211424,0.011960467,0.009060265,0.017393157,0.04438063,0.017189946,0.0030902033,-0.001530103,0.034817725,-0.0036299457,-0.007326036,0.0001996597,-0.03359715,0.018817252,-0.018812368,0.013077249,-0.009605704,0.019321391,-0.007719943,-0.026896518,0.010786859,0.0010529591,-0.015797997,-0.0064018606,-0.0008827148,-0.024244184,-0.011745543,-0.01583022,-0.010214501,0.02631137,0.0078105694,0.014995726,0.0035898567,0.0071892664,0.0055483268,0.009066173,0.024102844,-0.00069584395,-0.0040017455,-0.01187951,0.0031272587,0.0032939552,-0.0066247154,-0.03819251,0.0069586444,-0.040628795,0.0067503457,0.004971799,-0.013532631,0.015927017,-0.037794262,0.0076920222,-0.0070030936,-0.0071193688,-0.008221384,-0.027136581,-0.008375806,-0.010480984,0.0027353135,0.0020404716,0.0047269217,-0.020441018,0.005549061,0.016394334,0.01573657,0.01746865,-0.032292295,0.003497905,0.023617638,-0.029459462,0.0012984748,-0.0208596,0.018025944,-0.0031755737,-0.03346159,-0.016900772,0.02421083,0.0063460334,0.009322357,0.0072044334,-0.015186948,-0.000585132,-0.010740191,-0.02895756,0.00086123525,-0.016329437,-0.017796362,0.004463503,0.003489286,0.0056036534,-0.00864946,0.014498852,-0.01130835,0.0042264755,0.022975795,0.0006683636,0.0082548335,-0.029054299,-0.011568103,-0.029799387,-0.0064312452,-0.016374897,0.0034701081,0.016764536,0.0022125484,0.0023412681,0.012816829,0.002065836,0.0026482267,-0.037317816,-0.025008636,0.0046881195,0.015759759,-0.0076262476,0.018066905,0.0037210886,0.036927585,0.014857841,0.007323542,0.007587258,-0.03856594,0.0004980267,0.017167715,0.0054928404,-0.013179451,0.0073359576,0.0083546275,-0.024777558,-0.01804478,-0.00071327394,-0.017090572,-0.021407273,-0.00991084,0.009986942,-0.008402343,0.017884417,0.0014303523,0.00078848033,0.016186055,-0.013024595,0.014174138,0.018304681,0.011540516,0.005015755,-0.014301035,0.009117063,-0.0066557885,0.005345115,-0.008537983,-0.025811968,7.764317e-05,0.0031747539,-0.023469966,0.018615523,-0.0015507054,0.003910098,0.021640187,-0.015777687,0.009449831,0.0060443515,-0.008598009,-0.002188945,-0.018677102,0.015481696,0.0018966482,-0.016644508,-0.02375607,0.006483089,-0.007891701,-0.031392947,0.019775134,0.009530681,0.026181258,0.021341067,0.00088275457,0.031127889,-0.0063587823,-0.017794766,0.0042187977,-0.0053628916,0.0049903295,-0.020541726,-0.007094691,-0.020319542,-0.013738922,0.02131943,0.008574773,0.01969843,-0.02946907,-0.001164334,0.023001187,0.034040883,0.010992442,0.0110987425,0.022126982,-0.03392202,0.011428252,1.7161432e-05,0.018625608,0.04967986,-0.0031028423,0.0036422713,-0.0049449042,0.010310725,0.010765732,-0.0027523374,-0.0016795645,-0.012360728,0.008480335,0.019433063,-0.01030437,0.012483675,-0.0006610232,0.028969718,-0.011671,-0.0120607335,-0.008636226,0.0018832617,0.023211166,0.02918927,-0.00025395295,-0.020569682,0.017607443,-0.005084484,0.022211557,-0.023966955,-0.0025232784,0.011587848,-0.0040243585,0.010437499,0.023572026,-0.02990113,0.021006491,-0.02487003,0.0091791,0.021537708,-0.013288532,0.013993003,0.013022737,-0.02076557,-0.018078785,0.013091987,0.008560074,0.00269422,0.027823484,-0.015270859,-0.09210841,-0.014996496,0.014510671,0.009584169,0.0010939053,-0.011594185,-0.021487165,-0.00585281,0.005800062,0.00619419,0.019203138,0.004684941,0.02269226,0.00916102,-0.026303768,-0.008849296,-0.016385252,-0.017290827,0.011696665,-0.02357965,-0.0005781059,-0.027894903,0.002863182,0.0024957282,-0.009191558,-0.0010160104,0.00980275,-0.012549579,-0.011350312,0.0017185643,-0.0044459677,-0.003971653,0.018899003,0.020244079,0.013448642,-0.0150301345,0.0050448044,-0.014319732,-0.011370336,-0.015597066,-0.006340589,0.017945496,0.0029557669,0.0047577205,0.015096866,0.019116823,-0.0060587553,-0.022310816,0.0026478525,0.009653385,-0.028606107,-0.009162457,-0.002538105,-0.013093205,0.008624161,0.029046306,-0.022136716,0.020051276,-0.02039299,0.027305298,-0.028527465,0.003678344,-0.023705944,0.04716573,0.011013364,0.0030450907,-0.0062171905,-0.0009520486,0.016896311,-0.008853612,-0.028307809,-0.010615364,-0.016210798,0.00983644,-0.03294816,0.024101404,0.001086793,0.009146469,-0.0007844484,0.005947651,0.017021913,-0.0131418845,-0.07776714,-0.0037086552,0.0095118815,0.007427295,0.0039232057,-0.022647696,0.017425923,-0.009051443,0.007245453,-0.012923161,0.0031037452,-0.018726861,0.0013650755,-0.02468957,0.03866828,0.0023636608,0.011161903,0.0055611567,-0.0024018511,-0.0038616038,0.009624221,0.007949149,0.007870143,0.00014952752,0.005256978,0.02644649,-0.010808214,0.014629084,1.5534783e-05,0.007184051,0.0002982179,-0.12399423,-0.009222244,-0.002689604,-0.0015834608,-0.0077833734,0.023441963,0.002525378,-0.0020619307,-0.019724049,-0.013928657,0.0039081555,-0.013223494,-0.018965239,-0.013697185,-0.007837439,0.08566854,0.007623744,-0.0051860614,0.0175492,-0.021621019,-0.005298544,-0.033379942,0.005022403,0.018651627,-0.028663818,-0.009503361,-0.0014494127,-0.0076647657,0.018434744,-0.0017822293,0.0023649281,0.01420244,-0.013253159,-0.007216083,-0.0017031106,-0.0037177792,0.032454524,0.016582096,0.0030268934,0.015524866,0.006108574,0.014603857,0.013878736,-0.024291128,-0.0021183104,0.0030174197,0.0032219132,-0.03668183,0.021624757,-0.018257638,-0.0061162137,-0.077439286,-0.0067670103,-0.011072692,0.014687812,0.02138676,0.002856777,0.014271285,0.00993112,-0.0060398947,0.027232153,0.0146001745,0.009179464,0.044754896,0.0031402614,-0.0047545503,0.026481982,0.014042022,0.0009853466,-0.012805111,0.007895293,-0.027019886,0.0035003931,0.02170102,-0.01711631,0.0011182912,0.0071100593,0.001732265,-0.0068371277,0.0011644799,0.021989718,0.026545253,0.017789679,-0.016962536,-0.005064689,-0.0012846425,0.011988162,0.00687782,0.021163644,-0.000768531,0.011376903,0.049927775,-0.00050741393,-0.00019623,0.015751692,-0.0031219074,0.0148830395,0.037448056,0.004543791,-0.013205646,0.0017782907,0.002604473,0.008197528,-0.016527655,-0.0032337357,-0.015441659,-0.033556778,0.018531397,0.051823728,0.022630338]') ON CONFLICT DO NOTHING;

INSERT INTO public.vector_store VALUES ('3976e1c4-e188-4a4f-9339-c6ad14cca285', '## 할루시네이션과 LLM 평가

**할루시네이션(환각)** 은 LLM이 그럴듯하지만 틀린 내용을 지어내는 현상이다. 모델이 "확률적으로 그럴듯한" 문장을 생성할 뿐 사실을 검증하지 않기 때문에 발생한다.

완화책: RAG로 근거 제공, "모르면 모른다" 지시, 출처 인용 강제, 낮은 temperature.

**LLM 평가** 는 출력 품질을 수치로 재는 것이다.
- 검색(RAG) 평가: Hit@K, MRR, Recall처럼 정답 문서를 잘 찾는지
- 답변 평가: 정답셋 대비 정확도, 또는 **LLM-as-judge**(다른 LLM이 채점)
- 근거 충실도: 답이 제공된 근거에 실제로 기반했는지

평가셋(golden set)을 만들어 지표를 추적하면, 프롬프트·검색 파라미터 튜닝의 효과를 객관적으로 비교할 수 있다.', '{"title": "할루시네이션과 LLM 평가", "userSeq": 1, "guideSeq": 38, "isPublic": true, "chunk_index": 0, "total_chunks": 1, "parent_document_id": "4170ef87-dd35-4cfe-a44e-e867b45e31b1"}', '[-0.031884898,0.007697012,0.017363684,-0.061931822,-0.004419698,-0.01740107,0.008333599,1.5991973e-05,-0.016263023,0.009201755,0.010941664,-0.011078777,0.009697045,-0.0146959545,0.12760797,0.034358487,-0.0014090495,0.024170773,0.032010328,-0.022530355,0.0004965995,0.0072178547,-0.022702498,0.008068686,0.0032631955,-0.005231261,0.015088169,0.016827371,0.035847154,0.0007313014,-0.019341456,0.01177881,0.0054590884,0.047750644,0.0014831859,0.029817516,0.01971835,-0.043156564,0.021427218,0.048507888,0.0075678993,-0.011008659,0.0024423208,-0.029257614,-0.011186752,0.0059608333,0.024436042,0.008521006,0.0061788815,0.027194593,-0.01717667,0.018732069,0.012212184,-0.16888581,-0.018218923,-0.011965434,-0.0044367826,0.010682339,0.021493502,0.020772357,-0.042380575,0.011402624,-0.008696433,-0.017634153,-0.010175485,-0.01189636,0.024509655,0.005591829,-0.017289782,-0.026772441,0.006099255,0.007513937,0.008741135,-0.027524887,0.007234716,-0.035423025,0.035352685,0.013190762,0.0027473175,0.01707654,-0.0003980577,-0.02239577,-0.01690224,-0.011411647,-0.0043923906,0.008726326,-0.008318113,-0.039177984,0.023620797,0.010803697,0.005207911,-0.0066997265,0.009493783,0.04100559,-0.011606542,-0.012434128,-9.039435e-05,0.022407781,-0.027345119,-0.025181802,-0.009125368,-0.011585854,0.035603542,0.027940786,0.006984085,-0.0074077775,0.0062463866,0.008894269,-0.023663677,0.02335624,0.0044897306,-0.009577738,0.020467019,-0.02600485,-0.020367628,-0.15258698,-0.011150635,-0.008437414,-0.0011287922,0.001591188,-0.008873251,0.0046628015,0.002807814,0.021092398,-0.0009230877,-0.015025584,0.026616136,-0.016688906,0.007318183,0.021046998,-0.013746204,-0.0019501292,-0.002974316,-0.0031587693,0.0071069263,0.042903077,-0.0018564089,-0.0073027443,-0.024143774,-0.036255147,5.9915925e-05,0.018820005,0.022524172,-0.017241923,-0.021363415,0.0068317004,-0.012535856,-0.004783979,0.011272281,-0.0065619787,0.01786778,-0.004823755,-0.013114065,0.0076863985,0.028532522,-0.021451125,0.043047212,0.02431437,0.041303232,0.008860611,-0.007972598,0.020579586,-0.015957676,-0.014807893,0.020495338,0.015641628,-0.00815693,-0.015999703,-0.015170598,0.023252774,0.007674701,-0.023987193,-0.0055015692,-0.030094407,-0.017555866,-0.014530266,-0.004831899,-0.01887819,-0.01066217,0.001118959,0.031219006,0.0110693965,-0.028337931,0.013260171,-0.009619907,0.00082963554,0.030848986,0.032493755,0.008406444,0.028025866,-0.03864772,0.02784885,0.036962103,0.007581979,0.02184874,-0.007129069,0.0042953105,-0.008294551,0.012323321,0.025510902,0.0062356107,0.016209193,-0.0018395896,-0.013943072,0.029896082,-0.003924358,0.01391738,0.00029893857,0.02005445,-0.012239011,0.0048464285,0.028423274,-0.004716102,0.0017035535,-0.03199835,-0.01948844,-0.026929285,-0.012778503,0.003006268,-0.014791306,0.011010205,-0.009631545,0.0045215753,0.004878283,-0.019525379,-0.021036265,-0.0015784033,-0.019722851,0.0063188146,0.0034829793,-0.0047479165,-0.012599989,0.019964965,0.015431186,0.033191204,0.022175943,-0.008064493,0.04400932,0.00067514414,-0.011596194,-0.0019267738,-0.007685713,0.011280448,0.033938803,0.01832049,-0.024399284,0.005664925,-0.013274475,-0.0067970473,-0.012634749,-0.0037606303,0.010327772,-0.008543929,-0.017599048,-0.0006080092,-0.0072453213,-0.031567384,-0.014731272,-0.031227373,0.008439867,-0.005644065,0.0051366324,-0.007750251,0.011559558,-0.002215449,0.012183601,0.007887535,-0.017537436,-0.025357997,-0.012316636,-0.024828052,-0.002571299,-0.0050241095,-0.026608327,0.014768754,0.011896946,-0.04528207,0.020640565,0.013433878,-0.0013064941,0.00039402331,0.012617448,0.010864651,-0.011462206,-0.011812218,0.02687895,-0.0005724496,-0.007148851,0.012048393,-0.0044138436,0.0018974392,0.010786527,-0.021612301,-0.013193547,-0.0056977365,-0.025823316,-0.008465389,0.016265059,-0.028721392,0.0032870343,-0.016972693,0.0028467118,-0.018971287,0.0567626,-0.027554475,0.0053208754,0.0066849464,0.00519018,-0.028248368,0.00030401707,-0.0028534844,-0.0018685218,-0.0052623344,-0.008340425,-0.015297684,-0.019086046,0.012061298,-0.008414621,-0.005510612,0.008114546,0.008877975,-0.015135605,-0.021743996,0.0023036997,-0.008820765,-0.02439325,0.007244599,-0.010459386,0.022422174,-0.00434187,-0.004031367,0.022859342,0.0069199377,0.0018028254,-0.0013949905,-0.0036505854,-0.008454449,-0.012797928,-0.0072609056,0.005975095,-0.008969766,0.0030145342,-0.019030247,0.008509638,-0.033454373,0.022179462,-0.0003084697,0.006259994,-0.004413542,-0.030982295,0.014299134,-0.011179509,0.008192971,-0.0097527215,-0.0286854,0.024990728,-0.0051408336,-0.020555578,0.023700483,0.012140964,0.010082014,0.030017393,-0.014384023,0.0057466403,0.025658665,-0.030568447,0.019357199,0.023496127,-0.009452941,0.03736027,0.001292977,-0.011712201,0.011451672,0.0066442126,-0.0046019927,0.00086274924,-0.01996952,0.0045519196,-0.011352435,0.005564507,0.0012462188,0.014008253,-0.04074933,-0.000962166,0.009073388,0.01972427,0.020207737,-0.0060048853,-0.009930444,0.022244677,-0.017494826,0.009620759,0.0044645085,0.023581453,0.009653409,-0.011713786,-0.020528197,0.021705495,0.011683095,-0.005140368,0.0042475867,0.0054162736,-0.0070919525,-0.00053898117,0.016034268,0.024562024,-0.0060595428,0.01084494,0.0186533,-0.010320006,0.0077139367,0.002488996,0.014696532,-0.00020409335,-0.0029714734,0.0056827413,-0.015171362,-0.0058641676,0.0078949835,-0.0067955935,0.0030102564,0.043320697,-0.004944267,-0.016529165,-0.009928204,-0.019898899,-0.01845782,-0.00998967,0.0121491505,-0.015749637,0.0052365777,-0.033908237,-0.036330987,-0.023819977,-0.023208752,-0.004431285,-0.008869241,0.00010507803,-0.04577449,-0.001310291,0.03141501,0.0010961193,0.017538616,-0.003975377,0.022185585,-0.007544853,0.011348679,-0.010038341,-0.013435253,0.012029091,0.035212614,0.023736315,0.025623338,0.002604068,0.018329212,1.2593498e-06,0.0036573734,-0.011920537,0.0120659415,-0.008390477,0.0013075613,-0.03493601,0.029980954,0.03932396,0.013382716,-0.02838821,-0.006689526,-0.0008083221,0.003005904,0.024330338,-0.00075230165,0.014862737,0.005542345,0.0006816789,-0.008321251,0.007208173,0.005760134,0.025390703,-0.0002426273,-0.012155223,-0.022586877,-0.023434259,0.0013468242,0.009207615,-0.02414686,-0.018571006,0.017553741,0.0020459497,-0.010213323,0.016124172,0.008056136,0.031864546,5.1954354e-05,0.028542493,-0.024233403,-0.0073015536,0.008120607,-0.008086261,0.0013568148,-0.019688813,0.026291773,-0.0079702195,-0.0040635383,-0.022966059,-0.010539505,-0.0001599123,-0.025246488,0.013505099,-0.011491662,-0.010634878,-0.012565371,0.01823956,-0.0011612679,0.014570874,-0.00020723834,0.0033443314,0.00585369,0.01362875,-0.0017074249,-0.021820402,-0.0087255,-0.0126257315,-0.020670889,0.019270718,-0.017119795,-0.0032550315,0.009808454,0.019100143,0.014658672,0.03281126,0.008414474,0.015043145,0.00560985,-0.028056711,0.010785903,0.0062516886,0.045843728,0.022877695,0.014418158,0.02356195,-0.028575622,-0.006942355,-0.024919702,-0.02198386,0.04173211,-0.10105237,0.025752138,-0.00699989,0.0020253095,-0.0025647678,-0.025827242,-0.008654601,-0.008135538,0.010316905,0.014141384,-0.013763465,-0.0013937929,0.032445304,0.027437305,-0.011492488,-0.0013626816,0.012007203,-0.017114175,0.01726754,-0.011603612,0.022092,-0.0033607273,0.006543211,0.0272881,0.012921416,0.022018105,0.009423305,0.018609853,0.010175443,-0.005643314,-0.0025444836,0.00076863304,-0.0003456025,-0.0057389126,0.01665654,0.0019401007,0.03916625,-0.013866219,0.014551715,0.019271517,0.012244856,0.0023058506,-0.009453824,-0.031731106,-0.0026438741,-0.009133839,-0.012672999,0.0010753226,0.011759783,0.011177055,-0.033863958,0.0072232042,0.015280968,-0.013353296,-0.026237782,-0.017852934,-0.033391573,0.0059872,0.00192091,0.0015695734,-0.005987062,0.0026985044,0.009596022,0.021303507,0.003041194,0.008184464,0.021705868,0.029775854,0.008402169,0.002065135,-0.016458396,0.0026445738,0.0029789386,0.012498727,-0.028292349,-0.029628124,0.00836323,0.021003203,-0.028173553,-0.007759878,-0.015950674,-0.029264683,-0.094450474,0.0030491655,-0.002354985,-0.0012169004,0.039510306,0.0018376351,0.015524616,-0.029455194,-0.000506466,-0.01868559,0.010409365,-0.024853904,-0.0015146983,-0.01737652,0.012617136,-0.006517565,-0.020366943,-0.0136969155,0.0076916413,-0.021818232,-0.005215696,0.0059471712,0.009364643,-0.009302868,-0.02598704,0.0020073622,-0.0046972013,-0.0018718222,0.0029557913,0.0014819993,0.023629848,-0.12060775,-0.02140469,-0.017722007,-0.009101257,-0.03515231,-0.019069515,-0.020931816,0.03166461,0.014127896,0.011351967,-0.014097387,-0.04634253,-0.008155823,-0.023150243,-0.037324272,0.098887764,-0.026960745,-0.0032169025,-0.017516915,-0.0021418894,-0.00089984864,-0.004677753,-0.017625563,-0.01166072,0.014965296,0.0025644125,0.008215276,-0.011066598,0.014653151,0.049014617,0.017948033,0.011774327,-0.005479736,-0.00026012337,0.01745699,-0.020737814,-0.008861705,-0.015768023,-0.0013709147,0.013258844,0.014723222,0.01167513,0.021256296,-0.006343324,-0.005701657,-0.010345508,-0.028314564,-0.046926513,0.0026855094,-0.0033449347,-0.01738832,-0.049719565,0.014351597,0.0065428284,0.0014682909,0.026995543,0.00504609,0.00724695,-0.003913308,0.012499642,0.022693222,-0.022612508,0.005157542,0.0068739033,-0.009609361,-0.0029816085,0.01922159,0.017269596,-0.010983704,0.0103447465,-0.0134322215,-0.01600301,-0.007147413,-0.008214328,-0.0028357222,-0.012770481,0.0155563615,0.016459584,-0.0038387536,-0.011508561,0.0035440407,0.024078323,0.0017848993,-0.006066001,0.0035826906,0.006619338,0.009998889,0.0001276552,9.6575066e-05,0.0013566148,0.009800447,0.030304871,-0.009916204,-0.0009669104,-0.009186893,-0.015884772,-0.003526246,0.00015285006,0.015153103,-0.0052852724,0.005709425,-0.014921147,0.027654631,-0.035512183,-0.013032458,0.013836972,-0.008058568,0.027432382,0.007087091,-0.004516302]') ON CONFLICT DO NOTHING;

INSERT INTO public.vector_store VALUES ('41bb9acf-de56-4a6a-91bf-8de607fb45b1', '## 파인튜닝과 RAG 비교

둘 다 LLM을 특정 도메인에 맞추는 방법이지만 접근이 다르다.

**파인튜닝(fine-tuning)** 은 추가 데이터로 모델 가중치를 재학습해 **말투·형식·특정 작업 능력**을 내재화한다. 지식이 모델 안에 박히지만, 갱신하려면 다시 학습해야 하고 비용·시간이 든다.

**RAG** 는 가중치를 건드리지 않고 **외부 지식을 검색해 주입**한다. 지식 갱신이 문서 교체만으로 즉시 되고 출처를 댈 수 있다.

선택 기준
- 자주 바뀌는 사실·사내 문서 → RAG
- 고정된 말투·출력 형식·특수 작업 숙련 → 파인튜닝
- 실무에선 **둘을 병행**하기도 한다(형식은 파인튜닝, 지식은 RAG).', '{"title": "파인튜닝과 RAG 비교", "userSeq": 1, "guideSeq": 34, "isPublic": true, "chunk_index": 0, "total_chunks": 1, "parent_document_id": "e3c91411-60e5-43b1-b9af-b0af2f33ad0f"}', '[-0.019652868,0.004672647,0.017249925,-0.06490535,-0.036378745,-0.007389241,-0.013361873,0.018360827,-0.017482754,0.002183513,-0.0069551202,-0.013406652,0.019866955,-0.025676815,0.122527435,-3.8112026e-05,-0.021764275,0.015812637,0.0074021253,-0.014300876,0.008000488,0.039504427,-0.031880174,0.0016277456,0.009723993,0.0032002996,0.019594854,-0.009577031,0.032801315,-0.011609907,-0.00820551,-0.0012192031,0.021457355,0.042967964,0.002934382,0.018222332,0.007317559,-0.045032997,0.00044230657,0.012492113,0.00026491864,-0.0020235325,0.007885388,0.0009520009,-0.01721485,0.0019146565,-0.014090458,-0.029292386,-0.026088325,0.021803508,-0.023369234,-0.00070206163,0.0076968432,-0.17796063,0.030305546,-0.015488419,-0.019115742,0.013283423,0.015380255,0.034627814,-0.016422672,0.026904073,0.0054434715,0.015367586,-0.011822837,-0.02637034,-0.00311801,-0.008300275,-0.006471977,-0.017057499,0.028244585,-0.028810197,0.001183866,-0.022397174,0.0065683187,-0.023651423,0.044771094,0.014431868,0.023868151,0.012347742,0.008914846,-0.0066305976,-0.020196594,-0.00083694933,-0.008824146,0.0063627223,0.004778291,-0.024738943,-0.00035642032,0.011874101,-0.0028508864,-0.0011297308,0.018723954,0.02046873,0.0032953932,0.0054442636,-0.0052276943,-0.017241087,-8.4765656e-05,-0.03182076,-0.01508808,-0.020737316,-0.008836512,0.035161186,0.0034092048,-0.002378786,0.025628606,0.02151858,-0.008786426,0.026082367,-0.0056429124,-0.005627627,0.00037689085,-0.013752816,-0.0073589943,-0.13349274,-0.002906147,0.017646482,0.019807959,0.016055865,-0.005312689,0.022127965,0.029008783,0.030451087,0.016683286,-0.015394278,0.008219297,-0.031246759,0.008086778,0.020958312,-0.018651947,-0.014184992,0.00050168793,-0.004355419,0.0030775624,0.017837083,0.014759919,-0.02768562,-0.025155324,-0.022216022,0.00020859796,0.0146350525,0.0065328684,-0.00047232845,-0.0043901415,-0.02669691,-0.04763847,-0.00027768395,0.010146464,-0.028146565,0.020046543,0.0080480715,-0.020258468,-0.006459115,0.020011809,-0.033164352,0.02572374,0.028201597,0.021506598,0.00086644036,-0.025336606,0.010247366,0.038142286,-0.0011441733,0.018982712,-0.007990588,-0.016898528,0.0020903863,-0.026891181,-0.0065764696,0.01484899,-0.002264282,-0.015490814,-0.021722317,-0.001450033,0.0052737347,-0.0019907986,-0.015185094,-0.009141819,0.00409277,0.02249298,-0.008242227,-0.0050205383,0.0015290078,-0.015223163,-0.010274315,0.00014860954,0.0073955264,0.0131484,0.007386864,-0.04526941,0.01778238,0.020744307,0.0136986775,-0.010538716,-0.012638879,0.00652038,0.00082023756,0.009758419,0.031180706,0.0135255335,0.014179446,-0.005391328,0.004622618,0.045057632,-0.014957498,0.0017006084,-0.015391005,0.0346851,-0.020822462,0.0076281363,0.01403768,0.00478969,-0.016342642,-0.03746603,-0.034715906,0.010737177,-0.0060239844,-0.0032733863,-0.0055532535,0.021039767,0.008815409,-0.0012128575,-0.008379683,-0.008054637,0.0026759598,-0.03495272,-0.02506285,0.016140131,0.01998605,-0.0023143431,0.011527556,0.016850814,-0.0038246945,0.035221443,-0.009499891,-0.002773468,0.0282701,0.012229708,0.0021648684,-0.03190102,-0.0011464115,-8.5687556e-05,0.034156647,0.03081518,-0.013596069,0.010449816,-0.013932583,0.00013227023,0.012374072,-0.008527184,-0.0022008715,-0.022384528,0.0032704147,-0.022132318,-0.027883388,0.005326457,-0.005205798,0.009026666,-0.0009733713,-0.00436303,-0.008174985,0.013912785,-0.019783555,0.010826699,0.04460656,0.01688049,-0.011375198,-0.007106281,-0.002378096,-0.009314163,0.024083717,-0.000109222354,-0.023733338,0.014130938,-0.014617903,-0.040949706,0.03857677,0.012646965,-0.0014279615,-0.014256377,0.022585817,0.025489587,0.04399733,-0.0036788362,0.017058497,-0.012218449,-0.009276863,0.016390774,0.008061053,0.0035142074,0.022540806,-0.011190727,-0.008837339,0.0029109747,-0.022704532,-0.012246409,-0.004246787,-0.0051235794,-0.011887866,-0.006288101,0.011752563,-0.012552604,0.050387956,-0.002985812,0.0023578163,-0.0032383455,0.04572514,-0.01533369,-0.0007973945,0.0075317933,-0.013075907,-0.00018117476,-0.01508658,-0.013621414,0.00011568409,0.011262258,0.0041173412,-0.020286484,0.0067019826,-0.010764934,-0.006961104,-0.030491764,-0.01048555,-0.012665975,-0.021769408,-0.018619172,-0.004544579,0.033579413,0.009129499,0.022477685,0.020461686,0.030439671,-0.020315995,-0.0045335325,0.020266822,0.0038392532,-0.016400333,-0.04067895,0.008755733,0.025126709,-0.025434582,-0.013665743,0.008224316,-0.013871278,0.033300236,0.013985518,0.0087181395,-0.012834912,-0.021250749,0.015915314,-0.005216165,-0.0021433684,-0.05601078,-0.03616212,0.03951893,-0.028616227,0.0039274557,0.008586517,-0.010231782,-0.00021421403,0.016908702,-0.0036258043,0.016571691,0.04452989,-0.03758403,-0.0077749463,0.028746124,-0.0038470656,0.02922346,-0.007268067,-0.0150563875,0.008390439,-0.026855424,0.012324907,0.010130796,-0.03112476,-0.016316243,-0.0043608807,0.01612063,-0.008592129,0.027508544,-0.01035094,-0.011936783,-0.010177596,-0.011283197,0.018875035,0.011073891,0.012413793,0.011313583,-0.015156442,0.011012158,-0.011751179,0.029911716,0.021553574,-0.00980918,-0.011562908,0.00944585,-0.010540525,-0.028167918,-0.014116613,-0.011627117,-0.015326717,0.00910491,0.0023998062,0.0077894824,0.016443325,-0.004325055,-0.0394106,-0.02498331,0.0125218835,0.00022883117,-0.0081649665,0.007004557,0.009386535,0.0065941727,-0.008502907,0.014305565,0.016004272,-0.012798789,0.0072783325,0.047108393,0.004585256,-0.00537948,-0.027183251,-0.017232446,-0.036197532,-0.008367251,0.025160903,-0.023327712,-0.013772674,-0.020201614,-0.028125823,-0.023474906,-0.013907994,0.005164296,-0.002882736,0.0068605044,0.0030538668,-0.00091964065,0.032904066,0.014722382,-0.009177381,0.008700002,-0.010230843,-0.004542859,-0.008548982,-0.003452952,-0.0265094,-0.011556558,0.045523915,-0.0047319853,0.011215211,-0.008754324,0.024617579,0.0044253073,-0.010315335,-0.00070067734,-0.012425397,-0.014903446,-0.01796391,0.0054424745,0.019903224,0.031614125,0.022104098,-0.03385837,0.0048141736,-0.017653529,-0.014143256,0.036653165,0.0095091695,0.014407648,0.0117567815,-0.0031975182,-0.010388341,0.0051943194,0.018935988,0.012130492,0.0015404545,0.005420883,0.024417318,-0.014395061,0.010294796,0.016677845,0.010567579,0.016013797,0.015853494,-0.005011258,0.008192022,0.0068833553,-0.013380993,0.0024105178,0.015974035,-0.0032201775,0.005017435,-0.015625445,0.024607822,-0.037055608,-0.028565168,-0.01062638,-0.018386954,-0.0009926256,-0.021741603,-0.040501017,0.0019335214,-0.018118203,-0.017172193,0.023694346,0.0043125167,0.0065523083,-0.0061397837,0.044094257,0.0002192302,0.010517313,-0.014676678,0.011404906,0.013599452,0.020593707,0.009851734,-0.020058535,-0.018643055,0.014756856,-0.007913747,-0.0018934406,-0.020286547,-0.015360354,0.0141367875,0.008668047,0.009396111,0.016989566,-0.00051577284,-0.0037279523,-0.0010129538,-0.020032503,-0.007164358,-0.0048319236,0.020463783,0.029939638,-0.016712071,0.00978901,-0.012905749,0.011805835,-0.0041925767,-0.021527464,0.02327116,-0.09807879,0.018346194,0.009932084,0.0069022924,-0.008166865,-0.0056967805,-0.024305202,-0.01529463,-0.0004499628,0.017472671,-0.011352974,-0.0019411732,0.022521485,0.00797827,0.0059021953,-0.013547097,-0.016350096,-0.041142587,0.020386813,-0.012373478,0.03382994,-0.005793137,0.0016841942,0.012819488,-0.0053019775,-0.010993219,0.008781799,0.018401057,-0.0022881732,0.0028079222,-0.01231596,-0.013012672,0.0054981518,0.00322419,-0.010039872,-0.0051237782,0.039000235,0.0017410195,0.0069442694,0.006633721,-0.008417145,-0.00869347,0.023387747,-0.012390699,0.00847293,-0.0022170476,-0.035671722,-0.017638497,0.019511873,-0.0032249866,-0.038893837,-0.0046050795,0.004679384,-0.016555252,-0.010303076,-0.0026786758,-0.0147907585,0.02162433,0.0009898449,0.0035894632,-0.0022062906,-0.029952163,-0.0045721564,0.031907793,-0.035045527,-0.007761288,0.009088481,0.0212724,0.009163709,0.0024893312,-0.0014943384,-0.008735568,-0.0036462466,0.001181411,-0.03419135,-0.028069114,-0.0032031797,0.008252368,0.010664798,0.015336433,-0.026099183,-0.006465992,-0.08082066,0.0007894338,0.00022786217,-0.0017505307,0.034287572,-0.009296649,0.0055436273,-0.0051384075,0.006599121,-0.023575522,-0.0004436379,-0.032433536,-0.034252394,-0.029073102,0.058414713,0.018981457,-0.009263815,0.005800597,0.0031162838,-0.007955423,0.00042913182,0.004866785,-0.008104069,-0.00021595985,-0.037878904,0.004874578,-0.0065254434,0.00856338,-0.016883882,-0.0085120695,0.032308973,-0.112140566,-0.010209174,0.005191325,0.0128113525,0.012128321,-0.007831535,-0.021281261,0.028645948,0.007897274,-0.03549195,-0.0033233934,-0.033084743,-0.010366046,-0.015620459,0.002816857,0.11412691,-0.0012161017,0.00069055293,-0.0021609406,0.00044373758,-0.0045906985,-0.021238746,-0.0046061384,0.017477088,-0.013275552,-0.0104846805,-0.0007464171,-0.018363377,0.005390253,0.014114211,0.026743622,-0.0010640167,-0.015109814,-0.0028479774,0.016209764,0.008386667,7.2632747e-06,-0.0059106997,-0.0047338745,-0.0060490295,0.034768414,0.016232975,-0.0003029422,-0.022513473,-0.017309403,-0.003116093,-0.031499017,-0.025073124,-0.013637139,-0.0014321342,-0.015882649,-0.07333996,0.0053851157,0.0067847087,0.018509846,0.009780391,-0.012289328,0.016705101,0.012113663,0.019898718,0.016236162,-0.0056730886,-0.0047992794,-0.0016952854,-0.0053361533,-0.0016734326,0.0031898255,0.014886836,0.017158976,0.027314477,0.0039458442,-0.0024069527,0.0096462695,-0.007624066,0.009895202,-0.016019253,0.01747176,0.010796563,-0.004374318,-0.028161211,0.011604937,-0.0043188767,-0.016056083,0.001853647,-0.010519642,-0.007465844,-0.010986091,-0.012127236,0.011357022,-0.006731718,0.026539063,0.0384971,-0.009344236,0.00933259,0.010405216,-0.004901581,0.012292532,-0.0064253826,0.0072528413,0.0030558486,-0.022282861,-0.011299727,-0.009557856,-0.03067726,-0.00815892,0.0027684618,-0.01226243,0.008532063,0.0031317223,-0.009237313]') ON CONFLICT DO NOTHING;

INSERT INTO public.vector_store VALUES ('5621c950-f689-4795-b1e5-5266890a2388', '# ⚡ Redis 캐싱 전략 및 Spring Boot Caching 가이드

> **한 줄 요약**: Redis는 인메모리(In-Memory) 기반의 초고속 데이터 구조 저장소로, DB 조회를 줄여 응답 속도를 10배 이상 향상시키는 캐시(Cache) 레이어로 활용됩니다.

---

## 1. 🧠 대표적인 캐시 패턴

1. **Look-Aside (Cache-Aside) 패턴 (가장 보편적 ⭐)**:
   - 앱이 먼저 Redis 조회를 시도 ➔ 데이터가 있으면 반환(Cache Hit) ➔ 없으면 DB 조회(Cache Miss) 후 Redis에 저장.
2. **Write-Through 패턴**:
   - DB와 Redis에 동시에 데이터를 기록하여 항상 최신 데이터 유지.

---

## 2. 💻 Spring Boot `@Cacheable` 적용 예시

```java
@Service
public class CommonCodeService {

    // 공통코드 목록 조회 결과는 변경 빈도가 낮으므로 1시간 캐싱
    @Cacheable(value = "commonCodes", key = "#groupCode")
    public List<CommonCodeVo> getCodes(String groupCode) {
        return commonCodeRepository.findByGroupCode(groupCode);
    }
}
```', '{"title": "[Performance] Redis 캐싱 전략 및 Spring Boot Caching 가이드", "userSeq": 1, "guideSeq": 28, "isPublic": true, "chunk_index": 0, "total_chunks": 1, "parent_document_id": "d3fc8da1-74c9-40c3-9686-a5b187220c04"}', '[-0.0250984,0.0008262952,0.0056818253,-0.06582863,-0.033638716,0.0015416815,-0.033004086,-0.010557891,-0.0049730386,-0.0044144965,-0.01957482,0.012620624,0.01621367,-0.006948924,0.13830289,0.00079822534,0.00948603,0.011136548,-0.00075558876,-0.020630145,-0.003633687,0.023813708,0.002932446,-0.0017298309,-0.027675359,-0.0034949174,0.006980578,-0.0023718176,0.05267981,0.020260124,-0.03777714,0.018294841,0.017057827,-0.014840836,0.008603588,0.010992029,0.01588696,-0.008952235,-0.012049663,0.04413094,0.0072202864,-0.017018717,0.00033412315,0.0019357093,0.007451436,-0.0020315184,0.0026523962,-0.006152527,-0.016702663,0.055214204,0.011483777,-0.00805663,0.0023892974,-0.18606827,0.00850818,0.0016405152,-0.02056824,0.015534743,-0.025878899,-0.010027244,-0.00011334138,0.030786201,-0.0038559574,-0.012881287,-0.0061475122,-0.00793097,0.026590358,0.00090024126,-0.0005619819,-0.0059175477,-0.014113883,-0.0061348127,-0.011101744,-0.012484441,0.025984399,0.0041280836,0.017545471,-0.00676071,-0.022523507,0.008496948,0.011199044,-0.013473438,-0.0014189146,-0.00027103417,-0.00088067574,-0.0076394435,-0.009637518,-0.009547829,0.0100887325,0.021435643,0.027986167,-0.0066776136,0.0081086205,-0.01429007,-0.011081978,0.0015429629,-0.026459632,0.0006056734,0.008166359,-0.01873453,-0.0010128415,0.0040891934,-0.0038031777,0.0077319313,-0.0036526145,-0.016270122,-0.0016970218,-0.014505067,-0.01499948,0.017264837,0.015738957,-0.02034606,0.024246054,-0.0074506914,-0.017687546,-0.123410694,0.012061414,-0.023992281,0.007859193,0.0041513364,-0.018233022,0.017635543,0.016946042,0.0017546457,0.006918418,-0.028174205,0.015389809,-0.0076835975,0.032428674,-0.0029803552,0.00013236933,0.0032607487,-0.0012638128,-0.007005888,-0.003960578,0.014963402,-0.014817327,-0.02532297,-0.022283154,-0.0058994587,-0.006594563,0.017639332,-0.018927867,-0.0020363913,-0.007078833,0.003817622,-0.05635507,-0.011837305,-0.01122163,0.0018065852,0.012457624,-0.027398651,-0.012670272,0.014881191,0.025598994,-0.04567778,0.022365654,-0.013688185,0.022202156,-0.011107468,-0.021360891,-0.0037573013,0.023529405,0.02488891,0.009644091,0.0026824605,0.013578189,-0.010587438,0.0055917567,0.0062406715,-0.013179972,0.013918596,-0.0041492605,-0.006547596,-0.020587964,-0.026158625,-0.0017774228,-0.011918997,-0.012065212,-0.0022790064,0.010723019,-0.0048677674,-0.00788142,-0.004291592,-0.01896915,0.0013227786,0.01907974,0.002790161,0.016638502,0.017077226,0.01598771,0.008568457,0.026702348,-0.00032871906,-0.014873067,-0.053672582,0.004722353,-0.009906794,-0.0036746126,0.00021230069,0.0015528748,0.04469584,-0.0075138714,-0.01217435,-0.015805647,0.01576546,0.008295138,-0.022308819,0.0035447655,0.022115687,-0.024285052,0.003562736,0.031798214,-0.036791902,-0.0086949505,-0.02244722,-0.02003707,-0.00019129518,-0.0074722413,-0.019362945,-0.0029177498,0.00067533576,0.01216954,0.029604593,0.04228392,-0.009492121,-0.01967328,-0.0135950055,0.003862963,-0.0038400926,0.0369033,-0.020682639,-0.010910878,0.021362323,0.04074562,0.006149718,-0.022886042,0.016767822,0.02254549,0.014365603,-0.024024826,0.0042074784,-0.00073668605,0.034102395,0.02744132,0.0077845994,-0.01314039,0.03702158,-0.0056081903,0.018063778,0.0005482268,0.0031583426,-0.001712576,-0.017115146,0.003586234,-0.0236876,-0.020496948,-0.009659112,-0.009626311,0.018915761,-0.011271879,-0.013625067,0.0038748034,-0.014984818,-0.02620743,0.009283403,0.00077490805,0.011506159,-0.014372596,0.012001937,0.035525918,0.027148873,-0.005098994,-0.016694808,0.0023519425,-0.036389984,-0.01590745,0.020972611,-0.007814491,-0.03342015,-0.0063616196,0.020256633,0.001709189,0.020034293,-0.02929721,0.006765105,0.010579031,-0.014879865,0.022872133,-0.015030992,0.016087545,0.018007273,0.012013433,-0.019003913,0.005492471,-0.004761812,-0.028000886,-0.00588972,0.005493203,-0.007523631,-0.0043489346,-0.0016329441,-0.018072205,0.029429922,0.027112428,-0.008331873,-0.021782767,0.0141382525,-0.0041645057,0.023808416,-0.027081266,-0.011798961,-0.002232578,0.002643042,0.0027636467,-0.032915052,0.030903077,0.0030668525,-0.03804239,-0.012142941,-0.019858621,-0.011498355,-0.0034809266,0.028459106,-0.026273344,-0.012353907,0.004464603,-0.02108101,0.04518084,-0.026505105,0.0097211115,0.008106517,0.007852431,0.017732536,-0.002500915,-0.013458403,-0.007433634,0.0040168357,-0.01929173,0.0098554315,-0.023642657,-0.0073894463,-0.027963571,-0.008207465,-0.057459928,0.006680904,0.015480211,0.0048007676,-0.010304931,-0.016918818,-0.007982193,0.01382084,0.015335102,-0.0067886836,-0.0019832114,-0.0011141452,-0.024016047,-0.0014760741,0.0072624045,-0.003294295,-0.008231461,0.034974672,-0.010357358,0.004158296,0.017689725,-0.0051538767,0.01963421,0.017228028,-0.029661262,-0.014175162,-0.057215057,-0.006820786,0.009472099,-0.016432686,-0.004000514,0.022131741,-0.015783375,-0.005160108,-0.010776654,0.0064426432,-0.014274397,0.0063931807,-0.036997564,0.016752334,-0.02806641,-0.009129844,0.0001648961,0.0026768497,-0.00065582705,-0.019372372,-0.014087675,-0.0025434953,0.026722591,0.022737175,0.0067755077,-0.0006143254,-0.02210279,-0.009914794,-0.013480347,-0.03347819,-0.028611265,-0.008740877,-0.014214657,-0.0035645287,-0.0021635965,0.010916267,0.021279825,0.009449142,-0.000114029375,-0.0013208876,-0.004635973,-0.012839434,-0.008766367,-0.0067809937,-0.008389283,0.016091213,0.0060635856,0.005460436,0.00045255938,-0.035074312,0.0046496317,0.031723514,-0.008085175,-0.014380387,-0.021750368,0.0023457026,-0.017510982,-0.009871648,-0.025328416,-0.008519123,-0.0064004133,0.022630563,-0.017435627,-0.019051546,-0.0176971,-0.00021456442,-0.009298511,-0.011052396,0.004926191,-0.007895587,0.026352137,-0.009252853,-0.0029082678,0.009139422,0.019965038,-0.017399032,0.006667134,-0.0060521876,-0.0139703555,-0.02330149,0.0070022475,0.004958011,-0.009144879,-0.049196385,0.0068413955,0.017024446,0.013593824,0.020428406,-0.015999757,0.030635897,-0.008694818,-0.022867223,-0.0034059775,0.0017397754,0.0050204787,-0.019950392,-0.005854534,0.028584998,-0.031446017,0.013143187,0.00870832,0.025877185,0.03233813,-0.0073588425,0.0028991061,-0.0059943954,-0.022258297,-0.008012804,-0.012879444,-0.0023555604,-0.023456728,0.0050596185,-0.016071018,0.0037352769,-0.011402068,0.018347299,0.048997957,0.0025623275,-0.015867868,-0.0052861683,0.02036236,0.018688671,0.000391026,0.005795484,-0.014068446,0.005988473,-0.0014985882,-0.0037494742,-0.00323189,-0.006553483,-0.020753963,-0.015713431,-0.016522095,-0.016747992,0.0035901933,0.006735025,0.0009611926,-0.012078434,0.036444753,0.014786704,-0.027160093,0.008701271,0.014436487,-0.013741227,0.0003758368,-0.017093193,0.010091837,-0.0030022413,0.008194408,-0.005959756,-0.00021882492,0.0027808007,-0.018238619,0.045354676,-0.017131822,0.01146277,0.026942383,-0.015461412,0.01600197,0.002364314,-0.0058568204,0.02634833,-0.04198165,-0.0016125272,0.022524348,0.020700598,-0.012828368,0.0011754812,0.02123074,-0.00025802926,-0.010264605,-0.007026726,-0.011402111,0.0115126055,-0.0018000167,-0.095667,-0.016190715,-0.02844595,-0.0019293419,-0.011678817,-0.030670969,0.008058186,-0.025776362,0.017676247,0.025258,-0.013131818,-0.010002345,0.03508546,-0.017051782,-0.021006726,-0.013917278,0.010979384,0.011513039,0.020926028,-0.04760015,-0.008960708,-0.023422517,-0.014913775,-0.010903742,-0.018408528,-0.022126466,-0.010671398,-0.0061498955,-0.0026741524,-0.007942074,-0.019815115,-0.031179663,0.023728868,0.033528205,0.012919731,0.004942291,0.018227806,-0.027410518,0.008564276,-0.011952701,-0.012314798,0.006747856,0.01917932,0.004198011,0.023996834,0.004244941,0.007247469,-0.020488566,-0.0021915094,0.02066053,-0.043201678,0.00301174,0.00062900444,-0.024676422,0.014022421,0.019392123,-0.0104939705,0.02706132,-0.00054659025,0.011552806,-0.012643879,-0.00070372823,-0.018919768,0.037538517,0.000775003,-0.0043349043,0.0061470135,-0.0051785363,0.01775108,0.008510267,-0.027250804,0.007676186,-0.019574655,0.0056910333,0.0030185317,-0.005451768,-0.005097336,0.023277404,-0.0049796505,0.011010824,0.0027336145,-0.018208858,-0.09674527,0.0029408426,0.0043158177,-0.008113419,-0.0034223103,-0.01563152,0.018496506,0.00018550023,0.0015448711,-0.03578848,0.0016178251,-0.008279688,-0.0080263,-0.030716809,0.028209176,-0.007207473,0.00026197685,-0.0027540533,-0.014444841,-0.020280296,-0.007718357,-0.0059960834,0.023149379,0.019216245,0.007312853,0.017850263,0.005825673,0.0063049872,0.004873737,-0.011936501,-0.013522888,-0.12776199,-0.01673562,0.021444356,-0.004200688,-0.015645724,0.035965938,-0.021265993,-0.008875931,-0.024141826,-0.015221385,-0.030119948,-0.001813579,-0.025139462,-0.029215876,-0.004379847,0.072983555,0.009538134,-0.0028688603,0.0027526221,0.008083708,-0.019981826,-0.034692638,-0.0035963561,0.007918563,-0.018062754,-0.017875815,-0.003894015,0.0030107056,0.011722012,-0.0020442747,0.03561603,0.034430943,-0.006634675,0.012037528,-0.008565631,0.027555222,0.013942366,-0.009092413,0.027647857,0.005617639,0.0017081727,0.007937159,0.020226927,-0.017995689,-0.0035879035,-0.0010886734,-0.014318721,0.019748049,-0.02967648,0.002922542,-4.805193e-05,-0.07542986,-0.011768061,0.018229622,0.0119408555,0.012113505,-0.014843951,0.014207123,0.028196385,-0.0042310576,0.004646946,-0.014759866,0.0029709905,0.02313429,0.025168072,-0.00057778123,0.02149563,0.026078656,0.0020329368,0.0066850404,-9.3829505e-05,0.005985285,0.007107707,-0.019782994,0.016699662,0.010054201,0.029175892,0.030262861,-0.001260452,-0.014068598,0.012578937,-0.017346658,-0.016255591,-0.03339317,-0.010636413,-0.027507655,0.0063594915,0.00042308276,0.027495211,0.0005652443,0.013967841,0.0120554855,-0.0072998726,0.04377017,0.0039998014,0.003836416,0.00539589,0.029676909,0.012602039,-0.014565698,-0.014770481,-0.007912046,0.016727433,-0.011820738,0.009502107,0.011959363,-0.008229561,0.011626125,0.021658761,0.023166094]') ON CONFLICT DO NOTHING;

INSERT INTO public.vector_store VALUES ('5cfc322e-a31a-4ff2-9dda-1c2a13e60f69', '- **하이브리드 검색(Hybrid Search)**: 키워드 검색(BM25)과 시맨틱 벡터 검색을 결합하여 고유명사 및 모델명 검색 성능 보완.', '{"title": "RAG (Retrieval-Augmented Generation) 시스템 아키텍처 및 파이프라인 가이드", "userSeq": 1, "guideSeq": 32, "isPublic": true, "chunk_index": 1, "total_chunks": 2, "parent_document_id": "2a31ac64-3556-430f-ac2b-e8ab409c29c2"}', '[-0.016969092,0.009911161,0.0068073454,-0.065257646,0.012728354,0.0019435614,-0.020145819,0.0033639541,0.0064749033,-0.01624066,-0.015420277,-0.010645309,0.025657887,-0.013197877,0.112144075,0.020948399,0.00017736143,0.005525275,-0.011947494,-0.018105058,0.00769202,0.0328069,0.013411144,-0.0046156617,0.009338393,0.013152593,0.040876623,0.0020185045,0.032769386,0.008538738,0.0037367605,-0.0029393472,0.03187255,0.027651079,0.0003314805,0.022519609,0.015140991,0.0050240792,0.030763803,0.019668443,0.007120303,0.026934404,-0.008514351,-0.004233844,-0.030241149,0.014149514,-0.010534547,-0.013577932,0.01865876,0.055021666,0.014974862,0.008520548,0.027097315,-0.16437301,0.007624248,-0.011741863,-0.021946125,0.007844932,-0.006033154,0.012209778,-0.0072238096,0.026926601,-0.010112424,-0.027518373,0.015641695,-0.008217991,0.049889497,-0.009633996,0.003605684,-0.020399682,0.013524371,-0.014136953,-0.0023908382,-0.031414546,0.0070067137,-0.047099985,0.005412643,-0.01105073,-0.021084929,0.0087238345,0.017370803,-0.012274194,-0.014760486,-0.029277867,0.014613088,0.017282417,-0.008945579,-0.024580514,-0.023081264,0.021598171,0.006571429,0.008178554,0.026530243,-0.0027740346,-0.003962618,-0.0007336071,-0.03866172,0.0072743697,-0.0064298185,0.0033460914,-0.010604181,-0.021713868,0.014846116,0.005586854,0.017718844,-0.005082495,0.0145875085,-0.009594294,-0.0040112664,-0.003924435,-0.000112714224,-0.004727055,-0.0027634052,-0.0097011775,-0.014471281,-0.15372461,-0.010900153,-0.014556202,0.020635124,-0.0012777708,-0.011187473,0.0019238014,-0.003569752,0.01552758,0.0026515417,-0.014888772,0.01671654,-0.008711094,-0.008034735,0.013282884,0.010954561,0.027031848,-0.014983976,-0.010182562,0.020268876,0.032108586,0.020862835,-0.007973177,-0.0058302386,0.0013640415,-0.0072714034,-0.028930742,-0.0028236285,-0.023474846,-0.011565261,0.005122155,-0.019775543,0.015306538,0.0066903136,-0.026188418,0.010504982,-0.003441784,-0.010041649,-0.013616607,-0.0042170486,-0.02154373,-0.013115249,0.008892948,-0.027934045,0.0018872524,0.004239285,0.017526291,0.025928207,0.017439002,0.009554113,-0.0067094625,-0.009983245,-0.017757365,0.013209943,0.023705762,-0.0034032697,-0.0016510242,0.010495268,-0.0024106952,-0.029408367,0.000602735,0.0166433,-0.0030793871,-0.03174824,-0.0026437533,0.02678809,0.025579114,-0.00257839,-0.008675794,-0.010181213,0.006205442,-0.005361437,0.008723299,0.007976143,-0.0017018883,-0.0054001575,0.00083213666,0.029195651,0.010715263,-0.016881527,-0.0072780163,-0.01169425,-0.011577398,0.013153005,0.028959617,0.010704294,0.0038980378,0.013214368,-0.0042447294,0.033619728,0.00483818,-0.02255126,0.023901192,0.024784924,0.011041003,0.010987166,0.016588822,-0.0041616918,-0.034749478,0.007117858,-0.02137302,-0.017134583,-0.022279833,-0.013700276,-0.00940626,-0.013220536,-0.021565907,-7.465558e-05,0.0012457357,0.010264784,0.003228552,-0.0466487,-0.02531214,-0.011898983,-0.007064905,-0.0063121626,0.025594806,-0.0011747136,-0.023353908,0.025733456,-0.029881889,0.0108657675,0.027472021,0.0062815924,0.007231657,-0.036161795,0.007710701,0.0065192585,-0.0019668357,0.023381632,-0.012699261,-0.008974901,-0.01867358,-0.0043329634,0.0069894916,-0.010335705,0.0007221528,-0.0091253,0.0003731876,-0.0065815724,-0.020031832,-0.020922182,-0.0016817484,0.010389785,-0.004060929,0.011428181,-0.009241923,-0.016466629,0.00075157237,0.0068379613,0.017714329,0.041155618,-0.008112679,-0.016968047,-0.0016089797,0.010844372,-0.005079534,-0.02852535,-0.030735757,-0.012840722,0.024547301,-0.04422438,0.046828732,-0.02622156,-0.030063357,0.03175691,0.013670978,-0.013411577,-0.0067504197,-0.00092304946,0.001986949,-0.020391546,-0.03576685,0.003392605,0.021772945,0.0055163656,0.02655881,-0.017178638,0.027731238,-0.010065917,-0.029201549,0.008379459,0.0074747317,0.0047416533,0.007145493,0.006617462,0.008713725,-0.015180063,0.018911202,-0.0111621665,0.011919522,-0.014844192,0.02268996,-0.004479225,-0.01961354,-0.014671146,-0.00840732,-0.010704998,-0.0014595215,-0.015565859,-0.002099181,0.001399996,0.012360281,-0.026384333,0.007032024,0.008908795,-0.017037913,-0.018372692,0.0054250984,0.009279954,-0.01838082,-0.01735376,-0.008723984,0.03145683,-0.01053676,0.0145283835,-0.0014692654,-0.00019249378,-0.0392631,-0.009864266,0.02237127,0.02621723,0.0020413464,-0.041887354,0.02616219,0.011129743,0.005448986,-0.03286175,-0.005997485,-0.005962817,0.027405089,0.0039186226,0.013027628,-0.0020079194,0.014492962,-0.014662319,0.019547235,0.013828944,-0.020149514,-0.0053778607,0.025306016,-0.035467565,-0.024020705,0.009275547,0.032043193,-0.002134266,-0.010980133,-0.0004505867,0.0006403668,0.015868073,-0.04097848,-0.019550495,-0.0011213748,0.003983416,0.0026632296,-0.0008228118,-0.0020163192,0.007401141,0.01126526,-0.0040753135,0.0095165465,-0.018735114,0.0063341013,0.0015632124,0.026532376,0.006238367,0.013517975,-0.0058949403,-0.0014338766,0.003577138,0.008404828,-0.009370869,-0.0021803386,0.008082817,0.024841746,-0.016841698,0.019446833,0.0029635073,-0.0076986873,-0.001810523,0.0014945308,-0.010649496,0.008750463,-0.010022837,0.025376342,0.011898757,-0.0045811045,0.0114936745,0.009065994,0.020067312,0.015963644,-0.017430129,-0.011570139,-0.013182723,-0.008445836,-0.01006699,-0.024517423,0.0043403897,0.0014904016,0.014944316,-0.009396769,0.02002824,0.0045328834,0.0021667876,-0.011145466,0.014857257,0.03566761,-0.0067122346,0.0033027923,-0.018393332,-0.0381614,0.0050209654,-0.0030705994,0.007921982,-0.015984084,0.015699744,-0.012185615,-0.02642579,0.018543253,-0.017831668,0.016194312,-0.02230803,0.029637586,-0.010460491,-0.009739068,0.03243406,0.017695218,0.0076969997,0.03252691,-0.00029178936,-0.0063218866,-0.00516616,-0.0029789954,-0.0024184065,0.017087467,0.022326412,0.024079736,0.012564397,-0.018562436,-0.012899834,0.004204187,0.0083845565,-0.0039731152,-0.024929728,-0.021566583,-0.020489506,-0.0018844922,0.010641397,0.026314387,-0.008347419,-0.018183986,-0.00077948463,-0.027322425,-0.006118416,0.015998475,-0.026416782,0.03620613,0.008965601,0.0034372897,-0.028462792,0.0030495063,-0.013824942,0.016166585,0.027405951,-0.03514224,-0.0035054686,-0.025683148,-0.0050388086,-0.006535018,0.010489137,-0.0055661453,0.012716644,-0.011488613,-0.0029494285,-0.00019148145,-0.012359789,0.012854461,-0.0052468367,0.0046517113,-0.04620694,0.0038045333,0.007728874,-0.012591611,0.012403823,-0.0076588737,0.015250387,0.0056604953,-0.007967192,-0.029518206,-0.004849134,-0.014961566,0.014488431,0.006972503,0.014295233,-0.016183127,-0.0019073988,-0.016195746,0.016380673,0.02107984,0.006586581,0.010924521,0.022111006,0.0077027776,0.01531418,0.0007601168,-0.02396526,-0.023846101,-0.0046859975,-0.004165102,-0.007498122,-0.029535573,0.0012385302,-0.02326979,0.0124847405,0.034927547,-0.019958712,0.035891753,-0.013955045,-0.0024743467,-0.010204552,0.021015907,0.013186601,0.02732505,-0.0034063153,0.019921439,-0.011839448,-0.009476985,-0.024928339,-0.004756593,0.008287483,-0.08832912,-0.0055729644,0.0014537285,-0.010674992,-0.026116474,-0.0015048548,0.0042337244,-0.025988625,0.010357138,0.024192156,-0.01090165,0.005551355,0.027356287,0.0072560594,0.012383897,-0.0102605475,-0.00035245312,-0.009647581,0.022668337,0.0012873925,0.01188443,0.007401295,0.017567692,0.004957159,-0.021132011,-0.0261752,-0.007277285,0.0184419,0.021992184,0.034131818,-0.012946489,-0.012263439,0.01225039,0.06433636,0.007216371,-0.009317555,-0.0023475562,-0.012410464,-0.0062895543,0.0090350155,-0.013755102,-0.0081753805,-0.028526183,0.002605382,0.018322492,-0.004462755,-0.0006028726,0.020311434,0.012677529,-0.0029681076,-0.012851972,-0.0127196545,-0.004361315,-0.021495221,0.0031974139,-0.011129254,-0.024074594,-0.0031153297,0.039031938,0.015739039,-0.009356696,-0.005109965,-0.024289507,0.019969463,-0.021896983,-0.00061100087,-0.010478199,0.04804728,0.014610379,0.04594586,-0.012082179,-0.017387625,-0.033246145,0.01704047,-0.0038735266,0.027490161,-0.010147207,0.00854257,-0.021293672,-0.010548566,-0.028798549,-0.029924633,-0.12158695,-0.019468335,-0.009862743,-0.0070705772,0.020582912,-0.0023091855,0.027650552,0.00017134455,-0.0027326795,-0.0124680875,0.038119882,-0.021197045,-0.020529367,-0.02878915,0.024150252,0.021249272,-0.014078813,0.005565616,-0.021564994,-0.03279683,0.0017635507,-0.007430803,0.024524048,-0.003508179,-0.033380874,0.01887543,0.01120977,0.021611702,-0.00970727,0.0036076354,-0.003805849,-0.101843104,-0.035562787,0.008067566,-0.014155659,0.0044114473,0.03416266,-0.008701007,0.009268116,0.010319646,0.010097949,-0.0066831782,0.0058276188,-0.027808087,-0.027657649,0.008696568,0.10552911,-0.0074084094,0.020710394,0.0049832873,-0.009714523,-0.012887518,0.008688226,0.0070637856,0.017818507,0.009345816,0.011472428,-0.0029268817,-0.007406987,0.015983166,0.034833584,0.022842757,0.01808224,-0.013161659,0.016625185,-0.016489053,-0.018367182,-0.01669636,-0.0147099355,0.005529861,0.010625894,0.025666999,-0.0043959687,0.00688305,-0.02494216,-0.016296623,0.020119075,-0.017766587,-0.0024695212,-0.008571108,-0.017233517,0.0030143764,-0.08679126,0.013815965,-0.008265921,-0.0025722766,-0.008125903,-0.0069980314,0.00047239286,-0.00083968556,-0.0025757966,-0.0078003746,-0.0009890943,0.0009043488,0.022070538,0.0015488389,0.015684448,0.018150553,0.016598517,-0.0060996125,0.019679934,-0.0014625308,-0.003291497,-0.021075442,-0.0060451706,0.004488516,-0.011373028,0.018823335,-0.007026185,-0.008465638,-0.018946512,0.03332392,0.020723792,0.008137744,-0.027465876,-0.003750415,0.0032605561,0.0148071,-0.004625775,0.023053303,0.0004967466,0.035496052,0.032272052,0.009154183,-0.024610113,0.0026990748,-0.006172742,-0.001955279,0.021975033,-0.007497022,0.0025533896,-0.03526783,0.0008117434,0.0014503219,-0.02317265,0.02287222,0.024917798,-0.0016863702,0.024974478,0.04277347,0.0057026623]') ON CONFLICT DO NOTHING;

INSERT INTO public.vector_store VALUES ('65d50cf9-6886-448f-9c27-6cc47aa2f112', '## LangChain이란

**LangChain** 은 LLM 기반 애플리케이션을 조립하기 위한 프레임워크다. 모델 호출, 프롬프트 템플릿, 외부 도구, 메모리, 벡터 저장소 등을 표준 인터페이스로 연결해준다.

핵심 구성요소
- **체인(Chain)**: 여러 단계를 순차로 연결(프롬프트→모델→파서)
- **리트리버(Retriever)**: 벡터 DB에서 문서 검색(RAG 구성)
- **에이전트(Agent)**: 모델이 어떤 도구를 쓸지 스스로 결정
- **메모리**: 대화 맥락 유지

장점: RAG·에이전트 같은 패턴을 빠르게 프로토타이핑. 단점: 추상화가 두꺼워 디버깅이 어려울 수 있어, 규모가 커지면 직접 제어를 선호하기도 한다. (유사: LlamaIndex, Spring AI)', '{"title": "LangChain이란", "userSeq": 1, "guideSeq": 35, "isPublic": true, "chunk_index": 0, "total_chunks": 1, "parent_document_id": "57c84280-5de3-429c-bc2e-1450ccfb73dc"}', '[-0.032002743,0.00033779518,0.020617547,-0.0662497,-0.028741742,0.0013153355,-0.029167175,0.018870316,0.011561064,0.022590023,-0.015798,0.011984716,-0.0049936534,-0.016624212,0.1452972,-0.009898788,0.0058458843,0.0014320088,0.0037383514,-0.02039141,-0.0057349564,0.038369264,-0.0069793835,0.027231205,0.001952343,0.004342054,0.020498596,-0.00043612826,0.03847512,0.021129366,-0.007915861,-0.0034344585,0.008769192,0.025104206,0.0010549698,0.023543332,0.007950837,-0.046522044,-0.00987247,0.009004589,0.005226985,-0.011376125,-0.004830385,-0.0030112956,-0.013617284,0.0072318804,-0.0056026704,0.003863531,-0.0041909628,0.028592398,-0.009614143,0.0151400175,0.009037909,-0.16259052,0.008421783,-0.00018292606,-0.031137262,-0.0052300966,0.009114491,0.0018538438,-0.020467397,-0.023566924,-0.0068596466,-0.0001524663,0.024967264,-0.012053064,0.04810581,0.005002464,-0.015248304,-0.019397635,-0.0033567953,-0.023026122,-0.00015617986,-0.02934649,0.014775807,-0.0455516,0.0140051665,0.0006199311,0.001909546,0.01982124,0.015159256,-0.03162306,-0.011709445,0.026322156,0.0028457642,-0.010401604,0.005444917,-0.010431561,-6.9301304e-06,0.02416108,0.00369587,0.009142789,0.029001713,0.02245589,-0.01813852,-0.0033633441,-0.0049224743,-0.010470439,0.004771242,0.00450923,0.008100211,-0.0034870992,0.015679743,0.031458486,0.030523406,0.02064394,0.014015093,0.009720298,-0.01730488,0.008106117,0.016158387,0.0020811937,-0.014680168,0.00238382,-0.014401577,-0.139095,0.0018517474,-0.0055832,0.015006068,0.0002531809,-0.009816326,0.017331922,0.020161761,0.028726682,0.0112625575,-0.008150912,-0.0069121676,-0.0057561556,0.011296823,-0.016927412,-0.0012271565,-0.0040168576,-0.015367179,0.0044274856,-0.013129776,0.01672308,-0.02260925,0.005645687,-0.038827892,0.0061195204,-0.011467567,0.028804744,0.022289475,0.009996867,-0.048295807,-0.00824836,-0.055377573,0.005024752,-0.009123317,-0.029297117,0.024009008,-0.0021373532,-0.041461255,0.02598109,0.021984113,-0.021593206,-0.003497032,-0.0068009826,0.017845523,0.013495063,-0.006884082,0.009539086,0.000664763,0.014371556,-0.0025626295,0.019617435,-0.022933582,-0.0053657666,-0.012787156,0.028460199,0.019021053,-0.012428454,-0.001577086,-0.012038182,0.015706116,0.0011662112,0.009908899,-0.010119357,0.00058702583,-0.02592077,0.022317741,0.008296439,-0.021589344,0.013770693,-0.017965434,0.011593001,-0.012440492,0.024686249,0.026207328,0.0065577645,-0.020533456,-7.490754e-05,0.028292513,-2.2670215e-06,0.0033309872,-0.019800926,-0.021877868,0.010821492,-0.017562497,0.028982768,0.015737873,0.0014015214,0.01832472,-0.024122898,0.005594783,0.00022517222,-0.003588223,-0.0068576983,0.018934023,0.0080707,-0.009959782,-0.010669103,-0.0030088627,-0.006164762,-0.007897303,-0.02574551,-0.011377985,0.0091728205,-0.014297002,-0.004072441,0.012573796,-0.0009525499,0.013167883,-0.0009967193,0.011218357,-0.010743371,-0.008697858,-0.0071925824,-0.012172588,-0.011208793,-0.00094731693,-0.022123981,0.016038269,0.012999725,0.037866652,0.0017873792,-0.0031031806,0.035556424,0.009455186,0.004938692,-0.018305045,0.015505162,0.008493296,0.020781651,0.0370004,-0.019679401,-0.01726817,0.025905069,-0.025995843,-0.0053980486,-0.010705488,-0.010267504,0.002173671,-0.020197574,-0.0084967725,0.008343521,-0.014175807,-0.0058138426,0.0014198697,0.012461996,-0.015540915,-0.009428167,-0.013761181,-0.020639729,-0.0077096163,0.009790534,0.012646369,0.018926524,-0.012123708,-0.00033765112,0.012591886,0.03414879,-0.006160073,-0.011046758,0.0154036395,0.00991386,-0.020522956,0.031606633,-0.0046693087,0.0092641385,-0.00688075,0.0007020605,-0.010120445,0.01794101,0.014887972,0.0014307208,0.010257886,-0.00862273,0.00012523803,0.007694835,0.028707085,0.033536013,0.0042382306,-0.020894285,-6.893705e-05,-0.026894128,-0.0054575913,0.0123949135,0.0095656365,0.014324927,-0.015008579,0.010787187,-0.014590708,0.02438797,0.006508009,-0.0014900621,-0.0017272405,0.013076059,-0.0032004875,0.015006721,-0.019543162,0.023116117,0.005935903,-0.026126087,-0.000105618674,-0.012048513,0.019722426,-0.028347544,-0.009414378,-0.017604606,0.014602935,-0.023786632,-0.037489876,0.013079888,-0.024784083,-0.011681876,-0.010760872,-0.017229768,0.018374024,-0.02529898,0.020536328,0.004169565,0.024903432,-0.029728979,0.02000226,0.015350535,-0.01509426,-0.014630266,-0.006396448,0.011365878,-0.005962617,-0.022917949,-0.0064381845,-0.018471297,-0.007638924,0.026920283,0.029393949,0.0025719376,-0.013252598,-0.01956889,-0.013382385,-0.0042442167,-0.0038453331,-0.03308335,-0.030144032,-0.011079494,-0.027628116,-0.014255735,0.0273202,0.00087660365,0.010188733,0.0018930507,-0.029194487,-0.0028234965,0.025881637,-0.03418834,0.01897553,0.034936607,-0.02235384,0.027005985,-0.022917066,0.004877997,0.0004069505,-0.0018769188,0.0038016196,-0.000120811186,-0.018367166,-0.004660988,-0.017246168,-0.0079219425,-0.02283227,0.011090248,-0.03333977,-0.004778864,-0.01294928,0.010875849,0.020879807,-0.00058735063,0.018864773,0.016379466,-0.0038153997,0.021376537,-0.01828851,-0.0032439523,0.015039114,-0.014221119,-0.031717505,0.018875076,-0.017245017,-0.010280789,0.0018316176,-0.0028360863,-0.02599713,0.011520167,0.008618876,0.014376651,-0.0043366347,-0.022722583,0.0042216107,-0.019471847,-0.011903477,0.0032479228,0.027320502,0.016952263,0.007364571,0.032648962,-0.009828614,-0.0090518445,0.014437449,-0.013391944,0.005158903,0.026541797,-0.012157806,-0.023919206,-0.008782348,0.010795229,-0.021807488,0.012117898,-0.024158254,-0.0018415358,-0.012706815,-0.02411245,-0.018435229,-0.03134154,0.00038251324,0.015168244,-0.015582316,-0.019597886,-0.006077723,-0.015025981,0.04860072,0.0004785872,0.0063009993,0.017803647,-0.0009670782,-0.006115067,-0.015264183,-0.018470766,-0.017516686,0.0030439545,0.018122649,0.02308535,0.01592664,-0.008257892,0.026821231,0.010392261,-0.0154315075,0.010188981,-0.024162872,0.0042582643,-0.023585975,0.002894311,0.0018876088,0.025672564,0.0028481232,-0.024365673,-0.01667739,0.008930785,-0.0009994473,0.036954906,0.00031941655,0.025585663,0.02552984,1.0601659e-05,0.0021817163,-0.004889205,-0.014199567,0.0011484028,0.015698155,-0.017284626,0.008470906,-0.022543816,0.025892742,-0.006168875,0.003531742,0.024798078,-0.0014231805,-0.005339613,-0.015805444,-0.008025116,-0.00010577368,-0.00542482,0.012204842,-0.012649941,-0.006888571,-0.0074975453,0.00558191,0.008220513,0.014512772,-0.026367472,-0.02023824,-0.0095730955,-0.017698426,-0.0003212942,-0.004291332,-0.034574,-0.0021462068,0.0023437052,0.029508226,-0.018108236,-0.0039056097,0.00021918156,0.00011661589,0.014755458,0.0023763936,-0.0026893127,0.017838325,-0.008608612,-0.0022240581,0.010372485,0.0061621475,-0.0017250545,-0.054948874,0.024213985,-0.026281994,-0.00993237,0.006591208,-0.015419699,0.02114246,0.01609804,0.008274266,0.022093473,0.00551911,-0.030360512,-0.0015155647,-0.012821846,0.035156123,0.007884636,-0.014960339,0.016014444,-0.033977807,-0.0067524626,-0.022990344,0.0073708,0.05839578,-0.09538535,0.003238407,-1.9802455e-05,0.0031250224,-0.011684158,-0.0089763915,-0.0033638463,-0.017041942,0.0015305164,0.0064258953,0.00747876,-0.017151948,0.04942044,0.012557646,-0.010938227,-0.020531828,-0.014038304,-0.028516088,0.01756031,-0.029730221,0.016361753,0.0036459316,-0.00032412875,0.009830241,0.0063923304,-0.011446643,-0.0034729894,0.016490942,0.012258981,-0.016930962,-0.0014962119,-0.0139633035,-0.021413272,0.0032973816,0.000796438,-0.01130823,0.02725432,-0.019183015,0.0062522115,0.00949534,-0.0027663526,-0.003840643,0.011690737,-0.008213178,-0.0029161233,0.005005258,-0.01940065,-0.0062609157,0.018652724,0.0085231,-0.05574207,0.0045781205,0.0074808765,-0.008799645,0.012561395,-0.0018145742,-0.026863594,-0.00097231334,-0.01063189,0.013438053,-0.003804639,-0.018208068,-0.00016491198,0.06317325,-0.0101404395,0.022034185,0.011336233,0.0061454745,-0.0158655,0.00917516,-0.006679075,-0.025338644,-0.010139598,0.00011735405,-0.035386775,0.011709783,-0.0155069595,0.026216144,0.01588157,0.0059426934,-0.034251556,-0.011028313,-0.089407764,-0.009677256,0.014549781,-0.00944437,0.01674406,0.0053976327,-0.009072718,-0.038233515,-0.010538731,-0.029058708,0.020441495,-0.03544043,-0.027922412,-0.042308368,0.014368594,-0.0025320787,0.0006888471,0.0030393514,-0.021373274,-0.0036936034,-0.004253461,-0.008261776,0.017056907,0.0066465205,0.0042936667,-0.00055255333,-0.00078648236,0.01251334,-0.013527426,-0.00014196777,0.012057931,-0.11593143,-0.008441671,0.009733133,-0.00583305,-0.014767042,0.01544891,-0.018449359,0.0055322037,0.004165028,-0.02408768,0.026745914,-0.039761785,-0.009529137,-0.026531188,0.012174202,0.10544712,0.0069882474,-0.010145656,-0.007621088,0.00048122686,0.0045023724,-0.027006978,-0.019481301,0.00017514509,-0.021445477,-0.013249087,0.007930842,-0.009668464,-0.0022748173,0.02919297,0.029918676,0.03914964,-0.013436166,-0.0074232253,0.014114096,0.0042296383,0.005268232,-0.018876575,0.0005713872,0.012315126,0.018803082,0.020164963,-0.0043467977,-0.018079123,-0.009808087,-0.0077152993,-0.030387795,-0.017812334,0.0044580773,-0.025128659,-0.008517035,-0.07975459,-0.001373801,-0.009824823,0.018517058,0.021153547,-0.0074350927,0.032040216,0.0076771593,0.014012681,0.016863383,-0.027121624,-0.0034627137,0.020786203,-0.009007232,0.0007385638,0.037988078,0.02178641,-0.018887913,0.004848878,0.017310383,0.01974587,0.0032156287,-0.01831702,0.016401535,-0.004015168,0.01016359,0.017320067,0.025796644,0.0053341636,0.019504322,0.015805963,0.011928931,-0.000757096,0.015118205,0.023791362,0.008629515,-0.0048965733,0.025207656,0.00049573416,-0.0032118887,0.0373535,-0.010318553,0.015831424,0.0032370859,0.006237778,0.0050308174,0.01542315,-0.013091418,0.0012930101,-0.027032554,-0.02072021,0.004906794,-0.033977177,-0.007648632,0.03466575,-0.002277399,0.036815487,0.002568011,-0.001295738]') ON CONFLICT DO NOTHING;

INSERT INTO public.vector_store VALUES ('662eea7e-e042-4a92-8ad0-a9f08c563cbd', '## LLM이란 무엇인가

**LLM(대규모 언어 모델)** 은 방대한 텍스트로 학습해 "다음에 올 토큰(단어 조각)"을 확률적으로 예측하는 신경망이다. 이 단순한 목표를 대규모로 학습하면 번역·요약·코딩·추론 같은 능력이 창발한다.

핵심은 **트랜스포머(Transformer)** 구조와 **셀프 어텐션(self-attention)** 이다. 어텐션은 문장 안 단어들이 서로 얼마나 관련 있는지 가중치로 계산해, 긴 문맥의 의존 관계를 병렬로 처리한다.

용어
- 파라미터: 모델이 학습한 가중치 수(규모의 척도)
- 사전학습→미세조정: 일반 학습 후 특정 작업에 맞춤
- 생성은 확률적이라 같은 질문에도 답이 달라질 수 있다

대표 모델: GPT, Claude, Gemini, Llama.', '{"title": "LLM이란 무엇인가", "userSeq": 1, "guideSeq": 30, "isPublic": true, "chunk_index": 0, "total_chunks": 1, "parent_document_id": "9c883a9a-b2c4-4b0c-8573-4bfaf355d47e"}', '[-0.015953595,0.0154205,0.028195271,-0.055863157,-0.033862226,-0.016486285,-0.009512442,-0.011357441,0.0019758642,-0.0034843055,0.018033633,-0.015984245,-0.010554562,-0.023702487,0.1263812,0.019004865,-0.00817262,0.014441874,0.01369125,0.007755469,0.017638529,0.00023348293,-0.003205556,0.008961332,-0.023246817,-0.0028317103,0.016731001,-0.010570904,0.05099024,0.024500594,-0.023902992,0.017858645,0.0046158996,0.02079476,-0.022367507,0.0006789469,0.016402036,-0.0155999,0.0059082303,0.020035597,-0.022910621,-0.024389356,-0.0072458293,-0.021883475,-0.0062164934,-0.0022750706,-0.005340044,-0.041927654,-0.019136138,0.03226874,-0.030850083,0.028515544,-0.0009921563,-0.17205934,-0.0053868173,-0.0057622087,-0.01855405,-0.0041156323,0.0010587132,0.00032744568,0.00016383544,-0.00031742136,-0.0065261857,-0.011391942,0.027244885,-0.0077592586,0.038062397,0.013217185,-0.018041948,-0.0153760165,-0.0029910107,-0.0066007073,0.0050779027,0.0019973873,0.0038787932,-0.03268291,0.02667477,-0.0047401725,-1.6153692e-07,-0.019979967,0.024002727,-0.0056716595,-0.011980927,-0.0024918492,-0.0026821492,-0.007121327,-0.0030426977,-0.0043688687,0.009121425,0.006611283,0.0021841093,-0.0024575912,0.054400906,0.021974672,-0.0010581691,-0.00089871424,-0.014656664,0.011010643,0.015455345,-0.00818668,-0.013321349,-0.0137510495,0.0101395305,0.028915407,0.016040007,0.006399737,0.021494942,-0.0065505984,-0.0049615866,0.02272256,0.011417153,-0.017425299,0.014708083,0.0050118095,-0.0010799043,-0.14412488,0.006065279,0.0115023665,-0.016807351,-0.003934193,0.005000254,-0.0037283157,0.015512055,0.040201474,-0.0073485267,-0.01724162,0.0069901813,-0.006047602,0.00046284415,-0.002949774,-0.02624748,-0.009785478,-0.008034285,-0.011497359,-0.0119726965,0.03686722,-0.0069486634,-0.023849135,-0.02367183,-0.010295363,-0.017293675,-0.0035406367,0.010835833,0.0089389505,-0.00013329147,-0.017115215,-0.038343914,0.00045152832,0.01527058,-0.016517986,0.013860048,-0.0057825204,-0.014698145,0.0049913498,-0.00050472916,-0.028972583,-0.0067315483,0.012953258,0.01240505,0.0020920685,-0.0013546235,0.0057225307,-0.00034862943,-0.008089323,-0.0067899614,0.03663406,-0.019178476,0.001222052,0.009313716,-0.006399145,0.0029034584,-0.011666851,-0.012526912,-0.009117035,-0.012819888,-0.028542971,-0.013752117,-0.019444758,-0.0010762567,-0.010156638,0.028768074,0.018771285,-0.030815657,-0.01058207,0.013357572,0.010194586,-0.0102771735,0.0014992182,0.036291465,0.020955136,-0.031081427,0.005999974,0.025331624,-0.029053356,0.006384047,-0.0007539107,-0.01904865,-0.0026242447,-0.021287804,0.015034985,-0.0007993361,-0.010932993,0.02603061,-0.011021411,0.032388084,0.0038869611,-0.0074490854,0.011592613,0.025593769,-2.5864858e-05,0.019927038,0.0029425938,0.024646802,0.0042440644,0.004416942,-0.0025748115,-0.016807633,-0.020868199,0.0038026436,-0.0037862468,0.019862255,-0.018177243,-0.01043853,-0.0017787878,-0.01801305,-0.007848921,-0.014262367,-0.024064971,0.010908952,0.0017927386,-0.01732452,0.015539622,0.01688904,0.009571595,0.018145774,0.026542857,0.016747268,0.026707469,0.0035899156,0.00075438654,-0.006481905,0.02210119,0.022395637,0.044966497,0.022291286,-0.024767185,0.022415625,0.02677444,-0.011880569,0.015249149,-0.008091192,-0.011946621,-0.0038483501,0.0040298416,-0.029187351,0.010547892,-0.021645203,-0.015142042,-0.011441993,-0.0015463824,2.978879e-05,-0.01814227,-0.001243313,0.01116371,0.005290881,-0.0028424521,0.010926702,-0.009962242,-0.007143611,-0.010959138,0.0031394793,-0.0008308357,-0.008746155,-0.025621127,0.00394357,-0.0065879137,-0.032689508,0.028646277,0.009607523,0.00017571247,0.018785825,-0.020477396,0.03478385,0.021079358,0.000576114,0.009610994,-0.0071876617,0.027376115,-0.010778756,0.0049361945,0.03530951,0.04164669,-0.008969173,-0.026921317,0.004269591,-0.016955663,-0.004780865,0.025374154,-0.015384182,0.0044061653,-0.025874758,0.024448762,0.011764157,0.028035536,-0.0032852164,0.011677156,0.0054651517,0.017692873,-0.023857202,0.0067086457,0.009961507,-0.023639906,0.0179695,-0.030198552,-0.019648999,-0.0208549,0.03560683,-0.0020182384,0.0009919178,0.0013825214,0.006956787,-0.017154928,-0.02444964,-0.0036763921,-0.00909569,-0.0127446605,0.0053706723,0.020433007,0.03930553,-0.0043768045,-0.0037985304,0.006978918,0.040774915,-0.023407532,0.023319881,0.002812929,-0.02486854,-0.016992893,-0.020334436,0.010987272,-0.010009199,-0.008528655,-0.024691397,-0.0029007767,-0.02024849,0.04716258,0.0020399543,0.023887977,0.001026552,-0.04184839,0.0303239,-0.023224395,0.00022154258,-0.021342644,-0.023799056,0.012593217,-0.0074327285,-0.034333345,0.002463347,-0.002397929,0.012539713,0.0022029586,-0.007654728,0.0055019013,0.020774182,-0.03977195,-0.0008214367,-0.00057743257,-0.008622667,0.004593497,-0.002346714,-0.024085794,0.0012293804,-0.011498201,0.0016860467,-0.012579371,-0.0021459793,-0.0057897186,0.0051231813,0.03621764,-0.0039173984,0.024585124,-0.0064759394,0.008974301,0.002455173,-0.004811731,0.017340556,-0.02054397,0.014220643,0.015285308,-0.007889738,0.029276239,0.018285088,0.0045910142,-0.008669572,-0.011194707,-0.019363688,0.021980958,0.0030316187,0.002955551,0.031920116,0.010764693,0.010721628,-0.0018784399,0.026166642,0.01398252,0.010259,0.0015262116,-0.006676035,-0.022527881,-0.020766525,-0.0024457138,-0.002086213,0.005916634,-0.0025452005,0.005880315,-0.022176677,0.012100851,0.022898324,-0.019917162,0.010613838,0.027318329,-0.01783273,0.0091023445,-0.0111868745,-0.00016017929,-0.016979296,-0.010544145,0.0056119454,-0.0032237899,0.0050691897,-0.018512286,-0.04787114,-0.025365023,0.00030163143,0.0072885463,0.011074863,-0.0059789703,-0.0119294,-0.010291533,0.021729365,0.0005451785,-0.00010321462,0.016239395,-0.022461284,-0.012347574,0.00462414,-0.020095248,-0.025961889,0.0087364195,0.027411368,0.026646618,0.023913514,-0.023258647,0.0153205255,-0.0035312134,-0.020583102,0.0041224184,-0.0029880137,-0.0050667995,-0.02132295,-0.016231135,0.011718036,0.02373602,0.0099379495,-0.02663235,-0.016083427,0.024467183,-0.00090719695,0.019605016,-0.0036531338,0.021510245,0.031454198,0.004627001,-0.042185836,0.023435472,-0.0036467325,0.009947523,-0.009642013,-0.021339143,-0.024371991,-0.03606856,-0.010418469,-0.023822665,0.013279706,0.01056994,0.0029573052,-0.008043422,-0.007362414,-0.010527193,0.0045772386,0.018510405,0.029890172,0.014488635,-0.027930463,-0.015245485,0.011762959,-0.013948708,0.011942152,-0.015491685,-0.006838296,-0.010014967,0.023299677,-0.01422183,-0.009130093,-0.0077641974,-0.023872951,-0.017364483,0.012902201,-0.026052093,-0.00997327,0.013758794,0.0025688405,0.01854557,0.013407637,-0.016224554,-0.006462033,0.004327838,-0.0018486582,-0.008104611,-0.009049808,-0.022409473,-0.025840588,0.01635947,-0.020141011,-0.019330228,0.0148506155,-0.00467618,0.0138469,0.0016935817,0.0048667556,0.0052646594,0.009348581,-0.025020692,-0.005455241,-0.0072161225,0.029858796,0.039593317,0.00026181465,0.015729507,-0.013404881,-0.011855565,-0.0004185057,-0.024413407,0.050983395,-0.08428999,0.011915965,0.0008244594,0.0058486266,-0.027075069,-0.0040424843,-0.006879745,-0.015489442,0.004255743,0.019772321,0.0040556807,-0.0024466405,0.012176195,0.026312009,-0.011329821,-0.0026346592,-0.020479146,-0.018358244,0.008015156,-0.0067438437,0.031368807,-0.0012218837,-0.014814478,2.257849e-05,-0.008919596,0.017447894,0.030462945,-0.0022640266,-0.010612051,0.013326602,0.0011901398,-0.016057257,-0.01075807,0.014268381,0.0064437375,-0.011032652,0.037006747,-0.00050300965,0.0012343853,0.0025807589,0.013257121,0.016544608,-0.00015386174,-0.00244594,0.015934585,0.013963993,0.024859851,0.00032388407,0.0016335485,0.019313427,-0.02087229,-0.007474501,0.013924968,-0.00072170945,-0.010332597,-0.0032856388,-0.01513119,0.014554047,0.017460017,0.016966892,-0.032494493,-0.00016551859,-0.0016380292,0.031311277,-0.009212096,0.014471443,0.0031618676,0.009904067,-0.0014686807,0.0015819034,-0.012782361,-0.018339638,-0.004861162,0.01818448,-0.044847257,-0.008248685,0.008415251,0.014844128,0.0039241146,-0.000104777144,0.009842642,0.0068125934,-0.100748986,-0.0007194981,0.0012311543,0.011962615,-0.011167264,0.01068075,-0.011591334,-0.032148175,-0.019875428,-0.03863042,0.008030039,-0.041914105,-0.03012449,-0.03173123,0.025723733,-0.026540399,-0.0066596596,0.005827062,0.0045284424,-0.026983937,-0.020109136,-0.011228486,0.01609089,-0.013928448,0.0022076536,0.0046495907,-0.019088538,0.0056400225,0.0049316227,0.003715687,0.0026914703,-0.12171079,-0.034454215,0.0136790555,-0.0114335,0.017477794,-0.016781533,-0.011394457,0.022930795,0.0097824475,0.0040559582,0.0041377973,-0.04691623,-0.011244349,-0.018005472,-0.0019059005,0.0894774,-0.001269395,0.0067212884,-0.005068399,0.0027830526,0.006312919,-0.026452584,-0.01940385,-0.005180539,0.0099618165,-0.018739227,0.0035706842,-0.027883235,0.016779838,0.04366489,0.013153324,0.01753529,-0.012656894,0.0035623994,0.029841125,0.010419319,-0.002447598,-0.003541163,-0.021480799,0.009666052,0.022757115,-0.0032005906,-0.0121230865,-0.011750535,-0.03140762,-0.014206236,-0.036755975,-0.030936519,0.004405386,-0.007960623,0.002633959,-0.07212422,0.0057367464,0.015473742,0.014984147,0.0052115815,-0.00802762,0.024459142,-0.0039021343,0.01414999,0.004416436,-0.017264035,-0.015993996,0.02336444,0.016027477,-0.020716466,0.015590407,0.026393114,-0.014121875,-0.010466434,-0.008144634,0.011332049,-0.021943478,-0.014091987,0.0025197326,-0.023110319,0.038323496,0.027657934,0.022128318,-0.008819176,0.010549935,0.013262164,0.018570768,0.0032117788,0.00014167177,0.010374393,-0.0039804103,0.015093128,0.006163001,-0.0059096683,0.029226786,0.044962354,-0.020914003,0.020509658,0.009533679,-0.0147990575,0.010694262,-0.004084451,0.0030375419,-0.0066692233,-0.011228997,-0.017398054,0.009628148,-0.022573823,0.0027418141,-0.0029661797,-0.030963344,0.019846391,0.024756636,-0.007129076]') ON CONFLICT DO NOTHING;

INSERT INTO public.vector_store VALUES ('696730de-2808-4728-bf86-abe75d83c027', '## 임베딩과 벡터 검색

**임베딩(embedding)** 은 텍스트·이미지 등을 의미를 담은 고차원 숫자 벡터로 바꾼 것이다. 의미가 비슷한 것끼리 벡터 공간에서 가까이 위치한다.

**벡터 검색** 은 질의도 임베딩으로 바꿔, 저장된 벡터들과의 **거리(코사인 유사도 등)** 를 계산해 가장 가까운 것을 찾는다. 키워드 완전일치가 아니라 **의미 기반** 검색이라, "휴가"로 "연차"를 찾을 수 있다.

핵심 개념
- 코사인 유사도: 두 벡터의 방향이 얼마나 같은지(1에 가까울수록 유사)
- ANN(근사 최근접 이웃): HNSW 같은 인덱스로 대규모에서도 빠르게 검색
- 차원 수: 모델이 정한 벡터 길이(예: 768)

RAG의 검색 단계가 바로 이 벡터 검색이다.', '{"title": "임베딩과 벡터 검색", "userSeq": 1, "guideSeq": 31, "isPublic": true, "chunk_index": 0, "total_chunks": 1, "parent_document_id": "0546b5ee-54df-447e-9ddc-166ad977168a"}', '[-0.028140878,-0.013095916,-0.0064590042,-0.07923783,-0.016761122,-3.8055894e-06,-0.00746658,0.004343471,0.009250575,0.002234146,0.011618945,0.008559443,0.011467026,0.0009517915,0.13693804,-0.008827822,-0.013755353,-0.005229052,-0.005297103,-0.0201112,0.020575756,0.017799787,0.0006839061,0.012546828,0.015376472,-0.014591527,0.025922323,-0.011281532,0.021409908,0.02008155,0.005423137,0.014295134,0.033375714,0.034281634,0.0112985205,0.006010635,0.0016751984,-0.0187469,0.031776924,0.012940221,0.007678253,-0.025982544,0.006135539,-0.019296175,-0.0035964395,0.007127537,0.018098714,-0.029770186,0.02657661,0.014646374,0.0064062895,-0.0011414344,-0.008816671,-0.16238624,0.015806647,-0.01597565,-0.0036975536,0.0012967176,0.017716812,0.013918489,-0.01724876,-6.7447254e-05,-0.019861437,-0.009787018,0.0010607475,0.005651595,0.027852468,-0.002421513,-0.007677575,-0.014582197,0.0138652865,-0.006792465,-0.025395619,-0.015138999,-0.00093902735,-0.02541314,0.029991046,-0.015874878,0.010885567,0.011471999,-0.006716404,-0.0161501,-0.020725269,-0.015009569,-0.006364508,0.0137002235,-0.003695082,-0.018829156,-0.0043258965,0.011991324,0.021473562,0.009859984,0.038975663,0.010366185,0.010911354,0.0075141243,-0.020249182,-0.010322562,0.011035334,0.0019687198,-0.014374202,-0.019675702,-0.0008496262,-0.0016178786,0.01634746,0.030101651,0.039868183,-0.004216692,-0.0057104267,0.019862125,0.012612103,-0.02179468,-0.006201989,-0.010416741,-0.01660641,-0.14054067,-0.0062801973,-0.025649382,0.01500882,0.0045957477,-0.039602615,0.013100651,0.022994936,0.034012917,0.00850331,-0.022603536,-0.00012901383,-0.024669228,0.011111757,0.015400957,0.0054693636,0.017899692,-0.0034413326,-0.0025537333,-0.023037236,0.012015736,-0.029596582,-0.011897716,-0.038211536,-0.011283605,0.007926192,0.008092172,-0.0077336207,-0.013547805,-0.019114085,-0.00066691265,-0.04407779,0.026909372,-0.0035388565,-0.03465497,0.002626331,-0.022226127,-0.023872105,0.005814358,-0.0073221033,-0.027240973,0.0067683975,0.018172817,-0.0009578765,0.014768041,0.017914686,0.007939028,-0.0003241578,0.007880004,0.010220137,0.021816673,-0.023323257,-0.019899031,-0.00048489115,-0.002018411,-0.032982033,0.025118457,-0.0025428862,-0.019284427,-0.010720896,-0.034480065,0.01178551,-0.0075708227,-0.039934896,0.0067327567,0.017738506,0.010623591,-0.011356915,0.0012947739,-0.00977369,0.006135995,-0.0010744467,0.030841528,0.011127447,-0.005097151,-0.015889645,0.017050771,0.008147463,0.0047458555,0.008150312,-0.016034152,-0.019428477,0.008036589,-0.004906997,0.021353213,-0.011501199,0.0022242917,0.020670416,-0.024895893,0.037272155,-0.00021393935,-0.019454692,0.001495865,0.033619203,0.007478627,0.03623235,0.018326877,-0.010437684,-0.01649999,-0.0046329135,-0.027585221,-0.009595187,-0.01616149,-0.013875822,0.0065952116,0.0105909305,-0.0013465142,0.020358823,-0.0037067404,0.033009157,-0.0030038224,-0.03702853,-0.025355307,-0.017657572,-0.009025439,-0.02667119,-0.0003367348,0.019146334,0.0028816091,0.025160845,-0.029108007,0.008254173,0.025636904,-0.016847836,-0.0014847574,-0.013978317,0.02668814,0.0048910994,0.041447405,0.0022818577,-0.010859945,-0.010682802,0.03271794,-0.0077731097,-0.0047007515,-0.018165682,0.015037252,-0.013481636,-0.013711485,0.0024414184,0.0031211246,-0.031835895,-0.005792953,-0.009604039,0.015762746,-0.004138488,-0.014053377,-0.008434611,-0.0049149757,-0.023324665,0.030150041,0.025481366,0.0050527714,-0.016062012,-0.007855311,0.007516007,0.012343388,-0.0044526174,-0.028962981,-0.016616873,0.05332122,-0.020136217,0.03392936,0.007382612,0.008883179,0.013827037,0.0044484353,-0.003890935,0.021435628,-0.0040484206,0.007269959,-0.0079500815,-0.027657699,0.021494584,0.026778666,0.027112888,0.01516421,-0.0089346245,0.0022430778,0.022675285,-0.029450035,0.0020455807,0.026199905,0.024539022,-0.032833334,-0.009780105,-0.0044525536,-0.009204387,0.02277609,0.0012413048,0.02209156,-0.007314127,0.011752434,0.008912132,0.002735775,-0.013078368,-0.0047323196,0.012707116,0.004449759,-0.011558301,0.0014302669,0.027559094,0.0030768027,-0.02777966,0.028061233,-0.0044591003,-0.008751026,-0.026227122,-0.01546483,-0.030057507,-0.015231909,-0.019546047,0.00035687382,0.035679284,0.008577039,0.022213325,0.018279491,0.0042607477,-0.042287473,0.006937318,0.00761692,-0.0014690077,-0.019385658,-0.03454768,0.044785485,0.0070223506,-0.019736737,-0.022591034,-0.0035543044,-0.024603475,0.024287285,0.004224553,-0.01669777,-0.026842631,-0.013689792,-0.016598439,-0.008327681,0.029235974,-0.04803151,-0.0059148944,0.0015272896,-0.021861402,-0.024770724,0.02459842,0.005516582,-0.0061790585,0.00725051,-0.0066110524,0.015669208,0.020044554,-0.023336977,0.0064819176,0.05120619,-0.021695891,-0.008890745,-0.0059456453,-0.0032786918,0.006403998,0.009442297,-0.00013475642,-0.012898856,-0.024855148,-0.009303596,-0.0219878,0.02070556,-0.0025894684,0.035304938,-0.012669008,0.00081495056,-0.010380621,0.0042619756,0.014390108,0.012441096,-0.0022298193,0.020472731,-0.018397626,-0.011246446,-0.013097262,0.013138217,-0.0014444129,-0.008523265,-0.015352758,-0.0022477815,-3.844812e-05,0.008234038,-0.018892914,-0.0146747725,0.001708757,0.010112958,0.00092159986,0.006183879,0.0017063413,0.0036771432,-0.012504133,-0.016656829,-0.0025413528,-0.031361505,-0.006104433,-0.0074432837,0.011531811,0.00707643,0.0046352525,0.007467139,0.029109865,-0.015091125,0.0017707712,0.042387865,-0.0058036363,-0.016883282,-0.0056907767,-0.015109757,-0.021675874,-0.004349377,0.005425641,-0.022355678,0.017237559,-0.02751144,-0.023298798,0.004767907,-0.011273804,0.015994897,-0.0116849495,0.016526297,-0.015126462,0.006023728,0.042060025,0.012712534,0.018243216,0.027060105,-0.00063086214,-0.005885095,-0.0051273643,-0.0012176053,-0.012278191,0.010527852,0.018597042,0.00049116125,0.0063366964,-0.021640839,0.019355418,0.0041962415,0.00305856,0.0067874137,-0.019521011,-0.029164067,-0.031908903,-0.0083762705,0.025563506,0.03397849,0.0024823067,-0.027862437,-0.0042365496,-0.014906188,-0.024599219,0.01237407,-0.0016244433,0.010852934,-0.004312469,-0.00919556,-0.014036794,-0.014947067,-0.00618638,0.02528781,0.015999358,-0.02001331,-0.019916704,-0.017808564,-0.0016138126,-0.009816471,-0.0023828114,0.028770583,0.011404938,-0.0072296397,-0.010781477,-0.012955889,0.004822403,-0.0043925657,-0.0077407756,-0.00081862917,-0.008766849,-0.025100878,0.021161456,-0.028317494,0.020133348,-0.02476338,-0.009917488,-0.0010886225,0.0057694116,-0.04137962,-0.0017603677,-0.034357827,-0.011175546,0.0016430734,0.02005729,-0.024073027,0.003268001,-0.0052742953,0.0015588073,0.020914115,0.004597967,-0.0038764377,0.019202411,0.031104183,0.020876953,0.007161263,-0.020665301,-0.0039733425,-0.017337358,0.020131703,-0.010321818,0.006775018,0.010676163,-0.008160035,0.0055719777,-0.0028020786,-0.00348412,0.027748674,-0.0164894,-0.019246953,-0.008019322,0.015632981,0.030587738,0.012751708,-0.015031556,0.021959843,-0.0102260895,-0.012997746,-0.016847443,-0.008368234,0.035779867,-0.09414571,0.009476642,-0.0016465615,0.01023236,-0.04446551,-0.01622437,-0.012353207,-0.0036116936,0.0073662424,0.018418435,-0.007739074,0.0015683038,0.025209466,0.004739314,-0.004708801,-0.029709492,-0.009988649,-0.019684404,-0.0017342085,0.018483708,0.010042712,-0.021282516,-0.017479733,0.011849398,-0.0078804735,-0.01648926,0.012433281,0.0062846676,0.0078062187,0.01408652,0.01595364,-0.025472961,-0.008814957,0.035942107,0.011720047,-0.0023473557,0.015430978,-0.02075875,0.0016935776,0.0025328111,-0.00515768,0.0037283925,-0.015611488,-0.017483935,0.01406849,0.013981163,-0.022594897,-0.007967089,0.0060347226,-0.002426713,-0.036713876,0.012384798,-0.014568066,-0.009880657,0.0024497584,-0.01349254,-0.014542416,0.0011322706,0.017955607,0.012626983,0.014160937,0.007625052,0.0109057985,0.032473978,-0.00025371122,-0.0010361198,-0.008042651,0.007869203,0.010245227,0.007876795,-0.011844508,-0.014888982,-0.017242465,0.018050628,-0.028783696,-0.01152024,-0.00201713,0.012895279,0.009637227,-0.021523405,-0.032981202,-0.013284289,-0.10136451,-0.0111273285,0.01435787,0.00028399893,0.054173518,0.016278014,0.0030845152,-0.020003775,0.0005072818,-0.021358287,0.001869376,-0.04558933,-0.0064306627,-0.025478704,0.027291672,0.017514557,0.0074778744,-0.016779862,-0.014879761,-0.025337335,-0.010903866,0.00983445,0.010186302,-0.011777465,-0.019250283,0.015673475,0.007882362,-0.007488161,-0.003451452,0.015375757,0.023682602,-0.0950662,-0.028400507,0.016738674,0.009205866,-0.0062070796,0.00623101,-0.011050975,0.014007551,-0.012200311,0.0054468527,-0.012245296,-0.05204644,-0.020564368,-0.0075894822,-0.014476467,0.11232131,-0.006147858,0.018595736,-0.00016577313,0.020729337,0.021161482,-0.008597295,0.0010647557,0.015120038,-0.01216075,-0.008577614,0.00031448822,-0.011945382,0.009020332,0.036754075,0.030888075,0.04246193,-0.02449748,-0.009197292,0.015282703,-0.0071280925,0.0067155003,-0.031473644,0.03924305,0.016359905,0.016478367,0.016077647,-0.010483996,-0.025831528,-0.0047190506,0.008517609,-0.032880053,-0.024798643,-0.0027462894,-0.00844723,-0.0107374685,-0.08148579,0.0025664638,0.0017691655,0.017016692,-0.0075846137,-0.0117276525,0.016293975,-0.0041611446,0.0034940403,0.007279057,-0.019826194,0.001637054,0.025633844,-0.013740427,0.010346162,0.016040495,0.017704768,-0.0046120747,0.0069921785,-0.015482732,0.0106516415,0.0017931678,0.0080052465,-0.0066586295,0.009850145,0.016191505,0.004617297,-0.0027686907,-0.021675443,-0.0021969697,-0.0022453587,0.0122584505,0.009041065,-0.014184814,0.010285733,0.0031801616,-0.003986856,-0.009213425,-0.011926781,0.015835779,0.04480582,-0.010059887,0.0120969955,-0.016692748,-0.0038030825,0.009435478,0.008079983,0.02824566,0.0018283594,-0.03135746,-0.028089684,-0.0010924444,-0.02468677,0.0076751304,0.010930404,0.0103903655,0.034284744,0.032601904,0.0044189505]') ON CONFLICT DO NOTHING;

INSERT INTO public.vector_store VALUES ('7b69c3fa-f13c-4df4-8031-26aae2ae0025', '# 🌐 REST API vs GraphQL 실무 비교 및 설계 가이드

> **한 줄 요약**: REST는 리소스 기반의 표준 HTTP 메서드(GET, POST, PUT, DELETE)를 활용하는 명확한 웹 표준 아키텍처이며, GraphQL은 클라이언트가 필요한 데이터 구조를 단일 엔드포인트에서 쿼리로 직접 지정해 가져오는 쿼리 언어입니다.

---

## 1. 📊 핵심 비교 및 특징

| 구분 | REST API | GraphQL |
| :--- | :--- | :--- |
| **엔드포인트** | `/api/users`, `/api/orders` 등 복수 엔드포인트 | `/graphql` 단일 엔드포인트 (POST) |
| **데이터 오버패칭** | 불필요한 필드까지 고정 응답 (Over-fetching) | 필요한 필드만 선택 응답 (No Over-fetching) |
| **언더패칭** | 연관 데이터 조회를 위해 N번 호출 (Under-fetching) | 단 1번의 쿼리로 연관 데이터 통합 수신 |
| **캐싱** | HTTP 표준 캐싱 (ETag, Cache-Control) 용이 | HTTP 캐싱 복잡 (클라이언트 캐시 라이브러리 사용) |

---

## 2. 💻 GraphQL 쿼리 예시

```graphql
# 클라이언트가 필요한 사용자 이름과 영수증 결제 금액만 요청
query GetUserReceipts {
  user(id: "3") {
    name
    receipts {
      payAmount
      payDate
    }
  }
}
```', '{"title": "[API 아키텍처] REST API vs GraphQL 실무 비교 및 설계 가이드", "userSeq": 1, "guideSeq": 26, "isPublic": true, "chunk_index": 0, "total_chunks": 1, "parent_document_id": "7c4f8194-f9d3-41cb-a03d-693656b08f96"}', '[-0.029128158,-0.008058974,-0.0038177003,-0.055240083,0.006390821,0.019878298,-0.016163435,-0.0021204324,-0.01257027,0.014530637,-0.0075679775,0.022628352,-0.0033109316,-0.0014195365,0.1283306,-0.039069414,-0.009619242,0.025079887,-0.004497332,-0.012882752,0.010297125,0.010983132,0.010273324,0.00375228,0.0055111125,0.009244587,-0.014404118,0.0040881615,0.032776117,0.015471355,-0.03263096,0.005041889,0.015969899,0.031081906,-0.015925946,-0.0023473795,-0.009326323,-0.032596603,-0.01252706,0.01405217,-0.005522024,-0.014849003,0.013713934,-0.00566347,-0.014875946,0.008953151,-0.00067380833,-0.0074357972,-0.009198641,0.034183003,0.008792065,0.010251423,0.0033366792,-0.18665996,-0.0013653522,-0.00018469003,-0.0119391205,-0.0052618277,-0.01821815,0.01871877,-0.016699526,0.01981662,-0.0050406107,-0.0007553893,-0.003647323,-0.021438584,0.031921245,-0.008571553,0.0077057127,-0.014044872,-0.010352284,0.006064252,-0.030014485,-0.017547391,0.0070350603,-0.0063170516,0.024239274,0.026811324,0.027193094,0.0026696224,0.012486946,-0.031918105,-0.030356454,-0.0008795701,-0.022460477,-0.018194124,-0.003802636,-0.01668462,-5.5961074e-05,0.0182175,0.019573495,-0.0028649238,0.015027365,-0.010382106,0.0031193444,-0.017514208,0.0021504073,0.0038338958,-0.017990151,-0.02092162,-0.005671109,-0.005812113,-0.0046353037,0.023559816,-0.010635548,0.008989272,0.0022944224,0.000502288,-0.016567193,0.0031217616,0.0048092473,-0.02574293,-0.0013145991,0.013671822,-0.0063212984,-0.13699524,-0.007381271,-0.027960174,0.027401855,0.00070714473,-0.016687967,-0.0003304549,-0.016591376,0.0032155628,-0.009788714,0.0054167397,0.0048380047,0.0069041243,0.016554879,-0.033113353,-0.004529184,-0.010398159,-0.018295057,-0.015147971,-0.0013103185,0.016232412,-0.0005094127,-0.02176806,-0.041303653,-0.01727814,-0.0011502026,0.011293623,-0.008064822,0.008313952,0.0069147204,0.0031457643,-0.05162416,-0.003176215,0.004774036,-0.021078397,0.022096721,-0.008936174,-0.027324729,0.044109046,0.013174113,-0.005549683,-0.0011400167,0.008709649,0.010265045,0.004358629,-0.023279836,0.030558746,0.027406838,0.007246769,0.021433191,-0.021072004,0.006367918,-0.015247448,-0.008952363,-0.014732568,0.017372884,0.03354949,0.0028900648,-0.028307637,-0.0073796725,0.006845123,0.013622716,0.0023467075,-0.024324294,0.013911684,-0.021725567,-0.018412681,-0.010412962,-0.021247966,-0.019980988,-0.017779458,-0.0035583712,0.019388786,0.018447453,0.0024753637,-0.0050443923,0.018132715,0.016717188,0.001653829,0.021562137,-0.05355882,0.006496593,-0.016389621,-0.020498548,0.013466408,-0.0017477876,0.0093178535,-0.0018144984,-0.0007823104,-0.030161137,0.0016422751,0.0043503474,-0.04304008,0.021029845,0.0012882662,-0.024055997,-0.0047978046,0.023741337,-0.030624751,-0.0134841185,-0.02294647,-0.011718638,0.027376693,-0.020205809,-0.008306973,0.0048642997,0.0024279766,0.009817251,0.027674343,-0.0010043447,-0.0059922487,-0.0032347282,-0.0112409545,-0.011212703,0.0027042762,0.011405135,0.0186782,0.011541673,-0.026649302,0.055650312,0.008828132,-0.0021379087,0.040807683,0.023630818,0.027980948,-0.025709929,0.005078021,-0.011882436,0.009703363,0.027015029,-0.014520076,-0.028692167,0.0039568325,-0.008285323,0.00791331,-0.010434128,-0.0023339177,0.004738146,0.011448577,-0.0043154857,-0.017826205,-0.016785672,0.0048545925,-0.014441959,0.019778684,-0.0022591683,-0.008605123,-0.014645891,-0.009472343,-0.0022492798,0.019932594,0.022200597,0.00072466634,-0.018056354,-0.0083241565,-0.0054767877,0.028534846,-0.023177411,-0.02978251,0.0009773715,-0.00074969354,-0.031785775,0.033028584,0.010347649,-0.054316014,0.024807895,-0.015406274,0.016722558,-0.011581524,-0.0081302365,0.027272249,-0.014966037,-0.007051788,0.008608864,-0.0031752433,0.0060540345,0.056255933,0.008280397,0.00933184,0.020077031,-0.041106705,-0.027329007,-0.012162542,0.023400329,0.0055327923,-0.020792004,0.025219193,-0.011136733,0.036275446,-0.014242307,-0.016536748,0.029910766,0.046024136,-0.018798854,-0.013861,-0.009148294,-0.013711127,0.02006376,-0.016505197,-0.004547608,0.010053526,0.023491679,-0.006894812,-0.019233644,-0.007182907,-0.007491746,-0.007497783,-0.01890862,-0.0015568596,-0.004467261,-0.002425125,-0.008470637,0.004690561,0.044981778,-0.009516678,-0.0085932445,-0.0023109121,-0.011101426,0.014364457,-0.0015950869,0.026696183,0.022058642,-0.011507327,-0.010422231,-0.012994072,0.015869174,0.024161771,-0.022433897,-0.032167163,-0.036039412,0.001281513,0.026631312,-0.01467924,-0.0020029035,-0.047035977,-0.022227854,-0.0126277385,0.027225332,-0.009351314,-0.013507258,0.009428473,-0.0097737955,0.0013639572,0.0068016876,0.02928504,-0.0033085432,0.012071852,-0.0042691124,0.0050123893,0.029673878,0.0026764718,0.022188079,-0.0070301075,-0.012928645,0.0020685277,-0.044084474,0.005714585,-0.0022773368,-0.025991337,-0.014100603,0.017044611,-0.017417323,-0.023565812,-0.012638571,-0.024560293,-0.019490734,0.028371258,-0.035377566,0.00011938048,-0.025767218,0.0037695759,-0.007657775,0.0015370224,-0.0028418985,-0.007017488,0.002876738,0.0065318984,0.0044825887,0.030037507,-0.024267755,-0.014041694,-0.017293008,-0.0053773527,-0.0006529729,-0.024170114,-0.018011611,0.019130042,-0.012927531,-0.009665422,0.005930216,-0.000929166,0.022503782,-0.008509069,-0.008818277,-0.008698444,-0.0024362097,-0.013131345,0.0065467954,-0.0029705572,0.004583158,0.027296096,0.012510123,0.01960901,0.016322184,-0.04506222,-0.0023337842,0.022569325,0.010300355,-0.007251702,-0.0042508617,-0.01760691,-0.021812605,-0.010060708,-0.015575356,-0.0015990768,-0.0025669155,0.0053122803,-0.034040667,-0.017101016,-0.011400353,0.0062451884,-0.016617693,0.00024105294,0.013807561,0.004311858,0.016352303,-0.009993482,0.0040045637,0.0049392264,0.0063189436,-0.005239579,0.013744821,-0.0067035784,-0.023175605,-0.016231544,0.015186055,-0.011818792,-0.010600548,-0.054172598,0.016356809,0.011821435,-0.01668418,0.0011510989,-0.018400295,0.0012344925,-0.0133928815,0.003134797,-0.011961846,4.6353736e-05,-0.005098057,-0.019921761,-0.029602444,0.0073903827,-0.05236065,0.012949146,-0.02862365,0.020545723,0.022854874,-0.013014515,0.021401307,-0.0027838466,-0.025978025,-0.0023089624,-0.026985364,0.0013563469,-0.013415111,-0.00454871,-0.026594777,-0.03764082,0.005281838,-0.007198845,0.015564671,-0.013380016,0.032714896,-0.006459207,0.02278788,0.006107224,0.01559268,0.008277821,-0.0062690005,-0.0022614307,0.010411384,0.003987508,0.009199129,-0.008327336,-0.01170271,0.009502024,-0.014566979,-0.009188689,0.009451902,-0.011478237,0.017511265,-0.011111708,0.020113736,0.0005360985,-0.0045692204,0.023669517,0.018615287,-0.04698882,0.02919012,0.016998157,-0.017886043,0.002973718,-0.03299508,-0.035676625,1.4637881e-05,0.00924489,-0.02552073,0.03705841,-0.037103042,-0.00072237896,0.017875444,-0.0071097533,0.013897793,0.019594353,-0.02185716,-0.00043092205,-0.0060604136,-0.0031658374,-0.013292233,-0.012013918,0.012546292,0.0138489,-0.04215437,-0.015589735,-0.01695345,-0.02191799,-0.002215529,0.0037373626,0.011530861,-0.08295344,-0.016042724,-0.009596949,-0.024302734,-0.003299769,-0.008096942,-0.0028552404,-0.020543361,-0.0037778986,-0.0057842946,-0.0031700511,-0.010454258,0.035696946,-0.0010974858,-0.0057065827,-0.00860548,-0.02624538,-0.0003289538,0.01804322,-0.026268404,0.036733665,-0.01222306,0.024028853,0.025570847,-0.010214213,-0.020249486,0.008386979,0.01481908,-0.0037615024,-0.0042022886,-0.035786442,-0.022536434,0.023435004,0.001579514,-0.015423226,0.004344086,0.020438107,-0.014979618,0.008835649,0.0128837405,-0.02435872,-0.007027256,0.0019286658,-0.017801646,0.011340609,0.012601153,-0.012727757,-0.04719598,0.012450203,0.015151942,-0.025588468,-0.03386625,0.005050198,-0.021435726,0.02181663,0.01613619,-0.0052764527,0.00325397,0.008165799,-0.014952611,-0.02275326,-0.007060797,-0.007899184,0.0436979,-0.013563004,0.026112907,-0.0053021684,0.010137274,0.004112483,-0.009735222,-0.013472971,0.0054448717,-0.020376459,0.01529144,-0.024904193,0.013448384,-0.007537644,0.0018982389,-0.02214989,0.012262777,0.011275419,-0.006413266,-0.0943373,-0.0055464,-0.015120472,-0.010991392,0.0055851312,-0.0067175333,0.0044360016,0.001950285,0.006959797,-0.017246542,-0.0050332104,-0.025031814,0.0095884,-0.04435041,0.014423473,0.018101458,0.00058195845,-0.0046623717,-0.008169514,-0.0012583592,0.006627222,0.0071347854,0.015238313,0.025851985,-0.0326623,0.0125718275,0.00567754,0.0014008763,-0.008190757,0.0034080883,0.023840925,-0.118066564,-0.0114996545,0.007191086,0.0067012915,-0.0075559886,0.025349531,0.0032023396,-0.008870522,-0.0011398563,-0.009552563,-0.008610569,-0.027087294,-0.012625313,-0.011422825,-0.006686108,0.09925436,0.0077938694,0.016499318,-0.021606078,0.020237971,0.004173158,-0.020692775,0.01260037,0.008729857,-0.008202541,0.00027701,-0.024055075,0.022035362,-0.00012391455,0.023474317,-0.011807201,-0.011016175,-0.027237121,0.012695871,-0.0036312398,0.007837886,0.04039876,0.010282023,0.008433653,-0.009456828,0.024169665,0.024191123,-0.026889434,0.0012188174,0.0069183656,-0.016525466,-0.00851248,0.016443547,-0.010104983,-0.0243528,0.005113253,-0.054890458,0.0035781544,-0.0151570095,0.00016256227,0.0113427285,0.008583016,0.010549286,0.0018386971,-0.0024906509,0.023972869,0.004787974,0.029309629,0.008898818,1.660678e-05,-0.0040471572,0.0263189,0.037677653,-0.0011955912,-0.021169921,-0.027349131,0.009931723,-0.012890184,-0.0044343774,-0.0079367785,-0.015858341,-0.005678753,-0.0034023018,0.0011411635,-0.010113704,0.01495297,-0.0022028785,0.0017968784,-0.026463903,-0.0074566356,-0.0056084963,0.015829341,0.0016006436,0.012670346,-0.007628648,0.023721242,0.03522849,0.0041583213,0.033824805,0.010720962,-0.010149,-0.0005773331,0.011444664,0.021349924,-0.0022818071,-0.021111146,-0.022274595,0.00083946355,-0.002004518,0.016672298,-0.024488837,0.0132749,-0.0010661945,0.050576635,-0.02060572]') ON CONFLICT DO NOTHING;

INSERT INTO public.vector_store VALUES ('84001909-5891-4043-9511-01e53e993ded', '# 🐳 Docker와 Kubernetes의 실무 비교 및 오케스트레이션 가이드

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

## 3. ⚖️ 실무 도입 판단 기준', '{"title": "Docker와 Kubernetes의 실무 비교 및 오케스트레이션 가이드", "userSeq": 1, "guideSeq": 24, "isPublic": true, "chunk_index": 0, "total_chunks": 2, "parent_document_id": "5b7ee637-dc00-4fdd-a4ab-c221c6a77656"}', '[-0.0044169063,-0.010804638,0.015561696,-0.06710601,-0.0047816485,0.012343638,-0.0045694597,-0.018334042,-0.002886519,0.00937591,-0.014536861,-0.0026084643,0.0067596287,0.007813229,0.1478114,-0.0150893815,-0.007938987,0.022270204,0.005768925,-0.016321778,0.030209431,0.01947766,-0.02231236,0.01703408,0.0012955645,-0.005861306,0.02286974,0.0060336767,0.014293505,0.0050024544,-0.0067855273,0.016154557,-0.018924516,0.02124991,-0.013307085,0.01925671,0.013693292,-0.035633594,-0.026526414,0.031859513,0.0067583895,0.0034096746,0.011071117,0.009462161,-0.009180651,0.011981371,0.0006309737,0.0018236964,0.013238448,0.00046169435,-0.019553557,-0.017878935,0.01604982,-0.17485501,-0.012681395,-0.006634539,-0.03307896,-0.025932852,-0.025510298,-0.021820221,-0.018625524,0.01509216,-0.008036003,-0.020699548,-0.0076597026,-0.0014431587,0.032102708,0.007533824,0.0012544501,-0.017780412,0.008824247,-0.0016680944,-0.0033318605,0.01556825,0.012196723,-0.013334384,0.020120047,0.019468803,0.015842846,-0.0024506869,-0.0051303245,-0.03030979,-0.027984152,0.010134107,0.0045861257,-0.0060660997,-0.04378749,-0.017973877,0.006907931,0.026375065,0.02948261,-0.0063637886,0.009237274,0.012736189,-0.018197263,0.015748765,-0.0150475,-0.028419131,0.016534105,0.004274228,-0.01765789,0.0076278136,0.0018025977,0.043451834,-0.01405969,0.0038808223,0.0034235395,0.00035356625,-0.014301701,0.0051009567,-0.007851527,-0.031177988,0.00540622,0.015612506,0.00617586,-0.14064972,0.0009682271,-0.027566537,0.027636735,0.00935327,-0.00541703,-0.0027799353,-0.0013283556,-0.011277617,-0.008499071,-0.009925638,0.0055364487,0.007613504,0.0330261,-0.014864563,0.027996445,-0.041296627,0.0051846257,-0.0016246451,0.006038613,0.002528906,-0.012013922,-0.017497059,-0.032147333,-0.026488584,-0.0059181363,0.014521195,-0.026513845,0.008842499,-0.0397948,0.009497891,-0.04181869,-0.019202309,-0.016475834,-0.026365094,0.019640123,-0.032438815,-0.03110185,-0.006329863,0.019345889,-0.01858895,0.012137123,0.00565549,-0.023476463,-0.019392336,-0.013657767,0.022542529,0.020925269,0.0018390412,0.01261253,0.010701113,-0.011195474,0.03282132,0.0009876622,0.00018255638,-0.004370364,0.016931696,0.002007847,-0.002033762,-0.010293775,-0.015076982,-0.0016893143,-0.020933036,0.0078382725,-0.006812832,-0.0013186721,0.02205945,0.009081383,-0.017950848,-0.022403091,-0.007026485,0.010791316,0.019337166,0.047999695,0.0003991459,0.013436523,0.021246396,-0.00438934,-0.012549029,0.009810986,-0.059761345,-0.019479292,-0.018543236,-0.00044685783,0.019786157,-0.0011806731,0.04196747,0.0086402865,-0.0338845,-0.0027245623,0.022626584,0.012778039,-0.034749825,0.0009625807,-0.015001949,-0.005587461,0.018501176,0.027609669,-0.0060683745,-0.0100175375,-0.021439426,-0.018362226,-0.017508024,-0.0038798808,0.0023145408,0.017628739,0.007655024,0.0023622315,0.03406717,0.004872747,-0.009350197,-0.011758653,-0.00883581,0.0033435284,-0.008962084,0.0049804044,-0.010988714,-0.012014398,0.016201058,0.043648235,0.0011351778,7.114434e-05,0.02976521,-0.014682908,0.018205937,-0.036898106,0.015937915,-0.01151402,0.024366591,0.026010195,-0.017156688,-0.018307513,0.0047403355,0.0060159513,0.017359251,-0.018137261,0.009055953,-0.013682024,0.0041534044,0.008053703,-0.0037457852,0.008132001,-0.031309195,0.008947086,0.019538483,-0.013254737,-0.018656164,-0.0029123626,-0.0049840156,0.023789931,0.01293703,0.013339259,-0.010645028,0.009610328,0.0019282871,-0.01918911,0.011319734,-0.011623164,-0.026998682,-0.015627343,0.009408429,-0.026250103,0.034426726,0.014538093,-0.042508274,0.004098039,-0.019973928,0.0023668578,0.010945796,-0.010980893,0.023050357,0.010691087,0.0050581014,0.005761328,-0.022466168,0.026424663,0.02466562,-0.028245145,-0.01922411,0.021817762,-0.018044684,-0.027638488,0.014998259,-0.006027831,-0.01229873,-0.014457892,-0.030897813,0.012362383,0.05451284,0.029269686,0.007871413,0.02096536,0.013411622,-0.008122577,0.0009703709,-0.03223847,-0.016681423,0.006432562,0.0034723452,-0.009021585,-0.011907486,0.02627581,-0.011457714,-0.013918855,-0.016784154,0.0006049674,-0.013711476,-0.024246424,0.010236121,0.008523167,-0.013452313,0.000599613,-0.00013166564,0.019136136,0.001116564,0.01910905,0.0038569542,-0.004362441,-0.0015356747,-0.018855724,0.024148319,0.0143748345,0.015871156,-0.024919135,0.01508364,0.008045353,0.004881186,-0.027193645,-0.011979588,-0.039138913,0.006144813,0.019685347,-0.03745532,0.026977383,-0.03143906,0.0057936576,0.014531958,0.003759186,0.003996971,-0.014550522,-0.004256901,-0.0061207665,-0.018655833,0.002808862,0.026125522,0.0020549058,0.03872293,-0.0038594713,-0.011426307,0.005078687,-0.008822068,-0.016309995,-0.010354225,-0.022354187,0.023239862,-0.02257343,0.014067173,0.0006232705,0.003974215,-0.025758086,0.040211204,-0.021166747,-0.02670348,-0.0006736899,0.007463182,-0.007700783,0.034979295,-0.034998287,-0.005706188,-0.030089887,-0.02332902,0.031849932,0.008454475,-0.0009830515,0.014485751,0.0022278065,0.020747801,-0.009924975,-0.009165564,0.0049458244,-0.020201892,-0.043487232,0.011641856,-0.012163138,-0.027526023,-0.019073505,-0.0009881232,-0.011593043,0.012263207,-0.0031929456,-0.0073801926,-0.011832285,0.013422203,-0.028592754,-0.016934667,-0.010142298,0.0013235701,-0.010698093,0.015629837,-0.007410565,-0.019593082,0.0038604506,0.039372463,0.015532974,-0.024575207,-0.007867103,0.02786245,-0.013585159,-0.009432969,-0.001650362,0.013467803,-0.05288933,0.0065820008,-0.043677475,-0.008196167,0.0012834539,0.013279215,-0.023513155,-0.016002981,0.0047900816,0.0011969368,-0.033095177,0.0054589678,0.019391213,0.013557763,0.03953032,0.014008285,0.012725182,-0.017254133,-0.0064056027,-0.0043613743,-0.0032721232,-0.009668553,-0.02182332,-0.016664496,0.017004497,0.0057392423,0.026284942,-0.0051636607,-0.0008815024,0.006633113,-0.0025845221,-0.000628116,-0.0016484235,0.007132198,-0.007412002,0.0051446143,-0.021728804,-0.02440824,-0.03386122,-0.010933171,0.0016498808,-0.012735691,-0.031469055,-0.013528855,-0.013954929,0.016456002,0.020406941,-0.0047958265,0.007894248,-0.0072633387,-0.0020653042,0.009568228,-0.010034271,-0.008140929,-0.0033770094,0.026134862,-0.014198694,0.013190434,0.0059936554,0.0136216115,0.003035674,-0.018425765,-0.00084496004,0.015402039,0.014789629,0.0320443,-0.015691388,0.010572416,-0.016289381,0.016574424,0.00970469,0.001368322,-0.0022394946,-0.019402197,-0.008785147,0.015451488,-0.0073692217,-0.010629065,0.0013461182,-0.00443556,0.004037786,-0.017785639,0.0051479596,-0.005562331,-9.716207e-05,-0.00077312614,-0.012030616,0.013600322,0.00022594641,0.016924048,0.00654946,0.008986713,-0.0017165458,-0.009704072,-0.005445009,-0.019077,0.0046123196,0.028754594,-0.013785568,-0.020322293,0.007338889,-0.0017370008,0.0146229295,0.011521398,0.006078016,-0.0063406,0.0007059863,-0.015582122,0.0031648267,-0.01539307,-0.008845935,-0.009771734,-0.007040844,-0.008593023,0.019215167,-0.019844167,-0.004506239,-0.00083801214,-0.0018615663,-0.08051088,-0.008524196,-0.01000946,0.02899666,-0.02406965,-0.013694374,-0.035155304,0.014254635,0.007100441,0.018083593,-0.0077172304,-0.004351183,0.0312358,0.0019522771,-0.015133124,-0.004268571,-0.0033390722,-0.03301375,0.021263342,0.0026801988,0.020751478,-0.023348099,-0.009458114,0.010371099,-0.02749631,-0.018223992,0.0138187185,-0.0064846952,-0.017099926,-0.012624544,0.0091064675,-0.008234922,0.03385496,-0.035444837,0.0112004,-0.01826191,0.004485415,-0.037375685,0.009576932,0.018531311,-0.002869948,-0.013007233,0.010318177,0.006702987,0.0038086076,0.013065243,-0.02481461,-0.015138933,-0.01842337,0.017615484,-0.024190862,-0.014725347,-0.029983802,-0.02369007,0.013829364,0.00089688785,-0.0071785697,0.014018156,-0.014380953,0.005940936,-0.0036268656,0.021186024,0.0066828984,0.0382825,-0.037535585,-0.002133199,0.006866743,0.013414277,0.006932384,0.006902637,-0.0027878291,-0.0029537298,-0.0030554524,0.0030240861,-0.016105657,0.001984184,-0.028884204,0.030418364,0.016709743,-0.007606067,-0.027943337,-0.021890542,-0.09047291,-0.0026894994,-0.012849405,0.0028133695,0.024702271,-0.009211965,0.0035782608,-0.018506901,0.009975024,0.00049322937,0.004825743,-0.045408,-0.0036563089,-0.017539224,0.013061175,-0.0061175027,0.005876451,0.021286938,-0.033624362,-0.009030677,-0.0042982097,-0.023289353,0.019902537,0.0045358646,0.01756036,0.03763904,-0.00068636995,0.02013582,0.002034671,0.008457774,0.02250699,-0.10878838,-0.0074373866,0.008422677,0.000936253,0.010145265,-0.018458419,-0.00040563266,-0.01363855,-0.0005461401,-0.002946649,-0.027301097,-0.028323935,-0.04398271,-0.01363394,-0.024557568,0.09828266,-0.010280525,0.013570426,-0.0041755494,-0.005931616,-0.0049839267,-0.015178174,-0.012618127,0.008308887,-0.027675658,-0.018352747,-0.01214962,0.007753076,0.018619891,0.014626115,0.015390918,-0.019612031,-0.048902407,-0.0048597087,-5.8605337e-08,-0.024817524,0.0076466994,0.00849634,-0.014014405,-0.0021211545,0.008789991,0.0027014348,0.009308528,-0.017089693,0.011347004,-0.0004570582,-0.020909512,0.0036853054,-0.018245384,-0.0062553757,-0.020327473,-0.07958857,0.016786316,0.0010181214,0.012511003,-0.010057495,0.0024636968,-0.007833893,0.0123052,0.0029803202,0.031222034,-0.0009738218,0.006846096,0.028156126,0.0019924066,0.015737562,0.0143227605,0.025370957,-0.013053719,-0.011238544,-0.007783319,0.018009039,0.006218961,-0.0045692525,0.013788608,0.002230247,-0.009144613,-0.0016243161,0.00565866,-0.0053362325,0.008555904,0.011579265,0.008564549,-0.037520353,0.011150719,0.0015630614,0.0126718115,-0.020648101,-0.003072155,-0.012153876,0.0074559734,0.06129503,-0.017925825,0.02250682,0.03353646,0.016557403,0.006478114,0.047699943,0.02051078,-0.008440585,-0.0068603335,-0.028624307,0.015168921,0.0017784094,-0.012995943,-0.020526819,-0.0032676812,0.009660489,0.03204338,0.007686118]') ON CONFLICT DO NOTHING;

INSERT INTO public.vector_store VALUES ('934aed67-39c6-424a-b8fa-1983f8ed98a7', '# 📩 Event-Driven 메시지 큐 (Kafka vs RabbitMQ) 비교 가이드

> **한 줄 요약**: 메시지 큐(Message Queue)는 시스템 간 결합도를 낮추고 비동기 처리 및 대용량 트래픽 버퍼링을 제공하는 이벤트 기반 아키텍처의 핵심 부품입니다.

---

## 1. 📊 Apache Kafka vs RabbitMQ 비교

- **Apache Kafka (분산 스트리밍 디스크 파일 기반)**:
  - 대용량 로그 수집, 대규모 이벤트 스트리밍, 메시지 영속성 보장 (소비 후에도 메시지 보존).
- **RabbitMQ (AMQP 프로토콜 메모리 기반)**:
  - 복잡한 라우팅 규칙(Exchange), 미세한 우선순위 큐, 메시지 즉시 소비 및 삭제 처리.

---

## 2. 💻 Kafka Producer/Consumer 예시

```java
// Spring Kafka Producer 예시
@Service
public class EventProducer {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendAuditLog(String eventJson) {
        kafkaTemplate.send("audit-log-topic", eventJson);
    }
}
```', '{"title": "[Architecture] Event-Driven 메시지 큐 (Kafka vs RabbitMQ) 비교 가이드", "userSeq": 1, "guideSeq": 29, "isPublic": true, "chunk_index": 0, "total_chunks": 1, "parent_document_id": "9ceaa173-a920-410d-b340-4cd34aac2957"}', '[-0.0008332868,0.01383282,0.0033039043,-0.054322526,-0.031629987,0.012063594,-0.037520267,-0.016081452,-0.00025680242,0.02040469,-0.0024282234,0.016955754,0.0014667386,-0.010193978,0.12491574,-0.006666697,0.018576419,0.019323457,-0.0001717732,-0.011940226,-0.015962718,0.03143025,0.011162289,0.011568861,-0.019470317,-0.020727232,0.016401721,-0.02582413,0.021473072,0.021403927,-0.016347874,0.0144149065,-0.014300635,0.03559828,0.008327487,0.0056287344,0.047865547,-0.028094564,-0.019483078,0.02880713,0.03044404,-0.017913397,0.0018645962,0.029451912,-0.02396909,0.029072396,-0.0054191365,0.012713419,0.01770176,0.013010208,0.008021911,-0.0036307038,0.013376631,-0.17354105,-0.025523733,-0.0058478587,-0.024639033,-0.007006327,-0.0053556086,-0.008755196,-0.021255193,0.021130245,-0.03303319,-0.03688417,-0.009504925,-0.017292127,0.01826639,0.009306197,0.0027802624,-0.028497621,-0.0029821354,0.0040976997,-0.025393423,-0.0023523057,0.023375366,-0.03773607,0.0055383793,0.01124634,-0.012683987,0.006907871,-0.013492593,-0.03218635,0.003073275,0.0076236394,-0.019701984,-0.011227252,0.0031775017,-0.016034726,0.011027965,0.039020218,0.02008514,0.008942673,0.019977955,-0.032712217,-0.024024513,-0.0064501353,-0.0109654935,-0.024358971,-0.00065714435,-0.010031264,0.02843963,-0.0012893537,-0.009755558,0.024589697,0.0020099415,-0.0040888162,-0.0005535241,-0.007894767,-0.028222052,-0.0006068348,0.0026417049,-0.008945725,0.019469785,0.0044935113,-0.008812835,-0.13635957,-0.009459645,-0.021770814,0.019653803,-0.02735311,0.002903072,0.008304966,0.011154248,0.008849464,-0.017532775,-0.010990121,0.023188725,0.014701233,0.03303971,-0.011716376,-0.012748626,-0.018013908,-0.02789063,0.015664829,0.0059139333,0.03301544,-0.004178136,-0.023951046,-0.03994866,-0.017044587,-0.006571473,-0.012431294,-0.02790556,0.0071396152,-0.01854997,-0.012571189,-0.057328437,-0.02218393,-0.01531915,-0.04036253,0.016226904,-0.00032630964,-0.012041495,0.012834471,0.017101645,-0.014640514,0.026827928,-0.0033293646,-0.0011684492,-0.018749641,-0.037882637,0.021403637,0.0018935995,0.017101247,0.029648393,0.035941757,-0.012627456,0.019451749,-0.009971227,-0.00014246472,0.012714672,0.015140694,-0.018190803,-0.012400051,-0.0063233804,0.014481451,0.0065859472,-0.005736756,0.007106217,-0.017238367,0.007990093,0.00014033628,-0.01382623,-0.011168754,-0.02411646,-0.012160708,-0.010076642,0.0008443828,0.028801633,0.010670362,-0.004883223,0.01268707,0.0065423143,-0.031482402,0.021381427,-0.049557183,0.0037651495,-0.0075432905,-0.021527205,0.023986159,0.03959309,0.016111057,-0.0151396785,-0.016610535,0.009855235,-0.014682967,-0.005513145,-0.032727774,0.013551677,0.013499188,0.006222628,-0.0021451681,0.016317567,-0.017164586,-0.026140468,-0.027494788,-0.013424096,0.0053742267,-0.015379135,-0.02113416,-0.019863283,-0.016641686,-0.0008432861,0.02399715,0.0117745055,-0.014711344,0.0015429979,0.0060761147,-0.0017457901,-0.0060003223,0.011861924,0.007940684,-0.009188126,0.007983374,0.05016086,0.005205642,-0.010128547,0.04501233,0.005712684,-0.015772589,-0.038939178,0.021118512,-0.010550453,0.03901806,0.01812605,0.009207651,0.013989209,0.015398098,0.0022373255,0.006570551,0.00837703,-0.010583401,0.03601441,-0.005307059,-0.0018617465,-0.0151550155,-0.001973239,-0.0025077013,-0.0026846086,0.0014747597,-0.009291182,-0.006234316,-0.001408964,-0.0148033835,0.0071056653,0.017857712,0.01554301,0.016200546,-0.03498327,-0.00068049354,0.0019980364,0.008515294,-0.024740603,-0.02168667,-0.022679504,0.013705478,-0.017826475,0.017640553,-0.008065056,-0.03762304,0.0026926233,-0.02947394,-0.00030892433,0.006832469,-0.018052738,0.009182631,-0.00869335,0.007334885,-0.004108016,-0.0064767944,-0.010402681,0.012436318,0.006934698,0.016910143,0.020906255,-0.034249507,-0.023194833,0.0047272732,0.03915884,-0.007990169,0.019708231,0.0015056191,-0.0057629338,0.04409323,-0.0054843584,-0.0056603705,0.03334279,-0.0015271953,0.026351152,0.018422106,-0.008835807,-0.00777245,0.015021789,-0.002710008,0.0029224427,-0.019475771,0.020106215,0.0025997425,-0.016790768,-0.0032255938,-0.005905699,-0.021791033,0.006311593,0.019046333,-0.015653728,0.011661717,0.007547332,-0.019812241,0.025350325,-0.025838818,-0.012445136,-0.005410282,0.012168809,0.00808531,0.008578139,0.01202579,-0.022027936,0.012228984,-0.0061170124,0.039484482,-0.018336168,-0.018979827,-0.007303236,-0.008533327,-0.034725554,0.016336814,0.046426803,5.9201917e-05,-0.027257204,-0.024515636,-0.011731112,0.021922689,-0.002584852,-0.026803052,-0.020766972,0.0031133392,0.0070716594,-0.00520686,0.020845402,-0.00065753993,0.005551949,0.01938669,0.001633062,-0.010871355,-0.013445653,-0.01860275,0.009326186,0.014344738,-0.03289708,-0.00055617484,-0.036679655,0.04409139,-0.0035331056,0.025539005,-0.020631922,0.018985437,-0.0017507061,0.006685495,0.010646203,-0.006427469,-0.014534841,0.008613265,-0.020495864,0.012944788,-0.015698425,-0.0021100773,0.022967817,-0.016743667,-0.01600065,-0.014396257,0.00016369167,0.014613895,0.030159436,0.011617998,-0.006356156,-0.00225463,-0.023484208,0.006462413,-0.012723683,-0.011081115,-0.024122478,0.0035352057,-0.018737715,-0.011624098,0.0133730685,-0.027453337,0.014440203,-0.01948986,0.00038730356,0.0012533319,-0.022020586,0.0013168456,0.023161596,-0.011078769,0.0002322497,-0.015010919,0.012220678,-0.0012560934,0.02563343,-0.028365092,-0.0054538427,0.052370798,-0.02563102,-0.014377778,0.0017611575,0.010086291,-0.03639969,-0.006067178,-0.014951715,-0.021083156,0.0022170227,0.017233208,-0.008784989,0.0015294381,-0.004178861,0.011577311,-0.018863326,-0.03190378,0.005871908,0.01378058,0.014146642,0.0001826467,0.008807334,0.0040644137,0.035064235,-0.009519752,-0.0033626114,-0.002568875,-0.031396516,-6.3662475e-05,0.011625666,-0.0073149796,-0.019035744,-0.007114732,0.015347381,0.015458388,0.000529744,0.026891952,-0.008121158,-0.020523788,0.0064164363,-0.0055473195,-0.010579135,0.005577016,-0.020594835,-0.008498945,-0.007876788,0.011033124,-0.037262738,-0.002539648,-0.02170838,0.017300202,0.019854097,-0.013332723,-0.01245387,-0.01500069,-0.016147235,-0.027690083,-0.011308515,0.0012195858,0.0011974601,0.031051971,-0.0055775368,-0.00095474045,0.00095890986,0.019653447,0.03146617,0.00859814,0.013515216,-0.0071252375,0.023006877,-0.0016145207,0.0052268123,0.0013253061,0.0025606607,-0.018565776,0.025719216,-0.0042775576,0.032285176,0.00047297857,-0.02020664,0.012176288,-0.027274076,-0.0005329563,0.00071832835,0.02108951,0.019967515,-0.0019424238,0.00441017,-0.0057870047,-0.021522233,0.030317372,-0.005113383,-0.03610448,0.024743069,0.015875371,0.00057245,0.021713492,-0.01046213,-0.007977761,0.009691113,-0.00019050042,0.00058960065,0.035988607,-0.024733962,-0.0065051345,0.0017456448,0.016423492,0.03021409,0.00959019,0.004870761,0.005618606,-0.022099467,-0.001168545,0.039875496,-0.0042161336,0.007523826,0.017808795,-0.039979264,0.007059181,0.0029999127,0.0022264929,0.008376699,-0.01812837,0.025583047,-0.07912554,-0.015056974,0.0046202927,-0.0070041227,-0.009916774,-0.030422043,-0.0052501876,-0.010811661,0.024455754,0.0024034083,-0.0013764662,0.020725342,-0.0032307093,-0.029915303,-0.029829334,-0.0016226731,-0.01682688,-0.028274585,0.033869553,-0.02376365,0.0013483465,-0.026296327,-0.01702285,-0.009079893,-0.00892336,-0.002850381,-0.0033111745,0.015001048,-0.013664046,-0.010648492,-0.013876395,-0.0027627111,-0.0017811585,0.013972195,0.0065442524,-0.014843157,0.024119422,-0.015144033,0.015545012,0.021252211,-0.006150984,-0.0019876081,-0.0010656319,-0.0075247125,0.017167667,0.00013560349,-0.02095239,-0.025352715,0.0081907865,0.033723854,-0.024622636,-0.029838579,3.770786e-05,-0.033131618,0.0063150297,-0.0100576,-0.017938275,0.0014055871,-0.00023226227,0.012764214,-0.0021964475,-0.010939343,-0.00836316,0.03750252,-0.026789896,0.0023102039,0.00073697895,0.0030458004,0.015493527,0.014639239,-0.028152106,0.002812304,-0.018965557,0.010658491,-0.011383501,0.013680077,-0.024937768,0.0069602113,0.0063585564,0.02456967,0.0049870564,-0.027070688,-0.09625891,0.001900117,-0.01933409,0.0069546723,0.013012422,0.007558169,-0.0048411363,-0.0159201,0.0026284645,-0.022314608,-0.011960074,-0.0044714785,-0.005150236,-0.015150893,0.039383113,-0.012627933,0.0012047151,0.008641963,-0.016838307,-0.012686202,0.015093106,-0.020708023,0.018485753,0.0016613429,-0.0071221264,0.01335009,-0.009582121,0.0118635455,-0.0060881814,-0.008400201,-0.0051985052,-0.124117896,-0.021737313,0.0038186107,0.0012085787,0.004296453,0.024306996,0.01574763,-0.016222876,-0.0017166617,-0.024384947,-0.020975104,-0.021456607,-0.009432572,0.0021973816,-0.040674083,0.093833715,-0.007573748,-0.027093265,0.0063385833,-0.006605289,-0.006615079,-0.021351567,-0.005876878,0.012063347,-0.013493719,-0.020503206,-0.020176506,0.019513063,-0.002691534,0.0047546104,0.020918049,0.0007554168,-0.02652066,0.011952437,0.002386086,-0.0019370543,0.008793604,0.0073260213,-0.010030886,0.009317913,0.007834294,0.012165674,0.010299619,-0.02863004,-0.011569129,-0.010196791,-0.0012967451,0.022416515,-0.012451479,-0.021180704,-0.021398198,-0.09346951,-0.010215857,0.021175224,-0.011629905,-0.0031493392,-0.014086906,-0.0048293155,0.021055585,-0.0025403085,0.028084727,-0.006468007,0.019130675,0.030732099,-0.0039060519,-0.0028159143,0.017961355,0.0070018414,-0.0013248002,-0.0063242028,-0.0010732979,0.0002846374,0.013321042,-0.039488684,4.312718e-05,-0.00794631,-0.0009538963,0.0030336722,0.004251324,-0.014414877,0.022245541,0.0028898588,0.0039871684,-0.033562694,-0.0050395736,0.0012466722,7.985647e-05,0.010629308,0.013762428,-0.007307892,0.004044281,0.017798604,0.010899983,0.040127214,0.036723092,0.010526756,0.0051750033,0.042689394,0.014239613,0.0064167962,-0.0023027535,-0.013886603,0.024119757,-0.013718101,0.008373507,0.00012573371,-0.011097581,0.024375068,-0.007734556,-0.01888231]') ON CONFLICT DO NOTHING;

INSERT INTO public.vector_store VALUES ('c0450b7a-2d9c-490c-8ae9-f57962b2c64d', '- **Docker (또는 Docker Compose)**: 단일 서버, 개발 환경, MSA 초기 단계, 배치 작업에 적합.
- **Kubernetes**: 멀티 노드 서버 환경, 무중단 배포(Rolling Update) 필수인 서비스, 트래픽 폭주에 따른 Auto-scaling(HPA)이 필요한 엔터프라이즈 환경.', '{"title": "Docker와 Kubernetes의 실무 비교 및 오케스트레이션 가이드", "userSeq": 1, "guideSeq": 24, "isPublic": true, "chunk_index": 1, "total_chunks": 2, "parent_document_id": "5b7ee637-dc00-4fdd-a4ab-c221c6a77656"}', '[0.011447112,0.0024809954,0.011209897,-0.07397827,-0.015356577,0.006227404,0.0068050777,-0.015420139,0.013797974,0.012572098,-0.009608052,0.0011635385,-0.0021880502,0.020138295,0.1193333,-0.015472115,-0.011835174,0.0014614871,-0.021566875,0.004335628,0.036181163,-0.0041443356,-0.022211915,0.018950453,-0.0001623404,-0.0016400166,0.057350673,-0.0149382865,-0.00559951,0.014318987,0.0047329753,0.00028242508,0.0006152356,0.024535837,-0.006432336,0.027083723,0.04040393,-0.014077768,-0.02210149,0.010502871,-0.021875007,0.01199267,0.002705744,-0.008171561,-0.025285808,0.010190326,-0.00823116,-0.013638099,0.008108512,0.0013802494,-0.016744895,-0.03230841,0.008459674,-0.16566674,-0.013070799,-0.01782796,-0.024993174,-0.03258775,-0.00085148483,-0.008699776,-0.023981467,0.020407168,-0.01447204,-0.012598897,-0.0023057433,-0.027617505,0.0269793,0.006488519,-0.02275429,-0.033792585,0.017132474,0.002550127,0.007724445,0.011414895,0.0023407107,-0.016793644,0.0134997135,0.011187886,0.0071511017,0.021232465,0.01134899,-0.028589368,-0.008967,0.01457779,0.0048613343,-0.0032850592,-0.042580187,-0.018623268,-0.008750052,0.023068406,0.046507314,0.029726014,0.007301425,0.0053009605,-0.008558812,0.0095779905,-0.002740573,-0.009927823,0.01256543,0.0081349,-0.005689458,0.0066584386,0.0031213667,0.047459755,-0.012882457,-0.011642487,0.023237066,0.012379313,-0.005866674,0.0020187395,-0.0005700585,-0.0113548795,-0.004390482,0.0089916615,-0.013077526,-0.1318217,-0.0089888135,-0.015222544,0.009968962,0.0067564365,-0.023342336,0.009975284,0.024080364,0.013498948,-0.02468337,-0.011454164,0.021579552,0.0001473003,0.011350297,-0.019454379,0.010878386,-0.01162189,-5.190744e-05,-0.010677503,0.014170258,-0.0019532973,0.008743728,-0.01573456,-0.025996804,-0.02906087,0.008553269,0.03022347,-0.006218289,9.03512e-05,-0.06024763,0.012866519,-0.02831624,-0.013109477,-0.0027753278,-0.029503085,0.024281124,-0.021010574,-0.02367612,-0.005638717,0.021796705,-0.0059992913,0.0013521212,0.0106366845,-0.012026909,-0.023848003,-0.008357819,0.013174786,0.029069923,-0.0031002217,0.048464738,0.010357787,-0.004019106,0.015219204,-0.00073145947,0.0045033903,-0.017589107,0.018634913,-0.016300272,0.006141632,-0.0044012573,-0.01901864,-0.011003747,-0.0062932665,0.0030802877,-0.014266789,-0.003970948,0.029502055,0.018239668,-0.02864129,-0.024049567,-0.006371234,-0.013741416,0.010131129,0.019733515,-0.0035755367,0.021930495,0.001910817,-0.013223173,-0.020412551,0.0036138291,-0.057325944,-0.0028417928,-0.007401833,-0.004193247,0.018286297,-0.025165569,0.017433906,0.023029335,-0.023231262,-0.009350338,0.011844071,0.01774617,-0.021470014,0.029941726,-0.018835848,-0.011403541,0.0030494852,-0.002305903,-0.001954087,-0.008585073,-0.015547809,0.0062058335,-0.007956488,-0.017785797,0.0015312748,0.012295152,-0.0014810084,0.0022458916,0.032276385,-0.0024021184,-0.009233588,-0.007860945,0.0072669154,0.0012607339,-0.0048130355,0.012401951,-0.01149694,0.0045667533,0.023019629,0.039410077,0.0022384995,0.0013669429,0.019210069,0.0036284702,-0.004849141,-0.044892956,0.024606295,-0.014393521,0.030350516,0.01922349,-0.005283267,0.0011504213,-0.0009282767,0.020093273,0.022184119,-0.024076924,-0.0058929385,-0.0019839471,0.008488943,0.020306237,-0.008602495,0.0019064838,-0.017699655,0.00237222,0.0024741783,0.0019461532,-0.018685134,-0.00077498826,-0.0030221848,0.026010105,0.012450259,-0.0023282054,-0.012251862,0.0030808751,-0.0012054639,-0.014738087,0.010135974,-0.007039151,-0.026458014,-0.016911674,0.0029668896,-0.01962803,0.031612024,-0.02865279,-0.03701658,-0.0001746024,-0.01941938,-0.0028457292,0.016308157,-0.010026415,-0.005906152,0.0039415006,-0.016994717,-0.016804153,0.00070759293,0.0066355118,0.01102644,-0.023116257,-0.022911096,0.012711923,-0.02263736,-0.03665491,0.0136980405,-0.0012849902,0.0025909543,-0.008167229,-0.031713184,-0.004956941,0.071001954,0.016388075,0.02187159,0.029298933,0.014111596,-0.019590791,-0.011356186,-0.024442313,-0.013681859,-0.008018391,0.0060525998,-0.013499799,-0.0052331686,0.01914188,-0.013674946,-0.015291381,0.008558433,-0.002047786,-0.011814112,0.003403271,0.023175653,0.0032654803,-0.020522939,0.005599904,0.0044692103,0.007598587,-0.007519749,0.0031986265,0.00026907478,-0.011360132,0.0051536094,-0.017464321,0.017724408,0.025186313,0.010324665,-0.028383246,0.013062038,0.015688352,0.009598001,-0.012828917,-0.022210168,-0.021133626,0.0062218388,0.0104353605,0.0010522489,0.0034371074,-0.019219082,0.018359229,0.0109306695,-0.0067375815,-0.019773137,-0.016303785,0.0037488476,-0.014051501,-0.005434638,0.0059713544,0.032647427,0.0077779484,0.012767289,-0.012750374,-0.014935382,0.00923043,-0.03025235,-0.023078058,-0.009849883,-0.02271148,0.015837276,-0.021523174,0.0054895,0.015009995,-0.0056695873,-0.011718517,0.033724092,-0.027362857,-0.0095139025,0.0064923833,0.01521192,-0.015542214,0.0052932007,-0.01895594,-0.013914118,-0.0075231413,-0.025521327,0.0060767755,0.020810269,-0.0047553913,0.016349198,0.011341238,0.018803183,-0.003919662,0.004936862,-0.005466765,-0.00747962,-0.03506633,0.019803157,-0.024818644,-0.012104644,0.0030862708,0.011502192,0.0036611813,0.03157351,-0.017251262,-0.018375706,-0.017334415,-0.0010044401,-0.02681738,-0.029525058,0.022851793,0.020757934,-0.010613578,-0.0032034595,0.013515767,-0.021049583,-0.0116207935,0.033758007,0.015178387,-0.028198235,-0.0071421396,0.032096773,-0.009025906,-0.009285058,-0.02468218,0.0110159535,-0.049177047,0.011111848,-0.02469245,-0.021833865,0.019495895,0.0016747545,-0.023961043,0.0025123744,-0.015506067,0.018146457,-0.027248977,0.0036392098,-0.004745521,-0.026610836,0.033808943,0.013413624,-0.0013014689,-0.020986281,0.012574695,-0.009124709,-0.01669986,-0.022146745,-0.0149350595,0.007232745,0.01712513,0.04015847,-0.028208867,0.0044019297,0.015890285,0.017262058,-0.014761838,0.0014174987,-0.002150364,0.010830022,-0.014618823,-0.0018065352,-0.021033522,-0.019975875,-0.018830946,-0.01767492,-0.0068678223,-0.021977682,-0.022625163,-0.0065702326,-0.021882163,-0.0047410526,0.006266633,-0.024343861,0.0037459014,-0.016499633,-0.010150193,0.015010108,-0.00724943,-0.023771673,0.017094608,0.033018902,-0.0067042196,0.0077578803,0.03533864,0.012499111,0.021069624,0.007365473,0.004587126,0.03580761,0.015147673,0.053100318,0.0039061722,-0.0034410853,0.0041136,0.0006846325,0.017382031,-0.0026087605,0.009356675,-0.00033630806,-0.0067723216,0.017770054,0.010104106,-0.014741602,0.0061128093,-0.0062892516,0.009901618,-0.0005629344,0.021489328,0.0013776595,-0.02845606,0.001813089,-0.02061073,-0.013380592,-0.004360799,0.0049273367,0.0021642225,0.023223437,0.0011046941,-0.004937665,-0.0076904125,-0.011567217,-0.0153242005,0.007620086,-0.029175326,-0.0028042872,0.024075389,0.001302538,0.019805823,0.018508624,-0.0066538574,0.010317414,-0.0002592629,-0.016351681,0.0096077,-0.020496078,-0.010455294,-0.0044274745,-0.033497952,0.013021313,0.020126544,-0.012145705,0.011573058,-0.019903716,0.012998784,-0.09254015,-0.011326316,-0.0027729895,0.0060368557,-0.020459881,-0.01855029,-0.008436092,0.00014901148,-0.0029652647,0.015000422,-0.0015745027,-0.005316879,0.007859333,-0.006665737,-0.036313426,-0.011471092,-0.0009238791,-0.0061551426,0.03209374,6.874134e-05,0.034549132,0.011371142,-0.023944963,0.01087146,-0.012201615,-0.02291065,0.019902656,-0.011938273,-0.018972255,-0.032956798,-0.004786134,-0.010115912,0.027969344,-0.030098166,0.030398926,-0.019960217,-0.00061936694,-0.016388161,-0.0067191,0.0068488736,0.008735479,0.013858864,0.019484106,0.010824873,0.0049662073,0.008199437,-0.0020823807,-0.008545866,-0.00853621,0.0063904896,-0.014982622,-0.007419255,0.0007399113,-0.02370981,-0.00653257,-0.011696875,-0.011673055,0.00514297,-0.027025327,0.00038985492,-0.006850573,0.002380587,0.002183195,0.04575544,-0.023495635,-0.0094412435,-0.0026096741,-0.0043695555,-0.0061086724,0.011239597,-0.009865419,0.012080793,-0.0026092315,0.01241049,-0.019465799,0.0163579,-0.0052087926,0.004999305,0.0012320549,7.389087e-06,0.016794803,-0.0068064993,-0.09695777,-0.018048476,-0.013771705,0.029212115,0.017499678,-0.005167319,-0.0060220766,-0.020213654,-0.0072749127,0.024419295,0.0009749754,-0.032544676,0.0045689163,-0.02218982,-0.012444983,0.006240474,0.005865983,0.02812142,-0.021376653,-0.022439335,-0.0009945374,-0.017488116,0.016433645,0.0026661372,0.014409564,0.023100132,-0.010879725,0.028572846,0.0012834735,-0.0016583934,-0.017935261,-0.12069453,-0.010944093,-0.008830988,0.0114001995,0.0035426088,-0.008250961,-0.010920819,-0.010201728,0.0041390583,-0.01586791,-0.009477059,-0.01295904,-0.049335115,0.0022261662,-0.030879198,0.12224132,-0.027614312,-0.00043112482,0.009779448,-0.008976187,-0.0011137086,-0.022086341,-0.02735376,-0.0020129688,-0.033085916,-0.010525717,-0.0065230653,0.024882266,0.0109820375,-0.004583797,0.006671815,-0.0016271041,-0.03422025,0.0062683215,0.0005761701,-0.03155608,0.004104791,-0.0038057629,-0.009746207,-0.035382543,0.012201217,-0.009926057,0.018864026,-0.010081871,0.003361355,-0.008973067,-0.019700622,-0.0021773882,0.0033986876,-0.027205177,-0.010891519,-0.089574225,0.01855683,0.013149246,0.0028391967,0.0045221746,0.012643644,0.0029340864,-0.014567291,-0.00035410313,0.016874151,0.005314997,0.0038575486,0.021481909,-0.009614167,0.00890011,0.010434942,0.011334246,0.00058588333,0.0022097393,-0.019777253,0.016001955,0.0046789898,-0.010985007,0.014968096,-0.0017052547,-0.0022824884,0.002307088,0.0105000185,-0.017430503,0.013824397,0.02011511,0.017810041,-0.04240578,0.022977067,0.0020305705,0.007784477,-0.010100423,-0.008614589,-0.0073659415,0.008678767,0.04596138,-0.005808913,0.023228392,0.032902442,0.009319431,-0.011356989,0.052002735,0.014662343,-0.007202308,-0.015422976,-0.02247859,0.027563786,0.0102640875,-0.00454721,-0.01207967,-0.0065935636,0.0026065973,0.033047594,0.010897965]') ON CONFLICT DO NOTHING;

INSERT INTO public.vector_store VALUES ('c750017d-367a-408e-a2ac-1df8dc87fb7c', '# 🚀 GitHub Actions & Docker 기반 CI/CD 파이프라인 구축 가이드

> **한 줄 요약**: CI(지속적 통합)는 코드 변경 사항의 자동 빌드 및 테스트를, CD(지속적 배포)는 검증된 코드의 서버 자동 배포를 담당하여 소프트웨어 전달 속도와 안정성을 극대화합니다.

---

## 1. ⚙️ CI/CD 파이프라인 4단계 구성

```
[ Developer Push ] ➔ [ 1. CI 빌드 & 자동 테스트 ] ➔ [ 2. Docker 이미지 빌드 및 레지스트리 Push ]
                                                                      │
[ 4. Health Check 및 완료 ] ◄── [ 3. 운영 서버 SSH / K8s 자동 배포 ]
```

---

## 2. 📝 GitHub Actions 워크플로우 예시 (`.github/workflows/deploy.yml`)

```yaml
name: Workmate CI/CD Pipeline

on:
  push:
    branches: [ main ]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - name: 소스코드 체크아웃
        uses: actions/checkout@v4

      - name: JDK 17 세팅
        uses: actions/setup-java@v4
        with:
          java-version: ''17''
          distribution: ''temurin''

      - name: Gradle 빌드 및 테스트
        run: ./gradlew build --no-daemon

      - name: Docker Hub 로그인 및 이미지 빌드/Push
        uses: docker/build-push-action@v5
        with:
          push: true
          tags: ${{ secrets.DOCKERHUB_USERNAME }}/workmate-was:latest

      - name: 운영 서버 SSH 원격 배포
        uses: appleboy/ssh-action@v1.0.0
        with:
          host: ${{ secrets.SERVER_IP }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SSH_PRIVATE_KEY }}
          script: |
            docker pull ${{ secrets.DOCKERHUB_USERNAME }}/workmate-was:latest
            docker compose up -d db was web
```', '{"title": "[DevOps] GitHub Actions & Docker 기반 CI/CD 파이프라인 구축 가이드", "userSeq": 1, "guideSeq": 25, "isPublic": true, "chunk_index": 0, "total_chunks": 1, "parent_document_id": "7a2f5c9e-0188-485e-930d-c11781f205d3"}', '[-0.008041944,0.0068099205,0.020031635,-0.07140332,-0.0077157486,-0.007650409,0.0008535722,-0.015835473,-0.0071781185,0.013414912,0.0061131148,0.003747478,-0.005029216,0.013483022,0.14477737,0.01725846,-0.014862734,0.0121107,0.014065757,-0.020878173,0.009533441,0.012261084,-0.002155034,-0.019765006,-0.0113388775,-0.031684678,0.035401523,0.011551489,0.02548778,0.0055104736,-0.012319603,0.016488733,-0.0100196125,0.025250688,-0.011652449,0.008128964,-0.002426042,-0.01587462,-0.012899532,0.0064302897,0.005726739,0.0048602526,0.0012572182,0.0036611937,-0.001506197,0.015205551,0.008175862,-0.021722373,0.020562423,0.019799951,-0.0076145097,-0.012487152,-0.00617235,-0.17455846,-0.0024427115,-0.035077836,-0.0253485,-0.004223126,-0.015300607,-0.007363359,-0.019897496,0.019246245,-0.015525083,-0.022872936,-0.026276551,-0.026599212,0.03029079,0.0056002336,-0.012795737,-0.019131698,0.0043820487,0.0002037326,0.0038368292,-0.008196221,0.013265096,-0.013003991,0.0046918294,0.023922058,0.008018262,0.013130358,-0.026716968,-0.017598957,-0.025945326,-0.0006796263,0.013810868,-0.028809117,-0.00431331,-0.002247526,0.016847128,0.014973646,0.0372605,0.02013464,-0.011731724,0.008136552,-0.022910958,-1.8827063e-06,-0.012489138,-0.044561513,0.021060312,0.005585063,-0.017508078,-0.016021801,0.005198751,-0.0062718857,0.002547434,0.006656138,0.003385268,-0.02342332,0.002890666,-0.011932614,-0.010623889,-0.022614753,0.009301862,0.00027524537,0.010472127,-0.12581587,-0.015854375,-0.013037728,0.0102489805,-0.0031625002,-0.019181676,0.01069829,0.012682553,0.010464679,0.018591966,-0.019113472,0.019807514,0.011663989,0.025114851,-0.02923854,0.0131566115,-0.015818927,0.006904287,0.0036651604,-0.002254993,0.018113587,-0.029160343,-0.020843344,-0.008792744,-0.021488877,-0.009998123,0.03397846,-0.01938731,-0.0032694296,-0.037658267,-0.017264161,-0.03601753,0.014013926,-0.012815117,-0.024828868,0.019234624,-0.009042244,0.006083986,0.015347734,0.002157885,-0.03674734,0.006132645,-0.0053083403,0.02203668,0.0032359823,-0.022546325,0.020550188,0.021479068,-0.005970935,0.031595167,-0.0036553813,0.006481355,0.01989615,-0.0013939615,0.0080919,-0.012802106,0.021810487,0.012990956,0.0023517027,9.886546e-05,-0.012854745,-0.006393481,-0.0069370675,0.033681013,-0.0008117387,0.0001378498,0.007418404,-0.023636885,-0.024747137,0.002438395,-0.00073473476,-0.00465655,-0.0014381055,0.0041886326,0.0148635935,0.0035828326,0.0051549994,0.012064183,-0.029003695,0.025090275,-0.0585967,-0.017051175,-0.021405015,0.010122052,0.023219699,0.022256356,-0.003551348,0.007921675,-0.014256308,-0.016233834,0.037861925,0.005354552,-0.018000495,0.01235204,-0.0014005268,-0.001931914,0.016782017,0.03824804,-0.014627619,-0.008014389,-0.031929653,-0.013350069,-0.00081096165,-0.002851256,0.015336727,0.016692998,0.0044231787,-0.010748652,0.04534062,-0.0071948566,-0.01066346,0.00069059734,-0.015979603,0.0076404526,-0.0050185537,0.030196354,-0.0026935162,-0.008257213,0.014150609,0.03730163,-0.0037397149,0.0071285632,0.04134859,-0.0065380246,0.035896868,-0.011959882,0.040366888,-0.0065538613,-0.0074541,0.03264943,-0.005924067,-0.027522521,-0.011805937,-0.0056513557,0.0092496965,-0.021279529,-0.003985572,-0.009392938,-0.02807321,-0.0059369807,0.00040688695,0.0020999287,-0.025049914,-0.0083773555,0.0034144726,-0.00269973,0.002935003,-0.002713429,-0.027300978,0.012860273,-0.010310317,-0.015957888,0.0068327845,-0.015180838,-0.01206127,-0.0061492664,0.017167939,-0.006425526,-0.028411547,-0.015476001,0.014541377,-0.021063456,0.043429222,-0.010875482,-0.044609282,-0.0060888124,0.008715692,-0.007295347,-0.014432765,0.0058841067,0.008977072,-0.00394944,-0.025148947,0.024993537,-0.024724638,0.013279587,0.025949074,-0.014577191,-0.021945948,0.019572267,-0.0052535012,-0.010630576,0.007769527,0.00032943866,0.0047100894,0.001569336,-0.005815373,0.0078654345,0.046103094,0.001881464,-0.005291582,0.024662847,0.015049212,-0.0053181043,0.007578867,-0.0430832,0.0052541583,0.030209586,-0.0018366529,0.012178704,-0.00067827717,0.052960817,-0.031088324,0.00875168,0.003932035,0.0153077,-0.004653324,-0.0018636087,0.017098075,0.002621076,-0.0068296283,-0.008590428,0.021562107,0.0055085183,0.00045325927,0.0026086878,0.0066113756,0.015542708,-0.0034797024,-0.006117247,0.011727566,0.011804953,0.022784127,-0.020903153,0.016432418,0.02111296,0.011121017,-0.02613819,-0.01270171,-0.045483157,-0.017842695,0.015214921,-0.021908408,0.01973705,-0.03597324,-0.0064435187,-0.0103195375,0.008489371,-0.0050605917,-0.014172214,-0.0061162286,-0.018434102,-0.018315036,0.00057374046,0.026375178,-0.014810806,0.017148746,0.01860806,-0.027807446,0.028778074,0.0026309125,-0.0051990333,0.0051402575,-0.017726343,0.017167918,-0.008363405,0.017775718,-0.0061133895,-0.012554834,-0.011334422,0.030556576,-0.03960569,-0.004358042,0.0033687225,-0.0062729665,-0.0035752403,0.021417161,-0.008667458,0.019322662,-0.025453126,-0.008277339,0.019977741,0.0012722892,0.0010448621,0.0018483292,-0.008902138,-0.018232284,-0.007917865,-0.003933061,0.004802682,-0.009823936,-0.04383017,0.003897891,-0.039620053,-0.0069995984,-0.010599007,0.0060231257,-0.02689859,0.019847311,-0.021409707,-0.01159059,0.03042777,0.0028670472,-0.019510796,-0.015887363,-0.012353048,0.033417016,-0.01833313,0.00403079,-0.00030569173,-0.010398781,0.011199336,0.005523717,0.014780892,-0.023301952,0.0076197027,0.018695137,-0.011959902,-0.004021728,-0.011523032,-0.0037488246,-0.027796827,0.018989844,-0.029980276,-0.018198429,-0.0074626524,0.008988854,-0.015966708,-0.021072065,-0.017888902,-0.007040368,-0.03194621,-0.010232122,0.025630977,-0.014612262,0.00874016,0.0051767877,-0.0077396347,0.003208538,0.023359645,0.008518582,0.013329818,-0.014616351,-0.04974534,-0.0054839547,0.020590078,-0.00027421108,0.021830838,0.004326567,-0.0012627858,0.02654315,-0.018628506,-0.008912047,-0.0008638967,0.0049833683,0.008493596,0.004050382,-0.008216465,-0.04863144,-0.007363826,-0.011799732,0.011382413,0.0040612267,-0.031608403,0.012274432,0.0052472637,0.027055299,0.03130388,-0.0032043082,-0.01060292,-0.02338083,-0.0026662643,0.006155854,-0.012576318,0.0054340977,-0.006039376,-0.0017947869,-0.017419046,-0.0025319294,0.017164975,-0.004756979,-0.019194378,-0.025164094,-0.009247873,0.014447608,0.030211901,0.02520848,-0.017526273,0.012594682,-0.04526248,0.0118093705,0.023390595,0.022202648,0.004546266,-0.011431322,-0.0011839767,0.009278594,0.00494377,0.0053448225,7.569315e-05,0.004206475,0.010371034,-0.025617385,0.018824339,0.0010202673,-0.0017253723,-0.013474067,-0.0074729966,-0.0028159227,0.00059461023,0.0012693453,0.00017873432,0.022285527,0.006798299,0.0031591598,-0.0013917802,0.010278998,-0.0032330817,0.011470501,-0.038844433,0.0028512224,0.0025656885,-0.013926583,0.022154955,-0.0019300091,-0.002015145,0.0020310734,0.0033162527,-5.8292637e-05,0.01589351,0.0040549864,-0.012865191,-0.018799836,-0.0053083347,-0.015644748,0.0071533625,-0.007486897,0.0078673465,0.020332344,-0.012902515,-0.0648671,-0.033857387,0.004947729,0.026702832,-0.01773937,-0.0038053398,-0.021977685,-0.00766527,0.00028813485,0.03007075,-0.008063031,-0.0040358594,0.035441674,0.012725539,-0.020644104,-0.003678906,0.007320435,-0.018927108,0.018234223,-0.024394102,0.00663068,-0.017097674,0.0021787097,-0.0058131027,-0.009214925,-0.00980114,0.017307766,0.004971472,-0.011094449,-0.018005818,0.013550244,0.0010625599,0.0062390636,-0.004670292,-0.0064168796,-0.00702057,0.028031882,-0.021030808,0.01872765,-8.915077e-05,-0.019414173,0.014136918,0.0041869855,0.0045985,0.010713895,0.018111348,-0.015895763,-0.025269315,-0.004546494,0.015029708,-0.055045784,-0.013324449,-0.01612107,-0.034542877,0.0022300715,0.019279921,-0.023633845,0.013348116,-0.024221877,-0.0022862921,-0.017279405,-3.903393e-06,-0.0071881562,0.033100527,-0.023966553,-0.005219874,0.019343138,0.005555812,0.008349666,0.006833502,-0.009226151,-0.026880423,-0.0038405121,-0.0015095351,-0.0142061245,0.015197601,0.01009555,0.03502607,-0.028312558,0.011160585,0.005907148,-0.038131718,-0.0957839,0.017242106,0.0018764903,0.00984403,0.01489113,-0.016802225,0.02405716,0.01001383,0.010675095,-0.008454694,-0.0015556792,-0.038030513,0.0147796795,-0.013334966,0.017334953,-0.0006746479,6.22101e-05,0.017816974,-0.010535063,-0.010899285,-0.002205089,-0.018755225,-6.144213e-06,0.009820107,0.0052285586,0.018680917,-0.015211388,0.0063963057,0.007903844,-0.0052333674,0.0074308803,-0.12713367,0.00772827,-0.0047046184,-0.0019235399,-0.02460698,0.017601801,-0.010359913,-0.014819716,-0.012980685,-0.0007686117,0.0124247065,-0.029501919,-0.029745799,-0.017993338,-0.011798679,0.09338389,-0.010968473,0.0058802036,-0.00029100874,0.0059966156,0.0013168459,-0.02866517,-0.008612646,0.01232618,-0.0073462757,-0.0177538,0.001656927,-0.012763291,0.026201107,0.0016544777,0.029522315,-0.007245741,-0.013233675,-0.020447217,-0.00024122742,-0.006370481,-0.008090248,0.006804146,0.007967062,0.021259932,-0.018158766,0.018119851,0.019718729,-0.046368774,0.014264502,-0.013498082,-0.012554199,-0.007260526,-0.011755278,-0.05106837,-0.016765574,-0.06360679,0.013231693,-0.008282139,0.009707181,-0.0010370883,0.013913064,-0.009939175,0.017770994,-0.0025473936,0.034257762,0.011267483,0.0053993375,0.010139059,-0.014119707,0.002492236,0.04917029,0.012553966,0.0032295263,-0.020456363,-0.0031162628,0.00044992822,-0.0015149286,0.0017152452,-0.007654135,0.023176782,-0.003995102,-0.0013543203,-0.0011855363,-0.011921426,0.015236981,0.019423207,0.013357609,-0.014105098,0.0045532817,0.012590532,0.02455789,-0.0101567395,-0.026486153,0.013731941,-0.0023498281,0.026041416,-0.007543286,0.013756362,0.0342701,-0.022936486,-0.030262724,0.039640825,0.002643696,0.007872327,-0.011017889,-0.053698245,0.012218902,-0.010484313,0.0030568622,-0.009024485,-0.014287606,0.02861345,0.035042297,0.014442143]') ON CONFLICT DO NOTHING;

INSERT INTO public.vector_store VALUES ('dbf84d04-25d7-48b9-9e87-a784da2b1b71', '# 🐧 실무 필수 리눅스(Linux) 시스템 & 서버 관리 명령어 가이드

> **한 줄 요약**: 서버 운영, 로그 분석, 프로세스 모니터링 및 네트워크 상태를 파악하기 위한 엔지니어 필수 리눅스 CLI 명령어 집합입니다.

---

## 1. 🔍 파일 검색 및 텍스트 파싱 (find, grep, awk)

```bash
# 특정 디렉토리에서 .log 파일 중 최근 7일 이내 변경된 파일 검색
find /var/log/ -name "*.log" -mtime -7

# 로그 파일에서 ERROR 문구 추출 및 특정 키워드 카운트
grep -rn "ERROR" /var/log/was/ | wc -l

# p6spy 쿼리 실행 로그 중 수행시간(took)이 100ms 이상인 라인 추출 (awk)
awk ''$6 > 100 {print $0}'' /var/log/was/p6spy.log
```

---

## 2. 📊 프로세스 및 포트 점검 (ps, netstat, lsof, top)

```bash
# 8081 포트를 사용 중인 프로세스 PID 확인
lsof -i :8081
netstat -tulpn | grep 8081

# 자바(WAS) 프로세스 메모리/CPU 점유율 모니터링
ps aux | grep java
top -p $(pgrep -d, java)
```

---

## 3. ⚙️ systemctl 데몬 서비스 및 systemd 로그 관리 (journalctl)

```bash
# Docker 서비스 상태 확인 및 재시작
sudo systemctl status docker
sudo systemctl restart docker

# 특정 서비스의 실시간 로그 감시 (journalctl)
sudo journalctl -u docker -f -n 100
```

---

## 4. 🔒 권한 관리 (chmod, chown)

```bash
# 특정 디렉토리 및 하위 파일 소유권 변경
sudo chown -R ubuntu:ubuntu /app/workmate/

# 실행 권한 부여 (755)
chmod +x ./gradlew', '{"title": "[Linux] 실무 필수 리눅스(Linux) 시스템 & 서버 관리 명령어 가이드", "userSeq": 1, "guideSeq": 45, "isPublic": true, "chunk_index": 0, "total_chunks": 1, "parent_document_id": "de67201c-8450-4de0-bbe7-32ac45376a96"}', '[-0.022291217,0.01549418,0.010519304,-0.05499391,0.0016010578,0.022571895,-0.02326388,-0.030658776,-0.005306254,0.02245768,0.0037859383,-0.017612588,-0.0075118695,-0.0009447249,0.12193117,0.00044501614,0.00026388033,0.00024558883,0.022306064,-0.028606106,0.0083459895,0.0021907222,-0.0014709454,0.00263946,-0.0040339753,0.0030630007,0.018398194,-0.023797117,0.05541511,0.0035583903,-0.008198066,0.028180322,-0.00211451,0.015467421,-0.020063428,0.014302524,0.028818164,-0.020519076,-0.019871756,0.0015920948,0.0038943638,-0.008706199,0.004223902,-0.011501808,0.00846138,0.015001913,0.0010664869,-0.020257037,-0.0029039187,0.022468839,-0.0056018406,0.0016149778,-0.009417154,-0.1901999,0.016673995,-0.014320874,-0.020673288,-0.0026744343,0.004455847,-0.010388034,-0.036343656,0.045092944,-0.009122819,-0.013305226,-0.019547934,-0.014949156,0.03162715,0.00203921,-0.010600589,-0.017992586,-0.0027584133,0.0151981255,-0.030311482,-0.0074444134,0.005145029,-0.010351153,-0.024899561,0.022904214,0.00460493,0.0005343772,-0.0009378933,-0.020419553,-0.013748947,-0.018937796,0.015769241,-0.0014386631,-0.012277474,-0.002507819,-0.0071556387,0.037198182,0.010189019,-0.015336898,-0.013432084,0.016912222,-0.027311007,0.03191219,-0.009644751,-0.025703423,0.019784946,-0.009123589,-0.011043001,-0.0006616542,0.015253469,0.02471022,0.0076544117,0.017103525,0.026162853,-0.019692399,-0.016656619,-0.00094892527,0.013041478,-0.014821881,0.004388435,-0.0007586451,0.005965779,-0.15225436,-0.019472077,-0.0020497472,0.03177441,-0.02584565,-0.015987137,0.0022465768,-0.010537166,0.009075859,-0.0054113623,-0.02773948,0.007481134,0.005154044,0.01287841,-0.005483006,-0.0035766938,-0.028149126,-0.010419227,0.009239225,0.0017385924,0.043977834,-0.014519671,-0.012159079,-0.0069061564,-0.03664474,-0.0030835005,-0.007621548,0.00017687626,-0.0057040355,-0.03710055,0.00030904816,-0.015254195,-0.00084026426,0.008081876,-0.027518839,0.008246329,-0.03869498,-0.0077210213,0.0120517,0.00013892993,-0.040008746,0.026343005,0.007129741,-0.0028774517,-0.0045526903,-0.018531488,0.024321988,0.0075792195,0.029822625,0.010485665,-0.0031746987,-0.0176123,0.0153463725,-0.013492784,0.024117552,-0.021037374,0.016329361,0.018489044,-0.047448657,-0.016054321,-0.019942293,0.001576908,0.0026555234,-0.012146906,0.009782534,0.019127885,0.007310478,-0.008180145,-0.02571343,-0.015606253,-0.019801453,-0.002630048,-0.01254174,0.036693178,0.010570517,-0.0064072507,0.0010530971,0.00914599,-0.0101154,0.010079617,-0.033442084,-0.008013277,-0.022154393,-0.00027649823,0.015111077,0.038171463,0.004091649,0.009704998,-0.007822761,0.0054654125,0.017102633,-0.0048320196,-0.036341142,0.006959362,0.013464493,-0.0019662385,0.018220903,0.014682125,-0.019838268,-0.0019696732,-0.04821427,0.0015951953,0.020838134,-0.005588053,0.02277051,0.0031935812,0.015462311,-0.0004622411,0.028678646,0.0030592147,-0.024672946,0.0022972063,-0.009760836,-0.014785555,-0.0070590777,0.038422257,-0.016143013,-0.0071786195,0.016077671,0.033678263,-0.008884638,-0.014305004,0.030702356,-0.0037884314,0.028419798,-0.033454612,0.0021144634,-0.019058475,0.016900955,0.023121338,-0.00698984,-0.028393488,-0.0067942343,-0.018244868,0.005430632,0.032792997,-0.005837656,-0.0063281823,-0.012353912,-0.005572335,-0.0006328974,-0.0022927807,-0.012061837,0.013100521,-0.0040994706,0.01404521,-0.0005078414,-0.004811592,-0.015737174,0.022334144,0.020696536,0.019353583,0.019325532,-0.00680296,0.027288139,0.008903707,0.008002095,-0.0196791,-0.029362949,6.595372e-05,0.0011414094,-0.04112126,0.00096956943,0.014503545,-0.032601293,0.021605054,0.0025632218,0.004827867,-0.007202146,-0.008378478,-0.0023834922,0.001443124,0.013082508,0.01332277,-0.0012033513,0.014248764,0.018004538,-0.010145491,-0.019351384,-0.016844608,-0.008630928,-0.0060330136,-0.02580695,0.0068279267,-0.022644017,-0.014452009,-0.009806201,0.007185143,0.052902788,0.024890078,0.03139388,0.0044064578,0.019629814,0.007879403,-0.011103816,-0.0029039525,-0.013083315,0.032181628,0.013253984,-0.0039197253,-0.0213864,0.036562268,0.0009955565,-0.020022038,0.0015440927,-0.016146902,0.0020094696,0.0009462266,-0.009805667,-0.014286968,0.011572044,-0.0006190705,0.038398616,0.02451845,-0.009576162,-0.0032543838,0.008857909,0.018643461,-0.0027212882,-0.007585635,0.01207534,-0.008459146,-0.011776284,0.0072651743,0.018364122,-0.023328194,-0.0037386876,-0.011224463,0.024737066,-0.023802655,0.013250606,0.0074672997,-0.0005183319,0.0015360884,-0.03801048,0.017532293,0.0018591175,-0.0056804596,0.01083474,-0.007169451,-0.0016667864,0.010552808,0.00790861,-0.00062385685,0.017823994,0.00043654855,0.014684783,-0.007595194,0.016086278,0.011496652,-0.0141618075,-0.012462615,0.017439634,-0.009672852,0.01408729,0.0028703823,0.016426682,0.009296945,-0.024082728,-0.03061572,0.03353984,-0.029106272,0.0049489806,0.015866008,-0.002960617,0.006937111,0.019682894,0.0049210875,0.015831854,-0.01903234,-0.012884354,0.0027801613,0.03135223,-0.011844165,0.00955689,-0.02536398,0.02427634,0.0017619858,0.028874498,-0.012791869,0.013275122,-0.053713195,0.019113354,-0.008430135,0.009396152,-0.0034335018,0.0025275755,-0.023239184,-0.0044570128,-0.001512669,-0.001033672,-0.00762984,0.008979079,-0.01476831,-0.02106694,-0.026042761,0.023384092,-0.025719624,0.01557001,0.0075159287,-0.000785322,0.024300512,0.008859863,0.029018639,-0.022561293,0.012724718,0.03015035,-0.004051763,-0.0022349847,0.01693423,0.01705013,-0.015732678,-0.005213452,-0.01744436,-0.020745268,-0.015360622,0.015918085,-0.009934789,-0.025404546,-0.003792118,0.0036214152,-0.020360969,-0.0013805648,0.008817321,0.0060050352,0.036991224,0.017282953,0.0029513158,0.012923862,0.018026637,-0.019202482,0.01149299,0.0074056303,-0.03979532,-0.0091462135,0.0036577093,-0.014534402,0.018211966,-0.00837613,-0.0054631825,0.026670968,-0.01184895,0.015792606,0.0010700952,0.0011918667,-0.005351987,-0.010909726,-0.0047479635,-0.037707906,-0.017449358,-0.022573613,-0.00691076,-0.022243513,-0.00964012,0.009289626,-0.042505413,0.010587005,0.0121236425,-0.013799396,0.0018237301,0.005070087,-0.0034246524,-0.01263397,-0.031274628,0.01798988,0.007583708,0.01422728,0.00010068903,-0.011443733,0.0037542963,0.018128978,-0.018553395,-0.015229543,0.0002379587,0.015235028,-0.0027900992,-0.007118831,0.016522372,0.027779628,-0.035102144,-0.0061230566,-0.020133229,0.013525675,0.006623368,0.015389859,-0.022160495,0.0096365195,-0.01042355,-0.0071795625,-0.0026430239,-0.015617116,0.022426907,-0.0067848456,0.026896892,-0.019006679,-0.03415952,-0.011576914,0.008030895,0.0041471473,-0.0046861153,-0.012609848,-0.012313036,0.01781284,-0.00741826,-0.026207905,0.016677758,-0.029538035,-0.02070407,0.017741969,-0.015740644,0.007801051,0.016739754,0.0071113785,0.001015793,0.009612687,0.013088432,0.0009474782,-0.015063627,0.004830542,0.008991509,-0.012181309,-0.004745378,0.00429482,-0.015789809,0.020532541,0.028674617,-0.035109434,0.020867763,0.025019431,0.0014379722,-0.07726764,-0.032780807,0.015442676,0.007792505,-0.02823499,-0.010453907,-0.004977646,-0.013591679,-0.016074043,0.020614851,-0.012764597,0.009109506,0.033589475,-0.0011121371,-0.036265157,-0.014574645,-0.005515896,-0.015161129,0.015008072,-0.023732863,0.013001724,-0.022198953,0.00028713123,0.010784338,-0.019986587,-0.009243934,0.01388805,0.015642013,-0.0060820463,0.006612213,-0.007984575,-0.0067766462,0.017032664,-0.009139576,0.013785042,-0.0044887303,0.014315408,-0.012173024,0.0047718463,0.0061728507,-0.02200606,-0.0041943965,0.029999677,-0.014581502,0.023963647,0.024419291,-0.007908264,-0.0039277337,-0.010061194,0.00026187932,-0.05400852,-0.008365939,0.0030338538,-0.0033335318,0.009946053,0.0011940275,-0.0015840916,-0.0041282037,-0.028901223,0.0058024344,-0.013455385,0.017008368,-0.0043512695,0.020653443,-0.025551826,-0.012826489,0.008652207,0.015363675,0.01691079,0.00022419721,-0.02377258,0.005481604,0.010793944,0.00706711,-0.044752665,0.017640334,-0.005789115,0.01995138,0.00092360395,0.013357775,0.0046247276,-0.018414645,-0.08035696,-0.0026115975,-0.00871105,-0.007514655,0.0016247048,-0.03123744,-0.008963045,0.00091422943,-0.000591298,0.011682039,-0.002843382,-0.0015819729,0.00078914437,-0.036404364,0.018531729,-0.028557612,-0.00010877351,0.011592992,-0.030147143,-0.011743257,-0.00025946315,-0.011132135,0.001551346,-0.0059960727,-0.008868293,0.019014182,-0.004502872,0.011142558,-0.00404797,-0.014716917,0.006529515,-0.11215598,-0.005125499,-0.0051980205,0.0027989869,0.009987872,-0.0056276745,-0.0035206226,-0.033442922,-0.007124508,-0.010466555,0.041257866,-0.048012603,-0.040106002,-0.006406773,-0.004686526,0.0891754,0.015274459,0.025536472,0.02552954,0.014447059,0.00018022265,-0.026792103,-0.008287405,0.039516732,0.0035412775,-0.026150778,0.008727632,-0.0056605684,-0.004134263,0.028043512,0.013638834,0.017908484,-0.015923755,0.0054404107,-0.0056479517,-0.0105858045,-0.016818717,0.02017518,-0.0019455864,0.023703301,-0.021911947,0.026267966,0.0018225692,-0.023890315,-0.002297781,-0.012681691,-0.0035374688,-0.0020784982,0.012346843,-0.033123873,-0.027160455,-0.05226265,0.008363878,0.014300561,0.0142547935,0.033239707,-0.020025292,-0.011525236,-0.0037560386,-0.0021264933,0.035431296,-0.013398027,-0.006804195,0.0140244085,0.00895944,0.014854693,0.015350118,0.024045968,0.008864833,-0.0043796743,-0.006297426,-0.009082498,-0.013931352,0.01182622,-0.0031499718,-0.011937466,0.020694993,0.016174778,0.0021358358,-0.0065796534,-0.009820133,0.015554158,0.019434832,-0.031616386,0.009528507,-0.0016487541,0.005143124,-0.022192642,-0.010241818,0.01274821,0.0068502673,0.044282567,-0.009904631,-0.013422371,0.040349625,-0.014579484,-0.013799105,0.039098214,0.007003846,0.0121626565,-0.022099819,-0.037279464,0.017928343,-0.026699437,0.0012945674,-0.01765233,0.008069466,0.04142294,0.055883538,-0.014042074]') ON CONFLICT DO NOTHING;

INSERT INTO public.vector_store VALUES ('e17121c5-c3f1-44c7-aa1d-f3e87e1f6660', '# ⚡ PostgreSQL pgvector 기반 벡터 데이터베이스 구축 및 HNSW 인덱스 가이드

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
```', '{"title": "PostgreSQL pgvector 기반 벡터 데이터베이스 구축 및 HNSW 인덱스 가이드", "userSeq": 1, "guideSeq": 36, "isPublic": true, "chunk_index": 0, "total_chunks": 1, "parent_document_id": "ec210e55-b859-4023-871e-ba6c358d7b19"}', '[-0.01950427,-0.016268175,0.008888393,-0.06141094,-0.010283439,0.010412692,0.004776096,0.017354025,0.0008172159,-0.009400613,-0.01848667,-0.00040499956,0.02095321,-0.0068878923,0.13120809,-0.005720178,0.01984723,-0.0062874146,-0.014078585,-0.036563043,-0.010199337,0.02028826,-9.499834e-05,0.007847575,-0.008661878,-0.019424228,0.027851854,-0.014037469,0.026006702,0.012085453,-0.012376736,0.013820363,0.036199395,0.021208618,0.021389082,0.026246134,0.004992087,-0.030248184,0.000640964,0.017269464,0.013702445,0.0065002767,-0.03183726,-0.0076084696,0.0014498979,-0.011445926,0.01395751,-0.022261865,0.0011198653,0.030907031,0.013506959,-0.005938088,0.006877013,-0.15867466,0.010800656,0.01514694,-0.028583586,0.009447202,-0.001976534,-0.021518048,-0.003445693,0.030456647,-0.019527925,-0.005365956,0.0071907495,-0.008399198,0.020659469,-0.007147471,0.0067036906,-0.030494697,-0.027599536,-0.002786121,-2.0453397e-05,-0.028677069,-0.008243573,-0.02648393,0.04509669,-0.027710749,0.031149523,0.020056207,-0.009682084,-0.010803564,-0.032634888,-0.008884836,0.022309596,-0.0030634825,0.0053425683,0.0021528772,0.008901813,0.012898689,0.0077669537,0.01818953,-0.006222834,-0.020572254,0.001355096,0.0071714534,-0.004375824,-0.0037463934,-0.0021292525,-0.008609213,0.008257684,0.012577872,0.0054385653,0.0078191785,-0.001777174,0.040437873,0.017343111,-0.0094402265,-0.015384289,0.00024155807,0.0047520585,-0.0201463,0.022088256,-0.0003768213,0.015323602,-0.14300865,-0.007865021,-0.001879756,0.01566859,0.0037284642,-0.028241312,-0.008170047,0.031807512,0.019828558,0.011608597,-0.025530184,-0.0053471723,0.0024518764,0.008387063,0.010764745,0.014289942,0.0046805567,0.0065213647,0.0071205855,-0.0050850837,0.0017232555,-0.028302275,-0.0019117146,-0.0142276175,-0.0310901,-0.006649659,0.015163092,-0.014717445,-0.008222702,-0.010278992,-0.023115292,-0.0246043,0.046303753,0.0075189173,-0.020179493,0.007560898,-0.011999661,0.0020980926,0.018943792,-0.0066411733,-0.023377975,0.0069657927,0.009761793,-0.006744936,0.009081284,-0.029655,0.01783809,-0.00284833,0.0105591975,0.020120973,0.0031140447,0.011360525,-0.022709426,0.008225454,0.015895702,-0.005879053,0.018456476,-0.011097074,-0.020707667,-0.014972546,-0.010809636,0.020503903,0.009487583,-0.045564737,0.0023689957,0.032192044,0.010355619,-0.012504455,-0.008303383,0.0022261392,0.0124831125,0.005806709,0.006286198,0.02692419,0.006217378,0.006737008,0.016456487,0.022163538,-0.034798823,0.00090896053,-0.02030214,-0.02443139,-0.026787098,0.0007513498,0.014269767,0.005449703,0.0016655845,0.0030229152,-0.013991482,0.011879109,0.0050406135,-0.012322884,-0.035163503,0.009994786,-0.001581579,-0.015259352,0.022733545,-0.001923639,-0.024343371,-0.0020371545,-0.034250446,-0.0010393439,-0.00084164715,-0.001261368,0.00082430115,0.0041627903,-0.00977145,0.023919798,0.046780683,0.007953302,-0.035214882,-0.026171632,-0.014914316,0.0031589086,-0.010786721,-0.0008694926,-0.006277443,0.0024077767,0.013350816,0.019984765,-0.027846044,0.015102293,0.010522387,-0.025533425,0.021786442,-0.02956966,0.01596182,-0.0070353644,0.048441518,0.02105676,0.002159421,-0.0037768052,0.01971463,0.0071841,0.0031538955,-0.003952683,-0.0017848305,0.0060996497,-0.012484281,-0.024881264,-0.010712389,-0.033857755,-0.02276752,-0.027174966,0.02219876,-0.019889852,-0.00916085,-0.026462499,0.012423691,-0.017775942,-0.0037620023,0.01565395,0.004464901,-0.01294398,0.0027878368,0.016325075,0.029197933,-0.010851169,-0.02850158,0.0041906177,0.018584589,-0.023773482,0.021335753,-0.011804208,-0.017036574,0.020254347,0.010092176,0.028073335,0.0096606305,-0.0064898953,0.021165377,-0.0024378551,-0.012472683,0.009943827,-0.014794156,0.01828879,0.024949888,-0.010373068,-0.0016602385,0.011704303,-0.0070202947,-0.020907784,-0.0050784554,0.021742383,-0.018255845,0.0005824395,0.004765925,0.004193285,0.043836217,0.0031129157,0.016837649,0.011701562,0.012693111,0.0049205422,0.0037316766,-0.0045454865,-0.0034014452,0.027555449,-0.0042962823,0.0031263614,-0.0019638257,0.020542052,-0.0014905046,-0.015675928,-0.008267392,-0.015450601,-0.028070828,-2.8115925e-05,-0.0038405396,-0.006290438,-0.018562092,-0.013426868,0.028179785,0.015651315,-0.0054944986,-0.0005771485,-0.0033874747,0.008308272,-0.0022407128,0.009354116,0.015272997,-0.018601958,0.0058777747,-0.021997875,0.0412111,-0.009978059,-0.011403614,-0.020830179,-0.017257182,-0.0254508,0.021241466,0.026371079,-0.0047768424,-0.026318986,-0.014129154,-0.022878477,-0.008008568,-0.009143078,-0.004560959,-0.008042141,0.004474682,-0.03022088,-0.0077827047,0.018375931,-0.0037943008,0.008186621,-0.016368277,-0.0047957557,0.0011469058,0.028870502,-0.01610173,-0.008098555,0.028123433,-0.035577603,-0.0007466271,-0.01758766,0.016643396,0.010315886,-0.0049696835,-0.019798491,0.008026622,-0.009171236,-0.0043335063,0.0016532965,-0.0009301715,0.0129844295,0.018428491,-0.012437286,0.0031331382,-0.011003522,0.014324503,0.026983565,0.017045787,0.013903769,-0.0008675818,-0.003978093,0.0086477455,0.019412564,0.015402211,-0.008019733,-0.0133703165,-0.032156236,0.0008923317,-0.012754634,-0.002618914,-0.012504393,-0.019912282,-0.0182518,-0.008341564,0.01730813,0.001261214,0.004621866,0.011975569,-0.025629297,-0.024160683,-0.005404622,-0.011038136,0.010621004,-0.015420616,0.018522533,0.018439576,0.028349364,0.0027430095,0.037924856,-0.02614031,0.005430226,0.046438165,-0.023209514,-0.018873049,0.0016626812,-0.0066339727,-0.030896053,-0.030372923,0.010689311,-0.02998674,0.0032524506,-0.012412021,-0.024601776,-0.017282844,-0.036278076,0.016163638,-0.01243067,0.0030464758,0.0050683534,0.0077262395,0.004099466,0.019092979,-0.00074617926,0.005968051,0.012530083,-0.02283354,0.016284227,0.007093218,-0.025752181,0.010390347,0.007513522,-0.009289305,0.012940147,-0.022294844,-0.00396265,7.556833e-05,-0.005028581,-0.01583749,-0.012186964,-0.0379774,-0.029241275,-0.009479425,0.0026344224,0.032864593,-0.014663089,-0.03069541,0.008633383,0.007688292,-0.01762715,0.019746147,0.00056464254,0.038638707,-0.014293981,-0.0031407932,0.017998192,-0.011488605,-0.007694288,-0.00073416875,0.02981428,0.008663726,-0.023299562,-0.015139024,-0.018953653,0.0006671598,0.019820625,0.016411591,0.004458724,-0.0021788115,-0.008339522,0.0022364804,0.020146288,0.017643029,-0.004564678,0.007755631,-0.015984625,-0.021542473,0.002464288,-0.016872292,0.022144236,0.000703819,-0.013509273,0.006606694,0.0040256497,-0.020003999,0.0021116133,-0.010369547,0.017182114,-0.03691308,0.03431264,-0.0053841644,-0.019582782,-0.021468764,-0.0034884512,-0.027481837,-0.0010941696,0.018640662,-0.0043329108,0.0035334846,0.01961272,-0.0059669632,-0.0069920467,0.0059434124,-0.021599494,0.02144217,-0.021349037,0.016369471,0.015863862,-0.010209248,0.01342254,-0.011642914,0.009286445,0.0026201275,-0.013774239,-0.007871166,-0.0038995848,0.009423961,0.011829315,0.0073437737,-0.016541826,0.011891048,0.006285873,-0.012675882,-0.008910544,0.016913295,0.00850078,-0.09406276,-0.0101617975,-0.0031206806,0.017642258,-0.033338178,-0.026984502,-0.024225095,-0.02745246,-0.008497022,0.027136391,0.010558755,0.0039370875,0.002011159,0.0015539849,-0.0028821365,-0.008618845,-0.034372006,-0.021470485,0.033813022,-0.021880178,-0.00016429753,-0.018510574,0.008584079,0.021825293,-0.019128824,0.013535919,-0.0040278947,-0.0047167414,0.012414376,0.006602434,-0.006703816,-0.029770864,0.016989872,0.015637193,0.011170779,-0.015833385,0.005843019,-0.048108723,0.017144488,-0.008219845,-0.0040934393,0.008763991,-0.01338646,-0.0062717698,0.011982505,0.013758799,-0.007828337,-0.02332679,0.018595569,0.028585693,-0.04439539,-0.008983247,-0.0019065376,-0.025322687,0.005544417,0.0007474408,-0.038305447,0.017101327,0.004749785,0.03837094,-0.0046522757,-0.0066870837,0.0058456175,0.019961175,-0.008371722,-0.009102215,0.0029935127,0.011269164,-0.008539825,0.024112362,-0.00657413,-0.020786602,-0.007371247,0.00875465,-0.008657972,-0.009871695,0.0063922093,-0.0015135547,-0.004044843,-0.007186227,-0.01320896,-0.009015807,-0.09838527,-0.003276395,0.010575988,-0.020157749,0.032320216,0.018815558,-0.013045197,-0.014253369,0.004860569,-0.024677742,0.017429164,-0.043269966,-0.009769662,-0.023234364,0.004136292,0.0009099898,0.0027530917,-0.0044412226,-0.020625396,0.0027681557,-0.010885193,-0.00075802667,0.0034847222,-0.007228738,-0.014748301,0.030807685,0.033730783,-0.0030382518,-0.015238296,0.009289386,0.0062864237,-0.12225055,-0.042763587,0.02356107,-0.00081229664,0.007968983,0.025797777,-0.013170443,0.0033448085,-0.020491429,0.01415165,0.008869145,-0.030769762,-0.03288907,-0.008803743,-0.009146713,0.10312314,0.003134846,0.005935272,-0.00664069,0.0029907348,0.0072104665,-0.0034519297,-0.005452439,0.010182952,-0.019199494,-0.017077262,-0.0025973099,-0.0013890701,-0.0015031555,0.019913249,0.030191684,0.0421052,-0.020396814,-0.002525764,0.010813275,-0.013274475,-0.006831231,-0.010086347,0.03310058,0.03977294,-0.006519347,0.024330478,0.01365095,-0.0226608,-0.007862239,-0.014076965,-0.0426235,0.012284024,-0.015562487,-0.020028103,-0.020795729,-0.09930429,0.004335852,0.0037197638,-0.0014114828,0.0045518545,0.021073433,0.011548521,0.011803047,0.009546167,0.027981961,-0.020387487,0.016622845,0.026208553,-0.01885693,-0.0054813577,0.0333958,0.017644033,0.020267347,-0.008253478,-0.0036257228,0.025223514,-0.02498085,-0.020319961,-0.03659925,-0.015093533,-0.001040806,0.006139368,0.015455444,0.0049872827,-0.0029604968,0.0024714323,0.00862389,-0.032735337,-0.009194641,-0.0010313973,0.001805198,0.00013729252,0.006270812,-0.0017685249,0.029616399,0.01890185,0.0049869753,0.015430287,-0.027700737,-0.009270006,0.0020009766,0.016696418,0.010777364,0.0076205935,-0.014451337,-0.018273283,0.010849056,-0.036792185,0.0033249431,1.5314905e-05,0.006946201,0.04562796,0.010135709,0.017639738]') ON CONFLICT DO NOTHING;

INSERT INTO public.vector_store VALUES ('e215557c-6ff8-4bb7-8106-9a92d8624d1f', '# 📚 RAG (Retrieval-Augmented Generation) 시스템 아키텍처 및 파이프라인 가이드

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

- **유사도 임계값(Threshold) 설정**: 유사도가 0.7 미만인 저품질 청크는 검색 결과에서 제외하여 환각 방지.', '{"title": "RAG (Retrieval-Augmented Generation) 시스템 아키텍처 및 파이프라인 가이드", "userSeq": 1, "guideSeq": 32, "isPublic": true, "chunk_index": 0, "total_chunks": 2, "parent_document_id": "2a31ac64-3556-430f-ac2b-e8ab409c29c2"}', '[-0.013495721,-0.0017896611,0.010121628,-0.07057341,-0.015025866,0.010661274,-0.007868692,0.0052801045,-0.022607407,0.020847697,-0.00337707,-0.002994397,0.01199349,-0.020077672,0.14533953,0.009955409,0.0074657802,0.00993063,0.016108284,-0.042856332,-0.0071156533,0.015109298,-0.019923631,-0.00023023479,0.01156414,-0.008636228,0.017710367,-0.0046460335,0.03669586,-0.00421491,0.0007783726,0.007882314,0.03408247,0.024765484,-0.0057149017,0.019117616,-0.0042216587,-0.032467753,0.004452649,0.027868737,0.0057845213,-0.022013705,0.0043060826,-0.011878487,-0.003419512,-0.010457754,-0.0063390066,-0.024526492,0.009512102,0.025048533,0.007130503,0.011816444,0.0079561,-0.17178446,0.006131467,-0.0082999775,-0.005040948,0.014337604,0.013552881,-0.0056557055,-0.02650399,0.00070771814,-0.028837593,-0.019726409,-0.014021895,0.0009343325,0.022256646,-0.009229082,-0.016184883,-0.030456517,0.009752216,-0.015248485,-0.015866874,-0.019195352,-0.009758973,-0.032100335,0.03970562,-0.0045622517,0.012266675,0.004962431,0.0034620997,-0.014194986,-0.028281685,-0.00090856425,0.0012536751,-0.009281506,0.025306389,0.0046432335,0.012961071,0.021620125,0.015047692,0.007983344,0.01718942,0.003463688,0.0012163081,0.0050184308,-0.0061538285,-0.012240043,-0.0072506256,0.00306967,-0.014105023,-0.011078086,0.0063630315,0.020559998,0.00793265,0.0424028,0.011227901,0.002466605,0.0054786038,0.018698387,-0.00959767,-0.014941913,0.015739698,-0.01518312,0.011941602,-0.13539553,-0.00252097,-0.0035335354,0.022665882,-0.010609824,-0.01946245,0.009112695,0.037908267,0.015325362,0.021351973,-0.012745504,0.004661425,-0.017660646,0.000609707,0.0126860095,-0.0050653554,0.0076320567,-0.0066228732,0.00460457,-0.017888451,0.025637604,-0.031156734,-0.007484558,-0.027984094,-0.024966242,0.00047636588,0.03495337,0.004225944,-0.028264731,-0.0034739047,-0.00739607,-0.03077902,0.017838482,0.0004944135,-0.031742718,0.023650542,0.015450422,0.0014048391,0.02289456,0.002789738,-0.025794573,0.00032526863,0.015599884,-0.00017609146,0.007491713,-0.029556494,0.026753653,-0.011839157,0.017920772,0.023500493,0.01711953,0.0009544803,-0.013863288,-0.0070034773,0.018603787,-0.00941334,0.0017300266,-0.008289404,-0.028872516,-0.009244943,-0.0007315728,0.009129337,-0.008090885,-0.009238535,-0.0018578455,0.023200607,-0.003359113,-0.039885815,0.014365729,-0.0112723885,0.009768149,-1.5388514e-05,0.031198546,0.03687342,-0.0027971675,-0.023449661,0.02749072,0.008017911,-0.009674618,0.0076177353,-0.033261236,-0.0073824744,0.0033616703,0.02286326,0.04237107,0.019902147,0.016950903,0.0019717016,-0.013507291,0.018161826,0.005334361,-0.008429375,-0.024393909,0.026372818,-0.0014021879,-0.0027079054,0.024667261,0.0072368323,-0.010817314,-0.009296574,-0.044595793,-0.0015066252,0.002717762,-0.027916033,0.0018076991,-0.0036532104,0.0068515264,0.033891667,0.010936378,-0.0004332497,-0.01710102,-0.021438362,-0.025059007,0.01831581,-0.0008179166,0.011685046,-8.815607e-05,0.0004242026,0.016657697,0.038143877,-0.007645738,-0.0028519498,0.025236463,-0.011910689,0.0005523334,-0.01879664,0.022706924,0.0013657582,0.029009571,0.01698366,-0.020417696,0.0008942527,-0.0060114367,-0.005246848,-0.010446871,0.0013531733,-0.00023936149,-0.0023825376,-0.020905849,-0.013845868,-0.0044691674,-0.028818868,-0.033670302,-0.019678298,0.019267775,-0.0010604159,0.0008894454,-0.015470337,0.006739205,-0.010685263,0.017846217,0.026574308,0.0033223,-0.0032288032,-0.01676632,0.009249943,0.029591685,-0.0056884293,-0.019288706,0.018324269,0.009711592,-0.022182835,0.04269621,-0.008754005,-0.016717723,-0.0058042486,0.014501443,0.030236708,0.010001565,0.0033108967,0.022079116,-0.012075691,-0.016211433,0.014097783,0.0015503869,0.045627248,0.032360204,-0.0035262457,-0.017461972,0.020871865,-0.02759946,-0.010206754,0.009006814,0.01712756,0.004853015,-0.0043973303,0.007953341,0.010754635,0.039826594,-0.01066952,-0.0069172345,0.019017259,0.022664333,-0.0014037964,0.011479069,-0.013424959,-0.0023512132,0.015461536,-0.0051513854,-0.0014247362,-0.00640536,0.014559215,-0.014666073,-0.008801689,0.00064731063,-0.00012667029,-0.01619222,-0.024029013,0.004583239,-0.031073667,-0.02243028,-0.008068276,0.0088509815,0.029262975,-0.0042067263,0.011770702,0.0071817474,0.015190897,-0.017048728,-0.009791859,0.016684368,-0.023467548,-0.0015211315,-0.01538868,0.04410961,-0.0035446966,-0.006732217,-0.027259683,-0.02152945,-0.031346466,0.013895036,0.010741047,0.00039923692,-0.020919258,-0.044955343,-0.011138536,0.0076278807,0.009847369,-0.036137145,-0.022959648,0.01661334,-0.03722433,0.00028404864,0.01625973,0.0069261934,0.008711392,0.023143087,-0.0107204495,0.008665094,0.017792176,-0.013974358,-0.022632329,0.04074669,-0.010676174,0.036370832,0.005801665,-0.0033156103,0.010996934,-0.020916497,-0.0060090953,0.0027338394,-0.011434976,-0.0002301398,-0.012040032,-0.013820073,-0.020233123,0.023745043,-0.024615914,-0.014043326,-0.02400475,0.010558739,0.012679913,-0.0011924093,0.0074206768,0.018335966,-0.009001855,0.0059263627,-0.012100549,0.010425082,0.0062199645,-0.022776868,-0.02767869,0.008334355,-0.0035283593,0.004316763,-0.017740961,-0.0117807295,-0.007251604,-0.0071761874,0.015808119,0.0024416468,-0.0011203047,-0.009681514,-0.0061509055,-0.026073322,0.005310209,-0.0020508016,0.01935906,-0.00887608,-0.005286925,0.017550472,0.011233079,0.0074273148,0.030907305,-0.016949167,0.0106356,0.031471543,0.0059719365,-0.01765997,-0.0051954472,-0.014296325,-0.03592853,-0.012865005,0.012312749,-0.021907154,-0.0005296764,-0.016978623,-0.022430167,-0.029710779,-0.001274088,0.006415444,-0.021661188,-0.020265803,-0.0098376,-0.0035809276,0.035238776,-0.0038922029,-0.0021996917,-0.00785479,0.012927501,-0.021795634,0.011288187,-0.0023118258,-0.02073938,0.008109786,0.024242908,-0.014389668,0.009959781,-0.012254637,0.001366511,0.008167627,-0.0021354507,0.008292801,-0.008868882,-0.00070604467,-0.0082315365,0.004974007,0.020069279,0.020735182,0.0068391287,-0.041476082,-0.015257997,0.0034983451,-0.024575124,0.047513857,0.009822003,0.026896752,0.009458238,-0.012516962,-0.0026086709,-0.00067848485,-0.002478752,0.012570242,0.005683708,-0.007264323,-0.014260499,-0.02352598,-0.0026339907,0.0026522279,0.005734949,0.006013306,-0.0007355569,-0.0061922437,0.0024205388,-0.0015343805,0.006239723,0.023275169,0.0012428785,0.015111998,-0.030628093,-0.025520189,0.024667405,-0.0030133973,-0.0003911534,-0.003623366,-0.0069508497,0.0061468473,-0.002335584,-0.022865644,-0.0019946946,-0.023724232,-0.017352879,-0.009159172,0.028556617,-0.0012765881,-0.0071517746,0.019141225,0.006450601,-0.01993795,-0.01448995,0.0053033624,-0.002334008,0.008074392,0.007533093,-0.009305017,-0.012180031,0.007254584,-0.021656217,0.011471076,-0.02782026,-0.0036684426,0.009682085,0.015317831,0.00065104384,0.002854677,0.0020838499,0.021513592,-0.009171672,-0.006968018,0.0013974564,-0.004727959,0.028100658,0.021257147,-0.009882179,0.016671028,-0.008310995,-0.01862458,-0.0010717282,-0.0020504333,0.02897666,-0.09614668,0.0014337325,-0.010186011,0.0053877677,-0.019122774,-0.02045605,-0.0266187,-0.01698061,-0.011272452,0.023906607,-0.017987376,-0.010735336,0.042669363,0.017984862,0.011077298,-0.00808463,-0.025791278,-0.032778133,0.017197104,-0.0064355554,0.022215765,-0.02045166,0.0007980452,0.022540191,-0.018695163,-0.0028734363,-0.011355833,-0.008878884,0.029475952,0.031588104,-0.0071271514,-0.043593448,0.0027578783,0.0091733765,-0.0056374073,0.00037285607,0.037041504,-0.030828007,0.023611913,0.0013648511,-0.010552847,0.008431895,-0.007311657,-0.02634127,0.008450627,-0.009515456,-0.016863253,-0.018370522,0.023847464,-0.010035244,-0.037326574,-0.011689115,0.0048423256,-0.0106579345,0.0006197173,0.00583762,-0.030886862,0.02038743,0.008018847,0.022098308,0.0029829058,-0.0166471,0.006330646,0.011916699,-0.012061103,-0.0010238395,0.0051526297,0.022722565,0.0076797293,-0.0021587561,0.015864208,-0.03547689,7.599368e-05,-0.0042263605,-0.049431097,-0.016903335,-0.004622457,0.014558826,-0.011264037,-0.007686863,-0.02783672,0.005709493,-0.09172185,0.0016964176,0.0067513213,-0.009248691,0.023045974,0.01550298,-0.0057860655,-0.010671526,-0.0022029018,-0.042730812,0.0048102336,-0.034068655,-0.01512099,-0.011229293,0.037886385,8.9041634e-05,-0.01656887,0.003618913,-0.011817545,-0.0052763256,-0.024062324,0.008518198,0.0124749085,0.0035166857,-0.023307241,0.014049388,0.009303381,-0.00738926,-0.005250976,-0.0064228056,0.019412106,-0.10910608,-0.035028625,0.024002045,-0.014419603,-0.011468641,0.01149663,-0.020381218,0.018470336,-0.022684023,-0.003841804,0.007348281,-0.044192515,-0.0141424,-0.025905417,-0.011908757,0.119504675,-0.004685964,0.008310496,0.0030924692,0.023735566,0.008192012,-0.0040899278,-0.005412297,0.0042764395,-8.090604e-05,-0.010322821,0.0044438564,-0.014584649,0.0030204128,0.029068127,0.042971216,0.039424546,-0.028022334,-0.016435504,0.00063876895,0.0027728896,0.0056498665,-0.01701413,0.023213824,0.031287752,0.0035948905,0.029415859,-0.0039058689,-0.03620314,-0.0036779922,0.0073326374,-0.038879935,-0.021444846,-0.025414633,-0.030329574,-0.0058714985,-0.08346709,0.009668365,0.0022122138,0.010194866,0.024702268,-0.0030473927,0.021007348,0.019261163,-0.01107093,0.0041864864,-0.019997235,-0.008172704,0.03733973,0.0042067734,0.007239348,0.016420174,0.016774114,0.0029495717,-0.00090222975,-0.01212401,0.015489615,-0.0025426187,-0.019339321,-0.0011188888,-0.004488213,0.023863712,0.012575732,0.015191302,-0.011435148,0.016666198,0.0013598903,0.015792083,-0.004932219,-0.009706554,0.014534388,0.009880869,-0.008611544,0.019852819,-0.0037533506,0.024120614,0.03428037,0.0019475584,0.0036306484,0.0011641803,-0.028629165,-0.0034927449,0.0042485315,0.014165786,0.0113063315,-0.021566484,-0.027492426,3.5615503e-05,-0.032297354,-0.00044522545,-0.014839068,-0.016326653,0.046212606,0.029292002,0.0062951786]') ON CONFLICT DO NOTHING;

INSERT INTO public.vector_store VALUES ('f9bbfeeb-4fd5-4939-8106-e28174da846c', '# 🗄️ RDBMS (PostgreSQL) vs NoSQL 선택 및 데이터 모델링 가이드

> **한 줄 요약**: RDBMS는 엄격한 스키마와 ACID 트랜잭션으로 금융/결제/회원 데이터의 신뢰성을 보장하며, NoSQL은 유연한 문서/키-값 구조로 대용량 트래픽의 고속 읽기/쓰기에 최적화되어 있습니다.

---

## 1. ⚖️ 기술별 적합한 유즈케이스

- **PostgreSQL (RDBMS)**:
  - 회원/권한 관리, 결제/영수증 정산, RAG 벡터 검색(pgvector) 등 데이터 일관성이 중요한 핵심 비즈니스 로직.
- **Redis (NoSQL Key-Value)**:
  - 세션 저장소, 실시간 랭킹, 캐싱, 쿼터 제한(Rate Limiting) 카운터.
- **MongoDB (NoSQL Document)**:
  - 스키마 변형이 잦은 비구조화 로그, 이벤트 이력, 실시간 텍스트 데이터 보관.

---

## 2. 🔒 PostgreSQL ACID 트랜잭션 보장
```sql
BEGIN;
  UPDATE account SET balance = balance - 10000 WHERE user_seq = 1;
  UPDATE account SET balance = balance + 10000 WHERE user_seq = 2;
COMMIT;
```', '{"title": "[Database] RDBMS (PostgreSQL) vs NoSQL (MongoDB/Redis) 선택 가이드", "userSeq": 1, "guideSeq": 27, "isPublic": true, "chunk_index": 0, "total_chunks": 1, "parent_document_id": "7d584c67-b13b-4d8b-8750-6964c3c627d0"}', '[-0.011205837,-0.012870179,0.005243647,-0.062163606,-0.012721778,0.027704766,0.00043033969,-0.0027143972,0.0048635653,-0.0037381086,-0.003858906,0.011414606,0.0153783895,0.009719776,0.12567982,-0.004959795,0.007441115,-0.0019740744,-0.009418667,-0.016280122,-0.0063500158,0.007419325,0.011203765,-0.008059878,-0.0062446008,-0.0089481585,0.01452628,-0.015266014,0.039591998,0.037272602,-0.0036295743,0.00020530395,0.013534731,0.050114147,0.0012056406,0.021435743,0.01436036,0.013848107,-0.019594051,0.039267343,-0.001855956,0.005579062,-0.02141366,-0.015469209,0.005887398,0.00832466,0.0044799624,-0.0059372885,-0.004631395,0.031341646,0.023204045,0.010543397,0.00030250978,-0.18253581,0.0025310717,-0.009594247,-0.041691236,0.00862325,-0.019776935,-0.010278479,-0.004188389,0.030375527,-0.02667232,-0.0076377904,0.0041336124,-0.0075841737,0.03309734,-0.005156622,-0.023220597,-0.021491533,-0.0037601776,-0.017887501,-0.009928292,-0.019863414,0.007166031,-0.009301414,0.024476215,0.013131745,-0.030483546,0.010456314,-0.018965997,-0.0055660876,-0.032462597,0.016281627,0.002688499,-0.0068450775,0.010670907,-0.017261125,0.007997768,0.012659706,0.008052911,-0.003279177,0.008154233,-0.021996405,-0.014223972,-0.013009053,0.010956028,-0.0014748332,0.011266415,-0.01783628,0.004154482,0.01828021,0.0054983175,0.01175742,-0.005070096,0.021297447,-0.0059799026,-0.018358754,-0.044955213,0.0027440912,0.008789473,-0.037368216,0.008054615,0.004765019,0.0015359246,-0.14234406,-0.0010793835,-0.01611334,0.003348287,-0.01182302,0.0038238529,-0.0037581562,0.003940396,-0.006522339,0.009250769,-0.038860396,-0.011034223,0.004403338,0.007797756,-0.009644979,-0.025796095,0.012266669,-0.018895179,-0.0027631652,-0.02799815,0.028752917,0.008747386,-0.0019690015,-0.024735412,-0.05155606,-0.012035633,0.030055016,6.0103313e-05,0.01211895,-0.023541665,0.0056986036,-0.042813763,-0.006987319,0.015024685,-0.025090737,0.025975797,-0.0016734389,-0.012346354,0.03138787,0.020561881,-0.010935248,0.010615856,0.013566624,0.012385881,0.012832477,-0.027362982,0.007163071,0.004478158,0.016870784,0.024295378,0.0010927203,-0.003121118,0.0049961903,0.021176804,-0.00997662,-0.0218248,0.029528746,-0.030856246,0.02825196,-0.022881292,-0.01908418,0.010887012,-0.0034695882,-0.013524649,-0.0070180814,0.013829886,-7.684124e-06,-0.008499972,0.0049217893,-0.015597891,-0.022465505,0.0026793198,-0.007197888,0.025458101,0.008852745,-0.018103732,2.2646216e-05,0.0074381004,-0.02974037,-0.0057096886,-0.037432287,0.0111510595,-0.007939744,0.0022120967,0.038717676,0.024080442,0.033381015,-0.0019511619,-0.019414771,-0.015393771,-0.020593788,0.01209114,-0.018332787,0.001750711,-0.0077965884,-0.009222493,0.012115118,0.02838838,-0.011759242,-0.0463472,-0.04447055,-1.8931049e-05,-0.006085421,0.0017637118,-0.022281123,-0.0040585166,-0.010338527,0.03044371,0.046347138,0.015666923,-0.030240444,-0.016546376,-0.02514563,0.004442133,-0.008599492,0.020183515,-0.0019329675,-0.010223837,-0.0011313157,0.050611645,-0.019267796,0.0015229039,-0.0021480015,0.0048359623,0.0029191058,-0.02097538,0.010433316,-0.014807327,0.043241348,0.017132023,-0.0005333179,-0.012036785,0.021759244,0.007410261,0.024902944,-0.005722309,-0.009012894,0.007657041,-0.0053165723,-0.012720279,-0.03316398,-0.043980725,-0.028472545,-0.014960822,0.006837802,-0.006852542,-0.015022606,-0.0041731917,0.0010903991,0.00622895,0.02567876,0.0051999,0.030249923,-0.010195465,-0.0045444663,0.011759756,0.013944341,-0.009129127,-0.012783943,-0.005063476,0.000766412,-0.011974557,0.021875398,-0.004047428,-0.014610176,-0.0112544345,0.01141294,0.026887491,0.032730106,0.0026053535,0.029567225,-0.0024335885,0.0031627577,-0.006376693,0.014289975,0.039651174,0.013549542,0.016060365,-0.027881095,0.01564398,-0.03698973,-0.039648432,0.008759867,0.016349517,-0.0054713446,-0.025481677,0.012774362,-0.014875891,0.05755429,0.010314071,0.00014550168,-0.013024716,0.011214251,-0.009201559,-0.019470548,0.009875689,-0.03207298,0.018856911,-0.015313123,-0.014199507,0.007855732,0.0110407835,-0.011220082,-0.016074503,-0.005849766,-0.0047884737,-0.0063175038,-0.0052069,0.017187545,-0.01970579,-0.021119302,0.008411198,0.013616398,0.025855439,-0.010010989,-0.0070233354,0.0014119459,0.028374553,0.012120624,0.011200152,-0.0129983695,-0.011597591,0.006950155,-0.010546933,0.004722976,-0.005102953,0.001698372,-0.020739151,-0.0020788747,-0.04675782,0.01257567,0.013466131,-0.010986872,-0.03406252,-0.026817132,-0.002990585,0.015531568,0.0032549016,-0.0007916329,-0.021913126,0.016916826,-0.032522187,-0.0148827005,0.025277343,0.004694239,-0.0007689279,0.018945247,-0.017729273,0.009994416,0.014927661,-0.021561295,0.011325329,0.023294093,-0.006704958,0.015663613,-0.030190593,0.016377544,0.0072080684,-0.013191452,-0.019326737,0.005743234,0.0041108946,0.002393238,0.005873052,0.0058645955,0.00114768,-0.012051736,-0.02714607,0.009641029,0.005730368,-0.009243377,0.01633254,-0.0038996239,0.012237109,0.012056894,0.0068573067,-0.009305307,0.024911676,0.030264573,-0.0061115245,-0.0091512585,-0.009777184,0.002016525,-0.017708343,0.00013661574,-0.036101513,-0.0075736535,-0.016515719,-0.024275353,-0.017142918,0.0032000146,0.0048309863,0.0016887586,-0.009697379,-0.010234632,0.011092801,-8.2604805e-05,-0.0059557497,-0.013703204,0.02782841,0.03529796,0.0172281,0.014124942,0.008577662,-0.019795936,0.008950755,0.028826257,-0.008310353,0.0049752835,-0.01501477,0.010336076,-0.049852952,-0.03801876,-0.0150660435,-0.029049994,-0.009393261,0.012081196,0.002409004,0.0034693168,-0.006461777,0.018372156,-0.01743767,-0.013477247,-0.0113624595,-0.009070488,0.010662528,0.010808178,-0.016289445,0.0015774876,0.027028508,-0.00940059,0.0019955507,0.0011506763,-0.002072564,-0.014896382,0.018779289,-0.032189976,-0.0038605193,-0.041169833,0.005346297,0.013269015,-0.02217347,-0.004661103,-0.014948177,-0.02017834,-0.016579565,-0.012789001,0.011829623,0.007870446,-0.012574557,-0.0073347134,-0.009603758,-0.003994055,-0.045615777,2.1724602e-05,-0.022170164,0.044355545,0.009000106,-0.00037022086,0.00025716206,-0.006339904,-0.011587925,-0.031013245,0.009049259,-0.015863508,0.0036960745,-0.0073824544,-0.020484064,-0.025850171,0.011805324,0.03490839,0.038934212,0.0070837154,0.0051647644,0.005917642,0.0056328136,0.024094258,-0.0036664668,0.01165361,-0.012162127,-0.0018021171,0.0030660948,-0.00067186117,0.04075349,0.0048159123,-0.013193872,0.015205374,-0.017626224,-0.0202483,0.014273877,-0.011318048,0.0005851483,-0.01870237,0.042855084,-0.004933458,-0.039615788,0.015623164,0.007992823,-0.04568581,-0.020132562,-0.0006167054,-0.002957376,0.018496946,0.014571085,-0.021885123,0.0024029994,-0.020647846,-0.014717644,0.013695583,-0.018023184,-0.011399003,0.012102108,0.0048854053,0.01175159,0.016981045,0.001052128,0.028552745,-0.009629462,-0.032485344,0.010069358,-0.006292544,-0.0018283677,0.0099052265,-0.028674413,0.0029005057,-0.0073815547,-0.0028980672,0.004907852,0.0033107402,0.0038186375,-0.08301181,-0.007250799,0.007110579,0.021188658,-0.0039890907,-0.010395596,-0.02451886,-0.015457186,-0.0027620788,0.015492501,0.00875816,0.00654913,0.023580296,-0.007146063,0.014159915,-0.000821377,-0.042627864,0.012349275,0.035525776,-0.0041105114,0.008060795,-0.022582587,-0.007023971,0.010429656,-0.01423777,-0.0009403628,-0.003128181,-0.0068874005,-0.0026389754,0.021657554,-0.024625044,-0.005675792,0.0202058,0.0050895666,-0.00041112892,0.009364496,0.011809928,-0.019969108,0.008225205,0.011037858,0.005965269,-0.0027104786,-0.013760228,-0.000652919,0.030198377,0.0017469254,0.011689065,-0.026869206,0.010323427,0.0017414546,-0.046228796,-0.032284997,0.020181559,-0.01237096,-0.020564254,4.071493e-05,-0.028909557,0.029103432,0.0041109854,0.019515868,-0.02152934,-0.02091777,0.0015795984,0.029938059,-0.024943087,0.004043125,-0.008627872,0.0036086407,0.0015707209,0.01650689,-0.010750041,0.0075701163,-0.00076247024,0.006839318,-0.028383778,-0.0018681572,0.011161567,-0.0039685434,-0.021108465,0.0020734014,0.023089236,-0.020058066,-0.082306474,-0.0057141306,0.011277638,-0.014500445,0.021378282,0.006079638,-0.0033903287,-0.012039453,0.009439777,-0.026588902,0.013008732,-0.016474985,-0.013667042,-0.034749724,0.015933558,-0.023059718,0.0034918794,0.012208221,-0.0033908011,-0.014376888,-0.0055009373,-0.00180991,0.0034546172,-0.003103479,-0.022504484,0.026962679,-0.005036303,0.009760797,-0.016454209,0.005652015,0.026128627,-0.11757352,-0.03623932,-0.0017223563,0.014059091,0.012974574,0.0060610003,-0.016053358,-0.017032295,-0.011244293,-0.017173937,0.01209683,-0.022487994,-0.009524343,0.0032587997,0.0060391864,0.09637457,0.0006433382,-0.00877062,-0.0057849092,-0.009244769,-0.02596589,-0.0024694104,-0.01524489,0.0027122346,0.0007250163,-0.015256275,-0.023109224,0.0054604704,0.005358768,0.006239166,0.0020855442,-0.0022289178,-0.028169533,-0.012175168,-0.0040182024,-0.0022433775,-0.00084104715,0.009447876,0.0074189045,0.017157497,0.007236089,0.017933473,0.0065107737,0.017335268,-0.023023386,-0.002407057,-0.005981462,0.010035155,-0.035137538,-0.0018767178,0.010471005,-0.08685947,0.00068017654,0.009476339,-0.00077512034,0.0036769887,-0.016423501,0.019931737,0.013058767,-0.025626896,0.018778475,0.004984672,0.00017652461,0.020454261,0.0044196406,-0.020794827,0.004730707,0.027848616,0.014778445,-0.008952143,-0.009183474,0.02978702,-0.0035454482,-0.023103101,0.0033103183,-0.019887717,-0.016302092,0.00036303134,0.022355381,-0.021116132,0.020036938,0.030601885,-0.014559149,-0.03271946,-0.0018121548,-0.0048456355,-0.0023698655,0.006577944,0.00946138,-0.007869923,0.031767044,0.050055705,0.007774441,0.026509305,0.014603355,-0.0047608726,0.024813803,0.021915901,0.028416308,0.0061094193,-0.016730305,-0.02159427,0.010516314,-0.02218729,-0.023915056,-0.022077614,-0.0023512791,0.024788622,0.038558535,0.011742111]') ON CONFLICT DO NOTHING;

INSERT INTO public.vector_store VALUES ('fb7e583c-1ecd-4f8f-8a40-3eef7998f0fe', '## MLOps 개요

**MLOps** 는 머신러닝 모델을 안정적으로 개발·배포·운영하기 위한 실천과 자동화의 총칭이다. DevOps를 ML 특성(데이터·모델·실험)에 맞게 확장한 것이다.

일반 소프트웨어와 다른 점: 코드뿐 아니라 **데이터와 모델도 버전 관리**해야 하고, 배포 후 **데이터 드리프트(입력 분포 변화)** 로 성능이 저절로 떨어질 수 있다.

핵심 요소
- 데이터/모델 버전 관리, 실험 추적(MLflow 등)
- 재현 가능한 학습 파이프라인
- 모델 배포(서빙)와 A/B 테스트
- 모니터링: 성능·드리프트·지연·비용 관측
- 재학습 트리거

LLM 시대엔 프롬프트·RAG 평가·토큰 비용 관측을 포함해 **LLMOps** 라고도 부른다.', '{"title": "MLOps 개요", "userSeq": 1, "guideSeq": 40, "isPublic": true, "chunk_index": 0, "total_chunks": 1, "parent_document_id": "f441aace-48ff-4a05-96b6-679c4c928a41"}', '[-0.034835264,0.0076858173,0.029302474,-0.07054428,-0.004618594,-0.011415318,-0.009332583,0.012483644,-0.0009964966,0.0147233205,-0.00892653,-0.014291337,0.016954964,0.0022333302,0.14637636,0.04637465,0.015889594,0.008784042,0.003172606,-0.003941206,0.008301668,0.016369179,-0.011043414,0.009855967,-0.026692074,-0.020063246,0.015178423,0.030306391,0.042322036,0.008067814,-0.0063447254,0.017082453,0.028424354,0.0335162,-0.022783175,0.004693767,-0.015702972,-0.007693,0.017478963,0.02714116,-0.023147896,-0.0107904095,0.013486304,-0.03946378,-0.0382127,0.0061884616,0.022566497,-0.01968965,-0.0054495856,0.007468767,-0.013703678,0.0009143555,-0.0030772993,-0.16585097,0.007744482,-0.024849094,-0.019747617,-0.005344833,0.017211499,4.3433014e-05,-7.360466e-05,-0.004888829,-0.0013626656,-0.0007788553,-0.0007459262,-0.013093643,0.037006363,0.013343679,-0.008211024,-0.020197557,0.010293861,0.019785,0.0068848375,-0.023970587,-0.0015853686,-0.033735555,-0.0047238343,-0.008809148,-0.005276704,0.02131384,0.0023601803,-0.03106977,-0.019247277,0.00095324055,-0.0142413275,0.023182862,-0.008573918,-0.020650009,-0.0079853935,0.0009590743,0.039150868,0.0025364035,0.0051106494,0.010177262,-0.01661908,-0.014168211,-0.016660668,-0.02363003,-0.00518682,-0.017237263,-0.01165751,-0.012549371,0.022521457,0.035034757,-0.0030553841,0.00086428167,0.009137974,-0.021990243,-0.029705862,0.0008064938,0.011566459,-0.015293419,0.021235913,-0.0045333407,-0.004545822,-0.112694874,-0.013134352,-0.019191159,0.0015060718,-0.00911901,-0.010813167,0.0115511,0.017137898,0.033976015,0.0108912075,-0.0148279015,0.017098207,0.0015431738,0.018519184,-0.037717417,-0.014504274,-0.0048342734,-0.024546549,0.021781785,0.0060180794,0.032589577,-0.02767557,-0.025189202,-0.035209354,-0.0061671785,-0.02498555,0.02211192,0.0008053013,-0.010049329,-0.04116912,-0.016493099,-0.052572433,-0.012230921,-0.011723523,-0.007542171,0.010818039,-0.02410542,-0.037393034,0.008026883,-0.01852256,-0.013609556,0.011878078,-0.0011221159,-0.0021554623,0.025182795,-0.015883349,0.005081571,0.011740302,0.0028476708,0.019792419,0.030401247,-0.019354874,0.022169773,-0.0128091825,0.017342597,-0.006208203,-0.0064455443,0.0039018216,0.0049440376,-0.025196798,-0.014084336,-0.009622825,-0.003942518,0.018459627,-0.03287577,0.023692694,0.02012488,-0.010784222,0.017646262,-0.0132182045,0.010555294,-0.0053397855,0.00930608,0.009346232,0.019993553,-0.00627354,0.0069490043,0.0071884114,-0.018593859,0.01959182,-0.011939611,0.0020835572,-0.022303052,-0.0033847275,0.023409639,-0.002358684,-0.01323798,0.012334589,-0.029232914,-0.0039355336,-0.0045539862,0.0009170578,0.006499028,0.0070371656,-0.016755708,0.0065607126,0.013893843,0.023880158,-0.0052162576,-0.0052989195,-0.039721835,-0.019381687,0.0060724244,0.0039198776,0.016457383,0.026635742,-0.0065004104,-0.0032006996,0.011645669,-0.032358248,-0.017514113,0.025576206,-0.0018223716,-0.021271814,-0.006475083,0.014645554,0.0077792145,0.013284208,0.022540601,0.006678322,0.03637019,-0.007758976,0.043776464,-0.0050416356,0.016372949,-0.022319393,0.0075269025,0.02008798,0.004313126,0.010188027,-0.021991437,-0.004926251,0.021354053,-0.019426716,0.015401767,-0.0036900584,0.019140081,0.003103753,0.0035860718,0.008950359,-0.010539301,-0.026163572,0.009981365,-0.0015445863,0.012494551,-0.0066736853,-0.015459968,-0.00011613506,-0.016344452,0.025685718,0.002228556,0.013782703,0.009010859,-0.0053496873,0.0029104827,-0.005678178,0.015268016,-0.0006018366,-0.0065194,0.029202733,0.0031358367,-0.03301457,0.028343799,-0.008869907,0.0023435035,-0.0038309495,0.0027902678,0.002241785,-0.0011451527,0.012909247,0.030274002,-0.008457584,-0.00089637906,0.012533011,0.0066828174,0.01595857,0.0145260105,-0.012248219,-0.024973508,0.026528092,-0.015742412,-0.013265734,0.019627688,-0.0046092765,0.010972303,-0.016156869,-0.013314597,0.010179619,0.045427863,-0.015450112,0.023642631,0.018357946,0.027794141,-0.004416135,-0.00373053,-0.013703264,-0.011767926,0.00442894,-0.020341074,-0.02059187,-0.0051913694,0.026587963,-0.017779915,0.0057068984,0.014827121,0.015660668,-0.00051433267,-0.00987139,-0.013678915,-0.0027870333,-0.0079710875,-0.007526528,0.0024758452,0.030403284,-0.026893694,0.015136039,-0.009605035,-0.0029872784,-0.031090496,-0.0015375139,0.01857881,-0.023683624,0.001222775,-0.03709034,0.0071763345,-0.0074795773,-0.0010647521,-0.01437236,-0.010350449,-0.031275317,0.030551638,0.013777142,0.008620237,0.015798932,-0.034297716,0.052717026,-0.026595807,-0.0019286786,-0.0046184873,-0.01070229,-0.0041549876,0.014350107,-0.0028528487,0.020926565,-0.014805258,0.006581039,0.01781021,-0.01390982,0.00026542498,0.028004492,-0.016675325,-0.020292297,0.021720117,0.017631108,0.01488189,-0.004519418,0.0108052,0.008633177,-0.010479449,-0.01257055,0.0040881187,-0.0114857545,-0.021633,-0.00811411,0.009302094,-0.003356444,0.018081004,-0.030372906,-0.0082200905,0.007975146,-0.011780069,0.016557263,0.008220137,0.0007296912,0.0029301161,-0.011098498,-0.0017895568,0.0155086415,-0.00051113876,-0.006544763,0.004022272,-0.015102118,0.03201745,-0.0074533434,-0.021895988,0.0031496496,0.021742832,0.005868609,0.013513238,-0.022770576,0.009620785,-0.014560717,0.008833411,-0.030827044,-0.031021561,0.011046417,-0.012281688,-0.0034673056,0.010987194,0.0015998557,0.0039469795,-0.030111333,-0.01132725,0.02325414,-0.0038325258,-0.012317906,0.0326531,-0.010919999,-0.0050997958,0.0084655145,0.0008704615,-0.03382458,0.02920079,-0.019159826,-0.0222472,-0.0077820444,-0.0062138964,-0.029645298,-0.036039684,-0.0064389994,0.0016184067,0.011644085,0.0014934839,-0.014208228,-0.023098439,0.023860317,0.002849081,0.022360094,-0.008871854,-0.015773734,-0.0054851486,0.005615419,-0.008485612,-0.03627576,-0.012951362,0.03924673,0.02371743,0.033451684,0.0073670335,-0.010573764,-0.011800241,-0.009154118,-0.016731592,-0.0012199046,0.025240462,0.003516765,-0.0004480705,0.0068427483,-0.027225282,-0.0056208917,-0.012381219,0.011668518,-0.0077515375,0.021967854,6.980195e-06,-0.016242046,0.021617264,0.036301848,-0.00953343,-0.023808228,0.010292291,0.02256897,0.021153837,0.015367968,-0.016874777,0.016041312,-0.003529879,-0.0128139835,0.018477006,0.026241297,-0.001953933,-0.0019422389,0.0052048024,-0.008022189,0.008062175,-0.001676888,0.045112077,0.020172587,-0.008597137,-0.0030773277,-0.0062819202,0.0074498127,0.023382228,0.026103172,-0.024456624,-0.01901503,0.005867089,0.023547918,-0.0075574745,-0.011204532,-0.031570997,-0.0047926903,-0.0036552309,-0.024279447,0.0023157487,-0.021431062,0.011324395,0.016755847,0.0033489938,-0.008304505,0.012974599,0.008452857,0.011087635,-0.012367877,-0.008204792,0.020726588,0.0012127424,-0.02265937,0.021076895,-0.024129545,-0.01888268,0.013275749,0.001602242,0.05350951,0.029891971,0.023501445,0.003989009,-0.00988474,-0.01966421,0.007916258,-0.013941486,0.004135121,0.011085236,-0.006452792,-0.0064156633,-0.011948891,0.0004175131,-0.017303066,0.008304575,0.041458733,-0.07616285,-0.006928123,-0.005100073,0.007138677,-0.0071344795,-0.0007542144,-0.015699606,0.009954861,0.037012633,0.016329294,0.012296709,-0.001437596,0.003380778,0.017521217,-0.010607673,0.0002790593,-0.020402564,-0.04011914,0.020402923,-0.027474929,0.015261591,-0.009310638,0.012491953,0.004052624,-0.016640259,0.0029061437,0.020516563,0.013219915,-0.006598676,-0.0027038064,0.008806698,-0.007783607,0.03147442,-0.006789933,0.008118702,0.006221254,0.0093075065,-0.010763253,0.029294234,0.0015794013,0.010596804,0.0010916514,-8.657175e-06,0.013695512,-0.0029107747,0.0379454,-0.0038307735,0.0021071506,0.008127872,0.013188948,-0.030371627,-0.027061997,-0.02933259,-0.037591983,-0.020656439,-0.0047650374,-0.016633907,0.02043513,-0.0009827163,0.008021467,-0.0065470184,-0.016763404,-0.007235709,0.034264117,-0.034690302,0.011117096,0.009243555,-0.0062778206,-0.004192401,0.008009152,-0.010237667,-0.012560711,-0.0020072968,-0.008328744,-0.02578088,0.020518819,0.0008785979,0.012803616,-0.012612623,-0.0026654126,-0.008593354,-0.0261618,-0.090030596,-0.036871582,0.0045254882,-0.0003756236,0.06040253,-0.0032242069,0.011395611,-0.03788104,-0.030323382,-0.056965396,-0.0040092217,-0.025782974,-0.007902269,-0.0356193,0.017418055,-0.0125802625,-0.0035880676,0.009489528,-0.005500099,-0.0065088365,0.016994499,0.0014214045,0.0050657853,-0.010797306,-0.000584833,0.017288985,-0.0155323455,0.018557888,-0.0024870187,0.0032413746,0.018971901,-0.12494745,0.023387516,-0.0056509124,-0.019206334,-0.0055385954,-0.0005362292,-0.016458979,-0.0071119154,0.009316991,-0.030107912,0.0049436823,-0.031567466,-0.031227624,-0.01668363,-0.01294033,0.112335734,-0.018795572,0.0011764432,0.003642861,0.002318191,-0.032465443,-0.038054954,0.0028894783,0.01822807,0.007740129,-0.0113318395,-0.0008746863,0.0029042272,-0.00359293,0.022200271,0.01881398,0.008003549,-0.03102595,-0.02421059,0.0088639315,0.0032044123,0.0044602333,-0.033125408,-0.0027034872,-0.002768094,-0.0092149265,0.026427682,0.009908301,-0.030054731,-0.0008369889,-0.0048674047,-0.032097705,-0.01648529,0.005814657,-0.03233316,-0.028966608,-0.0579899,0.019330312,-0.015521238,0.014352699,0.013131874,-0.0061698514,0.014009364,0.008319028,0.011781918,0.039465424,-0.00017140851,0.022310566,0.038751535,-0.0128366845,-0.010631995,0.03988919,0.023253288,0.0021542239,0.0032231794,-0.00096786907,0.0032383827,-0.014001081,-0.006071599,0.014616418,-0.0038077887,0.023141624,0.0063812193,0.01727241,-0.01574937,0.0063554677,0.018545251,-0.0043042,-0.005248089,-0.015392037,-0.010545909,0.019433482,0.0017849244,0.0073764515,0.008423003,0.0042101974,0.041111987,-0.0059651346,0.0067320853,0.006103175,-0.010815607,-0.013060089,0.023298588,0.01955338,0.022013977,0.018972764,-0.021894502,0.016293583,-0.009152522,-0.011308444,-0.004725846,0.0017725806,0.021167004,0.032377344,0.019746462]') ON CONFLICT DO NOTHING;

INSERT INTO public.vector_store VALUES ('fd4980db-15fa-4e2c-8396-e4e84f10969e', '## 토큰과 컨텍스트 윈도우, 비용

**토큰(token)** 은 LLM이 텍스트를 처리하는 단위로, 대략 단어의 조각이다(영어 1토큰≈4자). 모델은 입력·출력을 토큰 단위로 센다.

**컨텍스트 윈도우** 는 모델이 한 번에 볼 수 있는 최대 토큰 수다. 이를 넘으면 앞부분이 잘리거나 요약이 필요하다. RAG로 넣는 근거 문서도 이 한도 안에 들어가야 한다.

**비용** 은 보통 입력·출력 토큰 수에 비례한다. 그래서 실무에선
- 불필요한 맥락 제거, 프롬프트 압축
- 검색 근거는 관련 top-k만
- 캐싱(프롬프트 캐시)으로 반복 비용 절감

토큰·지연·비용을 함께 관측(observability)하는 것이 운영 LLM의 핵심 역량이다.', '{"title": "토큰과 컨텍스트 윈도우, 비용", "userSeq": 1, "guideSeq": 37, "isPublic": true, "chunk_index": 0, "total_chunks": 1, "parent_document_id": "b0e25efa-0fb1-4600-95fb-9742688173a6"}', '[-0.012359903,0.015488876,0.020193608,-0.06896567,-0.022575222,0.0035886506,-0.022716513,-0.020771539,-0.001269924,0.013723298,0.0038367037,-0.009313293,0.014711318,-0.019722393,0.12896577,0.012286648,-2.8425393e-06,0.01277702,0.029045297,-0.013259577,0.0027488866,0.028685162,-0.018578948,0.013711936,0.00852147,0.0018617071,0.011315087,-0.015848294,0.042026803,0.029556038,-0.010831533,0.0069345646,0.025007188,0.008221071,-0.020775678,0.0033066405,0.010748621,-0.045323312,-0.006840138,0.03932356,-0.014277952,-0.017125614,0.020146107,-0.031047903,-0.031467125,0.0067036175,0.006271125,-0.029781274,0.02005677,0.04281127,0.012774482,0.0038596445,-0.0006275237,-0.1637041,-0.011985338,-0.008284936,-0.024561843,-0.006126421,0.009987943,0.0111057125,0.0019883113,0.008976694,-0.0027832096,0.01927967,0.0061201984,-0.0009181423,0.009650608,0.0065755737,-0.009578517,-0.023018863,0.015647924,0.0048446665,0.0010284432,-0.017158668,0.023253907,-0.0311456,0.026970172,-0.0012995582,-0.0071323677,0.012659461,0.014241042,-0.034415346,0.0076316404,0.011143992,0.0061250622,-0.0018272378,0.01708331,-0.010813876,0.006835241,0.0034698558,0.018799724,-0.0047950507,0.02404703,0.015456121,0.005106473,7.683955e-05,-0.012679092,0.0035686924,0.009774757,0.002758539,-0.010549224,-0.009356204,-0.00517599,0.025971161,8.5164036e-05,-0.00038769792,0.015678199,-0.002057739,-0.027340494,0.014212607,-0.016604204,-0.0047534886,0.013811011,0.0045349346,-0.023180466,-0.15029208,0.015853792,-0.011966466,-0.013810602,-0.022184527,-0.025704065,0.011396104,0.0018606534,0.030896593,-0.00090883137,-0.015698902,0.007823909,-0.012574817,0.01233182,-0.00678644,-0.021846073,0.00299121,-0.0067618336,-0.017976822,-0.005649604,0.03702859,-0.00917417,-0.022997476,-0.039021183,-0.029141577,-0.002174466,0.012386306,-0.008769215,-0.014837731,-0.016627818,-0.027529422,-0.037346963,-0.007326322,0.013770091,-0.01530271,0.02499284,-0.013045521,-0.019440794,0.024592819,-0.000623547,-0.02729202,0.022874577,0.0019134352,0.006426954,0.022139272,-0.008402098,0.009459546,-0.0037424865,0.03560437,-0.0014595218,0.015746806,-0.01873571,3.4078734e-05,-0.005274481,-0.01740547,-0.008594904,0.02276384,0.009689709,-0.025723815,-0.016647942,-0.009929791,0.0011701656,-0.0055198534,0.0066069867,-0.019565253,0.008560379,0.012949707,-0.023126325,0.003097235,8.525227e-05,0.022623518,-0.0100202,0.009401611,0.0299102,0.0045624343,0.012665715,0.012831055,0.026184445,-0.015285563,0.015791178,-0.009602451,-0.04152369,0.011255343,-0.0048747007,0.01652659,-0.0071015577,0.0040491633,0.019171603,-0.010388236,0.019437224,-0.0027737792,0.0022891713,-0.011236729,0.011293326,0.0084651485,0.018492062,0.0004875249,-0.0033442192,-0.0033363495,-0.0069149425,-0.025279876,-0.010718224,-0.003364958,0.0051224465,0.015154671,0.018082954,-0.0013069394,0.020803833,0.005433911,0.010455866,0.0036559177,0.0069601424,-0.010720921,0.0016580164,-0.015425781,0.004817481,-0.008579437,0.03408913,-0.016487923,0.009779779,0.0055218805,0.0121448785,0.016182695,0.011943877,0.012878571,0.0073269084,0.019610614,0.017312674,0.02551951,0.03182582,-0.017266536,0.027035557,0.015797094,-0.046580076,0.007321906,0.00093618187,0.00053365045,0.018099269,-0.0065511134,0.016708653,-0.007595104,-0.010986085,-0.028025746,-0.016447514,0.014757331,0.005730085,-0.03435464,0.0014227626,-0.0048878505,0.013399707,0.028930755,0.012577603,-0.008156999,-0.02607718,-0.01968665,-0.014305965,0.009873395,-0.0038291507,-0.03454971,0.019325484,-0.014985804,-0.024094896,0.022696171,0.008022894,-0.0010341228,-0.0037867385,-0.00802359,0.008126231,0.013531901,-0.0006581264,-0.016684888,-0.0046532685,0.019163806,0.014726742,-0.0022515112,0.026271123,0.029990867,-0.013889491,-0.015588287,0.011348963,-0.018578256,-0.0013567968,0.010447017,-0.016719855,-0.006771281,-0.0065880516,0.004642676,0.0056341477,0.04417428,-0.010860629,-0.0023794225,0.022087721,0.027061176,-0.01280929,0.0017783025,-0.017637175,-0.008729989,0.016443059,-0.013982085,-0.020308556,-0.023703922,0.026308954,-0.011480597,-0.0034193387,-0.0029368633,0.010112465,-0.011255904,-0.008931947,-0.0073651783,-0.033139028,-0.015594744,-0.005631912,0.0019643516,0.04044005,0.008587637,-0.002984315,0.005980512,0.029087983,-0.027598243,0.009956762,0.005953738,-0.0136795165,0.008238337,-0.016986901,0.019257054,0.011476595,0.0055511175,-0.024271373,-0.015159905,-0.017819675,0.027360275,0.043626837,0.02368924,-0.004969219,-0.025055464,0.02338313,-0.018422255,0.018804124,-0.023552215,-0.0031105124,0.022446392,-0.016224464,-0.008078218,0.011280425,0.011039863,0.0014101989,0.0068615186,-0.021784712,0.034399427,0.012703845,-0.03870524,0.0071196873,0.025706133,-0.011687043,0.011607149,-0.019355271,-0.024128849,0.012790247,0.0018647097,-0.007314142,-0.0055674585,-0.0077756117,0.0076122745,-0.0036074165,0.004258821,-0.031639643,0.024920478,-0.013186065,0.0065992777,-0.017810434,0.0030045006,0.01852752,0.0081354175,0.0110585615,0.011363728,-0.018689323,0.022214202,0.015171333,0.012074698,-0.016851034,0.00274473,-0.030954422,0.003543181,-4.5555535e-05,0.0049788626,0.0034416562,-0.016145568,-0.004636856,-8.018314e-05,-0.0051008817,0.024443405,-0.013309919,-2.0750918e-05,-0.008371837,0.0011490327,-0.01705857,0.011062184,-0.010280046,-0.00644928,0.024201794,0.008291217,0.001655029,0.015307015,0.012894002,-0.024602145,-0.0012135451,0.050534938,-0.006622425,-0.001070433,0.006980376,-0.013692824,-0.037138723,-0.019784153,-0.0031683084,-0.026212463,-0.0022069756,-0.014483455,-0.018219132,-0.019395461,0.0017402325,-0.004175772,0.013239582,-0.016672706,-0.013326887,0.0044662715,0.016052969,0.017559748,-0.0012226546,-0.008780606,-0.031629264,0.0031305675,0.02212598,-0.014212051,-0.004578467,0.01953475,0.033884387,0.016493121,0.014259987,-0.016808163,0.03166146,-0.0032340481,0.00015398591,0.011377148,-0.023879534,0.012869996,-0.016152954,-0.012257569,0.0126309125,0.043093238,-0.0041280105,-0.01867559,-0.023412392,-0.037556063,-0.020555729,0.03570581,-0.0142997075,-0.0016863148,0.0075163543,-0.005951369,-0.017702656,-0.0149436435,0.01476424,0.0035251023,0.0056819594,0.0053193583,-0.01550662,-0.036067717,-0.0042615863,0.01790485,-0.0019168516,0.015358845,0.0031040749,-0.015333958,-0.023358315,-0.00653565,-0.0077159493,0.041349698,-0.011997283,0.0075711366,-0.006013169,-0.023671813,0.004792885,0.016149864,0.0063723396,0.004864951,-0.0028971548,0.010153215,-0.0192422,-0.028826749,0.011436937,-0.030794008,-0.031847052,0.018997915,-0.0045160195,-0.031224547,-0.03627874,0.02379008,0.019460032,-0.014635696,0.0017641141,0.010193267,0.008083397,0.0056582512,-0.02844624,-0.0069791665,0.0017048124,-0.0040895185,-0.009629365,0.038813706,0.0008736437,-0.0045254477,0.040240884,-0.010304289,0.011023989,0.021091448,0.0017908878,0.020729773,-0.0029928926,-0.031235486,0.013648624,-0.014075339,0.028067466,0.017076612,-0.0136418855,-0.001256476,-0.010691685,-0.0031582175,-0.01030864,-0.0061186044,0.03429058,-0.0940522,0.019168302,-0.01528584,-0.023431508,-0.017196983,-0.023909492,-0.016181333,-0.016816827,0.006665493,0.01439129,-0.003534615,-0.015232151,0.04328889,-0.004917782,-0.013489432,0.014963884,-0.022385713,-0.032157954,-0.014804188,0.0036159004,0.013377375,-0.009424076,-0.008989527,0.0019425527,0.0152481105,0.0071135154,0.0030225401,-0.0048609227,0.022024367,-0.00022825417,-0.034996554,-0.01983315,-0.017393816,0.010955759,0.028048681,-0.011830655,0.024811212,-0.011711029,3.6165886e-05,0.02477335,0.014979361,-0.007689226,-0.014490115,-0.018398296,0.013388314,0.015827268,-0.017926153,0.006482404,0.030356932,-0.0044363146,-0.024732849,-0.00519876,-0.02986631,-0.0028954102,0.016342958,0.004988162,-0.015689828,-0.009576288,-0.0074185818,-0.0037255662,-0.019177876,-0.014302346,0.01759003,0.032309048,-0.019456467,-0.0030890964,0.011106341,0.0025924416,0.0013150983,-0.005728425,0.00019785948,-0.024921749,-0.011665122,0.0026709822,-0.023350377,0.00932021,0.0075272797,0.025386775,0.018127473,0.0019224948,-0.026825381,-0.007288465,-0.0929372,-0.02416836,-0.010650524,0.011638388,0.028940806,0.0024917691,-0.008124551,-0.02542902,-0.008678455,-0.032906484,0.019412229,-0.00905945,-0.0145704,-0.023743741,0.025393536,-0.011354386,-0.004829788,-0.004635006,-0.017346138,-0.008299783,-0.010757998,0.015288493,0.031605456,-0.025792452,-0.025315944,0.0029784832,-0.009709774,-0.0044662496,-0.013721729,0.01425596,0.0013300384,-0.12209023,-0.017706014,-0.015156969,0.0072976444,0.008248036,-0.016725352,-0.0005422458,0.02324629,-0.010045015,-0.02789711,0.0070368494,-0.048102606,-0.025775244,-0.00012839501,-0.0015124002,0.10779586,-0.007722839,-0.005340216,0.008114298,0.020207725,-0.009981301,-0.023680836,-0.006909279,-0.00017741558,-0.01808885,-0.0024364,0.014799742,-0.021421036,0.0045964993,0.030099802,0.02645255,0.026258156,-0.0029743465,-0.019470835,-0.0071539865,0.010096921,0.00780254,-0.026210789,-0.0050223353,0.0024697955,0.011405326,0.011674598,0.012826025,-0.034190707,0.0161253,-0.014617174,-0.038268786,-0.05177064,-0.013885541,-0.022816757,-0.004152174,-0.05335353,-0.0088843,0.00054374174,0.02638499,0.008146097,-0.011258769,0.009733642,0.013151165,-0.015699223,0.022233617,-0.021981427,0.0041052257,0.0044872817,0.003789691,-0.016559854,0.02723808,0.041349582,0.009059366,0.01564318,-0.021142136,0.009202467,-0.010210108,-0.014553155,0.028327553,-0.0010308778,0.021419397,-0.0047621117,0.012520521,-0.018364243,0.012216356,-0.0055310386,-0.015891835,-0.0037879285,-0.0072800256,0.004141852,-0.008270193,0.00789761,0.011072711,-0.018933771,0.011174436,0.054316793,-0.007695545,0.003205417,0.0014391032,0.004741631,0.008068564,-0.0022142848,-0.020749804,-0.003620462,-0.0042406097,-0.010793108,0.026806202,-0.01071619,-0.009177142,-0.0018680895,-0.010525606,0.01271589,0.013019539,0.0027619558]') ON CONFLICT DO NOTHING;



--
-- Name: guide_guide_seq_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.guide_guide_seq_seq', 51, true);


--
-- PostgreSQL database dump complete
--


