<script setup lang="ts">
/**
 * 영수증 [분석] 화면 (/receipt) — 업로드 → (미리보기) → 분석 → 결과 확인 폼 → 저장.
 * 상태·동작은 receipt store 가 보유하고, 여기선 화면 전환만 담당한다.
 * 저장에 성공하면 방금 등록한 건을 바로 보도록 이력 화면으로 이동한다.
 */
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { CheckCircle2, RotateCcw, ClipboardCopy } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { Badge } from '@/common/components/ui/badge'
import { Spinner } from '@/common/components/ui/spinner'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import { Card } from '@/common/components/ui/card'
import { Input } from '@/common/components/ui/input'
import { Label } from '@/common/components/ui/label'
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/common/components/ui/select'
import PageTabs from '@/common/components/PageTabs.vue'
import { formatBizNo, formatYmd } from '@/common/utils/format'
import { useReceiptStore } from '../stores/receipt.store'
import { receiptTabs } from '../routes'
import type { ReceiptOcrItem } from '../types'
import ReceiptUpload from '../components/ReceiptUpload.vue'

const router = useRouter()

const store = useReceiptStore()
// ref 상태는 storeToRefs 로 꺼내 반응성을 유지한다 (setup store 를 그냥 구조분해하면 반응성이 끊긴다)
const { previewUrl, analyzing, saving, saved, error, analysis, canSave } = storeToRefs(store)
const { selectFile, analyze, save, reset } = store
// form 은 reactive 객체 — store.form 으로 직접 접근해 반응성을 유지한다

// 금액(number|null)을 입력창(string)과 잇는 양방향 브리지 — 화면엔 천단위 콤마, 저장값은 숫자만
const amountStr = computed<string>({
    get: () => (store.form.payAmount === null ? '' : store.form.payAmount.toLocaleString('ko-KR')),
    set: (v) => {
        const digits = v.replace(/[^\d]/g, '')
        store.form.payAmount = digits === '' ? null : Number(digits)
    },
})

// 결제일을 화면엔 YYYY.MM.DD 로 보여주되 저장값(store.form.payDate)은 숫자 8자리만 유지하는 브리지
const payDateStr = computed<string>({
    get: () => formatYmd(store.form.payDate),
    set: (v) => {
        store.form.payDate = v.replace(/\D/g, '').slice(0, 8)
    },
})

// 사업자번호를 화면엔 하이픈(XXX-XX-XXXXX)으로 보여주되, 저장값(store.form.bizNo)은 숫자 10자리만 유지하는 브리지.
// WAS 체크섬 검증이 숫자 10자리를 요구하므로 하이픈은 표시용으로만 둔다.
const bizNoStr = computed<string>({
    get: () => formatBizNo(store.form.bizNo),
    set: (v) => {
        store.form.bizNo = v.replace(/\D/g, '').slice(0, 10)
    },
})

// 사업자번호 유효성 — form.bizNo 에 실시간 반응한다(입력·삭제 즉시 갱신).
// 서버의 1회성 결과가 아니라 현재 입력값으로 매번 체크섬을 계산하므로, 지우면 메시지가 사라지고
// 10자리가 아니면 판정을 보류('none')한다. 체크섬 규칙은 WAS ReceiptServiceImpl.isValidBizNo 와 동일.
const bizNoState = computed<'none' | 'valid' | 'invalid'>(() => {
    const no = store.form.bizNo
    if (!/^\d{10}$/.test(no)) return 'none'
    const weights = [1, 3, 7, 1, 3, 7, 1, 3]
    let sum = weights.reduce((acc, w, i) => acc + w * Number(no.charAt(i)), 0)
    const d9 = Number(no.charAt(8)) * 5
    sum += Math.floor(d9 / 10) + (d9 % 10)
    const checksum = (10 - (sum % 10)) % 10
    return checksum === Number(no.charAt(9)) ? 'valid' : 'invalid'
})

// 카드사 드롭다운 후보 — OCR 이 검출한 카드명을 (등장 순서 유지·중복 제거) 목록화.
// 첫 번째 후보는 서버가 이미 form.cardName 에 제안값으로 채워두어 자동 선택된 상태로 보인다.
const cardOptions = computed<string[]>(() => {
    const names = (analysis.value?.items ?? [])
        .map((item) => item.cardName?.trim())
        .filter((name): name is string => !!name)
    return [...new Set(names)]
})

/** 결제 후보 카드가 현재 선택(카드명+금액)과 일치하는지 — 선택 강조용 */
function isCandidateSelected(item: ReceiptOcrItem): boolean {
    return (
        item.cardName?.trim() === store.form.cardName &&
        (item.payAmount ?? 0) === store.form.payAmount
    )
}

/**
 * 추출 결과를 AI 원본 JSON 형태(선택된 카드 항목)로 클립보드에 복사한다.
 * items 는 OCR 원본(OcrResultVo)과 동일 구조라, 선택 항목을 그대로 직렬화하면 원본 JSON이 된다.
 */
async function copyResultJson(): Promise<void> {
    const items = analysis.value?.items ?? []
    // 현재 선택(카드+금액)과 일치하는 원본 항목을 찾고, 없으면 카드명 기준·첫 항목 순으로 폴백
    const selected =
        items.find((i) => isCandidateSelected(i)) ??
        items.find((i) => i.cardName?.trim() === store.form.cardName) ??
        items[0]
    if (!selected) {
        toast.error('복사할 추출 결과가 없습니다.')
        return
    }
    try {
        await navigator.clipboard.writeText(JSON.stringify(selected, null, 2))
        toast.success('추출 결과 JSON을 복사했습니다.')
    } catch {
        toast.error('클립보드 복사에 실패했습니다.')
    }
}

async function onSave(): Promise<void> {
    const ok = await save()
    if (ok) router.push({ name: 'receipt-history' })
}
</script>

<template>
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-4xl px-6 py-8">
            <h1 class="mb-6 text-2xl font-semibold">영수증</h1>

            <PageTabs :tabs="receiptTabs" />

            <div class="flex flex-col gap-4">
                <Alert v-if="error" variant="destructive">
                    <AlertDescription>{{ error }}</AlertDescription>
                </Alert>

                <!-- 1) 업로드 전 -->
                <ReceiptUpload v-if="!previewUrl" @select="selectFile" />

                <!-- 2) 이미지 선택됨 · 분석 결과 -->
                <div v-else class="flex flex-col gap-4 md:flex-row md:items-start">
                    <!-- 원본 이미지 미리보기 -->
                    <div class="md:w-2/5">
                        <img
                            :src="previewUrl"
                            alt="영수증 미리보기"
                            class="w-full rounded-lg border object-contain"
                        />
                    </div>

                    <!-- 우측: 분석 전이면 버튼, 분석 후면 확인 폼 -->
                    <div class="flex flex-col gap-4 md:flex-1">
                        <!-- 분석 전 -->
                        <template v-if="!analysis">
                            <div class="flex gap-2">
                                <Button :disabled="analyzing" @click="analyze">
                                    <Spinner v-if="analyzing" class="mr-2 size-4" />
                                    {{ analyzing ? '분석 중…' : '분석하기' }}
                                </Button>
                                <Button variant="outline" :disabled="analyzing" @click="reset">
                                    다시 선택
                                </Button>
                            </div>
                            <p class="text-sm text-muted-foreground">
                                분석하면 결과가 이력에 자동 저장되며, 아래에서 값을 확인·수정할 수
                                있습니다.
                            </p>
                        </template>

                        <!-- 분석 후: 확인 폼 — 왼쪽 이미지처럼 제목 있는 카드로 감싼다 -->
                        <Card v-else class="gap-0 overflow-hidden py-0">
                            <!-- 헤더: 제목 + 상태 배지 + 결과 JSON 복사 -->
                            <div class="flex items-center justify-between gap-2 border-b px-5 py-4">
                                <div class="flex items-center gap-2">
                                    <span class="text-sm font-semibold">추출 결과 확인</span>
                                    <Badge v-if="analysis.selectType === 'AUTO'" variant="default">
                                        ✅ 자동 선택
                                    </Badge>
                                    <Badge v-else variant="secondary">
                                        ✋ 후보 {{ cardOptions.length }}건
                                    </Badge>
                                </div>
                                <Button variant="outline" size="sm" @click="copyResultJson">
                                    <ClipboardCopy class="mr-1.5 size-4" />
                                    JSON 복사
                                </Button>
                            </div>

                            <div class="flex flex-col gap-5 p-5">
                                <!-- 카드사 (맨 위) — 후보 드롭다운, 없으면 직접 입력 -->
                                <div class="flex flex-col gap-1.5">
                                    <Label>카드사</Label>
                                    <Select
                                        v-if="cardOptions.length > 0"
                                        :model-value="store.form.cardName"
                                        @update:model-value="(v) => store.selectCard(String(v))"
                                    >
                                        <SelectTrigger class="w-full">
                                            <SelectValue placeholder="카드사 선택" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem
                                                v-for="name in cardOptions"
                                                :key="name"
                                                :value="name"
                                            >
                                                {{ name }}
                                            </SelectItem>
                                        </SelectContent>
                                    </Select>
                                    <Input
                                        v-else
                                        v-model="store.form.cardName"
                                        placeholder="카드사 직접 입력"
                                    />
                                </div>

                                <!-- 금액 히어로 -->
                                <div class="flex flex-col gap-1.5">
                                    <Label>결제 금액</Label>
                                    <div
                                        class="flex items-baseline gap-2 rounded-lg border px-4 py-2.5 focus-within:ring-2 focus-within:ring-ring"
                                    >
                                        <span class="text-xl text-muted-foreground">₩</span>
                                        <input
                                            v-model="amountStr"
                                            inputmode="numeric"
                                            aria-label="결제 금액"
                                            class="w-full bg-transparent text-3xl font-bold tabular-nums outline-none"
                                        />
                                        <span class="text-muted-foreground">원</span>
                                    </div>
                                </div>

                                <!-- 사업자번호 / 결제일 -->
                                <div class="grid gap-4 sm:grid-cols-2">
                                    <div class="flex flex-col gap-1.5">
                                        <Label>사업자등록번호</Label>
                                        <Input
                                            v-model="bizNoStr"
                                            inputmode="numeric"
                                            placeholder="000-00-00000"
                                        />
                                        <p
                                            v-if="bizNoState === 'invalid'"
                                            class="text-xs text-destructive"
                                        >
                                            ⚠ 체크섬 검증 실패 — 번호를 확인해 주세요.
                                        </p>
                                        <p
                                            v-else-if="bizNoState === 'valid'"
                                            class="text-xs text-green-600"
                                        >
                                            ✓ 유효한 사업자번호 형식입니다.
                                        </p>
                                    </div>
                                    <div class="flex flex-col gap-1.5">
                                        <Label>결제일</Label>
                                        <Input
                                            v-model="payDateStr"
                                            inputmode="numeric"
                                            placeholder="YYYY.MM.DD"
                                        />
                                    </div>
                                </div>

                                <!-- 검출된 결제 후보 — 2건 이상일 때. 누르면 그 항목 값으로 폼을 채운다 -->
                                <div
                                    v-if="analysis.items.length > 1"
                                    class="flex flex-col gap-2 border-t border-dashed pt-4"
                                >
                                    <p class="text-xs font-medium text-muted-foreground">
                                        🔎 검출된 결제 후보 · 누르면 그 값으로 채워집니다
                                    </p>
                                    <button
                                        v-for="(item, idx) in analysis.items"
                                        :key="idx"
                                        type="button"
                                        class="flex items-center justify-between gap-3 rounded-lg border px-3 py-2.5 text-left transition-colors hover:bg-accent"
                                        :class="
                                            isCandidateSelected(item)
                                                ? 'border-foreground bg-accent ring-1 ring-foreground'
                                                : ''
                                        "
                                        @click="store.selectItem(item)"
                                    >
                                        <span class="text-sm font-medium">
                                            {{ item.cardName || '카드사 미검출' }}
                                        </span>
                                        <span
                                            class="whitespace-nowrap text-xs tabular-nums text-muted-foreground"
                                        >
                                            <span class="font-semibold text-foreground">
                                                {{
                                                    (item.payAmount ?? 0).toLocaleString('ko-KR')
                                                }}원
                                            </span>
                                            · {{ formatYmd(item.payDate) || '—' }}
                                        </span>
                                    </button>
                                </div>

                                <!-- 액션 -->
                                <div class="flex items-center gap-2">
                                    <Button :disabled="!canSave" @click="onSave">
                                        <Spinner v-if="saving" class="mr-2 size-4" />
                                        {{ saving ? '저장 중…' : '저장' }}
                                    </Button>
                                    <Button variant="outline" @click="reset">
                                        <RotateCcw class="mr-2 size-4" />
                                        다른 영수증
                                    </Button>
                                    <span
                                        v-if="saved"
                                        class="flex items-center gap-1 text-sm text-green-600"
                                    >
                                        <CheckCircle2 class="size-4" /> 저장됨
                                    </span>
                                </div>
                            </div>
                        </Card>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
