<script setup lang="ts">
/**
 * 영수증 [이력] 화면 (/receipt/history) — 등록 목록(최신순) + CSV 다운로드.
 * 목록은 결제일·금액·사업자번호·검증상태를 보여준다.
 */
import { onMounted } from 'vue'
import { Download } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { Badge } from '@/common/components/ui/badge'
import { Spinner } from '@/common/components/ui/spinner'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import PageTabs from '@/common/components/PageTabs.vue'
import { useReceiptHistory } from '../composables/useReceiptHistory'
import { receiptTabs } from '../routes'

const { receipts, loading, error, load, downloadCsv } = useReceiptHistory()

onMounted(load)

/** YYYYMMDD → YYYY.MM.DD */
function formatDate(yyyymmdd: string): string {
    if (!/^\d{8}$/.test(yyyymmdd)) return yyyymmdd
    return `${yyyymmdd.slice(0, 4)}.${yyyymmdd.slice(4, 6)}.${yyyymmdd.slice(6, 8)}`
}

/** 10자리 사업자번호 → 123-45-67890 */
function formatBizNo(bizNo: string): string {
    if (!/^\d{10}$/.test(bizNo)) return bizNo
    return `${bizNo.slice(0, 3)}-${bizNo.slice(3, 5)}-${bizNo.slice(5)}`
}

/** 금액 천단위 콤마 */
function formatAmount(amount: number): string {
    return amount.toLocaleString('ko-KR')
}
</script>

<template>
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-4xl px-6 py-8">
            <h1 class="mb-6 text-2xl font-semibold">영수증</h1>

            <PageTabs :tabs="receiptTabs" />

            <div class="flex flex-col gap-4">
                <div class="flex items-center justify-between">
                    <p class="text-sm text-muted-foreground">
                        총 <span class="font-medium text-foreground">{{ receipts.length }}</span
                        >건
                    </p>
                    <Button
                        variant="outline"
                        size="sm"
                        :disabled="receipts.length === 0"
                        @click="downloadCsv"
                    >
                        <Download class="mr-2 size-4" />
                        CSV 다운로드
                    </Button>
                </div>

                <Alert v-if="error" variant="destructive">
                    <AlertDescription>{{ error }}</AlertDescription>
                </Alert>

                <div v-if="loading" class="flex justify-center py-16">
                    <Spinner class="size-6" />
                </div>

                <p
                    v-else-if="receipts.length === 0"
                    class="py-16 text-center text-sm text-muted-foreground"
                >
                    아직 분석한 영수증이 없습니다.
                </p>

                <div v-else class="overflow-x-auto rounded-lg border">
                    <table class="w-full text-sm">
                        <thead class="border-b bg-muted/40 text-left text-muted-foreground">
                            <tr>
                                <th class="px-4 py-2.5 font-medium">결제일</th>
                                <th class="px-4 py-2.5 text-right font-medium">금액</th>
                                <th class="px-4 py-2.5 font-medium">사업자번호</th>
                                <th class="px-4 py-2.5 font-medium">카드사</th>
                                <th class="px-4 py-2.5 font-medium">검증</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr
                                v-for="r in receipts"
                                :key="r.receiptSeq"
                                class="border-b last:border-0 hover:bg-accent/40"
                            >
                                <td class="px-4 py-2.5">{{ formatDate(r.payDate) }}</td>
                                <td class="px-4 py-2.5 text-right tabular-nums">
                                    {{ formatAmount(r.payAmount) }}원
                                </td>
                                <td class="px-4 py-2.5 tabular-nums">
                                    {{ formatBizNo(r.bizNo) }}
                                </td>
                                <td class="px-4 py-2.5">{{ r.cardName || '—' }}</td>
                                <td class="px-4 py-2.5">
                                    <Badge v-if="r.bizNoValid" variant="secondary">✅ 정상</Badge>
                                    <Badge v-else variant="destructive">⚠ 확인필요</Badge>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</template>
