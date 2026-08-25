import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { extractErrorMessage } from '@/common/utils/error'
import { authApi } from '../api/auth.api'
import { useAuthStore } from '../stores/auth.store'

/**
 * 인증 화면의 로직을 담는 composable (≈ Service).
 * 화면(View)은 이 함수들만 호출하고 api를 직접 부르지 않는다.
 * 회원가입은 관리자 전용 API 로 바뀌어(F1-1) 여기서 다루지 않는다.
 */
export function useAuth() {
    const store = useAuthStore()
    const router = useRouter()
    const route = useRoute()

    const loading = ref(false)
    const errorMessage = ref('')

    /**
     * 로그인 시도. 성공 시 원래 목적지(redirect 쿼리) 또는 /chat 으로 이동.
     */
    async function login(email: string, password: string): Promise<void> {
        loading.value = true
        errorMessage.value = ''
        try {
            const user = await authApi.login(email, password)
            store.setUser(user)
            const redirect = (route.query.redirect as string) || '/chat'
            await router.replace(redirect)
        } catch (error) {
            errorMessage.value = extractErrorMessage(error, '로그인에 실패했습니다.')
        } finally {
            loading.value = false
        }
    }

    /** 로그아웃 후 로그인 화면으로 이동 */
    async function logout(): Promise<void> {
        try {
            await authApi.logout()
        } finally {
            store.clear()
            // 하드 리로드로 로그인 화면으로 이동한다.
            // 이유: 채팅 방 목록·메시지·모델 선택 등 "사용자별" Pinia 상태가 메모리에 남아,
            // 다른 사용자로 재로그인하면 이전 사용자의 데이터가 잠깐 노출되던 문제가 있었다.
            // (client-side 라우팅은 스토어를 초기화하지 않아 새로고침 전까지 이전 상태가 보였다.)
            // 전체 리로드는 모든 스토어를 새로 생성해 사용자 간 상태 누수를 원천 차단한다.
            window.location.assign('/login')
        }
    }

    return { loading, errorMessage, login, logout }
}
