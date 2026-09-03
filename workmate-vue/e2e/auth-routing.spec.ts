import { test, expect } from './fixtures'

/**
 * 로그인과 라우팅.
 *
 * 여기 테스트들은 api 픽스처를 따로 요청하지 않는다 — 목이 auto 라 항상 설치되고,
 * 기본 상태(미로그인)가 곧 이 파일이 필요로 하는 출발점이기 때문이다.
 *
 * "로그인했는데 첫 화면이 404" 결함이 여기서 재현된다. 가드가 없는 주소를 목적지로 기억해
 * 로그인 성공 후 그 주소로 되돌아가던 문제였다. 단위 테스트(guards.spec.ts)도 이를 잡지만,
 * 실제 브라우저에서 주소창까지 확인하는 것은 여기서만 가능하다.
 */

/** 이메일 로그인 폼을 펼치고 값을 채워 제출한다 */
async function submitLogin(page: import('@playwright/test').Page, password: string) {
    await page.getByRole('button', { name: '이메일로 로그인' }).click()
    await page.getByLabel('이메일').fill('demo.admin@example.com')
    await page.getByLabel('비밀번호').fill(password)
    await page.getByRole('button', { name: '로그인', exact: true }).click()
}

test.describe('로그인·라우팅', () => {
    test('미로그인으로 접속하면 로그인 화면으로 보낸다', async ({ page }) => {
        await page.goto('/')

        await expect(page).toHaveURL(/\/login/)
        await expect(page.getByRole('heading', { name: '로그인' })).toBeVisible()
    })

    test('로그인하면 채팅 화면으로 간다', async ({ page }) => {
        await page.goto('/')
        await submitLogin(page, 'Workmate!2026')

        await expect(page).toHaveURL(/\/chat$/)
    })

    test('없는 주소로 들어와 로그인해도 404 로 떨어지지 않는다', async ({ page }) => {
        // 오래된 북마크로 들어온 상황
        await page.goto('/oldbookmark/from/v2')
        await expect(page).toHaveURL(/\/login/)
        // 없는 주소는 목적지로 기억하지 않는다 — 기억하면 로그인 직후 그리로 되돌아간다
        await expect(page).not.toHaveURL(/redirect=/)

        await submitLogin(page, 'Workmate!2026')

        await expect(page).toHaveURL(/\/chat$/)
        await expect(page.getByText('404')).toBeHidden()
    })

    test('정상 화면으로 가려다 튕기면 로그인 후 그 화면으로 복귀한다', async ({ page }) => {
        await page.goto('/usage')
        await expect(page).toHaveURL(/redirect=%2Fusage|redirect=\/usage/)

        await submitLogin(page, 'Workmate!2026')

        await expect(page).toHaveURL(/\/usage$/)
    })

    test('비밀번호가 틀리면 이동하지 않고 사유를 보여준다', async ({ page }) => {
        await page.goto('/login')
        await submitLogin(page, 'wrong-password')

        await expect(page).toHaveURL(/\/login/)
        await expect(page.getByText(/올바르지 않습니다|실패/)).toBeVisible()
    })

    test('로그인한 뒤 없는 주소로 가면 404 화면을 보여준다', async ({ page, api }) => {
        await api.signIn()
        await page.goto('/nope')

        await expect(page.getByText('404')).toBeVisible()
        // 404 화면의 '홈으로'는 채팅으로 간다
        await page.getByRole('link', { name: '홈으로' }).click()
        await expect(page).toHaveURL(/\/chat$/)
    })
})
