import type { UsageSummary } from '../../common/types/usage'

/**
 * 테스트용 응답 픽스처.
 *
 * 서버가 실제로 내려주는 모양을 그대로 흉내 낸다 — 특히 "0건인 기능도 채워서 내려준다",
 * "집계에서 빠진 건수를 따로 담는다" 같은 규칙은 화면이 그것을 표시하도록 만들어져 있어
 * 픽스처가 대충이면 테스트가 실제 화면을 검증하지 못한다.
 */

/**
 * 사용량 요약 픽스처.
 *
 * @param overrides 덮어쓸 필드 (합계만 바꾸고 싶을 때 등)
 * @returns 사용량 요약 응답
 */
export function usageSummary(overrides: Partial<UsageSummary> = {}): UsageSummary {
    return {
        period: { from: '2026-08-05', to: '2026-09-03' },
        total: {
            callCount: 40,
            inputTokens: 52000,
            outputTokens: 7300,
            untrackedCallCount: 3,
            unpricedCallCount: 0,
            estimatedCostUsd: 0.42,
            estimatedCostKrw: 560,
        },
        byFeature: [
            {
                feature: 'CHAT',
                callCount: 25,
                inputTokens: 40000,
                outputTokens: 6000,
                untrackedCallCount: 0,
            },
            {
                feature: 'ASSISTANT',
                callCount: 6,
                inputTokens: 9000,
                outputTokens: 900,
                untrackedCallCount: 0,
            },
            {
                feature: 'RECEIPT',
                callCount: 4,
                inputTokens: 2000,
                outputTokens: 300,
                untrackedCallCount: 0,
            },
            {
                feature: 'VOICE',
                callCount: 2,
                inputTokens: 1000,
                outputTokens: 100,
                untrackedCallCount: 0,
            },
            {
                feature: 'EMBEDDING',
                callCount: 3,
                inputTokens: 0,
                outputTokens: 0,
                untrackedCallCount: 3,
            },
        ],
        daily: [
            {
                date: '2026-09-01',
                callCount: 12,
                inputTokens: 16000,
                outputTokens: 2200,
                untrackedCallCount: 1,
            },
            {
                date: '2026-09-02',
                callCount: 18,
                inputTokens: 24000,
                outputTokens: 3300,
                untrackedCallCount: 1,
            },
            {
                date: '2026-09-03',
                callCount: 10,
                inputTokens: 12000,
                outputTokens: 1800,
                untrackedCallCount: 1,
            },
        ],
        ...overrides,
    }
}

/** 사용 기록이 하나도 없는 기간 — 화면의 빈 상태 문구를 검증할 때 쓴다 */
export function emptyUsageSummary(): UsageSummary {
    return usageSummary({
        total: {
            callCount: 0,
            inputTokens: 0,
            outputTokens: 0,
            untrackedCallCount: 0,
            unpricedCallCount: 0,
            estimatedCostUsd: 0,
            estimatedCostKrw: 0,
        },
        byFeature: [],
        daily: [],
    })
}
