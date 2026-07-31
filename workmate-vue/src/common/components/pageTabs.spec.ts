import { describe, it, expect } from 'vitest'
import { isTabActive, type PageTab } from './pageTabs'

describe('isTabActive', () => {
    it('라우트 이름이 탭 이름과 같으면 활성이다', () => {
        const tab: PageTab = { name: 'voice', label: '분석' }
        expect(isTabActive(tab, 'voice')).toBe(true)
        expect(isTabActive(tab, 'voice-history')).toBe(false)
    })

    it('match 에 포함된 라우트 이름도 활성으로 본다 (상세 화면에서 목록 탭 유지)', () => {
        const tab: PageTab = {
            name: 'voice-history',
            label: '이력',
            match: ['voice-history', 'voice-record'],
        }
        expect(isTabActive(tab, 'voice-record')).toBe(true)
        expect(isTabActive(tab, 'voice-history')).toBe(true)
        expect(isTabActive(tab, 'voice')).toBe(false)
    })

    it('라우트 이름이 없으면(초기 렌더) 어떤 탭도 활성이 아니다', () => {
        const tab: PageTab = { name: 'voice', label: '분석' }
        expect(isTabActive(tab, '')).toBe(false)
    })
})
