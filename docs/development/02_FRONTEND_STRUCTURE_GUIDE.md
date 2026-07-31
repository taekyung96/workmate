# 02. 프론트엔드 구조 가이드 (Vue3 SPA)

- **상태**: 확정 (구현 표준)
- **결정 근거**: [ADR-0002](../project/adr/0002-frontend-structure-and-ui.md)

이 문서는 workmate-vue(Vue3 SPA)의 **디렉토리 구조·계층·모듈 규칙**을 정규 표준으로 못 박는다.
백엔드의 `Controller > Service > ServiceImpl > DAO` 처럼, 프론트도 지켜야 할 계층이 있다.

---

## 1. 백엔드 계층 ↔ 프론트 계층 매핑

익숙한 백엔드 계층에 1:1로 대응시켜 이해한다.

| 백엔드 (익숙)         | 역할          | Vue3 SPA 대응           | 폴더                |
| --------------------- | ------------- | ----------------------- | ------------------- |
| Controller            | 요청 진입점   | **View(화면) + Router** | `views/`, `router/` |
| Service / ServiceImpl | 비즈니스 로직 | **composable(`use~`)**  | `composables/`      |
| (세션/공유 상태)      | 전역 상태     | **Pinia Store**         | `stores/`           |
| DAO / Repository      | 데이터 접근   | **api 계층(HTTP)**      | `api/`              |
| VO / DTO / Entity     | 데이터 구조   | **TypeScript 타입**     | `types.ts`          |

**계층 의존 방향 (백엔드의 "Controller는 DAO 직접호출 금지"와 동일)**

```
View  →  composable / store  →  api  →  (HTTP) 얇은 WEB → WAS
(Controller)   (Service)      (DAO)
```

- **View는 얇게** — composable/store만 호출, `api` 직접 호출 지양.
- **api 계층은 HTTP만** — 비즈니스 로직 넣지 않음.
- **components는 props/emit로만** 소통 — 재사용성 위해 api 직접 호출 지양.

> 용어: Vue 공식 라우트 컴포넌트 명칭은 `views/`(Nuxt는 `pages/`). 본 프로젝트는 **`views/`** 로 통일.

---

## 2. 디렉토리 구조 (기능별 모듈 + 공통 모듈)

```
workmate-vue/src/
├── main.ts                    앱 부트 (Vue 생성 + router·pinia 등록)
├── App.vue                    루트 셸 (레이아웃 진입)
├── router/
│   ├── index.ts               각 모듈의 routes 취합
│   └── guards.ts              전역 인증가드·권한가드
│
├── common/                    🌐 공통 모듈 — 어디서나 재사용 (특정 기능 비소속)
│   ├── components/
│   │   ├── ui/                shadcn-vue 컴포넌트 배치 (components.json 경로 설정)
│   │   ├── feedback/          안내창·확인창·토스트·로딩
│   │   ├── data/              페이지네이션·테이블·빈상태
│   │   ├── form/              입력·셀렉트·파일업로드
│   │   └── layout/            AppLayout·AuthLayout·모달·탭
│   ├── composables/           useDialog·usePagination·useToast
│   ├── api/client.ts          HTTP 인스턴스 + 401 인터셉터 (모든 모듈 공유)
│   ├── utils/                 포맷터(날짜·금액·사업자번호)·상수
│   └── types/                 공통 타입 (ApiResponse 등)
│
├── modules/                   📦 기능별 콜로케이션
│   ├── auth/
│   │   ├── views/             LoginPage.vue · SignupPage.vue   (≈ Controller)
│   │   ├── composables/       useAuth.ts                        (≈ Service)
│   │   ├── stores/            auth.store.ts                     (전역상태, auth 소유)
│   │   ├── api/               auth.api.ts                       (≈ DAO)
│   │   ├── routes.ts          이 모듈의 라우트 정의
│   │   └── types.ts           (≈ VO/DTO)
│   ├── chat/                  views · composables/useChatStream · stores · api · routes · types
│   ├── receipt/              (분석/이력 탭, 이미지 업로드)
│   ├── guide/                (CRUD + RAG 출처표시)
│   └── admin/                (사용자관리 · 감사로그)
│
├── assets/                    이미지·폰트
└── styles/                    전역 스타일 (Tailwind 엔트리 등)
```

---

## 3. 모듈 규칙 (좋은 습관 = 이 4가지)

1. **모듈 안에서도 계층 유지** — `view → composable/store → api` 흐름을 모듈 내부에서도 지킨다.
2. **진짜 전역은 `common/`으로** — HTTP client·공통 버튼·레이아웃은 특정 기능 소유가 아님. 억지로 한 모듈에 넣지 않는다.
3. **모듈 경계 존중 (제일 중요)** — 모듈 A가 모듈 B의 내부를 깊게 import하지 않는다. 공유가 필요하면 `common/`으로 승격.
4. **라우트도 모듈이 소유** — 각 모듈이 `routes.ts`를 내보내고 `router/index.ts`가 취합. 기능 추가 = 모듈 폴더 하나 추가.

---

## 4. 라우팅 & 레이아웃

- **AuthLayout** — 헤더·사이드바 없는 단독 화면: `/login`, `/signup`
- **AppLayout** — 헤더 + 사이드바: `/chat`(기본 랜딩)·`/receipt`·`/guide`·`/admin/*`
- **전역 인증가드**(`router.beforeEach`): 세션 없이 보호화면 접근 → `/login`(원래 목적지 기억)
- **권한가드**: `/admin/**`는 `meta:{requiresAdmin:true}`로 관리자 role 확인
- **캐치올**: `/:pathMatch(.*)*` → `NotFound.vue`

---

## 5. 상태관리 (Pinia)

| 스토어       | 담는 것                        | 왜 전역                            |
| ------------ | ------------------------------ | ---------------------------------- |
| `auth.store` | 로그인 사용자·role·인증여부    | 헤더·가드·여러 화면 공유           |
| `chat.store` | 채팅 세션·메시지·스트리밍 상태 | SSE 수신 중 화면 이동해도 유지     |
| 그 외        | 영수증·가이드·관리자           | ❌ 전역 불필요 → 각 view 로컬 상태 |

> 원칙: 여러 화면이 공유하는 것만 store. 한 화면용 데이터를 전역에 넣지 않는다(과설계 회피).

---

## 6. 스타일링 (Tailwind v4 + shadcn-vue)

- **Tailwind CSS v4** (`@tailwindcss/vite`) — 유틸리티 클래스 기반. 순수 CSS·인라인 style 지양.
- **shadcn-vue** (Reka UI 기반, 구 radix-vue) — 컴포넌트 소스를 복사해 **내가 소유**. 접근성(키보드·스크린리더) 내장.
- **설치**: `npx shadcn-vue@latest init` → `components.json`의 경로 alias를 **`common/components/ui`** 로 설정 → `add button dialog ...`
- **디자인 토큰**: 기존 디자인 가이드의 컬러·타이포를 **shadcn-vue 테마(CSS 변수)로 매핑**한다. → [디자인 시스템](../design/00_DESIGN_SYSTEM.md)
- **흐름 예시 (로그인)**:
    ```
    LoginPage.vue → useAuth().login() → authApi.login() → POST /api/auth/login → (WEB) → WAS
         ①클릭           ②로직            ③HTTP요청
    ④성공 → auth.store에 사용자 저장 → ⑤router.push('/chat')
    ```
