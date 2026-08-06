<script setup lang="ts">
/**
 * 화면 상단 공통 헤더 — (아이콘 박스) + 제목 + (한 줄 설명) + (우측 액션).
 * 영수증·회의록·가이드·관리자 등 여러 화면이 같은 모양을 공유해 통일감을 준다.
 *
 * Props
 *   - icon:        lucide 아이콘 컴포넌트. 있으면 primary 색 박스에 렌더, 없으면 박스 자체를 숨긴다.
 *   - title:       화면 제목 (필수)
 *   - description: 제목 아래 한 줄 설명 (선택)
 * Slots
 *   - actions:     우측 정렬 액션 영역 (예: "+ 새 문서", "CSV 다운로드"). 없으면 렌더 안 함.
 */
import type { Component } from 'vue'

defineProps<{
    icon?: Component
    title: string
    description?: string
}>()
</script>

<template>
    <div class="mb-6 flex items-center justify-between gap-3">
        <div class="flex min-w-0 items-center gap-3">
            <!-- 아이콘 박스: icon prop 이 있을 때만 렌더 -->
            <div
                v-if="icon"
                class="grid size-10 shrink-0 place-items-center rounded-xl bg-primary text-primary-foreground"
            >
                <component :is="icon" class="size-5" />
            </div>
            <div class="min-w-0">
                <h1 class="truncate text-2xl leading-tight font-semibold">{{ title }}</h1>
                <p v-if="description" class="text-sm text-muted-foreground">{{ description }}</p>
            </div>
        </div>

        <!-- 우측 액션: 슬롯 내용이 있을 때만 렌더 -->
        <div v-if="$slots.actions" class="flex shrink-0 items-center gap-2">
            <slot name="actions" />
        </div>
    </div>
</template>
