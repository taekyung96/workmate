<script setup lang="ts">
/**
 * 로그인 화면 (/login) — 단독 카드 레이아웃.
 *
 * 일반 사용자는 소셜 로그인으로 들어온다 (F1-1). 이메일/비밀번호는 관리자·데모 계정 전용이라
 * 기본 노출하지 않고 링크 뒤에 접어둔다.
 * 실패/잠금 사유는 서버 메시지를 그대로 표시한다(어느 쪽 오류인지 노출 안 함).
 */
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ChevronDown } from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import { Input } from '@/common/components/ui/input'
import { Label } from '@/common/components/ui/label'
import { Alert, AlertDescription } from '@/common/components/ui/alert'
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from '@/common/components/ui/card'
import BrandMark from '@/common/components/BrandMark.vue'
import { useAuth } from '../composables/useAuth'

const route = useRoute()
const { loading, errorMessage, login } = useAuth()

/**
 * 소셜 로그인 버튼 목록.
 * id 는 서버 registrationId 와 같아야 한다 (application.yml 의 registration 키).
 * 제공자를 늘릴 때 여기에 한 줄만 추가하면 된다.
 */
const socialProviders = [
    {
        id: 'naver',
        label: '네이버 로그인',
        buttonClass: 'bg-[#03C75A] text-white hover:bg-[#02b351]',
    },
    {
        id: 'kakao',
        label: '카카오 로그인',
        buttonClass: 'bg-[#FEE500] text-black hover:bg-[#f2d900]',
    },
]

const email = ref('')
const password = ref('')
const showEmailForm = ref(false)
const emailInput = ref<InstanceType<typeof Input> | null>(null)

// 소셜 로그인 실패 시 WEB 이 ?error= 로 사유를 실어 되돌려보낸다 (F1-1)
const socialError = computed(() => {
    const value = route.query.error
    return typeof value === 'string' && value !== '' ? value : null
})
// 미입력 시 로그인 버튼 비활성 (F1-05)
const canSubmit = computed(
    () => email.value.trim() !== '' && password.value !== '' && !loading.value,
)

// 이메일 로그인이 실패하면 폼이 접혀 사유만 덩그러니 남는 상황을 막는다
watch(errorMessage, (message) => {
    if (message) showEmailForm.value = true
})

/** 이메일 폼을 펼치고 첫 입력칸으로 포커스를 옮긴다 */
async function openEmailForm(): Promise<void> {
    showEmailForm.value = true
    await nextTick()
    emailInput.value?.$el?.focus()
}

function onSubmit(): void {
    if (!canSubmit.value) return
    login(email.value.trim(), password.value)
}

/**
 * 소셜 로그인 시작.
 * OAuth 는 XHR 이 아니라 제공자로의 전체 페이지 이동이라 axios 가 아닌 location 이동을 쓴다.
 * 개발 중에는 5173 에서 시작하더라도 콜백은 세션을 쥔 8080(BFF)으로 돌아온다.
 *
 * @param provider 제공자 registrationId ('naver' · 'kakao')
 */
function startSocialLogin(provider: string): void {
    window.location.href = `/oauth2/authorization/${provider}`
}
</script>

<template>
    <Card class="w-full max-w-sm">
        <CardHeader>
            <BrandMark class="mb-2 size-12" />
            <CardTitle class="text-2xl">로그인</CardTitle>
            <CardDescription>Workmate 업무 비서에 로그인하세요.</CardDescription>
        </CardHeader>
        <CardContent class="flex flex-col gap-4">
            <Alert v-if="socialError" variant="destructive">
                <AlertDescription>{{ socialError }}</AlertDescription>
            </Alert>

            <!-- 소셜 로그인 — 일반 사용자의 기본 경로 -->
            <div class="flex flex-col gap-2">
                <Button
                    v-for="provider in socialProviders"
                    :key="provider.id"
                    type="button"
                    class="w-full gap-2"
                    :class="provider.buttonClass"
                    @click="startSocialLogin(provider.id)"
                >
                    <span v-if="provider.id === 'naver'" class="text-base font-bold leading-none">
                        N
                    </span>
                    <!-- 카카오는 말풍선 마크가 상징이라 글자 대신 도형으로 둔다 -->
                    <svg
                        v-else-if="provider.id === 'kakao'"
                        viewBox="0 0 24 24"
                        class="size-4 fill-current"
                        aria-hidden="true"
                    >
                        <path
                            d="M12 3C6.99 3 3 6.2 3 10.14c0 2.5 1.65 4.7 4.14 5.96l-.9 3.3c-.09.32.27.57.55.39l3.94-2.6c.42.05.84.08 1.27.08 5.01 0 9-3.2 9-7.13S17.01 3 12 3z"
                        />
                    </svg>
                    {{ provider.label }}
                </Button>
            </div>

            <!-- 이메일 로그인 — 관리자·데모 계정용이라 기본은 접어둔다 -->
            <button
                v-if="!showEmailForm"
                type="button"
                class="mx-auto flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
                @click="openEmailForm"
            >
                이메일로 로그인
                <ChevronDown class="size-4" />
            </button>

            <form v-else class="flex flex-col gap-4" @submit.prevent="onSubmit">
                <div class="flex items-center gap-3">
                    <span class="h-px flex-1 bg-border" />
                    <span class="text-xs text-muted-foreground">이메일로 로그인</span>
                    <span class="h-px flex-1 bg-border" />
                </div>

                <div class="flex flex-col gap-2">
                    <Label for="email">이메일</Label>
                    <Input
                        id="email"
                        ref="emailInput"
                        v-model="email"
                        type="email"
                        autocomplete="email"
                        placeholder="you@example.com"
                    />
                </div>

                <div class="flex flex-col gap-2">
                    <Label for="password">비밀번호</Label>
                    <Input
                        id="password"
                        v-model="password"
                        type="password"
                        autocomplete="current-password"
                    />
                </div>

                <Alert v-if="errorMessage" variant="destructive">
                    <AlertDescription>{{ errorMessage }}</AlertDescription>
                </Alert>

                <Button type="submit" variant="outline" :disabled="!canSubmit">
                    {{ loading ? '로그인 중…' : '로그인' }}
                </Button>
            </form>
        </CardContent>
    </Card>
</template>
