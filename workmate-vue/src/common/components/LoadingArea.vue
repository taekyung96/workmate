<script setup lang="ts">
/**
 * 로딩 중 중앙 스피너 영역 — 목록·상세 화면이 공유하던 `flex justify-center py-16` 스피너 블록을 통일.
 * 표시 여부는 호출부에서 `v-if` 로 제어한다. (예: <LoadingArea v-if="loading" />)
 */
import { Spinner } from '@/common/components/ui/spinner'
</script>

<template>
    <div class="flex justify-center py-16">
        <Spinner class="size-6" />
    </div>
</template>
