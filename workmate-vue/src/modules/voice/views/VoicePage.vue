<script setup lang="ts">
/**
 * 음성 회의록 화면 (/voice, F8-1 MVP).
 * 오디오 파일 업로드 → Gemini 전사(STT) + 3단 구조화 요약 → 결과 2분할 표시 + TXT 다운로드.
 * (실시간 녹음·이력·공유는 다음 단계)
 */
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
    FileAudio,
    Download,
    Mic,
    RefreshCw,
    Upload,
    BookPlus,
    CheckCircle2,
} from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { Input } from '@/common/components/ui/input'
import { Spinner } from '@/common/components/ui/spinner'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import { renderMarkdown } from '@/common/utils/markdown'
import { useMarkdownCopy } from '@/common/composables/useMarkdownCopy'
import { useVoiceAnalyze } from '../composables/useVoiceAnalyze'

const { result, loading, error, registering, registeredGuideSeq, analyze, convertToGuide, reset } =
    useVoiceAnalyze()
const { onMarkdownClick } = useMarkdownCopy()

const title = ref('')
const selectedFile = ref<File | null>(null)
const dragOver = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const localError = ref('')

/** 최대 업로드 크기 (WAS max-file-size 와 맞춤) */
const MAX_SIZE = 25 * 1024 * 1024

const summaryHtml = computed(() => (result.value ? renderMarkdown(result.value.summaryMd) : ''))

/** 파일 선택/드롭 공통 검증 처리 */
function acceptFile(file: File | undefined): void {
    localError.value = ''
    if (!file) return
    if (!file.type.startsWith('audio/')) {
        localError.value = '오디오 파일만 업로드할 수 있습니다.'
        return
    }
    if (file.size > MAX_SIZE) {
        localError.value = '파일이 너무 큽니다. 최대 25MB까지 가능합니다.'
        return
    }
    selectedFile.value = file
}

function onDrop(e: DragEvent): void {
    dragOver.value = false
    acceptFile(e.dataTransfer?.files?.[0])
}

function onPick(e: Event): void {
    acceptFile((e.target as HTMLInputElement).files?.[0])
}

/** 분석 실행 */
async function onAnalyze(): Promise<void> {
    if (!selectedFile.value || loading.value) return
    await analyze(selectedFile.value, title.value)
}

/** 새로 분석하기 — 입력·결과 초기화 */
function onReset(): void {
    reset()
    selectedFile.value = null
    title.value = ''
    localError.value = ''
}

/** 사람이 읽기 좋은 파일 크기 표기 */
function formatSize(bytes: number): string {
    return bytes < 1024 * 1024
        ? `${(bytes / 1024).toFixed(0)} KB`
        : `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

/** 전사문 + 요약을 .txt 로 다운로드 */
function downloadTxt(): void {
    if (!result.value) return
    const r = result.value
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
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-5xl px-6 py-8">
            <div class="mb-6 flex items-center gap-2">
                <Mic class="size-6" />
                <h1 class="text-2xl font-semibold">회의록 요약</h1>
            </div>

            <!-- 업로드 섹션 -->
            <div class="rounded-lg border p-6">
                <label class="mb-1.5 block text-sm font-medium">회의 제목 (선택)</label>
                <Input
                    v-model="title"
                    placeholder="예: 2026-07-30 아키텍처 회의"
                    class="mb-4 max-w-md"
                />

                <!-- 드래그&드롭 / 파일 선택 -->
                <div
                    class="flex flex-col items-center justify-center rounded-lg border-2 border-dashed px-6 py-10 text-center transition-colors"
                    :class="dragOver ? 'border-primary bg-primary/5' : 'border-border'"
                    @dragover.prevent="dragOver = true"
                    @dragleave.prevent="dragOver = false"
                    @drop.prevent="onDrop"
                    @click="fileInput?.click()"
                    role="button"
                >
                    <input
                        ref="fileInput"
                        type="file"
                        accept="audio/*"
                        class="hidden"
                        @change="onPick"
                    />
                    <template v-if="selectedFile">
                        <FileAudio class="mb-2 size-8 text-primary" />
                        <p class="font-medium">{{ selectedFile.name }}</p>
                        <p class="text-sm text-muted-foreground">
                            {{ formatSize(selectedFile.size) }} · 클릭해서 다른 파일 선택
                        </p>
                    </template>
                    <template v-else>
                        <Upload class="mb-2 size-8 text-muted-foreground" />
                        <p class="font-medium">오디오 파일을 여기로 끌어다 놓거나 클릭해서 선택</p>
                        <p class="text-sm text-muted-foreground">
                            mp3 · wav · m4a · webm (최대 25MB)
                        </p>
                    </template>
                </div>

                <Alert v-if="localError || error" variant="destructive" class="mt-4">
                    <AlertDescription>{{ localError || error }}</AlertDescription>
                </Alert>

                <div class="mt-4 flex items-center gap-2">
                    <Button :disabled="!selectedFile || loading" @click="onAnalyze">
                        <Spinner v-if="loading" class="mr-2 size-4" />
                        {{ loading ? 'AI가 분석 중…' : 'AI 회의록 요약하기' }}
                    </Button>
                    <Button v-if="result || selectedFile" variant="outline" @click="onReset">
                        <RefreshCw class="mr-2 size-4" />
                        새로 분석
                    </Button>
                </div>
                <p v-if="loading" class="mt-2 text-xs text-muted-foreground">
                    오디오 길이에 따라 수십 초 걸릴 수 있습니다.
                </p>
            </div>

            <!-- 결과 2분할 -->
            <div v-if="result" class="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-2">
                <!-- 좌: STT 원문 -->
                <div class="rounded-lg border">
                    <div class="flex items-center justify-between border-b px-4 py-2.5">
                        <span class="text-sm font-semibold">STT 전사 원문</span>
                        <Button size="sm" variant="outline" @click="downloadTxt">
                            <Download class="mr-1.5 size-4" />
                            TXT 다운로드
                        </Button>
                    </div>
                    <div
                        class="slim-scroll max-h-[60vh] overflow-y-auto whitespace-pre-wrap px-4 py-3 text-sm leading-relaxed"
                    >
                        {{ result.sttText }}
                    </div>
                </div>

                <!-- 우: AI 구조화 요약 -->
                <div class="rounded-lg border">
                    <div class="flex items-center justify-between border-b px-4 py-2.5">
                        <span class="text-sm font-semibold">AI 요약 리포트</span>
                        <!-- 회의록 요약을 사내 가이드로 등록 → RAG 검색 대상 편입 (F8-1-6) -->
                        <RouterLink
                            v-if="registeredGuideSeq"
                            :to="{ name: 'guide-detail', params: { id: registeredGuideSeq } }"
                            class="inline-flex items-center gap-1 text-sm font-medium text-primary hover:underline"
                        >
                            <CheckCircle2 class="size-4" />
                            가이드에서 보기
                        </RouterLink>
                        <Button
                            v-else
                            size="sm"
                            variant="outline"
                            :disabled="registering"
                            @click="convertToGuide"
                        >
                            <Spinner v-if="registering" class="mr-1.5 size-4" />
                            <BookPlus v-else class="mr-1.5 size-4" />
                            {{ registering ? '등록 중…' : '가이드로 등록' }}
                        </Button>
                    </div>
                    <div
                        class="markdown-body markdown-doc slim-scroll max-h-[60vh] overflow-y-auto px-4 py-3"
                        v-html="summaryHtml"
                        @click="onMarkdownClick"
                    />
                </div>
            </div>
        </div>
    </div>
</template>
