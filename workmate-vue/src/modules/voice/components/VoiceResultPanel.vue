<script setup lang="ts">
/**
 * 회의록 결과 2분할 패널 (좌: STT 원문 / 우: AI 요약).
 * 분석 직후 화면과 이력 상세가 이 컴포넌트를 공유하므로,
 * TXT 다운로드가 과거 회의록에서도 그대로 동작한다.
 */
import { computed } from 'vue'
import { Download, FileText, Sparkles } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { renderMarkdown } from '@/common/utils/markdown'
import { useMarkdownCopy } from '@/common/composables/useMarkdownCopy'
import type { VoiceAnalysisResult } from '../types'

const props = defineProps<{ record: VoiceAnalysisResult }>()
const { onMarkdownClick } = useMarkdownCopy()

const summaryHtml = computed(() => renderMarkdown(props.record.summaryMd))

/** 전사문 + 요약을 .txt 로 다운로드 */
function downloadTxt(): void {
    const r = props.record
    const content =
        `# ${r.title}\n\n` +
        `===== AI 요약 =====\n${r.summaryMd}\n\n` +
        `===== STT 전사 원문 =====\n${r.sttText}\n`
    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${r.title}.txt`
    a.click()
    URL.revokeObjectURL(url)
}
</script>

<template>
    <div class="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <!-- 좌: STT 원문 -->
        <div class="rounded-lg border">
            <div class="flex items-center justify-between border-b px-4 py-2.5">
                <span class="flex items-center gap-1.5 text-sm font-semibold">
                    <FileText class="size-4 text-muted-foreground" />
                    STT 전사 원문
                </span>
                <Button size="sm" variant="outline" @click="downloadTxt">
                    <Download class="mr-1.5 size-4" />
                    TXT 다운로드
                </Button>
            </div>
            <div
                class="slim-scroll max-h-[60vh] overflow-y-auto whitespace-pre-wrap px-4 py-3 text-sm leading-relaxed"
            >
                {{ props.record.sttText }}
            </div>
        </div>

        <!-- 우: AI 구조화 요약 -->
        <div class="rounded-lg border">
            <div class="flex items-center justify-between border-b px-4 py-2.5">
                <span class="flex items-center gap-1.5 text-sm font-semibold">
                    <Sparkles class="size-4 text-primary" />
                    AI 요약 리포트
                </span>
            </div>
            <div
                class="markdown-body markdown-doc slim-scroll max-h-[60vh] overflow-y-auto px-4 py-3"
                v-html="summaryHtml"
                @click="onMarkdownClick"
            />
        </div>
    </div>
</template>
