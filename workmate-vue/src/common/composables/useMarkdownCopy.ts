/**
 * 마크다운 렌더 결과 안의 코드블록 복사 버튼(.copy-btn)을 이벤트 위임으로 처리하는 컴포저블.
 *
 * renderMarkdown(common/utils/markdown)이 코드블록마다 상단에 심는 복사 버튼을
 * 채팅(MessageBubble)·가이드 상세 등 v-html 로 마크다운을 렌더하는 여러 화면이 공유한다.
 * v-html 로 삽입된 요소에는 Vue 이벤트 바인딩을 붙일 수 없으므로, 컨테이너 클릭을 위임받아 처리한다.
 *
 * @returns onMarkdownClick — 마크다운 컨테이너의 @click 에 바인딩할 핸들러
 */
export function useMarkdownCopy() {
    /**
     * 마크다운 컨테이너 클릭 위임 핸들러 — 복사 버튼 클릭 시 data-code 를 클립보드에 복사한다.
     *
     * @param event 마우스 클릭 이벤트
     */
    async function onMarkdownClick(event: MouseEvent): Promise<void> {
        const target = event.target as HTMLElement | null
        const btn = target?.closest('.copy-btn') as HTMLButtonElement | null
        if (!btn) return

        const rawCodeEnc = btn.getAttribute('data-code')
        if (!rawCodeEnc) return

        try {
            const rawCode = decodeURIComponent(rawCodeEnc)
            await navigator.clipboard.writeText(rawCode)

            // 복사 성공 시 2초간 "복사됨!" 체크 피드백 표시
            const textSpan = btn.querySelector('.copy-text')
            if (textSpan) {
                const originalText = textSpan.textContent
                textSpan.textContent = '✓ 복사됨!'
                btn.classList.add('text-green-600', 'dark:text-green-400')

                setTimeout(() => {
                    textSpan.textContent = originalText ?? '복사'
                    btn.classList.remove('text-green-600', 'dark:text-green-400')
                }, 2000)
            }
        } catch (err) {
            console.error('코드 복사 실패:', err)
        }
    }

    return { onMarkdownClick }
}
