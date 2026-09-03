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
