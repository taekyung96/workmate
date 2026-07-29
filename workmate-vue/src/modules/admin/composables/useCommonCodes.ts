import { ref } from 'vue'
import { adminApi } from '../api/admin.api'
import { extractErrorMessage } from '@/common/utils/error'
import type { CommonCodeGroup, CommonCodeGroupSave, CommonCodeItem, CommonCodeSave } from '../types'

/**
 * 공통코드 관리 상태·동작 (F7).
 * 좌측 그룹 목록 + 선택 그룹의 코드 목록을 관리하고, 그룹·코드 CRUD를 제공한다.
 * CRUD는 실패 시 예외를 던지므로(호출부에서 catch해 메시지 표시), 성공 시 관련 목록을 갱신한다.
 *
 * @returns 목록 상태와 액션들
 */
export function useCommonCodes() {
    const groups = ref<CommonCodeGroup[]>([])
    const selectedGroupCode = ref<string | null>(null)
    const codes = ref<CommonCodeItem[]>([])
    const loading = ref(false)
    const error = ref<string | null>(null)

    /** 그룹 목록 로드 — 선택 그룹이 사라졌으면 첫 그룹을 자동 선택 */
    async function loadGroups(): Promise<void> {
        loading.value = true
        error.value = null
        try {
            groups.value = await adminApi.codeGroups()
            if (groups.value.length === 0) {
                selectedGroupCode.value = null
                codes.value = []
                return
            }
            const stillExists = groups.value.some((g) => g.groupCode === selectedGroupCode.value)
            await selectGroup(stillExists ? selectedGroupCode.value! : groups.value[0]!.groupCode)
        } catch (e) {
            error.value = extractErrorMessage(e, '공통코드 그룹을 불러오지 못했습니다.')
        } finally {
            loading.value = false
        }
    }

    /** 그룹 선택 → 해당 그룹의 코드 목록 로드 */
    async function selectGroup(groupCode: string): Promise<void> {
        selectedGroupCode.value = groupCode
        try {
            codes.value = await adminApi.codes(groupCode)
        } catch (e) {
            error.value = extractErrorMessage(e, '코드를 불러오지 못했습니다.')
            codes.value = []
        }
    }

    /** 그룹 등록 → 새 그룹 선택 후 갱신 */
    async function createGroup(body: CommonCodeGroupSave): Promise<void> {
        await adminApi.createGroup(body)
        if (body.groupCode) selectedGroupCode.value = body.groupCode
        await loadGroups()
    }

    /** 그룹 수정 후 갱신 */
    async function updateGroup(groupCode: string, body: CommonCodeGroupSave): Promise<void> {
        await adminApi.updateGroup(groupCode, body)
        await loadGroups()
    }

    /** 그룹 삭제 후 갱신 (삭제 대상이 선택 그룹이면 선택 해제) */
    async function deleteGroup(groupCode: string): Promise<void> {
        await adminApi.deleteGroup(groupCode)
        if (selectedGroupCode.value === groupCode) selectedGroupCode.value = null
        await loadGroups()
    }

    /** 코드 등록 후 현재 그룹 코드 갱신 */
    async function createCode(body: CommonCodeSave): Promise<void> {
        await adminApi.createCode(selectedGroupCode.value!, body)
        await selectGroup(selectedGroupCode.value!)
    }

    /** 코드 수정 후 갱신 */
    async function updateCode(code: string, body: CommonCodeSave): Promise<void> {
        await adminApi.updateCode(selectedGroupCode.value!, code, body)
        await selectGroup(selectedGroupCode.value!)
    }

    /** 코드 삭제 후 갱신 */
    async function deleteCode(code: string): Promise<void> {
        await adminApi.deleteCode(selectedGroupCode.value!, code)
        await selectGroup(selectedGroupCode.value!)
    }

    return {
        groups,
        selectedGroupCode,
        codes,
        loading,
        error,
        loadGroups,
        selectGroup,
        createGroup,
        updateGroup,
        deleteGroup,
        createCode,
        updateCode,
        deleteCode,
    }
}
