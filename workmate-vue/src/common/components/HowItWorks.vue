<script setup lang="ts">
/**
 * "이렇게 동작해요" 3단계 안내 카드 — 기능 진입(빈) 화면에서 실제 처리 흐름을 보여준다.
 * 영수증·회의록 등 여러 화면이 공유하는 공통 부품.
 */
import type { Component } from 'vue'

/** 단계 하나 (아이콘·제목·설명). 번호는 배열 순서로 자동 매겨진다. */
interface Step {
    icon: Component
    title: string
    desc: string
}

defineProps<{ steps: Step[] }>()
</script>

<template>
    <div>
        <p class="mb-2.5 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            이렇게 동작해요
        </p>
        <div class="grid gap-2.5 sm:grid-cols-3">
            <div
                v-for="(step, index) in steps"
                :key="index"
                class="relative rounded-xl border bg-card p-3.5 shadow-sm"
            >
                <component
                    :is="step.icon"
                    class="absolute top-3.5 right-3.5 size-4 text-muted-foreground"
                />
                <div
                    class="mb-2 grid size-6 place-items-center rounded-md bg-accent text-xs font-bold"
                >
                    {{ index + 1 }}
                </div>
                <h4 class="text-sm font-semibold">{{ step.title }}</h4>
                <p class="mt-0.5 text-xs text-muted-foreground">{{ step.desc }}</p>
            </div>
        </div>
    </div>
</template>
