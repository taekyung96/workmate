import { test, expect } from './fixtures'
import type { Page } from '@playwright/test'

/**
 * 좁은 화면에서 표가 줄어들지 않고 가로로 스크롤되는지 검증한다.
 *
 * <p><b>이 파일이 존재하는 이유.</b> 표를 감싼 <code>overflow-x-auto</code> 는 있었는데
 * 표가 <code>w-full</code>(=100%)이라 컨테이너보다 커질 수 없었다. 넘칠 수 없으니
 * 스크롤도 생길 수 없고, 글자를 줄바꿈해 밀어 넣는 수밖에 없었다. 393px 화면에서
 * 감사 로그 한 줄이 121px, 영수증 이력 한 줄이 <b>281px</b> 를 차지했다.</p>
 *
 * <p>유닛 테스트는 이걸 볼 수 없다 — jsdom 은 폭을 계산하지 않아 표가 줄었는지
 * 넘쳤는지 알 수 없다. 실제 브라우저에서 재는 것이 유일한 방법이다.</p>
 *
 * <p>단언을 "행 높이"로 하는 이유: 폭만 보면 <code>min-w</code> 를 지운 뒤에도
 * 표가 우연히 컨테이너보다 넓을 수 있다. <b>줄바꿈이 일어나면 행이 높아진다</b>는 것이
 * 사용자가 실제로 겪는 증상이라, 그쪽을 지킨다.</p>
 */

/** 표가 있는 화면과, 좁은 화면에서 보장해야 할 최대 행 높이 */
const PAGES = [
    { path: '/admin/audit-logs', name: '감사 로그', maxRowHeight: 60 },
    { path: '/admin/usage', name: '사용량', maxRowHeight: 60 },
    { path: '/receipt/history', name: '영수증 이력', maxRowHeight: 60 },
    { path: '/voice/history', name: '회의록 이력', maxRowHeight: 70 },
]

/**
 * 첫 번째 표의 상태를 잰다.
 *
 * @param page 대상 페이지
 * @returns 표 폭·행 높이·가로 스크롤 여부. 표가 없으면 null
 */
async function measureTable(page: Page) {
    return page.evaluate(() => {
        const table = document.querySelector('table')
        if (!table) return null
        const scroller = table.parentElement!
        const row = table.querySelector('tbody tr')
        return {
            tableWidth: Math.round(table.getBoundingClientRect().width),
            scrollerWidth: Math.round(scroller.clientWidth),
            rowHeight: row ? Math.round(row.getBoundingClientRect().height) : null,
            scrollsHorizontally: scroller.scrollWidth > scroller.clientWidth,
            bodyOverflows: document.documentElement.scrollWidth > window.innerWidth,
        }
    })
}

test.describe('표 — 좁은 화면에서 줄이지 않고 스크롤한다', () => {
    for (const { path, name, maxRowHeight } of PAGES) {
        test(`모바일: ${name} 표가 찌그러지지 않는다`, async ({ page, api }) => {
            test.skip(test.info().project.name !== 'mobile', '모바일 전용')
            await api.signIn()
            await page.goto(path)

            // 표가 없으면 건너뛰지 않고 실패시킨다. 조용히 통과하는 테스트는 없느니만 못하다 —
            // 처음 이 파일을 넣었을 때 목 데이터가 비어 10건이 전부 skip 됐고, 결과는 초록이었다.
            const table = page.locator('table')
            await expect(table, `${name}: 표가 렌더되지 않았다 — 목 데이터를 확인하라`).toBeVisible()
            await expect(page.locator('tbody tr').first()).toBeVisible()

            const m = (await measureTable(page))!

            // ① 표가 컨테이너보다 넓어야 한다 — 좁히지 않고 넘치는 것이 의도다
            expect(m.tableWidth, `${name}: 표가 컨테이너 폭으로 줄어들었다`).toBeGreaterThan(
                m.scrollerWidth,
            )
            // ② 그래서 가로 스크롤이 생겨야 한다
            expect(m.scrollsHorizontally, `${name}: 가로 스크롤이 생기지 않았다`).toBe(true)
            // ③ 줄바꿈으로 행이 부풀지 않아야 한다 (예전엔 121~281px 였다)
            expect(m.rowHeight, `${name}: 행이 줄바꿈으로 부풀었다`).toBeLessThan(maxRowHeight)
            // ④ 스크롤은 표 안에서만 — 페이지 본문이 가로로 밀리면 안 된다
            expect(m.bodyOverflows, `${name}: 페이지 본문이 가로로 넘쳤다`).toBe(false)
        })
    }

    test('데스크탑: 표가 컨테이너를 채우고 스크롤이 생기지 않는다', async ({ page, api }) => {
        test.skip(test.info().project.name !== 'desktop', '데스크탑 전용')
        await page.setViewportSize({ width: 1440, height: 900 })
        await api.signIn()
        await page.goto('/admin/audit-logs')

        const table = page.locator('table')
        await expect(table, '표가 렌더되지 않았다 — 목 데이터를 확인하라').toBeVisible()
        await expect(page.locator('tbody tr').first()).toBeVisible()

        const m = (await measureTable(page))!

        // min-w 를 준 뒤에도 넓은 화면에서는 예전처럼 컨테이너를 꽉 채워야 한다
        expect(
            Math.abs(m.tableWidth - m.scrollerWidth),
            '표가 컨테이너를 채우지 않는다',
        ).toBeLessThan(2)
        expect(m.scrollsHorizontally, '넓은 화면에서 가로 스크롤이 생겼다').toBe(false)
    })
})
