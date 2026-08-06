<script setup lang="ts">
/**
 * 파일 업로드 드롭존 — 드래그앤드롭 + 클릭 선택. 영수증(이미지)·회의록(오디오) 등이 공유하는 공통 부품.
 * 실제 형식·크기 검증은 상위(@select 핸들러)에서 하고, 여기선 고른 File 만 올려보낸다.
 *
 * Props
 *   - icon:   안내 아이콘 컴포넌트 (lucide)
 *   - title:  안내 제목 (예: '영수증 이미지를 올려주세요')
 *   - accept: file input 의 accept 속성 (예: 'audio/*')
 *   - chips:  형식·크기 안내 칩 목록 (예: ['JPG', 'PNG', '최대 10MB'])
 * Emits
 *   - select: 사용자가 고른 File
 */
import { ref, type Component } from 'vue'

defineProps<{
    icon: Component
    title: string
    accept: string
    chips: string[]
}>()

const emit = defineEmits<{ select: [file: File] }>()

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
            <component :is="icon" class="size-6 text-muted-foreground" />
        </div>
        <p class="font-semibold">{{ title }}</p>
        <p class="mt-1 text-sm text-muted-foreground">클릭하거나 여기로 끌어다 놓으세요</p>
        <div class="mt-4 flex flex-wrap justify-center gap-1.5">
            <span
                v-for="chip in chips"
                :key="chip"
                class="rounded-full bg-muted px-2.5 py-0.5 text-[11px] font-medium text-muted-foreground"
            >
                {{ chip }}
            </span>
        </div>
        <input ref="inputEl" type="file" :accept="accept" class="hidden" @change="onPicked" />
    </div>
</template>
