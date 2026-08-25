-- =============================================================
-- 데모 데이터 시드 (README 스크린샷 재현용)
--
-- README 의 화면 캡처는 목(mock)이 아니라 이 시드가 들어간 DB 를 실제로 찍은 것이다.
-- 가이드 문서·AI 응답·STT 요약·영수증 인식 결과는 모두 앱이 실제로 생성한 원본이다.
--
-- [주의] 이메일·전화번호는 .env 의 AES 키로 암호화된 값이라, 키가 다른 환경에서는
--        복호화·로그인이 되지 않는다. 그런 경우 계정만 앱의 회원가입으로 새로 만들고
--        (scripts/seed-demo-accounts.js) 아래 콘텐츠 INSERT 를 쓰면 된다.
--
-- 데모 로그인: demo.admin@example.com / Workmate!2026  (ROLE_ADMIN)
--
-- 기존 볼륨 환경에 수동 적용:
--   docker exec -i workmate-db psql -U workmate -d workmate_db < db/init/13-seed-demo-data.sql
-- =============================================================

-- 데모 계정 3종 (이메일·전화번호는 앱이 AES 암호화한 값, 비밀번호는 BCrypt 해시)
INSERT INTO admin_user (email, password, user_name, phone, role, login_fail_count, use_yn, created_at)
SELECT $wm$xR+UiqFJm3Zh51mjrCxuexTBixamx2JtRn4QzsN5P3o=$wm$, $wm$$2a$10$9Vfw6SMeojcOZfrUbH4QkeraxgsbuB1yY7op38kNjby2SZXXKdltm$wm$, $wm$관리자 (데모)$wm$, $wm$KwHxJTo9iTEyqRKuhmEbHA==$wm$, $wm$ROLE_ADMIN$wm$, 0, true, TIMESTAMP '2026-08-25 03:06:08'
WHERE NOT EXISTS (SELECT 1 FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$);
INSERT INTO admin_user (email, password, user_name, phone, role, login_fail_count, use_yn, created_at)
SELECT $wm$06oNEuB4v1ZJ7mabHIBvp0adl0XA7N+c8b+ldYGidd0=$wm$, $wm$$2a$10$WQY3iym9t9aZj1.WtytT8.AnDB9NZwR5BU4Q9RqLoZzcHZoc3QwKe$wm$, $wm$홍길동 (데모)$wm$, $wm$eUAGlwIanpGDpMYvE8WQxg==$wm$, $wm$ROLE_USER$wm$, 0, true, TIMESTAMP '2026-08-25 03:06:08'
WHERE NOT EXISTS (SELECT 1 FROM admin_user WHERE user_name = $wm$홍길동 (데모)$wm$);
INSERT INTO admin_user (email, password, user_name, phone, role, login_fail_count, use_yn, created_at)
SELECT $wm$tXqE/fWsgIqW2xdZ05s7Wg==$wm$, $wm$$2a$10$NkbUPahdMzt/wsyEKIcf1e0vW.lESgWkESqK7JePHASUWVbfJVXQm$wm$, $wm$김서연 (데모)$wm$, $wm$La+xF8tLbDq5iq58HK1tZg==$wm$, $wm$ROLE_USER$wm$, 0, true, TIMESTAMP '2026-08-25 03:06:08'
WHERE NOT EXISTS (SELECT 1 FROM admin_user WHERE user_name = $wm$김서연 (데모)$wm$);

-- 채팅 이력 — 실제 Gemini 응답 원문
WITH new_room AS (
    INSERT INTO chat_room (user_seq, title, use_yn, created_at)
    SELECT (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$), $wm$RAG에 대해서 설명해줘$wm$, true, TIMESTAMP '2026-08-05 01:41:55'
    WHERE NOT EXISTS (SELECT 1 FROM chat_room WHERE user_seq = (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$) AND title = $wm$RAG에 대해서 설명해줘$wm$)
    RETURNING room_seq
)
INSERT INTO chat_message (room_seq, role, content, model_name, created_at)
SELECT new_room.room_seq, v.role, v.content, v.model_name, v.created_at
FROM new_room, (VALUES
        ($wm$user$wm$, $wm$RAG에 대해서 설명해줘$wm$, NULL, TIMESTAMP '2026-08-05 01:41:55'),
        ($wm$assistant$wm$, $wm$**RAG (Retrieval-Augmented Generation, 검색 증강 생성)**는 거대언어모델(LLM)의 정보 한계와 환각(Hallucination) 현상을 극복하기 위한 기법입니다. 

외부 지식 데이터베이스에서 질문과 연관된 근거 문서를 먼저 **검색(Retrieval)**한 뒤, 이를 LLM의 프롬프트에 **합성(Augmentation)**하여 근거 있는 정확한 답변을 **생성(Generation)**합니다.

---

### 🔄 RAG의 4단계 파이프라인 동작 원리

```
[ 1. 문서 수집 & 청크 분할 ] ➔ [ 2. 텍스트 임베딩 & Vector DB 저장 ]
                                           │
[ 4. 프롬프트 합성 & LLM 답변 ] ◄── [ 3. 사용자 질문 유사도 검색 ]
```

1. **청크 분할 (Chunking)**
   * 긴 마크다운, PDF 등의 문서를 500~1000자 단위의 의미 있는 조각(Chunk)으로 자릅니다.
   * 문맥 끊김을 방지하기 위해 일정 부분 중복 영역(Overlap)을 둡니다.
2. **임베딩 및 저장 (Embedding & Storage)**
   * 임베딩 모델(예: `gemini-embedding-001`)을 사용해 텍스트 청크를 고차원 수치 벡터(예: 768차원)로 변환한 뒤 Vector DB(예: PostgreSQL `pgvector`)에 저장합니다.
3. **유사도 검색 (Similarity Search)**
   * 사용자 질문도 동일한 임베딩 모델로 벡터화한 뒤, Vector DB에서 **코사인 유사도(Cosine Similarity)** 등이 높은 상위(Top-K) 관련 청크를 검색합니다.
4. **프롬프트 합성 및 답변 생성 (Augment & Generate)**
   * 검색된 근거 청크들을 LLM 시스템 프롬프트의 "참고 자료"로 주입하여 환각을 방지하고 출처와 함께 정확한 응답을 생성합니다.

---

### 💡 RAG vs 파인튜닝(Fine-tuning) 비교

| 구분 | RAG (검색 증강 생성) | 파인튜닝 (Fine-tuning) |
| :--- | :--- | :--- |
| **방식** | 모델 가중치는 유지하고 외부 지식을 검색해 주입 | 추가 데이터로 모델의 가중치 자체를 재학습 |
| **지식 갱신** | 문서 교체만으로 즉시 반영 | 데이터 재학습 필요 (비용 및 시간 발생) |
| **강점** | 정확한 출처 제시 가능, 자주 바뀌는 문서에 유용 | 특정 말투, 출력 형식, 특수 작업 능력 내재화 |
| **주요 용도** | 사내 문서 검색, FAQ, 최신 정보 기반 Q&A | 고정된 형식 출력, 도메인 전용 말투 학습 |

> 💡 **실무 팁**: 실무에서는 형식이나 작업 숙련도는 파인튜닝으로 잡고, 최신 정보 및 지식 검색은 RAG를 활용하는 방식으로 두 기법을 병행하기도 합니다.

---

### 🛠️ RAG 최적화 전략
* **유사도 임계값(Threshold) 설정**: 유사도가 기준치(예: 0.7) 미만인 저품질 청크는 검색 결과에서 제외하여 환각을 방지합니다.
* **의미 기반 검색**: 키워드 완전 일치가 아닌 의미 공간 상의 거리를 계산하므로, "휴가"라는 단어로 "연차" 관련 규정을 찾아내는 등 유연한 검색이 가능합니다.$wm$, $wm$gemini-flash-latest$wm$, TIMESTAMP '2026-08-05 01:42:03')
    ) AS v(role, content, model_name, created_at);

WITH new_room AS (
    INSERT INTO chat_room (user_seq, title, use_yn, created_at)
    SELECT (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$), $wm$너가 나한테 알려줄수있는게 뭐야$wm$, true, TIMESTAMP '2026-08-04 07:58:52'
    WHERE NOT EXISTS (SELECT 1 FROM chat_room WHERE user_seq = (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$) AND title = $wm$너가 나한테 알려줄수있는게 뭐야$wm$)
    RETURNING room_seq
)
INSERT INTO chat_message (room_seq, role, content, model_name, created_at)
SELECT new_room.room_seq, v.role, v.content, v.model_name, v.created_at
FROM new_room, (VALUES
        ($wm$user$wm$, $wm$너가 나한테 알려줄수있는게 뭐야$wm$, NULL, TIMESTAMP '2026-08-04 07:58:52'),
        ($wm$assistant$wm$, $wm$안녕하세요! 저는 여러분의 개발 및 업무 AI 비서 **Workmate**입니다. 

저는 IT 개발, 리눅스, 아키텍처, 업무 지원 및 AI 관련 지식에 대해 다양하게 도움을 드릴 수 있습니다. 대표적으로 다음과 같은 내용을 안내해 드릴 수 있어요.

---

### 1. IT 및 소프트웨어 개발
* **리눅스 명령어 및 스크립트**: 리눅스 서버 조작, 시스템 관리, 문제 해결 명령어 안내
* **아키텍처 및 시스템 설계**: 백엔드/프론트엔드 구조, 데이터베이스 설계, 웹/클라우드 시스템 아키텍처
* **코드 작성 및 리뷰**: 프로그래밍 관련 질문 답변, 코드 개선 및 버그 수정 가이드

### 2. AI · LLM 및 RAG 시스템
* **LLM 기초 개념**: 대규모 언어 모델의 원리, 트랜스포머/어텐션 메커니즘, 사전학습 및 미세조정(Fine-tuning)
* **프롬프트 엔지니어링**: 역할 부여, Few-shot, 생각의 사슬(CoT) 등 효과적인 지시문 작성법
* **할루시네이션(환각) & LLM 평가**: 환각 현상 완화 기법, LLM-as-judge, 평가셋 구축 및 성능 측정
* **RAG(검색 증강 생성) 아키텍처**: 키워드 검색(BM25)과 시맨틱 벡터 검색을 결합한 하이브리드 검색 등 RAG 파이프라인 구성법

### 3. 업무 지원 및 기타 기능
* **문서 작성 및 요약**: 요약, 기획서/보고서 작성 보조, 가이드 문서 검색
* **지출 및 영수증 확인**: 월별 영수증 결제 금액 및 건수 조회 기능 지원

---

궁금하신 내용이나 필요한 도움이 있다면 언제든지 편하게 질문해 주세요!$wm$, $wm$gemini-flash-latest$wm$, TIMESTAMP '2026-08-04 07:58:59'),
        ($wm$user$wm$, $wm$리액트 관련해서 질문해도 알려줄수있어?$wm$, NULL, TIMESTAMP '2026-08-04 07:59:18'),
        ($wm$assistant$wm$, $wm$네, 그럼요! **React(리액트)** 관련 질문도 얼마든지 편하게 물어보세요.

다음과 같은 다양한 리액트 관련 주제에 대해 답변해 드릴 수 있습니다.

* **기초 및 핵심 개념**: 컴포넌트 구조, Virtual DOM, JSX 작성법, Props 및 State 관리
* **React Hooks**: `useState`, `useEffect`, `useMemo`, `useCallback`, `useRef`, `useContext` 등 각종 Hook 활용법 및 커스텀 Hook 작성
* **상태 관리(State Management)**: Context API, Redux/Redux Toolkit, Zustand, Recoil, TanStack Query(React Query) 등
* **TypeScript 연동**: React 컴포넌트 및 Hook의 타입 정의, 이벤트 객체 타입 지정 등
* **성능 최적화**: Re-rendering(재렌더링) 방지, `React.memo`, Lazy Loading 및 코드 분할(Code Splitting)
* **라우팅 및 프레임워크**: React Router 활용, Next.js(App Router/Pages Router) 개념 및 코드 작성

궁금하신 리액트 코드나 개념, 문제 상황(에러 메시지 등)이 있다면 언제든 편하게 질문해 주세요!$wm$, $wm$gemini-flash-latest$wm$, TIMESTAMP '2026-08-04 08:04:54'),
        ($wm$user$wm$, $wm$컴포넌트 구조에 대해서 설명해봐$wm$, NULL, TIMESTAMP '2026-08-04 08:05:16'),
        ($wm$assistant$wm$, $wm$리액트(React)에서 **컴포넌트 구조**는 크게 **① 단일 컴포넌트 내부의 코드 구조**와 **② 프로젝트 전체에서의 컴포넌트/디렉토리 구성 구조** 두 가지 관점으로 나누어 설명할 수 있습니다.

---

## 1. 단일 컴포넌트 내부 코드 구조

리액트 컴포넌트(함수형 컴포넌트 기준)는 일반적으로 다음과 같은 **5단계 흐름**으로 작성됩니다.

```tsx
// 1. Import 영역: 리액트 훅, 타 컴포넌트, 스타일, 유틸 함수 등 불러오기
import React, { useState, useEffect } from 'react';
import UserProfile from './UserProfile';
import { fetchUserData } from '../services/api';
import './UserContainer.css';

// TypeScript 사용 시 Props 타입 정의
interface UserContainerProps {
  userId: string;
}

// 2. 컴포넌트 정의
export default function UserContainer({ userId }: UserContainerProps) {
  // 3. 상태 관리 및 훅(Hooks) 영역
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // 4. 이벤트 핸들러 및 비즈니스 로직 영역
  useEffect(() => {
    async function loadData() {
      const data = await fetchUserData(userId);
      setUser(data);
      setLoading(false);
    }
    loadData();
  }, [userId]);

  const handleRefresh = () => {
    setLoading(true);
    // 재로딩 로직...
  };

  // 5. Render (JSX) 영역: 조건부 렌더링 포함
  if (loading) return <div>로딩 중...</div>;
  if (!user) return <div>유저 정보를 찾을 수 없습니다.</div>;

  return (
    <div className="user-container">
      <h2>사용자 프로필</h2>
      <UserProfile user={user} />
      <button onClick={handleRefresh}>새로고침</button>
    </div>
  );
}
```

---

## 2. 컴포넌트 설계 핵심 원칙

좋은 컴포넌트 구조를 만들기 위해서는 다음과 같은 원칙을 적용하는 것이 좋습니다.

1. **단일 책임 원칙 (Single Responsibility Principle)**
   * 하나의 컴포넌트는 **한 가지 일(또는 한 가지 역할)**만 담당해야 합니다.
   * 컴포넌트가 너무 커지고 로직이 복잡해지면 여러 개의 하위 컴포넌트로 분리하세요.

2. **비즈니스 로직과 UI 표현의 분리 (Custom Hook 활용)**
   * 데이터 요청, 상태 변환 등 복잡한 로직은 **커스텀 훅(Custom Hook)**으로 추출하여 컴포넌트를 가볍고 읽기 쉽게 유지합니다.

3. **재사용성 (Reusability) 및 Props 설계**
   * 버튼, 입력창, 모달과 같은 UI 요소는 `props`를 통해 데이터와 동작을 전달받아 어디서든 재사용할 수 있도록 설계합니다.

---

## 3. 프로젝트 전체의 컴포넌트 폴더 구조

프로젝트 규모가 커질수록 컴포넌트를 어떻게 배치하고 관리하는지가 중요해집니다. 대표적인 디렉토리 구조 2가지를 소개합니다.

### A. 역할/기능 중심 구조 (Feature-based Structure) — *추천*
기능(Feature) 단위로 폴더를 묶어 관련 컴포넌트, 훅, 스타일을 모아두는 방식입니다.

```text
src/
├── components/          # 전역 공통 컴포넌트 (Button, Header, Modal 등)
│   ├── Button/
│   │   ├── Button.tsx
│   │   └── Button.module.css
│   └── Header/
├── features/            # 기능(도메인)별 컴포넌트 및 로직
│   ├── auth/            # 인증 관련 (Login, Signup)
│   │   ├── components/
│   │   ├── hooks/
│   │   └── api/
│   └── dashboard/       # 대시보드 관련
│       ├── components/
│       └── hooks/
├── pages/               # 라우팅 페이지 컴포넌트
└── hooks/               # 전역 커스텀 훅
```

### B. 아토믹 디자인 (Atomic Design Structure)
컴포넌트를 크기와 역할에 따라 5단계(Atoms → Molecules → Organisms → Templates → Pages)로 나누어 관리합니다.

* **Atoms (원자)**: 가장 작은 UI 단위 (Button, Input, Icon)
* **Molecules (분자)**: 원자들이 결합된 단위 (SearchBar = Input + Button)
* **Organisms (유기체)**: 복잡한 독립적 영역 (Header, UserCardList)
* **Templates / Pages**: 전체 레이아웃 및 최종 뷰

---

요약하자면, 리액트 컴포넌트 구조는 **"가독성 높고, 재사용하기 쉬우며, 로직과 UI가 명확히 분리된 상태"**를 목표로 설계하는 것이 핵심입니다.

현재 작성 중이신 컴포넌트 구조나 고민 중인 폴더 설계 방식이 있으시다면 코드를 보여주세요! 개선점을 함께 살펴보겠습니다.$wm$, $wm$gemini-flash-latest$wm$, TIMESTAMP '2026-08-04 08:08:39')
    ) AS v(role, content, model_name, created_at);

WITH new_room AS (
    INSERT INTO chat_room (user_seq, title, use_yn, created_at)
    SELECT (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$), $wm$리눅스 8081 포트 찾는방법 알려줘$wm$, true, TIMESTAMP '2026-08-05 01:41:34'
    WHERE NOT EXISTS (SELECT 1 FROM chat_room WHERE user_seq = (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$) AND title = $wm$리눅스 8081 포트 찾는방법 알려줘$wm$)
    RETURNING room_seq
)
INSERT INTO chat_message (room_seq, role, content, model_name, created_at)
SELECT new_room.room_seq, v.role, v.content, v.model_name, v.created_at
FROM new_room, (VALUES
        ($wm$user$wm$, $wm$리눅스 8081 포트 찾는방법 알려줘$wm$, NULL, TIMESTAMP '2026-08-05 01:41:34'),
        ($wm$assistant$wm$, $wm$리눅스에서 **8081 포트**를 사용 중인 프로세스를 확인하는 대표적인 방법들을 안내해 드립니다.

---

### 1. `lsof` 명령어 이용 (가장 명확하고 추천)
포트를 점유하고 있는 프로세스의 PID와 실행 명령어를 바로 확인할 수 있습니다.

```bash
sudo lsof -i :8081
```

---

### 2. `netstat` 명령어 이용
네트워크 연결 상태와 함께 8081 포트를 사용 중인 프로세스(PID/Program name)를 확인합니다.

```bash
netstat -tulpn | grep 8081
```

---

### 3. `ss` 명령어 이용
최신 리눅스 환경에서 `netstat` 대신 권장되는 명령어입니다.

```bash
sudo ss -tulpn | grep 8081
```

---

### 💡 [참고] 해당 포트를 사용 중인 프로세스 종료하기
만약 8081 포트를 점유 중인 프로세스를 종료해야 한다면, 위 명령어로 확인한 **PID**를 이용해 다음과 같이 종료할 수 있습니다.

```bash
# 정상 종료 요청
sudo kill -15 [PID]

# 강제 종료 (정상 종료가 안 될 경우)
sudo kill -9 [PID]
```$wm$, $wm$gemini-flash-latest$wm$, TIMESTAMP '2026-08-05 01:41:41')
    ) AS v(role, content, model_name, created_at);

WITH new_room AS (
    INSERT INTO chat_room (user_seq, title, use_yn, created_at)
    SELECT (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$), $wm$Spring Boot Redis 캐싱 가이드$wm$, true, TIMESTAMP '2026-07-30 22:09:49'
    WHERE NOT EXISTS (SELECT 1 FROM chat_room WHERE user_seq = (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$) AND title = $wm$Spring Boot Redis 캐싱 가이드$wm$)
    RETURNING room_seq
)
INSERT INTO chat_message (room_seq, role, content, model_name, created_at)
SELECT new_room.room_seq, v.role, v.content, v.model_name, v.created_at
FROM new_room, (VALUES
        ($wm$user$wm$, $wm$Spring Boot에서 Redis 캐싱 적용 가이드를 정리해줘.$wm$, $wm$gemini-2.5-flash$wm$, TIMESTAMP '2026-07-30 22:09:49'),
        ($wm$assistant$wm$, $wm$Redis 캐싱 적용 가이드입니다.

```java
@Cacheable(value = "guides", key = "#guideSeq")
public GuideVo getGuide(Long guideSeq) {
    return guideRepository.findById(guideSeq);
}
```

- **Cache-Aside 패턴**: 데이터 요청 시 Redis 확인 후 DB 조회
- **TTL 주기**: 자주 조회되는 가이드 및 세션 부하 절감$wm$, $wm$gemini-2.5-flash$wm$, TIMESTAMP '2026-07-30 22:09:50')
    ) AS v(role, content, model_name, created_at);

-- 영수증 인식 이력 — 실제 멀티모달 OCR 결과
INSERT INTO receipt (user_seq, image_path, pay_amount, biz_no, pay_date, card_name, biz_no_valid, select_type, raw_json, created_at)
SELECT (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$), $wm$C:\ClaudeCode\workmate-v3-ws\workmate-v3\workmate-was\uploads\6c8e6ab5-8043-4ba4-a518-ab5d2747f116.png$wm$, 48100, $wm$2208162517$wm$, $wm$20260715$wm$, $wm$롯데법인카드$wm$, true, $wm$AUTO$wm$, $wm$[{"bizNo":"2208162517","payDate":"20260715","cardName":"롯데법인카드","payAmount":48100}]$wm$::jsonb, TIMESTAMP '2026-07-22 09:05:27'
WHERE NOT EXISTS (SELECT 1 FROM receipt WHERE user_seq = (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$) AND image_path = $wm$C:\ClaudeCode\workmate-v3-ws\workmate-v3\workmate-was\uploads\6c8e6ab5-8043-4ba4-a518-ab5d2747f116.png$wm$);
INSERT INTO receipt (user_seq, image_path, pay_amount, biz_no, pay_date, card_name, biz_no_valid, select_type, raw_json, created_at)
SELECT (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$), $wm$C:\ClaudeCode\workmate-v3-ws\workmate-v3\workmate-was\uploads\71d06fd4-731f-41de-9143-a6686588c373.jpg$wm$, 22000, $wm$6727600528$wm$, $wm$20260713$wm$, $wm$롯데법인카드$wm$, true, $wm$AUTO$wm$, $wm$[{"bizNo":"6727600528","payDate":"20260713","cardName":"롯데법인카드","payAmount":22000}]$wm$::jsonb, TIMESTAMP '2026-07-29 06:08:54'
WHERE NOT EXISTS (SELECT 1 FROM receipt WHERE user_seq = (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$) AND image_path = $wm$C:\ClaudeCode\workmate-v3-ws\workmate-v3\workmate-was\uploads\71d06fd4-731f-41de-9143-a6686588c373.jpg$wm$);
INSERT INTO receipt (user_seq, image_path, pay_amount, biz_no, pay_date, card_name, biz_no_valid, select_type, raw_json, created_at)
SELECT (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$), $wm$C:\ClaudeCode\workmate-v3-ws\workmate-v3\workmate-was\uploads\ebf1d7eb-8002-482a-a5c9-24ff4d35269d.png$wm$, 22000, $wm$6831602314$wm$, $wm$20260401$wm$, $wm$롯데 아멕스 블루 비즈니스 카드$wm$, true, $wm$MANUAL$wm$, $wm$[{"bizNo":"6831602314","payDate":"20260401","cardName":"롯데 아멕스 블루 비즈니스 카드","payAmount":22000}]$wm$::jsonb, TIMESTAMP '2026-07-30 02:00:10'
WHERE NOT EXISTS (SELECT 1 FROM receipt WHERE user_seq = (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$) AND image_path = $wm$C:\ClaudeCode\workmate-v3-ws\workmate-v3\workmate-was\uploads\ebf1d7eb-8002-482a-a5c9-24ff4d35269d.png$wm$);

-- 회의록 이력 — 실제 STT 전사문과 AI 3단 요약
INSERT INTO voice_record (user_seq, title, stt_text, summary_md, audio_file_name, origin_file_name, file_size, content_type, created_at)
SELECT (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$), $wm$Workmate v3 아키텍처 및 3-tier BFF 설계 회의$wm$, $wm$오늘 회의에서는 Workmate v3의 Vue3 SPA 전환과 얇은 BFF 계층 설계에 대해 논의하겠습니다. 백엔드는 Spring Boot 3와 Spring AI를 기반으로 구축되며, 프론트엔드는 shadcn-vue와 Tailwind CSS v4를 사용하여 단독 SPA로 동적 렌더링됩니다. 브라우저는 8080포트의 WEB BFF만 바라보며, DB 직접 접근은 금지되고 백엔드 WAS(:8081)로 프록시 호출합니다.$wm$, $wm$### 1. 주요 안건
- Vue3 SPA 전환 및 얇은 BFF 계층 설계 정립
- Spring AI 및 pgvector 기반 RAG 구조 도입

### 2. 결정 사항
- Session 기반 인증 (Spring Security, httpOnly 쿠키)
- 3-tier 보안 경계 유지 (브라우저 → 8080 BFF → 8081 WAS)

### 3. 향후 일정
- 프론트엔드 모듈화 구조 적용 및 통합 테스트 진행$wm$, $wm$meeting-test.wav$wm$, $wm$meeting-test.wav$wm$, 1923330, $wm$audio/wav$wm$, TIMESTAMP '2026-07-31 07:09:50'
WHERE NOT EXISTS (SELECT 1 FROM voice_record WHERE user_seq = (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$) AND title = $wm$Workmate v3 아키텍처 및 3-tier BFF 설계 회의$wm$);
INSERT INTO voice_record (user_seq, title, stt_text, summary_md, audio_file_name, origin_file_name, file_size, content_type, created_at)
SELECT (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$), $wm$Sprint 15 주간 기술 스크럼 및 RAG 성능 평가$wm$, $wm$Sprint 15 주간 회의입니다. 금주 진행된 RAG 파이프라인 성능 평가 결과 코사인 유사도 검색 정확도가 92%로 향상되었습니다. 영수증 OCR 자동 인식 모듈과 음성 회의록 STT 파이프라인 통합 작업도 성공적으로 마무리되었습니다.$wm$, $wm$### 1. 성과 공유
- RAG 검색 정확도 92% 달성
- 영수증 OCR 및 음성 STT 파이프라인 구축 완료

### 2. 논리적 이슈
- 대용량 임베딩 쿼터 모니터링 체계 도입 필요$wm$, $wm$weekly_scrum_20260729.mp3$wm$, $wm$weekly_scrum_20260729.mp3$wm$, 1420000, $wm$audio/mp3$wm$, TIMESTAMP '2026-07-31 07:09:50'
WHERE NOT EXISTS (SELECT 1 FROM voice_record WHERE user_seq = (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$) AND title = $wm$Sprint 15 주간 기술 스크럼 및 RAG 성능 평가$wm$);

-- 관리자 감사 로그 (append-only)
INSERT INTO admin_audit_log (admin_user_seq, target_user_seq, action, created_at)
SELECT (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$), (SELECT user_seq FROM admin_user WHERE user_name = $wm$홍길동 (데모)$wm$), 'UNLOCK', CURRENT_TIMESTAMP - interval '2 day'
WHERE NOT EXISTS (SELECT 1 FROM admin_audit_log WHERE admin_user_seq = (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$));
INSERT INTO admin_audit_log (admin_user_seq, target_user_seq, action, created_at)
SELECT (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$), (SELECT user_seq FROM admin_user WHERE user_name = $wm$김서연 (데모)$wm$), 'RESET_PASSWORD', CURRENT_TIMESTAMP - interval '1 day'
WHERE (SELECT count(*) FROM admin_audit_log WHERE admin_user_seq = (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$)) < 2;
INSERT INTO admin_audit_log (admin_user_seq, target_user_seq, action, created_at)
SELECT (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$), (SELECT user_seq FROM admin_user WHERE user_name = $wm$홍길동 (데모)$wm$), 'RESET_PASSWORD', CURRENT_TIMESTAMP - interval '3 hour'
WHERE (SELECT count(*) FROM admin_audit_log WHERE admin_user_seq = (SELECT user_seq FROM admin_user WHERE user_name = $wm$관리자 (데모)$wm$)) < 3;
