<script setup lang="ts">
/**
 * 로그인 화면 (/login) — 단독 카드 레이아웃.
 * 일반 사용자는 소셜 로그인으로 들어오고, 이메일/비밀번호는 관리자·데모 계정용으로 남겨둔다 (F1-1).
 * 실패/잠금 사유는 서버 메시지를 그대로 표시한다(어느 쪽 오류인지 노출 안 함).
 */
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
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
import { useAuth } from '../composables/useAuth'

const route = useRoute()
const { loading, errorMessage, login } = useAuth()

const email = ref('')
const password = ref('')

// 소셜 로그인 실패 시 WEB 이 ?error= 로 사유를 실어 되돌려보낸다 (F1-1)
const socialError = computed(() => {
    const value = route.query.error
    return typeof value === 'string' && value !== '' ? value : null
})
// 미입력 시 로그인 버튼 비활성 (F1-05)
const canSubmit = computed(
    () => email.value.trim() !== '' && password.value !== '' && !loading.value,
)

function onSubmit(): void {
    if (!canSubmit.value) return
    login(email.value.trim(), password.value)
}

/**
 * 소셜 로그인 시작.
 * OAuth 는 XHR 이 아니라 제공자로의 전체 페이지 이동이라 axios 가 아닌 location 이동을 쓴다.
 * 개발 중에는 5173 에서 시작하더라도 콜백은 세션을 쥔 8080(BFF)으로 돌아온다.
 *
 * @param provider 제공자 registrationId ('naver')
 */
function startSocialLogin(provider: string): void {
    window.location.href = `/oauth2/authorization/${provider}`
}
</script>

<template>
    <Card class="w-full max-w-sm">
        <CardHeader>
            <CardTitle class="text-2xl">로그인</CardTitle>
            <CardDescription>Workmate 업무 비서에 로그인하세요.</CardDescription>
        </CardHeader>
        <CardContent class="flex flex-col gap-5">
            <Alert v-if="socialError" variant="destructive">
                <AlertDescription>{{ socialError }}</AlertDescription>
            </Alert>

            <!-- 소셜 로그인 — 일반 사용자의 기본 경로 -->
            <Button
                type="button"
                class="w-full gap-2 bg-[#03C75A] text-white hover:bg-[#02b351]"
                @click="startSocialLogin('naver')"
            >
                <span class="text-base font-bold leading-none">N</span>
                네이버로 계속하기
            </Button>

            <div class="flex items-center gap-3">
                <span class="h-px flex-1 bg-border" />
                <span class="text-xs text-muted-foreground">또는</span>
                <span class="h-px flex-1 bg-border" />
            </div>

            <!-- 이메일 로그인 — 관리자·데모 계정용 -->
            <form class="flex flex-col gap-4" @submit.prevent="onSubmit">
                <div class="flex flex-col gap-2">
                    <Label for="email">이메일</Label>
                    <Input
                        id="email"
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
                    {{ loading ? '로그인 중…' : '이메일로 로그인' }}
                </Button>
            </form>
        </CardContent>
    </Card>
</template>
