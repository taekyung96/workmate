/**
 * README 스크린샷 13종 캡처 스크립트.
 *
 * 예전에는 모든 API 응답을 목(mock)으로 가로채 가짜 데이터를 찍었으나,
 * 지금은 실제로 로그인해 실제 DB 데이터가 그려진 화면을 찍는다.
 * 개인정보가 노출되는 관리자 사용자 목록만 데모 계정으로 검색을 걸어 캡처한다.
 *
 * 사전 조건: DB · WAS(8081) · WEB(8080) 기동 + 캡처 대상 서버(기본 5173) 기동,
 *            데모 계정 시드 적용(db/init/13-seed-demo-data.sql).
 *
 * 실행: node scripts/capture-all-perfect.js
 *       WM_BASE=http://localhost:8080 node scripts/capture-all-perfect.js   (운영과 동일한 경로로 캡처)
 */
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const sharp = require('sharp');

const OUTPUT_DIR = path.join(__dirname, '../docs/images');
const BASE = process.env.WM_BASE || 'http://localhost:5173';
const EMAIL = process.env.WM_EMAIL || 'demo.admin@example.com';
const PASSWORD = process.env.WM_PASSWORD || 'Workmate!2026';

/** 기본 뷰포트 — 캡처 직전에 내용 높이만큼 세로로 늘린다 */
const VIEWPORT = { width: 1280, height: 800 };
/** 세로로 늘릴 수 있는 한계 (너무 긴 문서까지 통짜로 찍으면 README 에서 읽기 어렵다) */
const MAX_CAPTURE_HEIGHT = 3600;

/** 관리자 사용자 목록에서 데모 계정만 남기는 검색어 (실제 사용자 이메일·전화번호 노출 방지) */
const DEMO_KEYWORD = '데모';
/** 채팅 캡처에 사용할 대화방 제목 */
const CHAT_ROOM_TITLE = 'RAG에 대해서 설명해줘';
/** 가이드 상세 캡처 대상을 좁히기 위한 목록 검색어 */
const GUIDE_SEARCH_KEYWORD = 'RAG';

if (!fs.existsSync(OUTPUT_DIR)) {
    fs.mkdirSync(OUTPUT_DIR, { recursive: true });
}

/**
 * 화면이 잘리지 않도록 뷰포트 높이를 내용에 맞춰 늘린다.
 *
 * 앱 셸이 `flex h-screen` + `main overflow-hidden` 이라 페이지 body 는 절대 뷰포트를 넘지 않고
 * 내용은 안쪽 컨테이너에서 스크롤된다. 그래서 Playwright 의 fullPage 옵션이 듣지 않는다.
 * 대신 스크롤 컨테이너가 넘치는 만큼을 재서 뷰포트 자체를 키운 뒤 찍는다.
 *
 * @param {import('playwright').Page} page
 * @returns {Promise<number>} 최종 뷰포트 높이(px)
 */
async function fitViewport(page) {
    await page.setViewportSize(VIEWPORT);
    await page.waitForTimeout(300);

    // 뷰포트를 키우면 레이아웃이 바뀌어 넘치는 양도 달라지므로 몇 번 반복해 수렴시킨다
    for (let i = 0; i < 4; i++) {
        const overflow = await page.evaluate(() => {
            let inner = 0;
            for (const el of document.querySelectorAll('*')) {
                const style = getComputedStyle(el);
                if (/(auto|scroll)/.test(style.overflowY) && el.scrollHeight > el.clientHeight + 1) {
                    inner = Math.max(inner, el.scrollHeight - el.clientHeight);
                }
            }
            const body = document.documentElement.scrollHeight - window.innerHeight;
            return Math.max(inner, body, 0);
        });
        if (overflow < 4) break;

        const current = page.viewportSize().height;
        const next = Math.min(current + overflow, MAX_CAPTURE_HEIGHT);
        if (next === current) break;
        await page.setViewportSize({ width: VIEWPORT.width, height: next });
        await page.waitForTimeout(400);
    }
    return page.viewportSize().height;
}

/**
 * 현재 페이지를 캡처해 docs/images 아래에 PNG 한 장으로 저장한다.
 * @param {import('playwright').Page} page - 캡처 대상 페이지
 * @param {string} fileName - 저장할 파일명 (예: '01_login.png')
 * @returns {Promise<void>}
 */
async function saveImage(page, fileName) {
    // Vite 개발 서버에서 뜨는 Vue DevTools 플로팅 패널이 캡처에 찍히지 않도록 숨긴다
    await page.addStyleTag({
        content: '#__vue-devtools-container__, #vue-inspector-container { display: none !important; }',
    }).catch(() => {});
    const height = await fitViewport(page);
    const buf = await page.screenshot();
    const opt = await sharp(buf)
        .resize({ width: 1280, fit: 'inside' })
        .png({ compressionLevel: 9, palette: true, quality: 85 })
        .toBuffer();

    fs.writeFileSync(path.join(OUTPUT_DIR, fileName), opt);
    console.log(`  [저장] ${fileName} (${VIEWPORT.width}x${height})`);
}

/**
 * 지정 경로로 이동하고 네트워크가 잠잠해질 때까지 기다린다.
 * @param {import('playwright').Page} page
 * @param {string} route - '/chat' 같은 SPA 경로
 * @param {number} settle - 렌더 안정화 대기 ms
 */
async function go(page, route, settle = 1200) {
    await page.setViewportSize(VIEWPORT);   // 이전 화면에서 늘어난 높이를 초기화
    await page.goto(`${BASE}${route}`);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(settle);
}

/**
 * 실제 로그인 폼을 채워 세션을 만든다.
 * @param {import('playwright').Page} page
 * @throws {Error} 로그인 후 /chat 으로 이동하지 못하면 실패로 간주
 */
async function login(page) {
    await go(page, '/login', 500);
    await page.fill('#email', EMAIL);
    await page.fill('#password', PASSWORD);
    await page.click('button[type="submit"]');
    // 로그인 성공 시 라우터가 /chat 으로 보낸다 — 이 전환을 성공 판정 기준으로 삼는다
    await page.waitForURL(/\/chat/, { timeout: 15000 });
    await page.waitForLoadState('networkidle');
    console.log(`  [로그인] ${EMAIL}`);
}

async function run() {
    const browser = await chromium.launch({ headless: true });
    const context = await browser.newContext({ viewport: VIEWPORT, deviceScaleFactor: 2 });
    const page = await context.newPage();

    console.log(`대상: ${BASE}`);

    // --- 1) 비로그인 화면 ---
    console.log('1~2. 로그인 · 회원가입');
    await go(page, '/login');
    await saveImage(page, '01_login.png');
    await go(page, '/signup');
    await saveImage(page, '02_signup.png');

    // --- 2) 로그인 후 화면 ---
    await login(page);

    console.log('3. 채팅 (실제 대화 이력)');
    await go(page, '/chat');
    // 사이드바의 실제 대화방을 눌러 저장된 질문·AI 답변을 띄운다
    const room = page.getByRole('button', { name: CHAT_ROOM_TITLE }).first();
    if (await room.count()) {
        await room.click();
        await page.waitForTimeout(1500);
    } else {
        console.warn(`  [경고] 대화방 "${CHAT_ROOM_TITLE}" 을(를) 찾지 못했습니다`);
    }
    await saveImage(page, '03_chat.png');

    console.log('4~5. 영수증 분석 · 이력');
    await go(page, '/receipt');
    await saveImage(page, '04_receipt_analysis.png');
    await go(page, '/receipt/history', 1500);
    await saveImage(page, '05_receipt_history.png');

    console.log('6~7. 사내 가이드 목록 · 상세');
    await go(page, '/guide', 1500);
    await saveImage(page, '06_guide.png');
    // 목록 검색으로 대상 문서를 좁힌 뒤 첫 카드를 눌러 상세로 진입한다 (문서 id 하드코딩 회피)
    await page.getByPlaceholder('제목·본문 검색').fill(GUIDE_SEARCH_KEYWORD);
    await page.waitForTimeout(1500);   // 검색 디바운스 + 재조회 대기
    await page.locator('div.cursor-pointer').first().click();
    await page.waitForURL(/\/guide\/\d+/, { timeout: 10000 });
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1200);
    await saveImage(page, '07_guide_detail.png');

    console.log('8~10. 회의록 분석 · 이력 · 상세');
    await go(page, '/voice');
    await saveImage(page, '08_voice_analysis.png');
    await go(page, '/voice/history', 1500);
    await saveImage(page, '09_voice_history.png');
    // 이력 첫 행을 눌러 상세로 진입한다 — STT 전사문·AI 3단 요약·오디오 재생이 함께 보이는 화면
    await page.locator('tr.cursor-pointer').first().click();
    await page.waitForURL(/\/voice\/history\/\d+/, { timeout: 10000 });
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1500);
    await saveImage(page, '10_voice_detail.png');

    console.log('11~13. 관리자 (사용자 · 감사 로그 · 공통코드)');
    await go(page, '/admin/users', 1500);
    // 실제 사용자의 이메일·전화번호가 캡처에 남지 않도록 데모 계정만 검색해 남긴다
    const search = page.getByPlaceholder('이메일 또는 이름 검색');
    await search.fill(DEMO_KEYWORD);
    await page.waitForTimeout(1500);   // 검색 디바운스 + 재조회 대기
    await saveImage(page, '11_admin_users.png');

    await go(page, '/admin/audit-logs', 1500);
    await saveImage(page, '12_admin_audit.png');
    await go(page, '/admin/common-codes', 1500);
    await saveImage(page, '13_admin_codes.png');

    await context.close();
    await browser.close();
    console.log('=== 13종 캡처 완료 ===');
}

run().catch(err => {
    console.error(err);
    process.exit(1);
});
