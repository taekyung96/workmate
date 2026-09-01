<script setup lang="ts">
/**
 * 관리자 — LLM 사용량 대시보드 (/admin/usage).
 * 기간별 호출 수·토큰·추정 비용을 요약하고, 기능별·일별·사용자별로 나눠 보여준다.
 * 접근 제어는 라우트 가드(requiresAdmin) + WEB Security(`/api/v1/admin/**`)가 담당한다.
 *
 * 차트 라이브러리를 쓰지 않는다 — 기능 5종 + 최근 30일 규모라 막대는 CSS 폭으로 충분하고,
 * 수백 KB 의존성을 SPA 번들에 더할 이유가 없다. 접근성을 위해 수치를 텍스트로 병기한다.
 */
import { computed, onMounted } from 'vue'
import { BarChart3 } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { Input } from '@/common/components/ui/input'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import PageHeader from '@/common/components/PageHeader.vue'
import PageTabs from '@/common/components/PageTabs.vue'
import LoadingArea from '@/common/components/LoadingArea.vue'
import Pagination from '@/common/components/Pagination.vue'
import { adminTabs } from '../constants'
import { useUsageStats } from '../composables/useUsageStats'

const {
    preset,
    customFrom,
    customTo,
    summary,
    users,
    page,
    totalPages,
    totalElements,
    loading,
    error,
    load,
    setPreset,
    changePage,
} = useUsageStats()

onMounted(load)

/** 천 단위 구분 기호. null·undefined 는 '—' 로 (0 과 구분해야 한다) */
function num(v: number | null | undefined): string {
    return v === null || v === undefined ? '—' : v.toLocaleString('ko-KR')
}

/** 막대 폭(%) — 최댓값 대비 비율. 0 건이어도 최소 폭을 주지 않는다(0 은 0 으로 보여야 한다) */
function barWidth(value: number, max: number): string {
    if (max <= 0 || value <= 0) return '0%'
    return `${Math.max((value / max) * 100, 2)}%`
}

const featureMax = computed(() =>
    Math.max(0, ...(summary.value?.byFeature ?? []).map((f) => f.callCount)),
)
const dailyMax = computed(() =>
    Math.max(0, ...(summary.value?.daily ?? []).map((d) => d.callCount)),
)

/** 기간 내 호출이 한 건도 없는가 — 빈 상태 안내를 띄울지 판단한다 */
const isEmpty = computed(() => !!summary.value && summary.value.total.callCount === 0)
</script>

<template>
    <div class="mx-auto w-full max-w-6xl px-4 py-6">
        <PageHeader
            :icon="BarChart3"
            title="사용량"
            description="LLM 호출 수와 토큰 사용량, 추정 비용을 기간별로 확인합니다."
        />
        <PageTabs :tabs="adminTabs" />

        <!-- 기간 선택 -->
        <div class="mb-6 flex flex-wrap items-end gap-2">
            <Button
                :variant="preset === '7' ? 'default' : 'outline'"
                size="sm"
                @click="setPreset('7')"
            >
                최근 7일
            </Button>
            <Button
                :variant="preset === '30' ? 'default' : 'outline'"
                size="sm"
                @click="setPreset('30')"
            >
                최근 30일
            </Button>
            <Button
                :variant="preset === 'custom' ? 'default' : 'outline'"
                size="sm"
                @click="setPreset('custom')"
            >
                직접 지정
            </Button>

            <template v-if="preset === 'custom'">
                <Input v-model="customFrom" type="date" class="w-40" aria-label="시작일" />
                <span class="pb-2 text-sm text-muted-foreground">~</span>
                <Input v-model="customTo" type="date" class="w-40" aria-label="종료일" />
                <Button size="sm" :disabled="loading" @click="load()">조회</Button>
            </template>
        </div>

        <Alert v-if="error" variant="destructive" class="mb-6">
            <AlertDescription>{{ error }}</AlertDescription>
        </Alert>

        <LoadingArea v-if="loading && !summary" />

        <template v-else-if="summary">
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
                    <p class="mt-1 text-2xl font-semibold">
                        ₩{{ num(summary.total.estimatedCostKrw) }}
                    </p>
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
                ※ 비용은 설정된 모델 단가로 계산한 <strong>추정치</strong>이며 실제 청구액이
                아닙니다.
                <template v-if="summary.total.untrackedCallCount > 0">
                    토큰이 집계되지 않은 호출 {{ num(summary.total.untrackedCallCount) }}건은 토큰
                    합계에서 제외됐습니다.
                </template>
                <template v-if="summary.total.unpricedCallCount > 0">
                    단가가 등록되지 않은 모델 {{ num(summary.total.unpricedCallCount) }}건은 비용
                    계산에서 제외됐습니다.
                </template>
            </p>

            <Alert v-if="isEmpty" class="mb-6">
                <AlertDescription>
                    선택한 기간에 기록된 LLM 호출이 없습니다. 채팅·영수증·회의록 기능을 사용하면
                    이곳에 집계됩니다.
                </AlertDescription>
            </Alert>

            <!-- 기능별 -->
            <section class="mb-8">
                <h2 class="mb-3 text-sm font-semibold">기능별</h2>
                <ul class="flex flex-col gap-2">
                    <li
                        v-for="f in summary.byFeature"
                        :key="f.feature"
                        class="flex items-center gap-3"
                    >
                        <span class="w-24 shrink-0 text-sm text-muted-foreground">
                            {{ f.feature }}
                        </span>
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

            <!-- 일별 추이 -->
            <section class="mb-8">
                <h2 class="mb-3 text-sm font-semibold">일별 추이</h2>
                <div class="overflow-x-auto">
                    <div class="flex min-w-max items-end gap-1" style="height: 120px">
                        <div
                            v-for="d in summary.daily"
                            :key="d.date"
                            class="flex w-6 flex-col items-center justify-end gap-1"
                            :title="`${d.date} — ${d.callCount}건`"
                        >
                            <span
                                class="w-full rounded-t bg-primary"
                                :style="{ height: barWidth(d.callCount, dailyMax) }"
                            />
                            <span class="text-[10px] text-muted-foreground">
                                {{ d.date.slice(5) }}
                            </span>
                        </div>
                    </div>
                </div>
            </section>

            <!-- 사용자별 -->
            <section>
                <h2 class="mb-3 text-sm font-semibold">
                    사용자별
                    <span class="font-normal text-muted-foreground">
                        (총 {{ num(totalElements) }}명)
                    </span>
                </h2>

                <div class="overflow-x-auto rounded-lg border">
                    <table class="w-full text-sm">
                        <thead class="bg-muted/50 text-left">
                            <tr>
                                <th class="px-4 py-2 font-medium">사용자</th>
                                <th class="px-4 py-2 text-right font-medium">호출</th>
                                <th class="px-4 py-2 text-right font-medium">입력</th>
                                <th class="px-4 py-2 text-right font-medium">출력</th>
                                <th class="px-4 py-2 text-right font-medium">추정 비용</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-if="users.length === 0">
                                <td colspan="5" class="px-4 py-8 text-center text-muted-foreground">
                                    표시할 사용자가 없습니다.
                                </td>
                            </tr>
                            <tr v-for="u in users" :key="u.userSeq" class="border-t">
                                <td class="px-4 py-2">
                                    <span>{{ u.userName }}</span>
                                    <span class="ml-2 text-xs text-muted-foreground">
                                        {{ u.maskedEmail }}
                                    </span>
                                </td>
                                <td class="px-4 py-2 text-right tabular-nums">
                                    {{ num(u.callCount) }}
                                </td>
                                <td class="px-4 py-2 text-right tabular-nums">
                                    {{ num(u.inputTokens) }}
                                </td>
                                <td class="px-4 py-2 text-right tabular-nums">
                                    {{ num(u.outputTokens) }}
                                </td>
                                <td class="px-4 py-2 text-right tabular-nums">
                                    ₩{{ num(u.estimatedCostKrw) }}
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <Pagination
                    v-if="totalPages > 1"
                    class="mt-4"
                    :page="page"
                    :total-pages="totalPages"
                    @change="changePage"
                />
            </section>
        </template>
    </div>
</template>
