import { test, expect } from './fixtures'
import type { Page } from '@playwright/test'

/**
 * 모든 화면을 여러 폭으로 훑어 <b>레이아웃이 무너지지 않는지</b> 본다.
 *
 * <p><b>왜 화면별 테스트가 아니라 훑기인가.</b> 2026-09-04 에 찾은 반응형 결함 다섯은
 * 화면도 원인도 제각각이었지만 뿌리가 하나였다 — <b>넓은 화면에서만 확인한 코드</b>다.
 * 도우미 버튼이 전송 버튼을 덮고(1300px 아래), 사이드바가 안 접히고(768px 아래),
 * 표가 찌그러지고, 도우미 패널 도킹으로 본문이 128px 가 됐다. 유닛 73건과 E2E 25건이
 * 모두 통과한 상태로 전부 살아 있었다.</p>
 *
 * <p>같은 계열을 화면마다 따로 막으려면 화면 수만큼 테스트를 써야 하고, <b>새로 만드는
 * 화면은 여전히 빠진다.</b> 그래서 화면 목록 × 폭 목록을 훑으며 공통 규칙만 본다.
 * 화면을 추가하면 아래 ROUTES 에 한 줄 넣는 것으로 끝난다.</p>
 *
 * <p>여기서 잡는 것은 <b>"무너졌는가"</b>이지 "예쁜가"가 아니다. 세밀한 단언은 각 화면의
 * spec 이 맡는다(chat-composer·table-scroll·assistant-panel).</p>
 *
 * <p><b>못 잡는 것 — 실제로 확인했다.</b> 화면을 열어 보기만 하므로 <b>상태에 따라 나타나는
 * 결함</b>은 지나친다. 도우미 버튼이 전송 버튼을 덮던 결함을 되돌려 놓고 돌려 봤는데 통과했다 —
 * 입력이 비어 전송 버튼이 <code>disabled</code> 라 검사 대상에서 빠지기 때문이다.
 * 그 결함은 글자를 채워야 드러나고, 그건 chat-composer.spec 이 맡는다.</p>
 *
 * <p>반대로 <b>구조가 무너지는 것</b>은 확실히 잡는다. 사이드바의 반응형 클래스를 되돌려 놓고
 * 돌리면 393px 에서 <b>열한 화면 전부</b>가 "본문이 가로로 넘쳤다"로 걸린다.</p>
 *
 * <p>정리하면 이 파일은 <b>넓게 훑고</b>, 화면별 spec 이 <b>깊게 판다.</b> 둘 다 필요하다.</p>
 */

/** 로그인 후 볼 수 있는 화면들. 새 화면을 만들면 여기 한 줄 추가한다 */
const ROUTES = [
    '/chat',
    '/receipt',
    '/receipt/history',
    '/guide',
    '/voice',
    '/voice/history',
    '/usage',
    '/admin/users',
    '/admin/audit-logs',
    '/admin/usage',
    '/admin/common-codes',
]

/** 훑을 폭 — 좁은 순서대로 휴대폰·세로 태블릿·노트북 */
const WIDTHS = [393, 768, 1280]

/**
 * 한 화면에서 레이아웃 규칙 위반을 모아 온다.
 *
 * <p>단언을 브라우저 안에서 하지 않고 <b>위반 목록을 돌려받는</b> 이유: 폭 하나에 화면이
 * 열한 개라, 처음 만난 실패에서 멈추면 나머지를 못 본다. 한 번에 다 보여야 원인이
 * 하나인지 여럿인지 판단할 수 있다.</p>
 *
 * @param page 대상 페이지
 * @returns 위반 설명 목록 (비어 있으면 정상)
 */
async function findLayoutViolations(page: Page): Promise<string[]> {
    return page.evaluate(() => {
        const problems: string[] = []
        const vw = window.innerWidth

        // ① 페이지 본문이 가로로 밀리면 안 된다.
        //    넘치는 내용은 그 내용을 담은 상자가 스크롤해야지, 페이지 전체가 밀리면 안 된다.
        if (document.documentElement.scrollWidth > vw + 1) {
            problems.push(
                `본문이 가로로 넘쳤다 (${document.documentElement.scrollWidth}px > ${vw}px)`,
            )
        }

        /**
         * 화면 밖 요소에 <b>닿을 수 있는가</b>.
         *
         * @param el 대상 요소
         * @param vw 뷰포트 폭
         * @returns 가로 스크롤로 닿거나, 통째로 화면 밖인 상자(접힌 서랍) 안이면 true
         */
        const isReachable = (el: Element, vw: number): boolean => {
            for (let p = el.parentElement; p; p = p.parentElement) {
                // 가로로 스크롤되는 상자 안이면 밀어서 닿는다
                if (p.scrollWidth > p.clientWidth + 1) return true
                // 상자 자체가 통째로 화면 밖이면 그 상자가 '닫힌 것'이다 (서랍)
                const pr = p.getBoundingClientRect()
                if (pr.width > 0 && (pr.right <= 0 || pr.left >= vw)) return true
            }
            return false
        }

        /** 요소를 사람이 알아볼 수 있게 적는다 */
        const describe = (el: Element) => {
            const label = el.getAttribute('aria-label') ?? (el as HTMLElement).innerText?.trim()
            const text = (label ?? '').replace(/\s+/g, ' ').slice(0, 20)
            return `${el.tagName.toLowerCase()}${text ? `("${text}")` : ''}`
        }

        const interactive = [...document.querySelectorAll('button, a[href], input, textarea')]
        for (const el of interactive) {
            const r = el.getBoundingClientRect()
            // 숨겨진 것(폭·높이 0)은 대상이 아니다 — 접힌 서랍 안의 요소 등
            if (r.width === 0 || r.height === 0) continue
            if ((el as HTMLButtonElement).disabled) continue

            // ② 화면 밖에 있으면서 <b>닿을 방법도 없는</b> 요소.
            //
            //    "화면 밖" 자체는 결함이 아니다. 두 가지 정상 경우가 있다.
            //      - 가로로 스크롤되는 상자 안 → 밀면 닿는다 (표가 그렇다)
            //      - 접힌 서랍 안 → 서랍을 열면 딸려 들어온다 (모바일 사이드바)
            //    이 둘을 걸러내고 남는 것만 문제다. 실제로 이 구분 없이 처음 돌렸을 때
            //    표 안의 버튼과 접힌 사이드바 링크가 전부 걸려 나왔다.
            if (r.right > vw + 1 || r.left < -1) {
                if (!isReachable(el, vw)) {
                    problems.push(
                        `${describe(el)} 가 화면 밖에 있고 스크롤로도 닿지 않는다 (left=${Math.round(r.left)}, right=${Math.round(r.right)}, vw=${vw})`,
                    )
                }
                continue
            }

            // ③ 다른 요소가 완전히 덮으면 누를 수 없다.
            //    중앙 한 점만 본다 — 모서리까지 보면 겹친 그림자·테두리에 걸려 잡음이 많다.
            const hit = document.elementFromPoint(r.left + r.width / 2, r.top + r.height / 2)
            if (!hit) continue
            // 자기 자신이거나 자기 안팎의 요소면 정상이다
            if (hit === el || el.contains(hit) || hit.contains(el)) continue
            // 라벨을 눌러도 입력으로 가는 구조가 흔하다 — 같은 폼 컨트롤을 가리키면 정상
            if (hit.closest('label') && hit.closest('label')!.contains(el)) continue
            problems.push(`${describe(el)} 를 ${describe(hit)} 가 덮고 있다`)
        }
        return problems
    })
}

test.describe('반응형 훑기 — 화면이 무너지지 않는다', () => {
    for (const width of WIDTHS) {
        test(`${width}px: 모든 화면에서 레이아웃이 무너지지 않는다`, async ({ page, api }) => {
            // 폭을 직접 지정하므로 프로젝트마다 반복할 이유가 없다
            test.skip(test.info().project.name !== 'desktop', '폭을 직접 지정하므로 한 번만 돈다')
            await page.setViewportSize({ width, height: 900 })
            await api.signIn()

            const failures: string[] = []
            for (const route of ROUTES) {
                await page.goto(route)
                // 화면이 그려질 때까지 기다린다 — 골격만 있는 상태로 재면 의미가 없다
                await page.waitForLoadState('networkidle')
                for (const problem of await findLayoutViolations(page)) {
                    failures.push(`${route} — ${problem}`)
                }
            }

            expect(failures, `${width}px 에서 레이아웃 문제:\n${failures.join('\n')}`).toEqual([])
        })
    }
})
