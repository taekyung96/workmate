/**
 * README 스크린샷 12종 캡처 스크립트.
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

/**
 * 채팅 캡처 때 실제로 던지는 질문.
 * 사내 가이드에 근거 문서가 있는 주제여야 답변에 출처가 붙는다.
 */
const RAG_QUESTION = 'pgvector에서 HNSW 인덱스는 어떤 기준으로 잡아야 해?';
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
    // 이메일 폼은 '이메일로 로그인' 링크 뒤에 접혀 있다 (F1-1) — 펼쳐야 입력칸이 생긴다
    await page.getByRole('button', { name: '이메일로 로그인' }).click();
    await page.waitForSelector('#email');
    await page.fill('#email', EMAIL);
    await page.fill('#password', PASSWORD);
    await page.click('button[type="submit"]');
    // 로그인 성공 시 라우터가 /chat 으로 보낸다 — 이 전환을 성공 판정 기준으로 삼는다
    await page.waitForURL(/\/chat/, { timeout: 15000 });
    await page.waitForLoadState('networkidle');
    console.log(`  [로그인] ${EMAIL}`);
}

/**
 * 새 대화에서 실제로 RAG 질문을 던지고, 답변에 출처가 붙은 상태를 캡처한다.
 *
 * 출처(sources)는 chat_message 에 저장되지 않아 이력을 다시 열면 사라진다.
 * 그래서 저장된 대화를 여는 대신, 캡처하는 순간에 한 번 실제로 물어본다.
 * @param {import('playwright').Page} page
 */
async function captureChatWithSources(page) {
    const input = page.getByPlaceholder('메시지를 입력하세요…');
    await input.fill(RAG_QUESTION);
    await input.press('Enter');

    // 출처 이벤트는 토큰보다 먼저 오므로 출처 칩만 보고 완료로 판단하면 안 된다.
    // 스트리밍 중에는 커서(▌)가 붙은 평문이 그려지고, 끝나야 마크다운으로 다시 그려진다.
    // 두 번째 인자는 페이지 함수에 넘길 값이고 옵션은 세 번째다 — 자리를 바꾸면 타임아웃이 무시된다
    await page.waitForFunction(
        () => !document.body.innerText.includes('▌') && document.querySelector('.markdown-body') !== null,
        null,
        { timeout: 180000 },
    );
    await page.waitForTimeout(1000);

    // 실패해도 임시 대화방은 반드시 지운다 — 안 그러면 재시도할 때마다 사이드바에 찌꺼기가 쌓인다
    try {
        // 실패로 끝난 응답(제공자 503·429 등)은 붉은 글씨로 그려진다 — 그대로 찍으면 잘린 답변이 박제된다
        if (await page.locator('.text-destructive').count()) {
            throw new Error('AI 응답이 실패로 끝났다 (제공자 오류 가능성) — 잠시 후 다시 실행할 것');
        }
        if (!(await page.locator('a[href^="/guide/"]').count())) {
            console.warn('  [경고] 출처가 표시되지 않았다 — 검색 결과가 없었을 수 있다');
        }
        await saveImage(page, '02_chat.png');
    } finally {
        await deleteTempRoom(page).catch(() => console.warn('  [경고] 임시 대화방 정리 실패'));
    }
}

/**
 * 캡처하려고 만든 임시 대화방을 지운다.
 * 이걸 안 하면 스크립트를 돌릴 때마다 데모 계정의 채팅 이력이 계속 불어난다.
 * @param {import('playwright').Page} page
 */
async function deleteTempRoom(page) {
    const row = page.locator('li.group').filter({ hasText: RAG_QUESTION.slice(0, 20) }).first();
    if (!(await row.count())) {
        console.warn('  [경고] 임시 대화방을 찾지 못해 삭제를 건너뛴다');
        return;
    }
    await row.hover();
    await row.locator('button[title="삭제"]').click();
    await page.getByRole('button', { name: '삭제' }).click();
    await page.waitForTimeout(1200);
    console.log('  [정리] 캡처용 임시 대화방 삭제');
}

async function run() {
    const browser = await chromium.launch({ headless: true });
    const context = await browser.newContext({ viewport: VIEWPORT, deviceScaleFactor: 2 });
    const page = await context.newPage();

    console.log(`대상: ${BASE}`);

    // --- 1) 비로그인 화면 ---
    // 회원가입 화면은 F1-1 에서 없앴다 — 일반 사용자는 소셜로만 가입한다
    console.log('1. 로그인');
    await go(page, '/login');
    await saveImage(page, '01_login.png');

    // --- 2) 로그인 후 화면 ---
    await login(page);

    console.log('2. 채팅 (실제 RAG 질문 → 출처 표시)');
    await go(page, '/chat');
    await captureChatWithSources(page);

    console.log('3~4. 영수증 분석 · 이력');
    await go(page, '/receipt');
    await saveImage(page, '03_receipt_analysis.png');
    await go(page, '/receipt/history', 1500);
    await saveImage(page, '04_receipt_history.png');

    console.log('5~6. 사내 가이드 목록 · 상세');
    await go(page, '/guide', 1500);
    await saveImage(page, '05_guide.png');
    // 목록 검색으로 대상 문서를 좁힌 뒤 첫 카드를 눌러 상세로 진입한다 (문서 id 하드코딩 회피)
    await page.getByPlaceholder('제목·본문 검색').fill(GUIDE_SEARCH_KEYWORD);
    await page.waitForTimeout(1500);   // 검색 디바운스 + 재조회 대기
    await page.locator('div.cursor-pointer').first().click();
    await page.waitForURL(/\/guide\/\d+/, { timeout: 10000 });
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1200);
    await saveImage(page, '06_guide_detail.png');

    console.log('7~9. 회의록 분석 · 이력 · 상세');
    await go(page, '/voice');
    await saveImage(page, '07_voice_analysis.png');
    await go(page, '/voice/history', 1500);
    await saveImage(page, '08_voice_history.png');
    // 이력 첫 행을 눌러 상세로 진입한다 — STT 전사문·AI 3단 요약·오디오 재생이 함께 보이는 화면
    await page.locator('tr.cursor-pointer').first().click();
    await page.waitForURL(/\/voice\/history\/\d+/, { timeout: 10000 });
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1500);
    await saveImage(page, '09_voice_detail.png');

    console.log('10~12. 관리자 (사용자 · 감사 로그 · 공통코드)');
    // 검색 필터를 걸지 않고 전체를 찍는다.
    // 앱이 이미 이메일·전화번호를 마스킹해 내려주고, 감사 로그 화면에는 필터가 없어
    // 한쪽만 걸러내면 "사용자 목록에 없는 사람이 감사 로그에 등장하는" 앞뒤 안 맞는 화면이 된다.
    await go(page, '/admin/users', 1500);
    await saveImage(page, '10_admin_users.png');

    await go(page, '/admin/audit-logs', 1500);
    await saveImage(page, '11_admin_audit.png');
    await go(page, '/admin/common-codes', 1500);
    await saveImage(page, '12_admin_codes.png');

    await context.close();
    await browser.close();
    console.log('=== 12종 캡처 완료 ===');
}

run().catch(err => {
    console.error(err);
    process.exit(1);
});
