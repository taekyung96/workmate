<script setup lang="ts">
/**
 * 확인 다이얼로그 (예/아니오) — 삭제·초기화 등 되돌리기 어려운 동작 실행 전 확인 공통 부품.
 * 각 화면에 복붙돼 있던 AlertDialog 마크업을 한 곳으로 통일한다. 열림 상태는 v-model:open 으로 제어한다.
 *
 * Props
 *   - open:        열림 여부 (v-model:open)
 *   - title:       확인 문구 제목
 *   - description: 부연 설명 (선택)
 *   - confirmText: 실행 버튼 라벨 (기본 '삭제')
 *   - cancelText:  취소 버튼 라벨 (기본 '취소')
 * Emits
 *   - update:open: 열림 상태 변경 (v-model)
 *   - confirm:     실행 버튼 클릭
 */
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/common/components/ui/alert-dialog'

withDefaults(
    defineProps<{
        open: boolean
        title: string
        description?: string
        confirmText?: string
        cancelText?: string
    }>(),
    {
        confirmText: '삭제',
        cancelText: '취소',
    },
)

const emit = defineEmits<{ 'update:open': [value: boolean]; confirm: [] }>()
</script>

<template>
    <AlertDialog :open="open" @update:open="emit('update:open', $event)">
        <AlertDialogContent>
            <AlertDialogHeader>
                <AlertDialogTitle>{{ title }}</AlertDialogTitle>
                <AlertDialogDescription v-if="description">
                    {{ description }}
                </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
                <AlertDialogCancel>{{ cancelText }}</AlertDialogCancel>
                <AlertDialogAction @click="emit('confirm')">{{ confirmText }}</AlertDialogAction>
            </AlertDialogFooter>
        </AlertDialogContent>
    </AlertDialog>
</template>
