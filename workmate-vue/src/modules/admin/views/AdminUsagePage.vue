<script setup lang="ts">
/**
 * 관리자 — LLM 사용량 대시보드 (/admin/usage).
 * 기간별 호출 수·토큰·추정 비용을 요약하고, 기능별·일별·사용자별로 나눠 보여준다.
 * 접근 제어는 라우트 가드(requiresAdmin) + WEB Security(`/api/v1/admin/**`)가 담당한다.
 *
 * 요약·기능별·추이 표시는 common/components/usage/UsageOverview 가 담당한다
 * (본인 사용량 화면과 같은 모양을 공유한다). 이 화면은 기간 선택과 사용자별 표를 더한다.
 */
import { onMounted } from 'vue'
import { BarChart3 } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { Input } from '@/common/components/ui/input'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import PageHeader from '@/common/components/PageHeader.vue'
import PageTabs from '@/common/components/PageTabs.vue'
import LoadingArea from '@/common/components/LoadingArea.vue'
import Pagination from '@/common/components/Pagination.vue'
import UsageOverview from '@/common/components/usage/UsageOverview.vue'
import { adminTabs } from '../constants'
import { useUsageStats } from '../composables/useUsageStats'

const {
    preset,
    customFrom,
    customTo,
    summary,
    chartBuckets,
    chartUnit,
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
</script>

<template>
    <!--
        AppLayout 의 <main> 이 overflow-hidden 이라 스크롤은 각 화면이 만든다.
        다른 관리자 화면과 같은 구조를 쓴다 — 이게 없으면 화면 아래쪽을 볼 수 없다.
    -->
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-5xl px-6 py-8">
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
                <UsageOverview :summary="summary" :buckets="chartBuckets" :chart-unit="chartUnit" />

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
                                    <td
                                        colspan="5"
                                        class="px-4 py-8 text-center text-muted-foreground"
                                    >
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
    </div>
</template>
