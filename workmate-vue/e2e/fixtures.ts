import { test as base, type Page, type Route } from '@playwright/test'

/**
 * E2E 공용 픽스처 — 백엔드를 브라우저 레벨에서 가로챈다.
 *
 * <p>서버를 띄우지 않는 이유는 playwright.config.ts 에 적어 두었다. 요약하면
 * 여기서 잡으려는 것이 레이아웃·라우팅이고, 실제 WAS 를 띄우면 CI 에 LLM API 키가 필요해지며
 * 무료 한도까지 소모하기 때문이다.</p>
 */

/** 서버 공통 응답 래퍼(ApiResponse)와 같은 모양 */
function ok(result: unknown) {
    return { success: true, message: 'success', result }
}

/** 로그인 사용자 — 데모 관리자와 같은 모양 */
export const ADMIN_USER = { userSeq: 12, userName: '관리자 (데모)', role: 'ROLE_ADMIN' }

/** 채팅 화면 모델 드롭다운이 받는 목록 — 실제 AI_MODEL 공통코드와 같은 순서 */
export const AI_MODELS = [
    { code: 'qwen/qwen3.8-27b', codeName: 'Groq · Qwen3.8 27B' },
    { code: 'gemini-flash-latest', codeName: 'Gemini Flash (latest)' },
]

/**
 * 목록 화면의 행 데이터.
 *
 * <p>빈 목록으로 두면 표가 아예 렌더되지 않아 <b>레이아웃을 잴 대상이 없다.</b>
 * 표가 좁은 화면에서 찌그러지는지 보려면 행이 실제로 있어야 한다
 * (e2e/table-scroll.spec.ts). 값은 실제 화면에서 가장 긴 축에 맞춰 골랐다 —
 * 짧은 값만 넣으면 줄바꿈이 안 일어나 결함을 재현하지 못한다.</p>
 */
const AUDIT_LOGS = [
    {
        auditSeq: 1,
        adminUserSeq: 1,
        adminUserName: '관리자 (데모)',
        targetUserSeq: 2,
        targetUserName: '홍길동 (데모)',
        action: 'RESET_PASSWORD',
        createdAt: '2026-08-25T00:07:00',
    },
    {
        auditSeq: 2,
        adminUserSeq: 1,
        adminUserName: '관리자 (데모)',
        targetUserSeq: 3,
        targetUserName: '김서연 (데모)',
        action: 'UNLOCK',
        createdAt: '2026-08-24T18:31:00',
    },
]

const RECEIPTS = [
    {
        receiptSeq: 1,
        userSeq: 1,
        imagePath: '/uploads/r1.jpg',
        payAmount: 48100,
        bizNo: '2208162517',
        payDate: '2026-07-15',
        cardName: '신한카드',
        bizNoValid: true,
        selectType: 'AUTO',
        rawJson: null,
    },
    {
        receiptSeq: 2,
        userSeq: 1,
        imagePath: '/uploads/r2.jpg',
        payAmount: 22000,
        bizNo: '6837602314',
        payDate: '2026-04-01',
        cardName: '국민카드',
        bizNoValid: true,
        selectType: 'MANUAL',
        rawJson: null,
    },
]

const VOICE_RECORDS = [
    {
        recordSeq: 1,
        title: '2026년 3분기 제품 로드맵 검토 회의',
        originFileName: 'roadmap-review-2026-q3.m4a',
        fileSize: 12_582_912,
        hasAudio: true,
        createdAt: '2026-08-30T14:02:00',
    },
    {
        recordSeq: 2,
        title: '주간 스프린트 회고',
        originFileName: 'sprint-retro.m4a',
        fileSize: 4_194_304,
        hasAudio: false,
        createdAt: '2026-08-28T10:00:00',
    },
]

const USER_USAGE = [
    {
        userSeq: 1,
        maskedEmail: 'd**@e*****.com',
        userName: '관리자 (데모)',
        callCount: 128,
        inputTokens: 45_120,
        outputTokens: 18_900,
        untrackedCallCount: 3,
        estimatedCostUsd: 0.42,
        estimatedCostKrw: 580,
    },
    {
        userSeq: 2,
        maskedEmail: 'h**@e*****.com',
        userName: '홍길동 (데모)',
        callCount: 34,
        inputTokens: 9_800,
        outputTokens: 4_210,
        untrackedCallCount: 0,
        estimatedCostUsd: 0.09,
        estimatedCostKrw: 124,
    },
]

/**
 * 사용량 요약.
 *
 * 사용자별 표가 <code>v-else-if="summary"</code> 안에 있어, 요약이 없으면 표가 아예
 * 렌더되지 않는다. 레이아웃을 재려면 이 응답이 필요하다.
 */
const USAGE_SUMMARY = {
    period: { from: '2026-08-01', to: '2026-08-31' },
    total: {
        callCount: 162,
        inputTokens: 54_920,
        outputTokens: 23_110,
        untrackedCallCount: 3,
        unpricedCallCount: 5,
        estimatedCostUsd: 0.51,
        estimatedCostKrw: 704,
    },
    byFeature: [
        { feature: 'CHAT', callCount: 120, inputTokens: 40_000, outputTokens: 18_000, untrackedCallCount: 0 },
        { feature: 'RECEIPT', callCount: 20, inputTokens: 8_000, outputTokens: 3_000, untrackedCallCount: 0 },
        { feature: 'VOICE', callCount: 12, inputTokens: 5_000, outputTokens: 1_900, untrackedCallCount: 0 },
        { feature: 'ASSISTANT', callCount: 7, inputTokens: 1_920, outputTokens: 210, untrackedCallCount: 0 },
        { feature: 'EMBEDDING', callCount: 3, inputTokens: 0, outputTokens: 0, untrackedCallCount: 3 },
    ],
    daily: [
        { date: '2026-08-29', callCount: 40, inputTokens: 14_000, outputTokens: 6_000, untrackedCallCount: 0 },
        { date: '2026-08-30', callCount: 62, inputTokens: 20_920, outputTokens: 9_110, untrackedCallCount: 1 },
        { date: '2026-08-31', callCount: 60, inputTokens: 20_000, outputTokens: 8_000, untrackedCallCount: 2 },
    ],
}

/** 목록 응답 한 페이지로 감싼다 */
const asPage = (content: unknown[]) => ({
    content,
    page: 0,
    totalPages: 1,
    totalElements: content.length,
})

/** 브라우저가 서버로 보내는 모든 요청을 가로채는 라우터 */
export interface ApiMock {
    /** 로그인 상태로 만든다 (세션 복원 요청이 사용자 정보를 돌려준다) */
    signIn(): Promise<void>
    /** 로그아웃 상태로 만든다 */
    signOut(): Promise<void>
}

/**
 * /api/* 를 전부 가로챈다.
 *
 * 기본값은 "로그인 안 된 상태 + 빈 목록"이고, 테스트가 signIn() 으로 상태를 바꾼다.
 * 정의하지 않은 엔드포인트는 빈 결과를 돌려준다 — 화면이 데이터 없이도 떠야 하기 때문이고,
 * 새 화면이 추가됐을 때 픽스처를 고치지 않아도 레이아웃 테스트는 계속 돈다.
 */
async function installApiMock(page: Page): Promise<ApiMock> {
    let signedIn = false

    /** 목록형 응답이 필요한 경로 — 화면이 .content 를 읽으므로 페이지 형태로 돌려준다 */
    const emptyPage = { content: [], page: 0, totalPages: 0, totalElements: 0 }

    await page.route('**/api/**', async (route: Route) => {
        const url = new URL(route.request().url())
        const path = url.pathname.replace(/^\/api/, '')
        const headers = {
            'content-type': 'application/json',
            'set-cookie': 'XSRF-TOKEN=e2e; Path=/',
        }

        // 세션 확인 — 미로그인은 401 이 정상 흐름이다
        if (path === '/auth/me') {
            if (!signedIn) {
                return route.fulfill({ status: 401, headers, body: JSON.stringify(ok(null)) })
            }
            return route.fulfill({ status: 200, headers, body: JSON.stringify(ok(ADMIN_USER)) })
        }

        if (path === '/auth/login') {
            const body = route.request().postData() ?? ''
            // 비밀번호가 틀리면 401 — 실패 경로도 화면이 다뤄야 한다
            if (!body.includes('password=Workmate')) {
                return route.fulfill({
                    status: 401,
                    headers,
                    body: JSON.stringify({
                        success: false,
                        message: '이메일 또는 비밀번호가 올바르지 않습니다.',
                        result: null,
                    }),
                })
            }
            signedIn = true
            return route.fulfill({ status: 200, headers, body: JSON.stringify(ok(ADMIN_USER)) })
        }

        if (path.startsWith('/common/codes/AI_MODEL')) {
            return route.fulfill({ status: 200, headers, body: JSON.stringify(ok(AI_MODELS)) })
        }

        if (path === '/v1/chat/rooms') {
            return route.fulfill({ status: 200, headers, body: JSON.stringify(ok([])) })
        }

        // 도우미·채팅 스트리밍 — 실제 LLM 대신 정해진 토큰을 SSE 로 흘린다.
        // 진짜 모델을 부르면 응답이 매번 달라 단언할 수 없고 무료 한도를 소모한다
        if (path.endsWith('/stream')) {
            return route.fulfill({
                status: 200,
                headers: { 'content-type': 'text/event-stream' },
                body:
                    'event:token\ndata:{"delta":"안녕하세요. "}\n\n' +
                    'event:token\ndata:{"delta":"무엇을 도와드릴까요?"}\n\n' +
                    'event:done\ndata:{}\n\n',
            })
        }

        // 목록 화면 — 행이 있어야 표 레이아웃을 잴 수 있다(table-scroll.spec.ts)
        if (path === '/v1/admin/audit-logs') {
            return route.fulfill({ status: 200, headers, body: JSON.stringify(ok(asPage(AUDIT_LOGS))) })
        }
        if (path === '/v1/receipts') {
            return route.fulfill({ status: 200, headers, body: JSON.stringify(ok(asPage(RECEIPTS))) })
        }
        if (path === '/v1/voice') {
            return route.fulfill({ status: 200, headers, body: JSON.stringify(ok(asPage(VOICE_RECORDS))) })
        }
        if (path.endsWith('/usage/summary')) {
            return route.fulfill({ status: 200, headers, body: JSON.stringify(ok(USAGE_SUMMARY)) })
        }
        if (path === '/v1/admin/usage/by-user') {
            return route.fulfill({ status: 200, headers, body: JSON.stringify(ok(asPage(USER_USAGE))) })
        }

        // 그 외는 빈 결과 — 화면이 데이터 없이도 떠야 한다
        const body =
            path.includes('history') || path.includes('users') || path.includes('logs')
                ? ok(emptyPage)
                : ok(null)
        return route.fulfill({ status: 200, headers, body: JSON.stringify(body) })
    })

    return {
        async signIn() {
            signedIn = true
        },
        async signOut() {
            signedIn = false
        },
    }
}

/**
 * api 목은 <b>auto</b> 다 — 테스트가 요청하지 않아도 항상 설치된다.
 *
 * 요청할 때만 설치하면, 목을 안 쓰는 테스트의 /api 호출이 vite preview 의 프록시를 타고
 * 진짜 서버(localhost:8080)로 나간다. 개발 머신에서는 백엔드가 떠 있어 그대로 통과하고
 * CI 에서는 502 로 실패한다 — 실제로 그 차이 때문에 CI 만 깨졌다.
 * 목이 항상 켜져 있어야 테스트가 환경과 무관해진다.
 */
export const test = base.extend<{ api: ApiMock }>({
    api: [
        async ({ page }, use) => {
            const mock = await installApiMock(page)
            await use(mock)
        },
        { auto: true },
    ],
})

export { expect } from '@playwright/test'
