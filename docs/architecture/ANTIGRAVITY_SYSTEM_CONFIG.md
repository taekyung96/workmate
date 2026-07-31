# [Antigravity AI Agent System] 전체 환경설정 및 아키텍처 명세서 🚀

## 1. 개요 (Overview)
본 명세서는 현재 가동 중인 **Google DeepMind Antigravity AI Agent System**의 정체성, 전역 사용자 규칙, 시스템 프롬프트, 도구(Tools) 및 스킬(Skills) 권한, 워크스페이스 세팅 상태를 통합적으로 정리한 공식 아키텍처 문서입니다.

---

## 2. 기본 정체성 & 메타데이터 (Identity & Metadata)

| 항목 | 상세 설정 내용 |
| :--- | :--- |
| **에이전트 이름** | **Antigravity (AGY)** |
| **개발 팀** | Google DeepMind Advanced Agentic Coding Team |
| **운영체제 (OS)** | Windows (PowerShell 기반 Shell) |
| **활성 워크스페이스** | `C:\ClaudeCode\workmate-v3-ws\workmate-v3` (Corpus: `teakyung96/workmate-v3`) |
| **App Data Directory** | `C:\Users\tkhwang\.gemini\antigravity-cli` |
| **Conversation ID** | `974366df-ee07-4ed9-9573-996a51818849` |
| **아티팩트 저장소** | `C:\Users\tkhwang\.gemini\antigravity-cli\brain\974366df-ee07-4ed9-9573-996a51818849` |

---

## 3. 글로벌 사용자 규칙 (Global User Rules) 🌐

사용자가 정의한 **최우선 준수 규칙**으로, 모든 대화 및 코드 작성 시 강제 적용됩니다.

1. **소통 언어**: 100% 자연스러운 **한국어** 진행 (작업 목표, 과정, 에러, 보고 포함).
2. **전문 용어 정의**: 어려운 전문 용어 등장 시 바로 옆에 **한 줄 풀이** 작성.
3. **코드 검토 전제**: 코드를 수정하기 전에 반드시 `view_file` 도구로 관련 파일 문맥 선조회.
4. **사전 방향 확인**: 대규모 코드 수정 및 DB 쓰기(`INSERT/UPDATE/DELETE/ALTER`) 실행 전 **사전 사용자 승인** 거침.
5. **실패 정지 규칙**: 동일한 문제로 3회 이상 실패 시 스스로 반복하지 않고 **작업 멈춤 후 사용자에게 확인 요청**.
6. **코딩 컨벤션**:
   - 들여쓰기 기본 **스페이스 4칸** (`.prettierrc` 우선).
   - Java/Spring: PascalCase 클래스, camelCase 메서드, `@Slf4j` 사용, BCrypt 암호화.
   - Vue3/TS: Composition API (`<script setup lang="ts">`) 전용 작성.
   - DB: 소문자 `snake_case`, 테이블명 단수형 (`user`, `receipt`), PII 마스킹.

---

## 4. 보유 도구 (Declared Tools) 🛠️

에이전트에 탑재되어 자율적으로 호출 가능한 시스템 도구 라이브러리입니다.

| 도구 분류 | 도구 명 (Tool Name) | 주요 역할 및 기능 |
| :--- | :--- | :--- |
| **파일 조작** | `view_file`, `write_to_file`, `replace_file_content`, `multi_replace_file_content`, `list_dir` | 로컬 파일 조회, 생성, 단일/다중 리플레이스 편집, 디렉토리 탐색 |
| **코드 검색** | `grep_search` | Ripgrep 기반의 고속 문자열/정규식 소스 코드 검색 |
| **명령어 실행** | `run_command`, `manage_task` | PowerShell 명령어 비동기 실행 및 백그라운드 프로세스(Task) 모니터링 |
| **서브에이전트** | `invoke_subagent`, `define_subagent`, `manage_subagents`, `send_message` | 병렬 하위 에이전트(Research, Self 등) 정의, 호출 및 에이전트 간 통신 |
| **스케줄러** | `schedule` | 일회성 타이머 또는 Cron 정기 백그라운드 알림 스케줄링 |
| **웹 & 이미지** | `search_web`, `read_url_content`, `generate_image` | 구글 웹 검색, HTTP 문서 텍스트 변환, AI 이미지 렌더링 |
| **인터랙션** | `ask_question`, `ask_permission`, `list_permissions` | 사용자 다중 선택 설문, 권한 요청 및 권한 상태 조회 |

---

## 5. 탑재된 스킬 라이브러리 (Available Skills) 📚

특정 전문가 역할이 필요할 때 불러와 사용할 수 있는 시스템 및 커스텀 스킬 목록입니다.

| 스킬 명 (Skill Name) | 경로 | 핵심 역할 |
| :--- | :--- | :--- |
| **`spring-vue-scaffold`** | `...\builtin\skills\spring-vue-scaffold` | Spring Boot + Vue 3 뼈대 프로젝트 대화형 자동 구축 스킬 |
| **`kordoc`** | `...\builtin\skills\kordoc` | HWP / HWPX 한글 공문서, 보고서 작성 및 마크다운 변환 스킬 |
| **`accessibility`** | `...\builtin\skills\accessibility` | WCAG 2.2 기준 웹 접근성 및 스크린 리더 심사 스킬 |
| **`best-practices`** | `...\builtin\skills\best-practices` | 최신 웹 개발 보안, 코드 품질 및 모범 사례 시큐리티 심사 |
| **`core-web-vitals`** | `...\builtin\skills\core-web-vitals` | LCP, INP, CLS 성능 측정 및 웹 바이탈 최적화 |
| **`performance`** | `...\builtin\skills\performance` | 웹 애플리케이션 로딩 및 실행 성능 감사 |
| **`seo`** | `...\builtin\skills\seo` | 검색엔진 최적화, 메타 태그, 구조화 데이터 최적화 |
| **`web-quality-audit`** | `...\builtin\skills\web-quality-audit` | 웹 종합 품질(성능, 접근성, SEO 등) 통합 오디트 |
| **`test-generator`** | `...\builtin\skills\test-generator` | 단위/통합/E2E 테스트 코드 자동으로 작성 스킬 |
| **`antigravity-guide`** | `...\builtin\skills\antigravity_guide` | Antigravity CLI/IDE 전체 가이드 및 사용자 명령어 가이드 |
| **`docs-guide-knowledge`**| `...\builtin\skills\docs-guide` | llms.txt 및 공식 문서 탐색 및 지식 가져오기 |

---

## 6. 사용 권장 슬래시 커맨드 (Slash Commands) 💬

채팅창에 `/`로 입력하여 실행할 수 있는 유용한 단축 명령어입니다.

- **/goal**: 장시간(오버나이트 등) 철저한 목표 달성을 지시할 때 추천.
- **/plan**: 복잡한 대형 작업 시작 전 단계별 설계 수립을 지시할 때 추천.
- **/grill-me**: 인터뷰 방식으로 아키텍처 및 요구사항을 꼼꼼히 정리할 때 추천.
- **/schedule**: 백그라운드 타이머 또는 정기 스케줄링 등록 시 추천.
- **/learn**: 에이전트에게 새로운 피드백이나 교정한 패턴을 학습시킬 때 추천.
