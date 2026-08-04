<script setup lang="ts">
/**
 * 회의록 [분석] 화면 (/voice, F8-1).
 * 오디오 업로드 → Gemini 전사(STT) + 3단 구조화 요약 → 결과 2분할 표시.
 * 분석 상태는 store 에 있어 이력 탭을 다녀와도 유지된다.
 */
import { ref } from 'vue'
import { FileAudio, Mic, RefreshCw, Upload, Plus, FileText, ListTree } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { Input } from '@/common/components/ui/input'
import { Spinner } from '@/common/components/ui/spinner'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import PageTabs from '@/common/components/PageTabs.vue'
import HowItWorks from '@/common/components/HowItWorks.vue'
import { useVoiceStore } from '../stores/voice.store'
import { voiceTabs } from '../routes'
import VoiceResultPanel from '../components/VoiceResultPanel.vue'

const store = useVoiceStore()

/** 지원 오디오 형식 안내 칩 */
const audioChips = ['MP3', 'WAV', 'M4A', 'WEBM', '최대 25MB']

// 빈 화면 "이렇게 동작해요" 3단계 — 회의록 처리 흐름
const voiceSteps = [
    { icon: Upload, title: '업로드', desc: '회의 녹음 파일을 선택합니다.' },
    { icon: FileText, title: 'AI 전사', desc: '음성을 텍스트로 정확히 받아씁니다.' },
    { icon: ListTree, title: '3단 요약', desc: '요약·논의·결정으로 정리합니다.' },
]

const title = ref('')
const selectedFile = ref<File | null>(null)
const dragOver = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const localError = ref('')

/** 최대 업로드 크기 (WAS max-file-size 와 맞춤) */
const MAX_SIZE = 25 * 1024 * 1024

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
    if (!selectedFile.value || store.loading) return
    await store.analyze(selectedFile.value, title.value)
}

/** 새로 분석하기 — 입력·결과 초기화 */
function onReset(): void {
    store.reset()
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
</script>

<template>
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-5xl px-6 py-8">
            <div class="mb-6 flex items-center gap-3">
                <div
                    class="grid size-10 place-items-center rounded-xl bg-primary text-primary-foreground"
                >
                    <Mic class="size-5" />
                </div>
                <div>
                    <h1 class="text-2xl leading-tight font-semibold">회의록 요약</h1>
                    <p class="text-sm text-muted-foreground">
                        녹음을 올리면 AI가 받아쓰고 핵심을 3단으로 정리합니다.
                    </p>
                </div>
            </div>

            <PageTabs :tabs="voiceTabs" />

            <!-- 업로드 섹션 -->
            <div class="rounded-lg border p-6">
                <label class="mb-1.5 block text-sm font-medium">회의 제목 (선택)</label>
                <Input
                    v-model="title"
                    placeholder="예: 2026-07-30 아키텍처 회의"
                    class="mb-4 max-w-md"
                />

                <div
                    class="flex flex-col items-center justify-center rounded-xl border-2 border-dashed px-6 py-7 text-center transition-colors"
                    :class="
                        dragOver
                            ? 'border-primary bg-primary/5'
                            : 'border-border hover:border-muted-foreground/40'
                    "
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
                        <div
                            class="mb-3 grid size-14 place-items-center rounded-full border bg-card shadow-sm"
                        >
                            <FileAudio class="size-6 text-primary" />
                        </div>
                        <p class="font-semibold">{{ selectedFile.name }}</p>
                        <p class="mt-1 text-sm text-muted-foreground">
                            {{ formatSize(selectedFile.size) }} · 클릭해서 다른 파일 선택
                        </p>
                    </template>
                    <template v-else>
                        <div
                            class="mb-3 grid size-14 place-items-center rounded-full border bg-card shadow-sm"
                        >
                            <Upload class="size-6 text-muted-foreground" />
                        </div>
                        <p class="font-semibold">오디오 파일을 올려주세요</p>
                        <p class="mt-1 text-sm text-muted-foreground">
                            여기로 끌어다 놓거나 버튼으로 선택하세요
                        </p>
                        <Button class="mt-4" @click.stop="fileInput?.click()">
                            <Plus class="mr-1.5 size-4" />
                            파일 선택
                        </Button>
                        <div class="mt-3.5 flex flex-wrap justify-center gap-1.5">
                            <span
                                v-for="chip in audioChips"
                                :key="chip"
                                class="rounded-full bg-muted px-2.5 py-0.5 text-[11px] font-medium text-muted-foreground"
                            >
                                {{ chip }}
                            </span>
                        </div>
                    </template>
                </div>

                <Alert v-if="localError || store.error" variant="destructive" class="mt-4">
                    <AlertDescription>{{ localError || store.error }}</AlertDescription>
                </Alert>

                <div class="mt-4 flex items-center gap-2">
                    <Button :disabled="!selectedFile || store.loading" @click="onAnalyze">
                        <Spinner v-if="store.loading" class="mr-2 size-4" />
                        {{ store.loading ? 'AI가 분석 중…' : 'AI 회의록 요약하기' }}
                    </Button>
                    <Button v-if="store.result || selectedFile" variant="outline" @click="onReset">
                        <RefreshCw class="mr-2 size-4" />
                        새로 분석
                    </Button>
                </div>
                <p v-if="store.loading" class="mt-2 text-xs text-muted-foreground">
                    오디오 길이에 따라 수십 초 걸릴 수 있습니다.
                </p>
            </div>

            <!-- 빈 화면(결과 전) 동작 안내 -->
            <HowItWorks v-if="!store.result" :steps="voiceSteps" class="mt-6" />

            <VoiceResultPanel v-if="store.result" :record="store.result" class="mt-6" />
        </div>
    </div>
</template>
