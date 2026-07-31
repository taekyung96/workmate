<script setup lang="ts">
/**
 * 회의록 [분석] 화면 (/voice, F8-1).
 * 오디오 업로드 → Gemini 전사(STT) + 3단 구조화 요약 → 결과 2분할 표시.
 * 분석 상태는 store 에 있어 이력 탭을 다녀와도 유지된다.
 */
import { ref } from 'vue'
import { FileAudio, Mic, RefreshCw, Upload } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { Input } from '@/common/components/ui/input'
import { Spinner } from '@/common/components/ui/spinner'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import PageTabs from '@/common/components/PageTabs.vue'
import { useVoiceStore } from '../stores/voice.store'
import { voiceTabs } from '../routes'
import VoiceResultPanel from '../components/VoiceResultPanel.vue'

const store = useVoiceStore()

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
            <div class="mb-6 flex items-center gap-2">
                <Mic class="size-6" />
                <h1 class="text-2xl font-semibold">회의록 요약</h1>
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

            <VoiceResultPanel v-if="store.result" :record="store.result" class="mt-6" />
        </div>
    </div>
</template>
