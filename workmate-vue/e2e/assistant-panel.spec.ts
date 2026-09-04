import { test, expect } from './fixtures'

/**
 * 도우미 패널 레이아웃.
 *
 * 이 파일이 존재하는 이유는 하나다 — 패널이 본문을 <b>덮고</b> 있던 결함이 단위 테스트를
 * 전부 통과했기 때문이다. jsdom 은 폭을 계산하지 않아 "가렸는지"를 알 수 없다.
 * 실제 브라우저에서 본문 폭이 실제로 줄어드는지 재는 것이 유일한 방법이다.
 */

/**
 * 채팅 화면으로 들어간다.
 *
 * 로그인 폼을 거치지 않는다 — api.signIn() 으로 이미 세션이 있는 상태라 /login 으로 가면
 * 가드가 곧바로 /chat 으로 돌려보내 폼 자체가 없다. 로그인 흐름은 auth-routing.spec 이 맡는다.
 */
/**
 * 요소의 크기가 더 이상 변하지 않을 때까지 기다린 뒤 박스를 돌려준다.
 *
 * 패널은 220ms 동안 폭이 커진다. 보이자마자 재면 전환 도중 값이라 본문과 겹쳐 보인다.
 *
 * @param locator 측정할 요소
 * @returns 안정된 뒤의 박스
 */
async function stableBox(locator: import('@playwright/test').Locator) {
    let previous = -1
    for (let i = 0; i < 20; i++) {
        const box = (await locator.boundingBox())!
        if (box.width === previous) return box
        previous = box.width
        await locator.page().waitForTimeout(50)
    }
    return (await locator.boundingBox())!
}

async function openChat(page: import('@playwright/test').Page) {
    await page.goto('/chat')
    await expect(page.getByRole('button', { name: '도우미 열기' })).toBeVisible()
}

test.describe('도우미 패널', () => {
    test('데스크탑: 열면 본문이 밀린다 — 덮지 않는다', async ({ page, api }) => {
        test.skip(test.info().project.name !== 'desktop', '데스크탑 전용 레이아웃')
        await api.signIn()
        await openChat(page)

        const main = page.locator('main')
        const before = (await main.boundingBox())!.width

        await page.getByRole('button', { name: '도우미 열기' }).click()
        const panel = page.getByRole('complementary').filter({ hasText: '도우미' })
        await expect(panel).toBeVisible()

        // 전환(220ms)이 끝난 뒤에 잰다 — 도중에 재면 아직 좁은 패널과 넓은 본문이 겹쳐 보인다
        const panelBox = await stableBox(panel)
        const mainBox = (await main.boundingBox())!

        // 본문이 실제로 좁아져야 한다. 덮는 구현이면 폭이 그대로다 — 그게 이전 결함이었다
        expect(mainBox.width).toBeLessThan(before)
        // 본문과 패널이 겹치지 않는지 좌표로 확인한다
        expect(mainBox.x + mainBox.width).toBeLessThanOrEqual(panelBox.x + 1)
    })

    test('데스크탑: 닫으면 본문 폭이 되돌아온다', async ({ page, api }) => {
        test.skip(test.info().project.name !== 'desktop', '데스크탑 전용 레이아웃')
        await api.signIn()
        await openChat(page)

        const main = page.locator('main')
        const before = (await main.boundingBox())!.width

        await page.getByRole('button', { name: '도우미 열기' }).click()
        const panel = page.getByRole('complementary').filter({ hasText: '도우미' })
        await stableBox(panel)

        // 닫기 버튼은 패널 안에 있다 — 화면의 다른 '닫기'와 섞이지 않게 패널로 좁힌다
        await panel.getByRole('button', { name: '닫기' }).click()
        await expect(page.getByRole('button', { name: '도우미 열기' })).toBeVisible()
        await page.waitForTimeout(300)

        expect((await main.boundingBox())!.width).toBeCloseTo(before, 0)
    })

    test('모바일: 화면이 좁아 덮는 것이 맞다', async ({ page, api }) => {
        test.skip(test.info().project.name !== 'mobile', '모바일 전용 레이아웃')
        await api.signIn()
        await openChat(page)

        await page.getByRole('button', { name: '도우미 열기' }).click()
        const panel = page.getByRole('complementary').filter({ hasText: '도우미' })
        await expect(panel).toBeVisible()

        // 모바일은 나눠 쓸 폭이 없다 — 뷰포트를 꽉 채워야 한다
        const panelBox = await stableBox(panel)
        const viewport = page.viewportSize()!
        expect(panelBox.width).toBeGreaterThan(viewport.width * 0.9)
    })

    test('스트리밍 답변을 화면에 이어 붙인다', async ({ page, api }) => {
        await api.signIn()
        await openChat(page)

        await page.getByRole('button', { name: '도우미 열기' }).click()
        await page.getByPlaceholder('질문을 입력하세요').fill('이 화면 뭔가요')
        await page.getByPlaceholder('질문을 입력하세요').press('Enter')

        // 토큰 두 개가 이어 붙어야 한다 (가로챈 SSE 가 정해진 값을 흘린다)
        await expect(page.getByText('안녕하세요. 무엇을 도와드릴까요?')).toBeVisible()
    })

    /**
     * 도킹 기준을 md(768) 에서 xl(1280) 로 올린 것을 지킨다.
     *
     * <p>768px 에서 도킹하면 사이드바(256) + 패널(384) 을 빼고 본문에 <b>128px</b> 만 남았다.
     * 글자가 세로로 쪼개지고 채팅 입력 바가 잘렸다. 폭이 부족하면 나란히 두는 이득이 이미
     * 사라지므로 덮는 편이 낫다.</p>
     */
    const NARROW_WIDTHS = [768, 1024, 1279]

    for (const width of NARROW_WIDTHS) {
        test(`태블릿 ${width}px: 도킹하지 않고 덮는다`, async ({ page, api }) => {
            test.skip(test.info().project.name !== 'desktop', '폭을 직접 지정하므로 한 번만 돈다')
            await page.setViewportSize({ width, height: 900 })
            await api.signIn()
            await openChat(page)

            await page.getByRole('button', { name: '도우미 열기' }).click()
            const panel = page.getByRole('complementary').filter({ hasText: '도우미' })
            await expect(panel).toBeVisible()

            const box = await stableBox(panel)
            // 뷰포트를 꽉 채워야 한다 — 도킹되면 384px 로 좁아지고 본문이 찌그러진다
            expect(box.width, `${width}px 에서 패널이 도킹됐다 — 본문이 찌그러진다`).toBeGreaterThan(
                width - 2,
            )
        })
    }

    test('1280px: 여기서부터는 도킹한다', async ({ page, api }) => {
        test.skip(test.info().project.name !== 'desktop', '폭을 직접 지정하므로 한 번만 돈다')
        await page.setViewportSize({ width: 1280, height: 900 })
        await api.signIn()
        await openChat(page)

        const main = page.locator('main')
        const before = (await main.boundingBox())!.width

        await page.getByRole('button', { name: '도우미 열기' }).click()
        const panel = page.getByRole('complementary').filter({ hasText: '도우미' })
        const box = await stableBox(panel)

        // 덮지 않고 한 칸을 차지해야 한다
        expect(box.width).toBeLessThan(1280 / 2)
        expect((await main.boundingBox())!.width).toBeLessThan(before)
    })

    test('닫았다 열어도 대화가 남는다 — 새 대화로만 비운다', async ({ page, api }) => {
        await api.signIn()
        await openChat(page)

        await page.getByRole('button', { name: '도우미 열기' }).click()
        const panel = page.getByRole('complementary').filter({ hasText: '도우미' })
        await panel.getByPlaceholder('질문을 입력하세요').fill('이 화면 뭔가요')
        await panel.getByPlaceholder('질문을 입력하세요').press('Enter')
        await expect(page.getByText('안녕하세요. 무엇을 도와드릴까요?')).toBeVisible()

        // 닫는다 — 좁은 화면에서는 본문을 보려면 닫을 수밖에 없다. 그때 대화가 날아가면 못 쓴다
        await panel.getByRole('button', { name: '닫기' }).click()
        await expect(page.getByRole('button', { name: '도우미 열기' })).toBeVisible()

        await page.getByRole('button', { name: '도우미 열기' }).click()
        await expect(page.getByText('안녕하세요. 무엇을 도와드릴까요?')).toBeVisible()

        // 비우는 것은 "새 대화" 로만 일어난다
        const reopened = page.getByRole('complementary').filter({ hasText: '도우미' })
        await reopened.getByRole('button', { name: '새 대화' }).click()
        await expect(page.getByText('안녕하세요. 무엇을 도와드릴까요?')).toBeHidden()
        await expect(page.getByText('지금 보고 있는 화면에 대해 물어보세요')).toBeVisible()
    })

    test('대화가 없으면 새 대화 버튼을 보이지 않는다', async ({ page, api }) => {
        await api.signIn()
        await openChat(page)

        await page.getByRole('button', { name: '도우미 열기' }).click()
        const panel = page.getByRole('complementary').filter({ hasText: '도우미' })
        await expect(panel).toBeVisible()

        // 비어 있는데 버튼이 있으면 누를 이유가 없는 버튼이 하나 더 있는 셈이다
        await expect(panel.getByRole('button', { name: '새 대화' })).toBeHidden()
    })
})
