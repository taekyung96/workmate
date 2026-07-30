import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'

// linkify: URL 자동 링크, breaks: 줄바꿈을 <br>로
const md = new MarkdownIt({ linkify: true, breaks: true })

// 마크다운 코드 블록(fence) 렌더링 규칙 커스텀 — 상단 툴바(언어 표기 및 복사 버튼) 추가
const defaultFence = md.renderer.rules.fence || ((tokens, idx, options, _env, self) => self.renderToken(tokens, idx, options))

md.renderer.rules.fence = (tokens, idx, options, env, self) => {
    const token = tokens[idx]!
    const lang = token.info ? token.info.trim().split(/\s+/)[0] : ''
    const rawCode = token.content
    const renderedCode = defaultFence(tokens, idx, options, env, self)

    // 코드 블록 상단 툴바 헤더 및 감싸는 wrapper HTML 구성
    return `<div class="code-block-wrapper my-3 overflow-hidden rounded-lg border border-border bg-muted/40">
    <div class="code-header flex items-center justify-between border-b border-border bg-muted/80 px-3 py-1.5 text-xs text-muted-foreground select-none">
        <span class="code-lang font-mono font-semibold uppercase">${lang ? md.utils.escapeHtml(lang) : 'CODE'}</span>
        <button type="button" class="copy-btn flex items-center gap-1 rounded px-2 py-0.5 text-xs font-medium hover:bg-background hover:text-foreground transition-colors cursor-pointer" data-code="${encodeURIComponent(rawCode)}">
            <span class="copy-text">복사</span>
        </button>
    </div>
    ${renderedCode}
</div>`
}

/**
 * 마크다운 문자열을 안전한 HTML로 변환한다.
 * AI 응답은 외부(LLM) 생성물이므로 DOMPurify로 XSS를 살균한 뒤 렌더한다.
 * 복사 기능을 위한 data-code 속성을 허용 목록에 추가한다.
 *
 * @param text 마크다운 원문
 * @returns 살균된 HTML 문자열 (v-html용)
 */
export function renderMarkdown(text: string): string {
    return DOMPurify.sanitize(md.render(text), {
        ADD_ATTR: ['data-code'],
    })
}
