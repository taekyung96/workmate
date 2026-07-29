<script setup lang="ts">
/**
 * 마크다운 에디터 (Toast UI Editor 래퍼).
 * WYSIWYG(일반 에디터) 모드로 편집하되 값은 **마크다운 문자열**로 저장한다 → RAG·상세보기 무변경.
 * v-model 로 마크다운 문자열을 주고받는다.
 */
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Editor from '@toast-ui/editor'
import '@toast-ui/editor/dist/toastui-editor.css'
import '@toast-ui/editor/dist/theme/toastui-editor-dark.css'

const props = withDefaults(defineProps<{ modelValue: string; height?: string }>(), {
    height: '420px',
})
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const container = ref<HTMLElement | null>(null)
let editor: Editor | null = null
// setMarkdown 으로 값을 주입하는 동안 change 이벤트가 되돌아 emit 되는 무한 루프를 막는 플래그
let syncingFromProp = false

onMounted(() => {
    if (!container.value) return
    // 에디터 테마는 OS 설정(prefers-color-scheme)이 아니라 앱의 실제 테마를 따른다.
    // 앱은 shadcn 규칙대로 <html>에 .dark 클래스가 붙을 때만 다크모드다. 그렇지 않으면
    // 라이트인 앱에서 에디터만 검게 보이는 문제가 생긴다.
    const isDark = document.documentElement.classList.contains('dark')
    editor = new Editor({
        el: container.value,
        height: props.height,
        initialEditType: 'wysiwyg', // 일반 에디터(WYSIWYG) 모드로 시작
        previewStyle: 'vertical',
        initialValue: props.modelValue,
        usageStatistics: false,
        theme: isDark ? 'dark' : 'light',
    })
    editor.on('change', () => {
        if (syncingFromProp) return
        emit('update:modelValue', editor!.getMarkdown())
    })
})

// 외부에서 값이 바뀌면(수정 모드 진입 등) 에디터에 반영
watch(
    () => props.modelValue,
    (value) => {
        if (!editor) return
        if (value !== editor.getMarkdown()) {
            syncingFromProp = true
            editor.setMarkdown(value ?? '', false)
            syncingFromProp = false
        }
    },
)

onBeforeUnmount(() => {
    editor?.destroy()
    editor = null
})
</script>

<template>
    <div ref="container" />
</template>
