<script setup lang="ts">
/**
 * Workmate 심볼 — 라운드 스퀘어 안의 W 워드마크.
 *
 * 색은 Tailwind 유틸(fill-primary·stroke-primary-foreground)로 지정해 라이트/다크 테마를 따라간다.
 * 반짝임(sparkle)은 AI 를 암시하는 요소인데 작은 크기에서는 뭉개지므로 끌 수 있게 뒀다.
 *
 * @prop sparkle - 반짝임 표시 여부 (기본 true). 24px 이하에서는 false 를 권장한다.
 */
withDefaults(defineProps<{ sparkle?: boolean }>(), { sparkle: true })
</script>

<template>
    <svg viewBox="0 0 128 128" aria-hidden="true">
        <rect width="128" height="128" rx="28" class="fill-primary" />
        <path
            d="M32 50 L45 92 L60 64 L75 92 L88 50"
            fill="none"
            stroke-width="10"
            stroke-linecap="round"
            stroke-linejoin="round"
            class="stroke-primary-foreground"
        />
        <path
            v-if="sparkle"
            d="M101 24 L104.5 33.5 L114 37 L104.5 40.5 L101 50 L97.5 40.5 L88 37 L97.5 33.5 Z"
            class="fill-primary-foreground"
        />
    </svg>
</template>
