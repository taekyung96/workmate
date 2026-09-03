import { test, expect, AI_MODELS } from './fixtures'

/** 드롭다운 트리거에 현재 선택된 모델 이름이 표시된다 (옵션 목록과 구분하기 위해 트리거로 좁힌다) */
function selectedModel(page: import('@playwright/test').Page) {
    return page.locator('[data-slot="select-value"]')
}

/**
 * 채팅 화면.
 *
 * 모델 드롭다운은 단위 테스트로 라벨을 확인할 수 없었다 — Reka Select 가 목록을 펼치기
 * 전까지 선택값을 jsdom 에서 그리지 않기 때문이다. 실제 브라우저에서는 확인할 수 있다.
 */

test.beforeEach(async ({ page, api }) => {
    await api.signIn()
    await page.goto('/chat')
})

test.describe('채팅', () => {
    test('기본 모델은 공통코드의 첫 번째 코드다', async ({ page }) => {
        // 서버의 sort_order 가 곧 화면 기본값이다 — 기본 모델을 바꾸는 유일한 손잡이다
        await expect(selectedModel(page)).toHaveText(AI_MODELS[0]!.codeName)
    })

    test('드롭다운에는 켜 둔 모델만 나온다', async ({ page }) => {
        await selectedModel(page).click()

        for (const model of AI_MODELS) {
            await expect(page.getByRole('option', { name: model.codeName })).toBeVisible()
        }
        // 꺼 둔 모델(use_yn=false)은 서버가 내려주지 않으므로 목록에 없어야 한다
        await expect(page.getByRole('option', { name: /GPT-OSS/ })).toHaveCount(0)
    })

    test('모델을 바꾸면 선택이 유지된다', async ({ page }) => {
        await selectedModel(page).click()
        await page.getByRole('option', { name: AI_MODELS[1]!.codeName }).click()

        await expect(selectedModel(page)).toHaveText(AI_MODELS[1]!.codeName)
    })

    test('빈 상태에서 안내 문구를 보여준다', async ({ page }) => {
        await expect(page.getByText('무엇을 도와드릴까요?')).toBeVisible()
    })
})
