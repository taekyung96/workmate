<script setup lang="ts">
/**
 * 가이드 작성·수정 화면 (/guide/new, /guide/:id/edit).
 * 라우트에 id가 있으면 수정 모드로 기존 값을 불러오고, 없으면 신규 작성.
 */
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Button } from '@/common/components/ui/button'
import { Input } from '@/common/components/ui/input'
import { Label } from '@/common/components/ui/label'
import MarkdownEditor from '@/common/components/form/MarkdownEditor.vue'
import { Switch } from '@/common/components/ui/switch'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import { useGuideEditor } from '../composables/useGuideEditor'

const route = useRoute()
const router = useRouter()
const guideSeq = computed(() => (route.params.id ? Number(route.params.id) : null))
const isEdit = computed(() => guideSeq.value !== null)

const { form, loading, error, loadForEdit, submit } = useGuideEditor()

onMounted(() => {
    if (guideSeq.value !== null) loadForEdit(guideSeq.value)
})

// 제목·본문이 있어야 저장 가능
const canSubmit = computed(
    () => form.title.trim() !== '' && form.content.trim() !== '' && !loading.value,
)

async function onSubmit(): Promise<void> {
    if (!canSubmit.value) return
    const saved = await submit(guideSeq.value)
    if (saved) router.replace({ name: 'guide-detail', params: { id: saved.guideSeq } })
}
</script>

<template>
    <div class="slim-scroll h-full overflow-y-auto">
        <div class="mx-auto max-w-3xl px-6 py-8">
            <h1 class="mb-6 text-2xl font-semibold">
                {{ isEdit ? '가이드 수정' : '새 가이드 문서' }}
            </h1>

            <form class="flex flex-col gap-4" @submit.prevent="onSubmit">
                <div class="flex flex-col gap-2">
                    <Label for="title">제목</Label>
                    <Input id="title" v-model="form.title" placeholder="문서 제목" />
                </div>

                <div class="flex flex-col gap-2">
                    <Label>본문</Label>
                    <MarkdownEditor v-model="form.content" />
                </div>

                <div class="flex items-center gap-3">
                    <Switch id="isPublic" v-model="form.isPublic" />
                    <Label for="isPublic">공개 문서로 게시</Label>
                </div>

                <Alert v-if="error" variant="destructive">
                    <AlertDescription>{{ error }}</AlertDescription>
                </Alert>

                <div class="flex justify-end gap-2">
                    <Button type="button" variant="outline" @click="router.back()">취소</Button>
                    <Button type="submit" :disabled="!canSubmit">
                        {{ loading ? '저장 중…' : '저장' }}
                    </Button>
                </div>
            </form>
        </div>
    </div>
</template>
