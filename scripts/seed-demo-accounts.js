/**
 * 데모 계정을 앱의 회원가입 API로 생성한다.
 * 이메일·전화번호 AES 암호화와 비밀번호 BCrypt 해싱을 앱이 직접 수행하도록 하기 위해
 * DB 직접 INSERT 대신 HTTP 회원가입을 쓴다.
 */
const BASE = process.env.WM_BASE || 'http://localhost:8080';

const DEMO_USERS = [
    { email: 'demo.admin@example.com', password: 'Workmate!2026', userName: '관리자 (데모)', phone: '01000000001' },
    { email: 'hong@example.com',       password: 'Workmate!2026', userName: '홍길동 (데모)', phone: '01000000002' },
    { email: 'kim@example.com',        password: 'Workmate!2026', userName: '김서연 (데모)', phone: '01000000003' },
];

/** 세션 쿠키와 CSRF 토큰을 확보한다 */
async function handshake() {
    const res = await fetch(`${BASE}/api/auth/me`);
    const cookies = (res.headers.getSetCookie?.() || []).join('; ');
    const token = /XSRF-TOKEN=([^;]+)/.exec(cookies)?.[1];
    if (!token) throw new Error('CSRF 토큰을 받지 못했습니다');
    return { cookie: cookies.split(',').map(c => c.split(';')[0]).join('; '), token };
}

(async () => {
    const { cookie, token } = await handshake();
    for (const u of DEMO_USERS) {
        const res = await fetch(`${BASE}/api/auth/signup`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': token, Cookie: cookie },
            body: JSON.stringify(u),
        });
        const body = await res.json().catch(() => ({}));
        console.log(`${u.email.padEnd(24)} → ${res.status} ${body.success ? 'OK' : (body.message || '')}`);
    }
})().catch(e => { console.error('실패:', e.message); process.exit(1); });
