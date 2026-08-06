<script setup lang="ts">
/**
 * 목록 페이징 컨트롤 (이전 / 현재·전체 / 다음) — 서버 페이징 화면 공통 부품.
 * `usePagination` 의 page(0-based)·totalPages 와 짝을 이루며, 페이지 이동은 change 이벤트로 위임한다.
 * (기존에 각 목록 화면에 복붙돼 있던 페이징 UI와 표기 규칙 `Math.max(totalPages, 1)` 을 한 곳으로 통일.)
 *
 * Props
 *   - page:       0-based 현재 페이지
 *   - totalPages: 전체 페이지 수
 * Emits
 *   - change: 이동할 0-based 페이지 번호
 */
import { Button } from '@/common/components/ui/button'

const props = defineProps<{
    page: number
    totalPages: number
}>()

const emit = defineEmits<{ change: [page: number] }>()
</script>

<template>
    <div class="flex items-center gap-3">
        <Button
            size="sm"
            variant="outline"
            :disabled="props.page <= 0"
            @click="emit('change', props.page - 1)"
        >
            이전
        </Button>
        <span class="text-sm text-muted-foreground">
            {{ props.page + 1 }} / {{ Math.max(props.totalPages, 1) }}
        </span>
        <Button
            size="sm"
            variant="outline"
            :disabled="props.page >= props.totalPages - 1"
            @click="emit('change', props.page + 1)"
        >
            다음
        </Button>
    </div>
</template>
