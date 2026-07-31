<script setup lang="ts">
/**
 * 회의록 상세 화면 (/voice/history/:recordSeq, F8-1 확장).
 * 오디오 재생 + 분석 화면과 동일한 결과 2분할을 보여준다.
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Mic } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { Spinner } from '@/common/components/ui/spinner'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import PageTabs from '@/common/components/PageTabs.vue'
import { extractErrorMessage } from '@/common/utils/error'
import { voiceApi } from '../api/voice.api'
import { voiceTabs } from '../routes'
import VoiceResultPanel from '../components/VoiceResultPanel.vue'
import VoiceAudioPlayer from '../components/VoiceAudioPlayer.vue'
import type { VoiceAnalysisResult } from '../types'

const route = useRoute()
const router = useRouter()

const record = ref<VoiceAnalysisResult | null>(null)
const loading = ref(false)
const error = ref('')

const recordSeq = computed(() => Number(route.params.recordSeq))

onMounted(async () => {
    loading.value = true
    try {
        record.value = await voiceApi.getRecord(recordSeq.value)
    } catch (e) {
        error.value = extractErrorMessage(e, '회의록을 불러오지 못했습니다.')
    } finally {
        loading.value = false
    }
})
</script>

<template>
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-5xl px-6 py-8">
            <div class="mb-6 flex items-center gap-2">
                <Mic class="size-6" />
                <h1 class="text-2xl font-semibold">회의록 요약</h1>
            </div>

            <PageTabs :tabs="voiceTabs" />

            <Button variant="ghost" size="sm" class="mb-4" @click="router.back()">
                <ArrowLeft class="mr-1.5 size-4" />
                이력으로
            </Button>

            <Alert v-if="error" variant="destructive">
                <AlertDescription>{{ error }}</AlertDescription>
            </Alert>

            <div v-if="loading" class="flex justify-center py-16">
                <Spinner class="size-6" />
            </div>

            <template v-else-if="record">
                <h2 class="mb-3 text-lg font-semibold">{{ record.title }}</h2>

                <VoiceAudioPlayer
                    class="mb-4"
                    :record-seq="record.recordSeq"
                    :origin-file-name="record.originFileName"
                    :has-audio="record.hasAudio"
                />

                <VoiceResultPanel :record="record" />
            </template>
        </div>
    </div>
</template>
