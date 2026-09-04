import { test, expect } from './fixtures'
import type { Page } from '@playwright/test'

/**
 * 채팅 입력 바가 다른 요소에 가리지 않는지 검증한다.
 *
 * <p><b>이 파일이 존재하는 이유.</b> 도우미 토글(<code>fixed bottom-6 right-6</code>)이
 * 전송 버튼을 완전히 덮고 있었다. 전송 버튼을 겨냥해 눌러도 도우미 패널이 열렸다.
 * 유닛 테스트 73건은 전부 통과했다 — jsdom 은 폭을 계산하지 않아 "가렸는지"를 모른다.</p>
 *
 * <p><b>기존 E2E 도 놓쳤다.</b> desktop 프로젝트 뷰포트가 1440×900 인데, 겹침은
 * <b>1300px 아래에서만</b> 일어난다. 입력 바는 <code>max-w-5xl</code>(1024px) 이라
 * 넓은 화면에서는 좌우 여백이 남아 뷰포트 모서리의 도우미 버튼에 닿지 않는다.
 * 화면이 좁아지면 입력 바가 오른쪽 끝까지 차면서 비로소 겹친다. 그래서 이 파일은
 * 프로젝트 뷰포트에 기대지 않고 <b>폭을 직접 지정해</b> 잰다.</p>
 *
 * <p>단언은 좌표 비교가 아니라 <code>elementFromPoint</code> 로 한다 — 실제로 그 지점에서
 * 잡히는 요소가 무엇인지가 사용자가 겪는 결과이기 때문이다. Playwright 의
 * <code>click()</code> 은 가려진 요소를 스스로 피해 가므로 이 결함을 드러내지 못한다.</p>
 */

/** 겹침이 일어나던 폭과, 일어나지 않던 폭을 함께 넣는다 */
const VIEWPORT_WIDTHS = [1280, 1366, 1440]

/**
 * 화면의 한 점에서 실제로 잡히는 버튼의 접근성 이름을 돌려준다.
 *
 * @param page  대상 페이지
 * @param point 뷰포트 기준 좌표
 * @returns 그 지점 최상단 버튼의 aria-label (버튼이 없으면 태그명, 화면 밖이면 null)
 */
async function buttonAtPoint(page: Page, point: { x: number; y: number }) {
    return page.evaluate(({ x, y }) => {
        const el = document.elementFromPoint(x, y)
        if (!el) return null
        const button = el.closest('button')
        return button ? (button.getAttribute('aria-label') ?? button.tagName) : el.tagName
    }, point)
}

/** 로그인 상태로 채팅 화면을 열고, 전송 버튼이 활성화되도록 문구를 채운다 */
async function openChatWithDraft(page: Page) {
    await page.goto('/chat')
    await expect(page.getByRole('button', { name: '도우미 열기' })).toBeVisible()
    // 전송 버튼은 입력이 비어 있으면 disabled 라 히트테스트 대상이 되지 못한다
    await page.getByPlaceholder('메시지를 입력하세요…').fill('겹침 확인용 입력')
    await expect(page.getByRole('button', { name: '전송' })).toBeEnabled()
}

test.describe('채팅 입력 바 — 도우미 버튼과 겹치지 않는다', () => {
    for (const width of VIEWPORT_WIDTHS) {
        test(`${width}px: 전송 버튼을 도우미 버튼이 가리지 않는다`, async ({ page, api }) => {
            test.skip(test.info().project.name !== 'desktop', '폭을 직접 지정하므로 한 번만 돈다')
            await page.setViewportSize({ width, height: 900 })
            await api.signIn()
            await openChatWithDraft(page)

            const send = page.getByRole('button', { name: '전송' })
            const box = (await send.boundingBox())!

            // 모서리까지 전부 본다 — 예전 결함은 5개 지점 중 4개가 가려진 상태였다
            const points = {
                중앙: { x: box.x + box.width / 2, y: box.y + box.height / 2 },
                좌상: { x: box.x + 3, y: box.y + 3 },
                좌하: { x: box.x + 3, y: box.y + box.height - 3 },
                우상: { x: box.x + box.width - 3, y: box.y + 3 },
                우하: { x: box.x + box.width - 3, y: box.y + box.height - 3 },
            }

            for (const [name, point] of Object.entries(points)) {
                expect(
                    await buttonAtPoint(page, point),
                    `${width}px 전송 버튼 ${name} 지점이 도우미 버튼에 가렸다`,
                ).not.toBe('도우미 열기')
            }
        })
    }

    test('전송 버튼 클릭이 도우미가 아니라 전송으로 이어진다', async ({ page, api }) => {
        test.skip(test.info().project.name !== 'desktop', '폭을 직접 지정하므로 한 번만 돈다')
        // 겹침이 가장 심했던 폭에서 확인한다
        await page.setViewportSize({ width: 1280, height: 900 })
        await api.signIn()
        await openChatWithDraft(page)

        await page.getByRole('button', { name: '전송' }).click()

        // 도우미 패널이 열렸다면 결함이 되살아난 것이다
        await expect(page.getByRole('complementary').filter({ hasText: '도우미' })).toHaveCount(0)
        // 입력창이 비워졌다면 전송이 실제로 일어났다는 뜻이다
        await expect(page.getByPlaceholder('메시지를 입력하세요…')).toHaveValue('')
    })

    /**
     * 모바일에서 입력 바가 화면을 넘치던 결함을 지킨다.
     *
     * <p>예전에는 AppSidebar 가 <code>w-64</code> 고정이고 반응형 클래스가 없어, 393px
     * 화면에서도 사이드바가 256px 를 차지했다. 본문에 137px 만 남아 입력 바가 통째로 넘치고
     * 전송 버튼이 화면 오른쪽 밖 222px 지점에 놓였다. 지금은 md 미만에서 사이드바를
     * 서랍으로 접고, 입력 바도 두 줄로 나눈다.</p>
     */
    test('모바일: 전송 버튼이 화면 안에 있고 누를 수 있다', async ({ page, api }) => {
        test.skip(test.info().project.name !== 'mobile', '모바일 전용')
        await api.signIn()
        await openChatWithDraft(page)

        const send = page.getByRole('button', { name: '전송' })
        const box = (await send.boundingBox())!
        const viewport = page.viewportSize()!

        // 화면 밖으로 밀려나면 사용자는 전송 자체를 할 수 없다
        expect(box.x + box.width, '전송 버튼이 뷰포트 오른쪽 밖으로 나갔다').toBeLessThanOrEqual(
            viewport.width,
        )
        expect(box.x, '전송 버튼이 뷰포트 왼쪽 밖으로 나갔다').toBeGreaterThanOrEqual(0)

        expect(
            await buttonAtPoint(page, { x: box.x + box.width / 2, y: box.y + box.height / 2 }),
        ).not.toBe('도우미 열기')
    })

    test('모바일: 사이드바는 접혀 있고 햄버거로 연다', async ({ page, api }) => {
        test.skip(test.info().project.name !== 'mobile', '모바일 전용')
        await api.signIn()
        await page.goto('/chat')

        const sidebar = page.locator('aside')
        const viewport = page.viewportSize()!

        // 접혀 있어야 한다 — 펼쳐진 채로 두면 본문이 그만큼 좁아져 입력 바가 넘친다
        await expect(page.getByRole('button', { name: '메뉴 열기' })).toBeVisible()
        expect((await sidebar.boundingBox())!.x, '사이드바가 접히지 않았다').toBeLessThan(0)

        // 햄버거로 열리고, 화면 안으로 들어와야 한다
        await page.getByRole('button', { name: '메뉴 열기' }).click()
        await expect
            .poll(async () => (await sidebar.boundingBox())!.x, { timeout: 2000 })
            .toBeGreaterThanOrEqual(0)
        expect((await sidebar.boundingBox())!.width).toBeLessThanOrEqual(viewport.width)

        // 메뉴를 고르면 스스로 닫힌다 — 안 닫히면 이동해도 서랍이 본문을 계속 가린다
        await page.getByRole('link', { name: '가이드', exact: true }).click()
        await expect
            .poll(async () => (await sidebar.boundingBox())!.x, { timeout: 2000 })
            .toBeLessThan(0)
    })

    test('데스크탑: 사이드바는 항상 보이고 상단 바는 없다', async ({ page, api }) => {
        test.skip(test.info().project.name !== 'desktop', '데스크탑 전용')
        await page.setViewportSize({ width: 1440, height: 900 })
        await api.signIn()
        await page.goto('/chat')

        // 모바일 대응을 넣으면서 데스크탑이 바뀌지 않았는지 지킨다
        const sidebar = page.locator('aside')
        expect((await sidebar.boundingBox())!.x).toBe(0)
        await expect(page.getByRole('button', { name: '메뉴 열기' })).toBeHidden()
        // 본문이 사이드바 오른쪽에서 시작해야 한다 (겹치면 x 가 0 이 된다)
        expect((await page.locator('main').boundingBox())!.x).toBeGreaterThanOrEqual(256)
    })
})
