/**
 * 읽기 경로 부하 테스트 (k6).
 *
 * [무엇을 재나]
 * 브라우저 → WEB(세션·CSRF) → 프록시 → WAS → PostgreSQL 로 이어지는 실제 요청 경로다.
 * 세션 조회는 Redis 를, 가이드 목록은 DB 까지 태운다.
 *
 * [왜 채팅은 안 재나]
 * 채팅·도우미는 실제 LLM 을 부른다. 부하를 주면 (1) 무료 티어 한도를 테스트가 소모하고
 * (2) 응답 시간이 모델·네트워크에 좌우돼 우리 스택의 처리량을 측정할 수 없으며
 * (3) 사용자당 분당 레이트리밋에 먼저 걸린다. 여기서 알고 싶은 것은 우리 서버의 한계지
 * LLM 제공자의 응답 속도가 아니다.
 *
 * [세션을 하나만 쓰는 이유]
 * WEB 은 중복 로그인을 막는다(maximumSessions(1)). VU 마다 로그인하면 서로의 세션을
 * 만료시켜 401 이 쏟아진다. setup 에서 한 번 로그인해 그 쿠키를 모든 VU 가 공유한다 —
 * 같은 사용자가 탭 여러 개를 연 상황과 같다. 대신 '로그인 폭주' 는 이 테스트의 범위가 아니다.
 *
 * [실행]
 *   docker run --rm --network host -v "$PWD/scripts/loadtest:/s" grafana/k6 run /s/api-load.js
 *
 * [환경변수]
 *   TARGETS  대상 주소들(쉼표 구분). 인스턴스를 늘려 재려면 여러 개를 준다.
 *            예: http://localhost:8080,http://localhost:8081  → VU 가 번갈아 때린다
 *   VUS      최대 동시 사용자 (기본 50)
 *   STAGE    각 단계 길이 (기본 30s)
 *   EMAIL / PASSWORD  로그인 계정
 */
import http from "k6/http";
import { check, fail } from "k6";
import { Trend } from "k6/metrics";

const TARGETS = (__ENV.TARGETS || "http://localhost:8080").split(",");
const VUS = parseInt(__ENV.VUS || "50", 10);
const STAGE = __ENV.STAGE || "30s";
const EMAIL = __ENV.EMAIL || "demo.admin@example.com";
const PASSWORD = __ENV.PASSWORD || "Workmate!2026";

/** 엔드포인트별로 따로 본다 — 세션만 읽는 것과 DB 까지 가는 것은 성격이 다르다 */
const meLatency = new Trend("latency_auth_me", true);
const guideLatency = new Trend("latency_guide_list", true);

export const options = {
    stages: [
        { duration: STAGE, target: Math.ceil(VUS / 5) },
        { duration: STAGE, target: Math.ceil(VUS / 2) },
        { duration: STAGE, target: VUS },
        { duration: STAGE, target: VUS },
        { duration: "10s", target: 0 },
    ],
    thresholds: {
        // 실패가 섞이면 지연 수치는 의미가 없다 — 먼저 0 에 가까운지 본다
        http_req_failed: ["rate<0.01"],
        latency_auth_me: ["p(95)<500"],
        latency_guide_list: ["p(95)<1000"],
    },
    // 요약에 p99 까지 넣는다. 평균만 보면 꼬리 지연을 놓친다
    summaryTrendStats: ["avg", "min", "med", "p(95)", "p(99)", "max"],
};

/**
 * 로그인해서 세션·CSRF 쿠키를 얻는다.
 *
 * @returns {{cookie: string}} 모든 VU 가 공유할 Cookie 헤더 값
 */
export function setup() {
    const base = TARGETS[0];

    // 첫 GET 에서 XSRF-TOKEN 쿠키가 내려온다 (CsrfCookieFilter)
    const seed = http.get(`${base}/`);
    const xsrf = seed.cookies["XSRF-TOKEN"]
        ? seed.cookies["XSRF-TOKEN"][0].value
        : "";
    if (!xsrf)
        fail("XSRF-TOKEN 쿠키를 받지 못했다 — 서버가 떠 있는지 확인하라");

    // Spring Security 폼 로그인이라 form-urlencoded 로 보낸다 (usernameParameter=email)
    const login = http.post(
        `${base}/api/auth/login`,
        { email: EMAIL, password: PASSWORD },
        { headers: { "X-XSRF-TOKEN": xsrf, Cookie: `XSRF-TOKEN=${xsrf}` } },
    );
    if (login.status !== 200)
        fail(`로그인 실패 (status ${login.status}) — 계정·비밀번호를 확인하라`);

    const session = login.cookies["SESSION"]
        ? login.cookies["SESSION"][0].value
        : "";
    if (!session) fail("SESSION 쿠키를 받지 못했다");

    return { cookie: `SESSION=${session}; XSRF-TOKEN=${xsrf}` };
}

export default function (data) {
    // 인스턴스가 여러 개면 VU 별로 나눠 붙는다 — 로드밸런서 없이 분산을 흉내 낸다
    const base = TARGETS[__VU % TARGETS.length];
    const params = { headers: { Cookie: data.cookie } };

    // 1) 세션 조회 — WEB 이 Redis 에서 세션을 읽는다 (WAS·DB 를 타지 않는다)
    const me = http.get(`${base}/api/auth/me`, params);
    meLatency.add(me.timings.duration);
    check(me, {
        "/auth/me 200": (r) => r.status === 200,
        // 세션이 인스턴스를 건너 유효한지 — 여러 대로 돌릴 때 이 검사가 핵심이다
        "/auth/me 세션 유효": (r) =>
            r.body && r.body.indexOf('"success":true') >= 0,
    });

    // 2) 가이드 목록 — WEB → WAS → PostgreSQL 까지 가는 전체 경로
    const guides = http.get(`${base}/api/v1/guides?page=0&size=10`, params);
    guideLatency.add(guides.timings.duration);
    check(guides, {
        "/v1/guides 200": (r) => r.status === 200,
    });
}
