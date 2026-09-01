<script setup lang="ts">
/**
 * 내 사용량 (/usage) — 로그인한 사용자가 본인의 LLM 사용량을 확인한다.
 *
 * 관리자 대시보드(/admin/usage)와 표시부(UsageOverview)를 공유하되, 사용자별 목록은 없다.
 * 조회 대상은 서버가 세션에서 정하므로 이 화면은 사용자 번호를 다루지 않는다.
 */
import { onMounted } from 'vue'
import { BarChart3 } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import PageHeader from '@/common/components/PageHeader.vue'
import LoadingArea from '@/common/components/LoadingArea.vue'
import UsageOverview from '@/common/components/usage/UsageOverview.vue'
import { useMyUsage } from '../composables/useMyUsage'

const { preset, summary, chartBuckets, chartUnit, loading, error, load, setPreset } = useMyUsage()

onMounted(load)
</script>

<template>
    <!-- AppLayout 의 <main> 이 overflow-hidden 이라 스크롤은 각 화면이 만든다 -->
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-5xl px-6 py-8">
            <PageHeader
                :icon="BarChart3"
                title="내 사용량"
                description="내가 사용한 AI 호출 수와 토큰, 추정 비용을 확인합니다."
            />

            <!-- 기간 선택 -->
            <div class="mb-6 flex flex-wrap items-center gap-2">
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
            </div>

            <Alert v-if="error" variant="destructive" class="mb-6">
                <AlertDescription>{{ error }}</AlertDescription>
            </Alert>

            <LoadingArea v-if="loading && !summary" />

            <UsageOverview
                v-else-if="summary"
                :summary="summary"
                :buckets="chartBuckets"
                :chart-unit="chartUnit"
                empty-message="선택한 기간에 사용 기록이 없습니다. 채팅·영수증·회의록 기능을 사용하면 이곳에 집계됩니다."
            />
        </div>
    </div>
</template>
