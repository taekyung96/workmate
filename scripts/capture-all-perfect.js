const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const sharp = require('sharp');

const OUTPUT_DIR = path.join(__dirname, '../docs/images');
if (!fs.existsSync(OUTPUT_DIR)) {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true });
}

/**
 * 현재 페이지를 캡처해 docs/images 아래에 PNG 한 장으로 저장한다.
 * @param {import('playwright').Page} page - 캡처 대상 페이지
 * @param {string} fileName - 저장할 파일명 (예: '01_login.png')
 * @returns {Promise<void>}
 */
async function saveImage(page, fileName) {
  const buf = await page.screenshot();
  const opt = await sharp(buf)
    .resize({ width: 1280, fit: 'inside' })
    .png({ compressionLevel: 9, palette: true, quality: 85 })
    .toBuffer();

  fs.writeFileSync(path.join(OUTPUT_DIR, fileName), opt);
  console.log(`[SAVED SUCCESS] ${fileName}`);
}

async function run() {
  const browser = await chromium.launch({ headless: true });

  // Common Routes setup for logged-in pages
  async function setupMocks(page) {
    await page.route(url => url.pathname.endsWith('/api/auth/me'), async route => {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({
          code: '200', message: 'SUCCESS',
          result: { userSeq: 10, email: 'demo@example.com', userName: '홍길동', role: 'ROLE_ADMIN' }
        })
      });
    });

    await page.route(url => url.pathname.includes('/api/common/codes/'), async route => {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({
          code: '200', message: 'SUCCESS',
          result: [
            { groupCode: 'AI_MODEL', code: 'gemini-flash-latest', codeName: 'Gemini Flash (latest)', sortOrder: 1 },
            { groupCode: 'AI_MODEL', code: 'gemini-pro-latest', codeName: 'Gemini Pro (latest)', sortOrder: 2 }
          ]
        })
      });
    });

    await page.route(url => url.pathname.endsWith('/api/v1/chat/rooms'), async route => {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({
          code: '200', message: 'SUCCESS',
          result: [
            { roomSeq: 1, title: 'Redis 캐싱 전략 및 Spring Boot Caching 가이드', createdAt: '2026-07-31T10:00:00Z' },
            { roomSeq: 2, title: 'Workmate v3 아키텍처 및 RAG 관련 문의', createdAt: '2026-07-31T09:00:00Z' },
            { roomSeq: 3, title: '사내 경비 및 영수증 OCR 처리', createdAt: '2026-07-31T08:00:00Z' }
          ]
        })
      });
    });

    await page.route(url => url.pathname.includes('/api/v1/chat/rooms/') && url.pathname.endsWith('/messages'), async route => {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({
          code: '200', message: 'SUCCESS',
          result: [
            { 
              messageSeq: 101, role: 'user', 
              content: 'Redis 캐싱 전략 및 Spring Boot Caching 가이드 내용 요약해줘' 
            },
            { 
              messageSeq: 102, role: 'assistant', 
              content: `제공해주신 **[Performance] Redis 캐싱 전략 및 Spring Boot Caching 가이드**의 핵심 요약입니다.\n\n⚡ **핵심 요약**\nRedis는 인메모리(In-Memory) 기반의 초고속 저장소로, DB 조회를 줄여 **응답 속도를 10배 이상 향상**시키는 캐시 레이어로 활용됩니다.\n\n1. **Look-Aside (Cache-Aside) 패턴 (보편적 ⭐️)**\n  - Redis 조회(Hit) 후 없을 경우(Miss) DB 조회 결과를 Redis에 저장합니다.\n\n\`\`\`java\n@Cacheable(value = "guides", key = "#guideSeq")\npublic GuideVo getGuide(Long guideSeq) {\n    return guideRepository.findById(guideSeq);\n}\n\`\`\``,
              sources: [
                { guideSeq: 28, title: '[Performance] Redis 캐싱 전략 및 Spring Boot Caching 가이드' },
                { guideSeq: 25, title: '[Architecture] Spring Boot & Vue 3 표준 뼈대 구축 가이드' },
                { guideSeq: 27, title: '[Database] RDBMS (PostgreSQL) vs NoSQL 선택 가이드' }
              ]
            }
          ]
        })
      });
    });

    await page.route(url => url.pathname.endsWith('/api/v1/receipts'), async route => {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({
          code: '200', message: 'SUCCESS',
          result: [
            { receiptSeq: 1, payAmount: 48500, bizNo: '2208162517', payDate: '20260725', cardName: 'KB국민카드', bizNoValid: true, selectType: 'AUTO', createdAt: '2026-07-25T14:20:00Z' },
            { receiptSeq: 2, payAmount: 125000, bizNo: '6727600528', payDate: '20260728', cardName: '현대카드', bizNoValid: true, selectType: 'AUTO', createdAt: '2026-07-28T11:15:00Z' },
            { receiptSeq: 3, payAmount: 22000, bizNo: '6831602314', payDate: '20260730', cardName: '신한카드', bizNoValid: true, selectType: 'AUTO', createdAt: '2026-07-30T19:40:00Z' }
          ]
        })
      });
    });

    await page.route(url => url.pathname.endsWith('/api/v1/guides'), async route => {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({
          code: '200', message: 'SUCCESS',
          result: {
            content: [
              { guideSeq: 28, title: '[Performance] Redis 캐싱 전략 및 Spring Boot Caching 가이드', content: 'Redis 캐싱 성능 가이드 본문...', isPublic: true, createdAt: '2026-07-25T10:00:00Z' },
              { guideSeq: 25, title: '[Architecture] Spring Boot & Vue 3 표준 뼈대 구축 가이드', content: 'Spring Boot & Vue 3 아키텍처...', isPublic: true, createdAt: '2026-07-24T09:00:00Z' },
              { guideSeq: 27, title: '[Database] RDBMS (PostgreSQL) vs NoSQL 선택 가이드', content: 'RDBMS vs NoSQL 기술 비교 가이드...', isPublic: true, createdAt: '2026-07-23T11:00:00Z' },
              { guideSeq: 26, title: '[Backend/Java] Spring Boot 3 & JPA N+1 문제 해결 가이드', content: 'JPA Fetch Join 및 EntityGraph 가이드...', isPublic: true, createdAt: '2026-07-22T15:00:00Z' }
            ],
            totalElements: 4, totalPages: 1, page: 0, size: 12
          }
        })
      });
    });

    await page.route(url => url.pathname.includes('/api/v1/guides/'), async route => {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({
          code: '200', message: 'SUCCESS',
          result: {
            guideSeq: 28, userSeq: 10, title: '[Performance] Redis 캐싱 전략 및 Spring Boot Caching 가이드',
            content: `# [Performance] Redis 캐싱 전략 및 Spring Boot Caching 가이드\n\n## 1. 개요\nRedis는 인메모리(In-Memory) 기반의 초고속 저장소로, DB 조회를 줄여 응답 속도를 10배 이상 향상시킵니다.\n\n\`\`\`java\n@Cacheable(value = "guides", key = "#guideSeq")\npublic GuideVo getGuide(Long guideSeq) {\n    return guideRepository.findById(guideSeq);\n}\n\`\`\`\n\n- **Look-Aside 패턴**: 데이터 요청 시 Redis 확인 후 DB 조회\n- **TTL 주기**: 자주 조회되는 가이드 부하 절감`,
            isPublic: true, createdAt: '2026-07-25T10:00:00Z', updatedAt: '2026-07-25T10:00:00Z'
          }
        })
      });
    });

    await page.route(url => url.pathname.endsWith('/api/v1/voice'), async route => {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({
          code: '200', message: 'SUCCESS',
          result: [
            { recordSeq: 1, title: 'Workmate v3 아키텍처 및 3-tier BFF 설계 회의', audioFileName: 'meeting-test.wav', fileSize: 1923330, createdAt: '2026-07-29T15:30:00Z' },
            { recordSeq: 2, title: 'Sprint 15 주간 기술 스크럼 및 RAG 성능 평가', audioFileName: 'weekly_scrum_20260729.mp3', fileSize: 1420000, createdAt: '2026-07-29T10:00:00Z' }
          ]
        })
      });
    });

    await page.route(url => url.pathname.endsWith('/api/v1/admin/users'), async route => {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({
          code: '200', message: 'SUCCESS',
          result: {
            content: [
              { userSeq: 10, email: 'de***@example.com', userName: '홍길동', phone: '010-****-5678', role: 'ROLE_ADMIN', loginFailCount: 0, locked: false, useYn: true, createdAt: '2026-07-01T00:00:00Z' },
              { userSeq: 11, email: 'us***@example.com', userName: '김철수', phone: '010-****-1234', role: 'ROLE_USER', loginFailCount: 0, locked: false, useYn: true, createdAt: '2026-07-02T00:00:00Z' },
              { userSeq: 12, email: 'le***@example.com', userName: '이영희', phone: '010-****-9876', role: 'ROLE_USER', loginFailCount: 2, locked: false, useYn: true, createdAt: '2026-07-03T00:00:00Z' }
            ],
            totalElements: 3, totalPages: 1, page: 0, size: 20
          }
        })
      });
    });

    await page.route(url => url.pathname.endsWith('/api/v1/admin/audit-logs'), async route => {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({
          code: '200', message: 'SUCCESS',
          result: {
            content: [
              { auditSeq: 1, adminUserName: '홍길동', targetUserName: '김철수', action: 'UNLOCK', createdAt: '2026-07-30T16:00:00Z' },
              { auditSeq: 2, adminUserName: '홍길동', targetUserName: '이영희', action: 'RESET_PASSWORD', createdAt: '2026-07-30T17:30:00Z' }
            ],
            totalElements: 2, totalPages: 1, page: 0, size: 20
          }
        })
      });
    });

    await page.route(url => url.pathname.endsWith('/api/v1/admin/common-codes/groups'), async route => {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({
          code: '200', message: 'SUCCESS',
          result: [
            { groupCode: 'AI_MODEL', groupName: 'AI 답변 모델', description: '선택 가능한 AI 모델 목록', useYn: true },
            { groupCode: 'USER_ROLE', groupName: '사용자 권한', description: '시스템 사용자 권한 구문 목록', useYn: true },
            { groupCode: 'CARD_TYPE', groupName: '카드사 구분', description: '법인카드 결제 카드사 목록', useYn: true }
          ]
        })
      });
    });

    await page.route(url => url.pathname.includes('/api/v1/admin/common-codes/groups/') && url.pathname.endsWith('/codes'), async route => {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({
          code: '200', message: 'SUCCESS',
          result: [
            { groupCode: 'AI_MODEL', code: 'gemini-flash-latest', codeName: 'Gemini Flash (latest)', sortOrder: 1, useYn: true },
            { groupCode: 'AI_MODEL', code: 'gemini-pro-latest', codeName: 'Gemini Pro (latest)', sortOrder: 2, useYn: true }
          ]
        })
      });
    });
  }

  // --- PART 1: Public Pages (Login & Signup) - NO AUTH INJECTION ---
  {
    const context = await browser.newContext({ viewport: { width: 1280, height: 800 }, deviceScaleFactor: 2 });
    const page = await context.newPage();

    // Mock 401 for /api/auth/me on public pages so authStore remains unauthenticated
    await page.route(url => url.pathname.endsWith('/api/auth/me'), async route => {
      await route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify({ code: '401', message: 'UNAUTHORIZED' }) });
    });

    console.log('1. Capturing 01_login.png...');
    await page.goto('http://localhost:5173/login');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);
    await saveImage(page, '01_login.png');

    console.log('2. Capturing 02_signup.png...');
    await page.goto('http://localhost:5173/signup');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);
    await saveImage(page, '02_signup.png');

    await context.close();
  }

  // --- PART 2: Protected Pages - WITH AUTH INJECTION & FULL MOCKS ---
  {
    const context = await browser.newContext({ viewport: { width: 1280, height: 800 }, deviceScaleFactor: 2 });
    await context.addInitScript(() => {
      window.addEventListener('DOMContentLoaded', () => {
        const timer = setInterval(() => {
          const pinia = window.__pinia__;
          if (pinia && pinia._s && pinia._s.has('auth')) {
            const authStore = pinia._s.get('auth');
            authStore.user = { userSeq: 10, email: 'demo@example.com', userName: '홍길동', role: 'ROLE_ADMIN' };
            authStore.resolved = true;
            clearInterval(timer);
          }
        }, 30);
      });
    });

    const page = await context.newPage();
    await setupMocks(page);

    // 3. Chat Page
    console.log('3. Capturing 03_chat.png...');
    await page.goto('http://localhost:5173/chat');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);
    const roomBtn = await page.$('ul li button');
    if (roomBtn) await roomBtn.click();
    await page.waitForTimeout(1500);
    await saveImage(page, '03_chat.png');

    // 4. Receipt Form
    console.log('4. Capturing 04_receipt_analysis.png...');
    await page.goto('http://localhost:5173/receipt');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);
    await saveImage(page, '04_receipt_analysis.png');

    // 5. Receipt History
    console.log('5. Capturing 05_receipt_history.png...');
    await page.goto('http://localhost:5173/receipt/history');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1500);
    await saveImage(page, '05_receipt_history.png');

    // 6. Guide List
    console.log('6. Capturing 06_guide.png...');
    await page.goto('http://localhost:5173/guide');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);
    await saveImage(page, '06_guide.png');

    // 7. Guide Detail
    console.log('7. Capturing 07_guide_detail.png...');
    await page.goto('http://localhost:5173/guide/28');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1500);
    await saveImage(page, '07_guide_detail.png');

    // 8. Voice Form
    console.log('8. Capturing 08_voice_analysis.png...');
    await page.goto('http://localhost:5173/voice');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);
    await saveImage(page, '08_voice_analysis.png');

    // 9. Voice History
    console.log('9. Capturing 09_voice_history.png...');
    await page.goto('http://localhost:5173/voice/history');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1500);
    await saveImage(page, '09_voice_history.png');

    // 10. Admin Users
    console.log('10. Capturing 10_admin_users.png...');
    await page.goto('http://localhost:5173/admin/users');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1500);
    await saveImage(page, '10_admin_users.png');

    // 11. Admin Audit Logs
    console.log('11. Capturing 11_admin_audit.png...');
    await page.goto('http://localhost:5173/admin/audit-logs');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1500);
    await saveImage(page, '11_admin_audit.png');

    // 12. Admin Common Codes
    console.log('12. Capturing 12_admin_codes.png...');
    await page.goto('http://localhost:5173/admin/common-codes');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1500);
    await saveImage(page, '12_admin_codes.png');

    await context.close();
  }

  await browser.close();
  console.log('=== PERFECT CAPTURE OF ALL 12 PAGES COMPLETED ===');
}

run().catch(err => {
  console.error(err);
  process.exit(1);
});
