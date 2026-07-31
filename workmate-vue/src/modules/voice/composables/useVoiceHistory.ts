import { ref } from 'vue'
import { extractErrorMessage } from '@/common/utils/error'
import { voiceApi } from '../api/voice.api'
import type { VoiceRecordSummary } from '../types'

/**
 * 회의록 이력 화면 로직 (≈ Service).
 * 목록 조회와 삭제를 담당한다.
 *
 * @returns 이력 상태와 액션들
 */
export function useVoiceHistory() {
    const records = ref<VoiceRecordSummary[]>([])
    const loading = ref(false)
    const error = ref('')

    /** 이력 목록 로드 (최신순) */
    async function load(): Promise<void> {
        loading.value = true
        error.value = ''
        try {
            records.value = await voiceApi.history()
        } catch (e) {
            error.value = extractErrorMessage(e, '회의록 이력을 불러오지 못했습니다.')
        } finally {
            loading.value = false
        }
    }

    /** 회의록 삭제 후 목록에서 제거 */
    async function remove(recordSeq: number): Promise<void> {
        error.value = ''
        try {
            await voiceApi.remove(recordSeq)
            records.value = records.value.filter((r) => r.recordSeq !== recordSeq)
        } catch (e) {
            error.value = extractErrorMessage(e, '회의록 삭제에 실패했습니다.')
        }
    }

    return { records, loading, error, load, remove }
}
