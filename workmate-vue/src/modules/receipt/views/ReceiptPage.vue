<script setup lang="ts">
/**
 * 영수증 화면 (/receipt) — [분석]/[이력] 한 화면 + 탭 전환 (설계 F3-13).
 * 분석 탭은 진행 상태를 잃지 않도록 v-show로 유지, 이력 탭은 방문할 때마다 최신을 재조회(v-if).
 * 저장에 성공하면 방금 등록한 건을 바로 보도록 이력 탭으로 전환한다.
 */
import { ref } from 'vue'
import ReceiptAnalyzeTab from '../components/ReceiptAnalyzeTab.vue'
import ReceiptHistoryTab from '../components/ReceiptHistoryTab.vue'

type Tab = 'analyze' | 'history'
const tab = ref<Tab>('analyze')

const tabs: { key: Tab; label: string }[] = [
    { key: 'analyze', label: '분석' },
    { key: 'history', label: '이력' },
]
</script>

<template>
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-4xl px-6 py-8">
            <div class="mb-6 flex items-center gap-6">
                <h1 class="text-2xl font-semibold">영수증</h1>
                <div class="flex gap-1 rounded-lg bg-muted p-1">
                    <button
                        v-for="t in tabs"
                        :key="t.key"
                        type="button"
                        class="rounded-md px-4 py-1.5 text-sm font-medium transition-colors"
                        :class="
                            tab === t.key
                                ? 'bg-background text-foreground shadow-sm'
                                : 'text-muted-foreground hover:text-foreground'
                        "
                        @click="tab = t.key"
                    >
                        {{ t.label }}
                    </button>
                </div>
            </div>

            <!-- 분석 탭: 상태 유지 -->
            <ReceiptAnalyzeTab v-show="tab === 'analyze'" @saved="tab = 'history'" />
            <!-- 이력 탭: 방문 시 재조회 -->
            <ReceiptHistoryTab v-if="tab === 'history'" />
        </div>
    </div>
</template>
