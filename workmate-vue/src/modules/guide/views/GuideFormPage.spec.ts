import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mountPage, flush } from '../../../test/support/mountPage'
import type { Guide } from '../types'

const detail = vi.fn<(...args: unknown[]) => unknown>()
const create = vi.fn<(...args: unknown[]) => unknown>()
const update = vi.fn<(...args: unknown[]) => unknown>()
vi.mock('../api/guide.api', () => ({
    guideApi: {
        detail: (...a: unknown[]) => detail(...a),
        create: (...a: unknown[]) => create(...a),
        update: (...a: unknown[]) => update(...a),
    },
}))

// Toast UI 에디터는 실제 DOM 측정을 해서 jsdom 에서 뜨지 않는다.
// 여기서 검증할 것은 '에디터가 잘 동작하는가'가 아니라 '폼 화면이 그려지고 저장이 연결되는가'라
// 값만 주고받는 최소 대체물로 바꾼다
vi.mock('../../../common/components/form/MarkdownEditor.vue', () => ({
    default: {
        name: 'MarkdownEditor',
        props: ['modelValue'],
        emits: ['update:modelValue'],
        template: '<textarea data-test="editor" :value="modelValue" />',
    },
}))

import GuideFormPage from './GuideFormPage.vue'

function guide(): Guide {
    return {
        guideSeq: 1,
        userSeq: 12,
        title: '휴가 신청 절차',
        content: '연차는 3일 전까지 신청한다.',
        isPublic: true,
        createdAt: '2026-09-01T10:00:00',
        updatedAt: '2026-09-01T10:00:00',
    }
}

describe('GuideFormPage', () => {
    beforeEach(() => {
        detail.mockReset()
        create.mockReset()
        update.mockReset()
    })

    it('새 문서 화면은 서버를 부르지 않는다', async () => {
        const { wrapper } = await mountPage(GuideFormPage)

        expect(detail).not.toHaveBeenCalled()
        expect(wrapper.find('input').exists()).toBe(true)
    })

    it('수정 화면은 기존 내용을 불러와 채운다', async () => {
        detail.mockResolvedValue(guide())

        const { wrapper } = await mountPage(GuideFormPage, { params: { id: '1' } })
        await flush(4)

        expect(detail).toHaveBeenCalledWith(1)
        expect(wrapper.find('input').element).toHaveProperty('value', '휴가 신청 절차')
    })

    it('불러오기에 실패해도 화면이 터지지 않는다', async () => {
        detail.mockRejectedValue(new Error('boom'))

        const { wrapper } = await mountPage(GuideFormPage, { params: { id: '1' } })

        expect(wrapper.exists()).toBe(true)
    })
})
