import { defineStore } from 'pinia'
import { computed, reactive, ref } from 'vue'
import { receiptApi } from '../api/receipt.api'
import { extractErrorMessage } from '@/common/utils/error'
import type { ReceiptAnalysis, ReceiptOcrItem, ReceiptSaveRequest } from '../types'

/** 업로드 허용 형식·크기 (설계 F3-01) */
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp']
// 10MB — WEB/WAS 의 spring.servlet.multipart.max-file-size 와 반드시 일치시킬 것(어긋나면 서버가 먼저 거절)
const MAX_BYTES = 10 * 1024 * 1024

/**
 * 영수증 분석 상태.
 * 탭이 라우터 이동형이라 화면을 벗어나면 컴포넌트가 파괴된다.
 * OCR 결과와 사용자가 확인·수정 중인 값을 잃지 않도록 store 에 둔다.
 */
export const useReceiptStore = defineStore('receipt', () => {
    const file = ref<File | null>(null)
    const previewUrl = ref<string | null>(null)
    const analyzing = ref(false)
    const saving = ref(false)
    const saved = ref(false)
    const error = ref<string | null>(null)
    const analysis = ref<ReceiptAnalysis | null>(null)

    // 사용자가 확인·수정하는 최종 값 (분석 결과로 초기화)
    const form = reactive({
        payAmount: null as number | null,
        bizNo: '',
        payDate: '',
        cardName: '',
    })

    /** 저장 가능 조건 — 금액 0 이상, 사업자번호 10자리, 결제일 8자리 (WAS 검증과 동일) */
    const canSave = computed(
        () =>
            !saving.value &&
            form.payAmount !== null &&
            form.payAmount >= 0 &&
            /^\d{10}$/.test(form.bizNo) &&
            /^\d{8}$/.test(form.payDate),
    )

    /** 파일 선택/드롭 처리 — 형식·크기 선검증 후 미리보기 준비 */
    function selectFile(picked: File): void {
        if (!ALLOWED_TYPES.includes(picked.type)) {
            error.value = 'jpg / png / webp 형식만 업로드할 수 있습니다.'
            return
        }
        if (picked.size > MAX_BYTES) {
            error.value = '이미지 크기는 10MB 이하여야 합니다.'
            return
        }
        revokePreview()
        error.value = null
        analysis.value = null
        saved.value = false
        file.value = picked
        previewUrl.value = URL.createObjectURL(picked)
    }

    /** 선택된 이미지를 분석하고 편집 폼을 채운다 */
    async function analyze(): Promise<void> {
        if (!file.value || analyzing.value) return
        analyzing.value = true
        error.value = null
        try {
            const result = await receiptApi.analyze(file.value)
            analysis.value = result
            form.payAmount = result.payAmount
            form.bizNo = result.bizNo ?? ''
            form.payDate = result.payDate ?? ''
            form.cardName = result.cardName ?? ''
        } catch (e) {
            error.value = extractErrorMessage(e, '영수증 분석에 실패했습니다.')
        } finally {
            analyzing.value = false
        }
    }

    /**
     * OCR 항목 하나의 값으로 폼 전체(카드사·금액·사업자번호·결제일)를 채운다.
     * 사업자번호·결제일은 저장 규격(숫자만)에 맞춰 정규화한다(백엔드 정규화와 동일).
     * @param item 반영할 OCR 검출 항목
     */
    function applyItem(item: ReceiptOcrItem): void {
        form.cardName = item.cardName?.trim() ?? ''
        form.payAmount = item.payAmount ?? 0
        form.bizNo = (item.bizNo ?? '').replace(/\D/g, '').slice(0, 10)
        form.payDate = (item.payDate ?? '').replace(/\D/g, '')
    }

    /**
     * 카드사 드롭다운에서 카드를 고르면 호출 — 그 카드의 첫 OCR 항목 값으로 폼을 채운다.
     * items 는 카드별로 금액 등이 다를 수 있어(한 영수증에 여러 결제 검출), 카드만 바뀌고
     * 금액이 그대로 남지 않도록 함께 갱신한다.
     * @param cardName 선택한 카드명
     */
    function selectCard(cardName: string): void {
        const matched = analysis.value?.items.find((item) => item.cardName?.trim() === cardName)
        if (matched) applyItem(matched)
        else form.cardName = cardName
    }

    /**
     * 결제 후보 카드를 클릭하면 호출 — 그 항목 값으로 폼을 채운다(드롭다운과 달리 항목을 정확히 지정).
     * @param item 선택한 OCR 항목
     */
    function selectItem(item: ReceiptOcrItem): void {
        applyItem(item)
    }

    /** 확인·수정한 값을 최종 저장 */
    async function save(): Promise<boolean> {
        if (!analysis.value || !canSave.value) return false
        saving.value = true
        error.value = null
        try {
            const payload: ReceiptSaveRequest = {
                imagePath: analysis.value.imagePath,
                payAmount: form.payAmount!,
                bizNo: form.bizNo,
                payDate: form.payDate,
                cardName: form.cardName || null,
                selectType: analysis.value.selectType,
                rawJson: analysis.value.rawJson,
            }
            await receiptApi.save(payload)
            saved.value = true
            return true
        } catch (e) {
            error.value = extractErrorMessage(e, '영수증 저장에 실패했습니다.')
            return false
        } finally {
            saving.value = false
        }
    }

    /** 처음 상태로 초기화 (다른 영수증 분석) */
    function reset(): void {
        revokePreview()
        file.value = null
        analysis.value = null
        saved.value = false
        error.value = null
        form.payAmount = null
        form.bizNo = ''
        form.payDate = ''
        form.cardName = ''
    }

    /** objectURL 메모리 해제 */
    function revokePreview(): void {
        if (previewUrl.value) {
            URL.revokeObjectURL(previewUrl.value)
            previewUrl.value = null
        }
    }

    return {
        file,
        previewUrl,
        analyzing,
        saving,
        saved,
        error,
        analysis,
        form,
        canSave,
        selectFile,
        analyze,
        selectCard,
        selectItem,
        save,
        reset,
    }
})
