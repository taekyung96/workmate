<script setup lang="ts">
/**
 * 회의록 [분석] 화면 (/voice, F8-1).
 * 영수증과 동일한 점진적 흐름: 빈 드랍존 → 파일 선택 후 분석 → 결과(제목 확정 + 2분할).
 * 오디오는 이미지 미리보기가 없으므로 파일 카드로 대체하고, 제목은 분석이 끝난 뒤 결과 화면에서 정한다.
 * 분석 상태는 store 에 있어 이력 탭을 다녀와도 유지된다.
 */
import { ref, watch } from 'vue'
import { toast } from 'vue-sonner'
import { FileAudio, Mic, RefreshCw, Upload, FileText, ListTree } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { Input } from '@/common/components/ui/input'
import { Label } from '@/common/components/ui/label'
import { Spinner } from '@/common/components/ui/spinner'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import { Card } from '@/common/components/ui/card'
import PageTabs from '@/common/components/PageTabs.vue'
import PageHeader from '@/common/components/PageHeader.vue'
import HowItWorks from '@/common/components/HowItWorks.vue'
import FileDropzone from '@/common/components/FileDropzone.vue'
import { formatFileSize } from '@/common/utils/format'
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

const selectedFile = ref<File | null>(null)
const localError = ref('')

// 결과 화면에서 확정하는 회의 제목 — 분석이 끝나면 서버 기본 제목으로 채운다
const titleInput = ref('')

/** 최대 업로드 크기 25MB — 오디오는 WEB 프록시를 거치므로 WEB·WAS 의 max-file-size 와 반드시 일치시킬 것 */
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

/** 선택 취소 — 빈 드랍존으로 되돌린다 */
function clearFile(): void {
    selectedFile.value = null
    localError.value = ''
}

/** 분석 실행 (제목 없이 파일만 — 제목은 결과에서 저장) */
async function onAnalyze(): Promise<void> {
    if (!selectedFile.value || store.loading) return
    await store.analyze(selectedFile.value)
}

// 분석 결과가 새로 도착하면 제목 입력칸을 비운다.
// (제목은 분석 후 사용자가 직접 입력하는 값이라, 서버 기본 제목 "회의록 {날짜}" 은 placeholder 로만 보여준다.)
watch(
    () => store.result,
    (r) => {
        if (r) titleInput.value = ''
    },
)

/** 결과 화면에서 확정한 제목 저장 */
async function onSaveTitle(): Promise<void> {
    const ok = await store.saveTitle(titleInput.value)
    if (ok) toast.success('제목을 저장했습니다.')
}

/** 새로 분석하기 — 입력·결과 초기화 */
function onReset(): void {
    store.reset()
    selectedFile.value = null
    titleInput.value = ''
    localError.value = ''
}
</script>

<template>
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-5xl px-6 py-8">
            <PageHeader
                :icon="Mic"
                title="회의록 요약"
                description="녹음을 올리면 AI가 받아쓰고 핵심을 3단으로 정리합니다."
            />

            <PageTabs :tabs="voiceTabs" />

            <div class="flex flex-col gap-4">
                <Alert v-if="localError || store.error" variant="destructive">
                    <AlertDescription>{{ localError || store.error }}</AlertDescription>
                </Alert>

                <!-- 1) 업로드 전 (빈 화면) — 드랍존 + 동작 안내 -->
                <div v-if="!selectedFile && !store.result" class="flex flex-col gap-5">
                    <FileDropzone
                        :icon="Upload"
                        title="오디오 파일을 올려주세요"
                        accept="audio/*"
                        :chips="audioChips"
                        @select="acceptFile"
                    />
                    <HowItWorks :steps="voiceSteps" />
                </div>

                <!-- 2) 파일 선택됨 · 분석 전 — 파일 카드 + 분석 버튼 -->
                <div v-else-if="!store.result" class="flex flex-col gap-4">
                    <div class="flex items-center gap-3 rounded-xl border bg-card px-4 py-3.5">
                        <div
                            class="grid size-11 shrink-0 place-items-center rounded-lg bg-primary/10"
                        >
                            <FileAudio class="size-5 text-primary" />
                        </div>
                        <div class="min-w-0">
                            <p class="truncate font-medium">{{ selectedFile!.name }}</p>
                            <p class="text-sm text-muted-foreground">
                                {{ formatFileSize(selectedFile!.size) }}
                            </p>
                        </div>
                    </div>

                    <div class="flex items-center gap-2">
                        <Button :disabled="store.loading" @click="onAnalyze">
                            <Spinner v-if="store.loading" class="mr-2 size-4" />
                            {{ store.loading ? 'AI가 분석 중…' : 'AI 회의록 요약하기' }}
                        </Button>
                        <Button variant="outline" :disabled="store.loading" @click="clearFile">
                            다시 선택
                        </Button>
                    </div>

                    <p class="text-sm text-muted-foreground">
                        분석하면 결과가 이력에 자동 저장되며, 아래에서 회의 제목을 정할 수 있습니다.
                    </p>
                    <p v-if="store.loading" class="text-xs text-muted-foreground">
                        오디오 길이에 따라 수십 초 걸릴 수 있습니다.
                    </p>
                </div>

                <!-- 3) 분석 후 — 제목 확정 카드 + 결과 2분할 -->
                <div v-else class="flex flex-col gap-4">
                    <!-- 제목 확정 카드 — 영수증 확인 폼과 같은 결의 카드 -->
                    <Card class="gap-0 overflow-hidden py-0">
                        <div class="flex items-center justify-between gap-2 border-b px-5 py-4">
                            <span class="text-sm font-semibold">회의록 정보</span>
                            <Button variant="outline" size="sm" @click="onReset">
                                <RefreshCw class="mr-1.5 size-4" />
                                새로 분석
                            </Button>
                        </div>
                        <div class="flex flex-col gap-1.5 p-5">
                            <Label>회의 제목</Label>
                            <div class="flex gap-2">
                                <Input
                                    v-model="titleInput"
                                    maxlength="255"
                                    :placeholder="
                                        store.result?.title || '예: 2026-07-30 아키텍처 회의'
                                    "
                                    class="flex-1"
                                    @keydown.enter="onSaveTitle"
                                />
                                <Button
                                    :disabled="store.savingTitle || !titleInput.trim()"
                                    @click="onSaveTitle"
                                >
                                    <Spinner v-if="store.savingTitle" class="mr-2 size-4" />
                                    저장
                                </Button>
                            </div>
                        </div>
                    </Card>

                    <VoiceResultPanel :record="store.result" />
                </div>
            </div>
        </div>
    </div>
</template>
