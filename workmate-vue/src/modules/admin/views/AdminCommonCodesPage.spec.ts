import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mountPage, flush } from '../../../test/support/mountPage'
import type { CommonCodeGroup, CommonCodeItem } from '../types'

const codeGroups = vi.fn<(...args: unknown[]) => unknown>()
const codes = vi.fn<(...args: unknown[]) => unknown>()
vi.mock('../api/admin.api', () => ({
    adminApi: {
        codeGroups: (...a: unknown[]) => codeGroups(...a),
        codes: (...a: unknown[]) => codes(...a),
        createGroup: vi.fn<(...args: unknown[]) => unknown>(),
        updateGroup: vi.fn<(...args: unknown[]) => unknown>(),
        deleteGroup: vi.fn<(...args: unknown[]) => unknown>(),
        createCode: vi.fn<(...args: unknown[]) => unknown>(),
        updateCode: vi.fn<(...args: unknown[]) => unknown>(),
        deleteCode: vi.fn<(...args: unknown[]) => unknown>(),
    },
}))

import AdminCommonCodesPage from './AdminCommonCodesPage.vue'

const groups: CommonCodeGroup[] = [
    { groupCode: 'AI_MODEL', groupName: 'AI 모델', description: '채팅 모델 목록', useYn: true },
]

/** 실제 AI_MODEL 그룹과 같은 모양 — 꺼 둔 코드(use_yn=false)가 섞여 있다 */
const aiModels: CommonCodeItem[] = [
    { code: 'qwen/qwen3.8-27b', codeName: 'Groq · Qwen3.8 27B', sortOrder: 1, useYn: true },
    { code: 'gemini-flash-latest', codeName: 'Gemini Flash (latest)', sortOrder: 2, useYn: true },
    { code: 'openai/gpt-oss-120b', codeName: 'Groq · GPT-OSS 120B', sortOrder: 3, useYn: false },
]

describe('AdminCommonCodesPage', () => {
    beforeEach(() => {
        codeGroups.mockReset()
        codes.mockReset()
        codes.mockResolvedValue(aiModels)
    })

    it('코드 그룹을 그린다', async () => {
        codeGroups.mockResolvedValue(groups)

        const { wrapper } = await mountPage(AdminCommonCodesPage)

        expect(wrapper.text()).toContain('AI_MODEL')
    })

    it('그룹을 고르면 그 그룹의 코드를 조회한다', async () => {
        codeGroups.mockResolvedValue(groups)

        const { wrapper } = await mountPage(AdminCommonCodesPage)
        const group = wrapper.findAll('button').find((b) => b.text().includes('AI 모델'))
        expect(group).toBeDefined()

        await group!.trigger('click')
        await flush(5)

        expect(codes).toHaveBeenCalledWith('AI_MODEL')
        // 관리 화면에서는 꺼 둔 코드(use_yn=false)도 보여야 한다 — 다시 켜려면 보여야 하기 때문이다
        expect(wrapper.text()).toContain('Groq · GPT-OSS 120B')
    })

    it('조회에 실패해도 화면이 터지지 않는다', async () => {
        codeGroups.mockRejectedValue(new Error('boom'))

        const { wrapper } = await mountPage(AdminCommonCodesPage)

        expect(wrapper.exists()).toBe(true)
    })
})
