import { defineConfig, devices } from '@playwright/test'

/**
 * E2E 설정 — 빌드된 SPA 를 실제 브라우저로 검증한다.
 *
 * <p><b>왜 백엔드를 띄우지 않나.</b> 여기서 잡으려는 것은 <b>레이아웃과 라우팅</b>이다.
 * 도우미 패널이 본문을 덮던 결함, 로그인 직후 404 로 떨어지던 결함처럼 jsdom 이 폭을 계산하지
 * 않아 단위 테스트로는 잡히지 않는 것들이다. 이를 위해 WAS·DB·Redis 를 띄우면
 * CI 에 LLM API 키가 필요해지고(키 없이는 WAS 가 기동하지 않는다), 무료 티어 한도까지
 * 테스트가 갉아먹는다. API 는 브라우저 레벨에서 가로채고(e2e/fixtures.ts), 서버 계약은
 * 백엔드 테스트가 따로 지킨다.</p>
 *
 * <p>SSE 스트리밍도 가로채기로 흉내 낸다 — 실제 LLM 을 부르면 응답이 매번 달라 단언할 수 없다.</p>
 */
export default defineConfig({
    testDir: './e2e',
    // 레이아웃 단언이 많아 병렬로 돌려도 서로 간섭하지 않는다
    fullyParallel: true,
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 1 : 0,
    reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : 'list',

    use: {
        baseURL: 'http://localhost:4173',
        trace: 'on-first-retry',
    },

    projects: [
        {
            name: 'desktop',
            use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } },
        },
        {
            /*
             * 1280×800 — 흔한 노트북 해상도다.
             *
             * 이 폭을 따로 두는 이유: 2026-09-04 에 찾은 반응형 결함 다섯 중 셋이
             * 1300px 아래에서만 드러났다(도우미 버튼이 전송 버튼을 덮음, 도우미 패널
             * 도킹으로 본문이 128px, 표 찌그러짐). desktop(1440) 하나만 보던 동안
             * 유닛 73건·E2E 25건이 모두 통과한 채 살아 있었다.
             *
             * 레이아웃 단언이 있는 spec 은 대개 폭을 직접 지정하므로 desktop 에서 한 번만
             * 돌지만, 그 밖의 기능 테스트는 여기서도 돌아 이 폭의 회귀를 잡는다.
             */
            name: 'laptop',
            use: { ...devices['Desktop Chrome'], viewport: { width: 1280, height: 800 } },
        },
        {
            // 도우미 패널은 넓은 화면에서 본문을 밀고 좁은 화면에서는 덮는다 — 둘 다 확인해야 한다
            name: 'mobile',
            use: { ...devices['Pixel 5'] },
        },
    ],

    // 빌드 산출물을 그대로 서빙한다. dev 서버가 아니라 preview 를 쓰는 이유는
    // 실제 배포되는 번들(코드 분할·최소화 후)에서 확인해야 의미가 있기 때문이다
    webServer: {
        command: 'npm run build-only && npm run preview -- --port 4173',
        url: 'http://localhost:4173',
        reuseExistingServer: !process.env.CI,
        timeout: 180_000,
    },
})
