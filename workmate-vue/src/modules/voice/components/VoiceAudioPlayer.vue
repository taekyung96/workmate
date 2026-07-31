<script setup lang="ts">
/**
 * 회의 오디오 재생기.
 * <audio> 에 스트리밍 URL 을 직접 물려 브라우저가 Range 요청으로 구간 이동하게 한다.
 * MVP 기간에 저장된 회의록은 오디오가 없어 안내 문구만 보여준다.
 */
import { ref } from 'vue'
import { FileAudio } from 'lucide-vue-next'
import { voiceApi } from '../api/voice.api'

const props = defineProps<{
    recordSeq: number
    originFileName: string | null
    hasAudio: boolean
}>()

const failed = ref(false)
</script>

<template>
    <div class="rounded-lg border px-4 py-3">
        <template v-if="props.hasAudio">
            <div class="mb-2 flex items-center gap-1.5 text-sm text-muted-foreground">
                <FileAudio class="size-4" />
                <span>{{ props.originFileName }}</span>
            </div>
            <p v-if="failed" class="text-sm text-destructive">오디오를 재생할 수 없습니다.</p>
            <audio
                v-else
                class="w-full"
                controls
                preload="metadata"
                :src="voiceApi.audioUrl(props.recordSeq)"
                @error="failed = true"
            />
        </template>
        <p v-else class="text-sm text-muted-foreground">이 회의록에는 저장된 오디오가 없습니다.</p>
    </div>
</template>
