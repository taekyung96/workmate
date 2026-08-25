/**
 * 확정 로고(C안)를 프로젝트 에셋으로 뽑는다.
 * - 카카오 앱 아이콘 등 외부 업로드용 PNG (docs/brand)
 * - 파비콘 PNG 폴백 (workmate-vue/public)
 * 화면 안에서 쓰는 심볼은 BrandMark.vue 가 SVG 를 직접 그리므로 여기서 만들지 않는다.
 */
const fs = require('fs');
const path = require('path');
const sharp = require('sharp');

const INK = '#262626';
const PAPER = '#ffffff';
const ROOT = path.join(__dirname, '..');

/** 확정안 — W 워드마크 + 오른쪽 위 반짝임 */
const FULL = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 128 128" width="128" height="128">
  <rect width="128" height="128" rx="28" fill="${INK}"/>
  <path d="M32 50 L45 92 L60 64 L75 92 L88 50" fill="none" stroke="${PAPER}"
        stroke-width="10" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M101 24 L104.5 33.5 L114 37 L104.5 40.5 L101 50 L97.5 40.5 L88 37 L97.5 33.5 Z" fill="${PAPER}"/>
</svg>`;

/** 파비콘용 단순형 — 16~32px 에서는 반짝임이 뭉개져 사라지므로 W 만 남긴다 */
const SIMPLE = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 128 128" width="128" height="128">
  <rect width="128" height="128" rx="28" fill="${INK}"/>
  <path d="M34 42 L48 88 L64 58 L80 88 L94 42" fill="none" stroke="${PAPER}"
        stroke-width="11" stroke-linecap="round" stroke-linejoin="round"/>
</svg>`;

const targets = [
    ['docs/brand/workmate-app-icon-128.png', FULL, 128],
    ['docs/brand/workmate-app-icon-512.png', FULL, 512],
    ['workmate-vue/public/favicon-96.png', SIMPLE, 96],
];

(async () => {
    for (const [rel, svg, size] of targets) {
        const file = path.join(ROOT, rel);
        await sharp(Buffer.from(svg), { density: 384 }).resize(size, size).png({ compressionLevel: 9 }).toFile(file);
        console.log(`${rel.padEnd(42)} ${String(fs.statSync(file).size).padStart(6)} bytes`);
    }
    // SVG 파비콘 — 벡터라 어떤 배율에서도 또렷하다
    fs.writeFileSync(path.join(ROOT, 'workmate-vue/public/favicon.svg'), SIMPLE + '\n', 'utf8');
    console.log('workmate-vue/public/favicon.svg'.padEnd(42), '작성');
})().catch(e => { console.error('실패:', e.message); process.exit(1); });
