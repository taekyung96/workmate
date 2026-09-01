<script setup lang="ts">
/**
 * 사용량 개요 표시부 (요약 카드 · 기능별 · 사용 추이).
 *
 * 관리자 대시보드(전체)와 본인 사용량 화면이 같은 모양을 공유한다.
 * 데이터를 가져오는 책임은 없다 — 받은 것을 그리기만 하는 표시 전용 컴포넌트다.
 *
 * 차트 라이브러리를 쓰지 않는다: 기능 5종 + 막대 7개 이하 규모라 CSS 로 충분하고,
 * 수백 KB 의존성을 SPA 번들에 더할 이유가 없다. 수치는 텍스트로도 병기한다.
 */
import { computed } from 'vue'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import type { UsageSummary } from '@/common/types/usage'
import type { UsageBucket } from '@/common/utils/usageBuckets'

const props = withDefaults(
    defineProps<{
        /** 서버가 준 기간 집계 */
        summary: UsageSummary
        /** 차트용 막대 (일별 또는 주별로 묶인 것) */
        buckets: UsageBucket[]
        /** 막대 단위 표기 — '일별' | '주별' */
        chartUnit: string
        /** 데이터가 없을 때 보여줄 안내 문구 */
        emptyMessage?: string
    }>(),
    {
        emptyMessage:
            '선택한 기간에 기록된 LLM 호출이 없습니다. 채팅·영수증·회의록 기능을 사용하면 이곳에 집계됩니다.',
    },
)

/** 막대 영역 높이(px) — 상수로 두고 막대도 px 로 계산한다 */
const CHART_HEIGHT = 96

/** 천 단위 구분 기호. null·undefined 는 '—' 로 (0 과 구분해야 한다) */
function num(v: number | null | undefined): string {
    return v === null || v === undefined ? '—' : v.toLocaleString('ko-KR')
}

/** 가로 막대 폭(%) — 0 건에는 최소 폭을 주지 않는다(0 은 0 으로 보여야 한다) */
function barWidth(value: number, max: number): string {
    if (max <= 0 || value <= 0) return '0%'
    return `${Math.max((value / max) * 100, 2)}%`
}

/**
 * 세로 막대 높이(px).
 * %로 주면 부모 높이가 auto 일 때 0 으로 붕괴하므로 px 로 계산한다.
 * 1건 이상이면 최소 3px 을 줘서 "있는데 안 보이는" 상태를 막는다.
 */
function barHeight(value: number, max: number): string {
    if (max <= 0 || value <= 0) return '0px'
    return `${Math.max(Math.round((value / max) * CHART_HEIGHT), 3)}px`
}

const featureMax = computed(() => Math.max(0, ...props.summary.byFeature.map((f) => f.callCount)))
const bucketMax = computed(() => Math.max(0, ...props.buckets.map((b) => b.callCount)))

/** 기간 내 호출이 한 건도 없는가 — 빈 상태 안내를 띄울지 판단한다 */
const isEmpty = computed(() => props.summary.total.callCount === 0)
</script>

<template>
    <p class="mb-4 text-sm text-muted-foreground">
        {{ summary.period.from }} ~ {{ summary.period.to }}
    </p>

    <!-- 요약 카드 -->
    <div class="mb-2 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <div class="rounded-lg border p-4">
            <p class="text-sm text-muted-foreground">총 호출</p>
            <p class="mt-1 text-2xl font-semibold">{{ num(summary.total.callCount) }}</p>
        </div>
        <div class="rounded-lg border p-4">
            <p class="text-sm text-muted-foreground">입력 토큰</p>
            <p class="mt-1 text-2xl font-semibold">{{ num(summary.total.inputTokens) }}</p>
        </div>
        <div class="rounded-lg border p-4">
            <p class="text-sm text-muted-foreground">출력 토큰</p>
            <p class="mt-1 text-2xl font-semibold">{{ num(summary.total.outputTokens) }}</p>
        </div>
        <div class="rounded-lg border p-4">
            <p class="text-sm text-muted-foreground">추정 비용</p>
            <p class="mt-1 text-2xl font-semibold">₩{{ num(summary.total.estimatedCostKrw) }}</p>
            <p class="text-xs text-muted-foreground">
                ${{ summary.total.estimatedCostUsd.toFixed(4) }}
            </p>
        </div>
    </div>

    <!--
        집계에서 빠진 건수를 반드시 드러낸다.
        토큰이 NULL 인 행(임베딩 등)을 0 으로 합산하면 합계가 거짓말을 한다.
    -->
    <p class="mb-6 text-xs text-muted-foreground">
        ※ 비용은 설정된 모델 단가로 계산한 <strong>추정치</strong>이며 실제 청구액이 아닙니다.
        <template v-if="summary.total.untrackedCallCount > 0">
            토큰이 집계되지 않은 호출 {{ num(summary.total.untrackedCallCount) }}건은 토큰 합계에서
            제외됐습니다.
        </template>
        <template v-if="summary.total.unpricedCallCount > 0">
            단가가 등록되지 않은 모델 {{ num(summary.total.unpricedCallCount) }}건은 비용 계산에서
            제외됐습니다.
        </template>
    </p>

    <Alert v-if="isEmpty" class="mb-6">
        <AlertDescription>{{ emptyMessage }}</AlertDescription>
    </Alert>

    <!-- 기능별 -->
    <section class="mb-8">
        <h2 class="mb-3 text-sm font-semibold">기능별</h2>
        <ul class="flex flex-col gap-2">
            <li v-for="f in summary.byFeature" :key="f.feature" class="flex items-center gap-3">
                <span class="w-24 shrink-0 text-sm text-muted-foreground">{{ f.feature }}</span>
                <span class="h-2 min-w-0 flex-1 rounded-full bg-muted">
                    <span
                        class="block h-2 rounded-full bg-primary"
                        :style="{ width: barWidth(f.callCount, featureMax) }"
                    />
                </span>
                <span class="w-32 shrink-0 text-right text-sm tabular-nums">
                    {{ num(f.callCount) }}건
                </span>
            </li>
        </ul>
    </section>

    <!--
        사용 추이 — 기간이 길면 호출부에서 주 단위로 묶어 막대를 7개 이하로 넘긴다.
        30일을 일별로 그리면 막대가 30개라 좁아서 라벨이 겹치고, 대부분이 0 이라 읽을 것도 없다.
    -->
    <section class="mb-8">
        <h2 class="mb-3 text-sm font-semibold">
            사용 추이
            <span class="ml-1 font-normal text-muted-foreground">({{ chartUnit }})</span>
        </h2>
        <div class="flex items-end gap-2">
            <div
                v-for="b in buckets"
                :key="b.label"
                class="flex min-w-0 flex-1 flex-col items-center"
                :title="b.title"
            >
                <!-- 막대 위 건수 — 막대만으로는 2건인지 3건인지 구분되지 않는다 -->
                <span class="mb-1 text-xs tabular-nums text-muted-foreground">
                    {{ b.callCount }}
                </span>
                <!-- 막대 영역: 높이를 고정해야 px 계산이 의미를 갖는다 -->
                <div
                    class="flex w-full items-end justify-center"
                    :style="{ height: `${CHART_HEIGHT}px` }"
                >
                    <span
                        class="w-full rounded-t bg-primary"
                        :style="{ height: barHeight(b.callCount, bucketMax) }"
                    />
                </div>
                <span
                    class="mt-1 whitespace-nowrap text-xs text-muted-foreground"
                    :class="{ 'opacity-60': b.callCount === 0 }"
                >
                    {{ b.label }}
                </span>
            </div>
        </div>
    </section>
</template>
