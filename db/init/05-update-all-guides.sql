-- =============================================================
-- 전체 사내 가이드 문서 고도화 및 신규 문서 추가 SQL
-- =============================================================

-- 1. CI/CD 파이프라인 (guide_seq = 25)
UPDATE guide SET 
    title = '[DevOps] GitHub Actions & Docker 기반 CI/CD 파이프라인 구축 가이드',
    content = '# 🚀 GitHub Actions & Docker 기반 CI/CD 파이프라인 구축 가이드

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
```',
    updated_at = CURRENT_TIMESTAMP
WHERE guide_seq = 25;

-- 2. REST와 GraphQL 비교 (guide_seq = 26)
UPDATE guide SET 
    title = '[API 아키텍처] REST API vs GraphQL 실무 비교 및 설계 가이드',
    content = '# 🌐 REST API vs GraphQL 실무 비교 및 설계 가이드

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
```',
    updated_at = CURRENT_TIMESTAMP
WHERE guide_seq = 26;

-- 3. 관계형 DB와 NoSQL (guide_seq = 27)
UPDATE guide SET 
    title = '[Database] RDBMS (PostgreSQL) vs NoSQL (MongoDB/Redis) 선택 가이드',
    content = '# 🗄️ RDBMS (PostgreSQL) vs NoSQL 선택 및 데이터 모델링 가이드

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
```',
    updated_at = CURRENT_TIMESTAMP
WHERE guide_seq = 27;

-- 4. 캐싱과 Redis (guide_seq = 28)
UPDATE guide SET 
    title = '[Performance] Redis 캐싱 전략 및 Spring Boot Caching 가이드',
    content = '# ⚡ Redis 캐싱 전략 및 Spring Boot Caching 가이드

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
```',
    updated_at = CURRENT_TIMESTAMP
WHERE guide_seq = 28;

-- 5. 메시지 큐 (Kafka와 RabbitMQ) (guide_seq = 29)
UPDATE guide SET 
    title = '[Architecture] Event-Driven 메시지 큐 (Kafka vs RabbitMQ) 비교 가이드',
    content = '# 📩 Event-Driven 메시지 큐 (Kafka vs RabbitMQ) 비교 가이드

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
```',
    updated_at = CURRENT_TIMESTAMP
WHERE guide_seq = 29;

-- 신규 필수 가이드 1: 리눅스 명령어 가이드 (INSERT)
INSERT INTO guide (user_seq, title, content, is_public) VALUES
(1, '[Linux] 실무 필수 리눅스(Linux) 시스템 & 서버 관리 명령어 가이드', 
'# 🐧 실무 필수 리눅스(Linux) 시스템 & 서버 관리 명령어 가이드

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
', true);

-- 신규 필수 가이드 2: Git 명령어 가이드 (INSERT)
INSERT INTO guide (user_seq, title, content, is_public) VALUES
(1, '[Git] 실무 필수 Git 브랜치 전략 & 고급 명령어 가이드', 
'# 🌿 실무 필수 Git 브랜치 전략 & 고급 명령어 가이드

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
', true);
