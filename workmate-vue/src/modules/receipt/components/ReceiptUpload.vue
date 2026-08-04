<script setup lang="ts">
/**
 * 영수증 이미지 업로드 영역 — 드래그앤드롭 + 클릭 선택.
 * 실제 형식·크기 검증은 상위(useReceiptAnalyze.selectFile)에서 하고, 여기선 File만 올려보낸다.
 */
import { ref } from 'vue'
import { ImageUp, Plus } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'

const emit = defineEmits<{ select: [file: File] }>()

/** 지원 형식 안내 칩 */
const formatChips = ['JPG', 'PNG', 'WEBP', '최대 10MB']

const inputEl = ref<HTMLInputElement | null>(null)
const dragging = ref(false)

function openPicker(): void {
    inputEl.value?.click()
}

function onPicked(event: Event): void {
    const target = event.target as HTMLInputElement
    const file = target.files?.[0]
    if (file) emit('select', file)
    target.value = '' // 같은 파일 재선택 허용
}

function onDrop(event: DragEvent): void {
    dragging.value = false
    const file = event.dataTransfer?.files?.[0]
    if (file) emit('select', file)
}
</script>

<template>
    <div
        class="flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed px-6 py-8 text-center transition-colors"
        :class="
            dragging
                ? 'border-primary bg-primary/5'
                : 'border-border hover:border-muted-foreground/40'
        "
        role="button"
        tabindex="0"
        @click="openPicker"
        @keydown.enter="openPicker"
        @dragover.prevent="dragging = true"
        @dragleave.prevent="dragging = false"
        @drop.prevent="onDrop"
    >
        <div class="mb-3 grid size-14 place-items-center rounded-full border bg-card shadow-sm">
            <ImageUp class="size-6 text-muted-foreground" />
        </div>
        <p class="font-semibold">영수증 이미지를 올려주세요</p>
        <p class="mt-1 text-sm text-muted-foreground">여기로 끌어다 놓거나 버튼으로 선택하세요</p>
        <Button class="mt-4" @click.stop="openPicker">
            <Plus class="mr-1.5 size-4" />
            파일 선택
        </Button>
        <div class="mt-3.5 flex flex-wrap justify-center gap-1.5">
            <span
                v-for="chip in formatChips"
                :key="chip"
                class="rounded-full bg-muted px-2.5 py-0.5 text-[11px] font-medium text-muted-foreground"
            >
                {{ chip }}
            </span>
        </div>
        <input
            ref="inputEl"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            class="hidden"
            @change="onPicked"
        />
    </div>
</template>
