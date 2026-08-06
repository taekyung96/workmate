<script setup lang="ts">
/**
 * 영수증 [이력] 화면 (/receipt/history) — 등록 목록(최신순) + CSV 다운로드.
 * 목록은 결제일·금액·사업자번호·검증상태를 보여준다.
 */
import { onMounted } from 'vue'
import { Download, Receipt } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { Badge } from '@/common/components/ui/badge'
import LoadingArea from '@/common/components/LoadingArea.vue'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import PageTabs from '@/common/components/PageTabs.vue'
import PageHeader from '@/common/components/PageHeader.vue'
import Pagination from '@/common/components/Pagination.vue'
import { formatBizNo, formatYmd, formatAmount } from '@/common/utils/format'
import { useReceiptHistory } from '../composables/useReceiptHistory'
import { receiptTabs } from '../routes'

const { receipts, page, totalPages, totalElements, loading, error, load, goToPage, downloadCsv } =
    useReceiptHistory()

onMounted(load)
</script>

<template>
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-4xl px-6 py-8">
            <PageHeader
                :icon="Receipt"
                title="영수증"
                description="사진 한 장이면 금액·사업자번호·카드사를 AI가 읽어 정리합니다."
            />

            <PageTabs :tabs="receiptTabs" />

            <div class="flex flex-col gap-4">
                <div class="flex items-center justify-end">
                    <Button
                        variant="outline"
                        size="sm"
                        :disabled="totalElements === 0"
                        @click="downloadCsv"
                    >
                        <Download class="mr-2 size-4" />
                        CSV 다운로드
                    </Button>
                </div>

                <Alert v-if="error" variant="destructive">
                    <AlertDescription>{{ error }}</AlertDescription>
                </Alert>

                <LoadingArea v-if="loading" />

                <p
                    v-else-if="totalElements === 0"
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
                                <td class="px-4 py-2.5">{{ formatYmd(r.payDate) }}</td>
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

                <!-- 페이징 -->
                <div
                    v-if="!loading && totalElements > 0"
                    class="flex items-center justify-between text-sm text-muted-foreground"
                >
                    <span>총 {{ totalElements }}건</span>
                    <Pagination :page="page" :total-pages="totalPages" @change="goToPage" />
                </div>
            </div>
        </div>
    </div>
</template>
