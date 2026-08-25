# 🐳 WSL2 & Docker Engine 기반 PostgreSQL (pgvector) DB 환경 구축 가이드

- **상태**: 작성 완료
- **상위 문서**: [01_ARCHITECTURE.md](01_ARCHITECTURE.md) · [HANDOVER.md](../project/HANDOVER.md)

이 문서는 **WSL2(Ubuntu) 환경에서 Docker Desktop 없이 Docker Engine을 직접 설치하여 PostgreSQL 17 + pgvector DB 컨테이너를 구동하고 초기화하는 절차**를 상세히 다룹니다.

---

## 1. 🐧 WSL2 우분투 내 Docker Engine 직접 설치

Docker Desktop의 묵직함이나 라이선스 제약 없이, WSL2 Ubuntu 내부에서 가볍게 Docker 데몬을 직접 구동하는 방법입니다.

### 1.1 구버전 삭제 및 필수 패키지 설치 (WSL2 터미널)

```bash
# 기존 패키지 정리
sudo apt-get remove docker docker-engine docker.io containerd runc

# 필수 패키지 설치
sudo apt-get update
sudo apt-get install -y \
    ca-certificates \
    curl \
    gnupg \
    lsb-release
```

### 1.2 Docker 공식 GPG 키 및 저장소 추가

```bash
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
```

### 1.3 Docker Engine 및 Docker Compose Plugin 설치

```bash
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 현재 사용자를 docker 그룹에 추가 (sudo 없이 docker 명령 실행)
sudo usermod -aG docker $USER
```

### 1.4 Docker 데몬 실행 및 확인

```bash
# Docker 서비스 시작
sudo service docker start

# Docker 동작 확인
docker version
```

---

## 2. 🗄️ Workmate DB (PostgreSQL 17 + pgvector) 구동

### 2.1 환경변수 파일 (`.env`) 작성

프로젝트 루트(`C:\ClaudeCode\workmate-v3-ws\workmate-v3\.env`) 경로에 실제 환경변수를 작성합니다.

```env
POSTGRES_DB=workmate_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=workmate_secret!
GEMINI_API_KEY=your_gemini_api_key_here
```

### 2.2 Docker Compose를 통한 DB 컨테이너 독립 실행

로컬 IDE(IntelliJ / VS Code) 개발 시에는 WAS/WEB을 제외하고 **DB만 실행**합니다.

```bash
# 프로젝트 루트로 이동하여 DB 컨테이너 실행
docker compose up -d db
```

> **Note**: `docker-compose.yml` 내 `volumes` 설정(`- ./db/init:/docker-entrypoint-initdb.d`)에 의해 최초 컨테이너 생성 시 `db/init/` 하위의 `01~04-schema.sql` 파일들이 자동 실행됩니다.

### 2.3 DB 헬스체크 및 로그 확인

```bash
# 컨테이너 상태 확인 (STATUS가 healthy 여야 함)
docker ps

# DB 실행 및 스키마 초기화 로그 확인
docker logs -f workmate-db
```

---

## 3. 🧪 DB 접속 및 pgvector 익스텐션 가동 검증

### 3.1 psql을 통한 컨테이너 직접 접속

```bash
docker exec -it workmate-db psql -U postgres -d workmate_db
```

### 3.2 pgvector 설치 및 스키마 검증 쿼리

```sql
-- pgvector 익스텐션 활성화 상태 확인
SELECT * FROM pg_extension WHERE extname = 'vector';

-- vector_store 테이블 확인 (768차원 임베딩 컬럼 확인)
\d vector_store;

-- 쿼리 종료
\q
```

---

## 4. 🔧 자주 발생하는 트러블슈팅 (Troubleshooting)

### 4.1 WSL2 5432 포트 충돌 문제 (`bind: address already in use`)
- **원인**: Windows 호스트 또는 WSL2 내부에 이미 로컬 PostgreSQL 서비스가 실행 중인 경우
- **해결**:
  - WSL2 내부: `sudo service postgresql stop`
  - Windows 호스트: `netstat -ano | findstr 5432` 확인 후 PID 프로세스 종료

### 4.2 WSL2 재부팅 시 Docker 데몬 미실행
- **원인**: WSL2는 기본적으로 부팅 시 `service` 데몬을 자동 구동하지 않음
- **해결**: WSL2 `~/.bashrc` 또는 `~/.zshrc` 하단에 아래 구문 추가
  ```bash
  if ! wsl.exe -l -v | grep -q "Running"; then :; fi
  if ! pgrep -x "dockerd" > /dev/null; then
      sudo service docker start > /dev/null 2>&1
  fi
  ```

---

## 🔗 관련 문서

- 🏗️ [01_ARCHITECTURE.md](01_ARCHITECTURE.md) — 전체 시스템 모듈 구조
- 📌 [HANDOVER.md](../project/HANDOVER.md) — 개발 단계 셋업 체크리스트
