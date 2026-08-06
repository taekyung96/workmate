import './styles/index.css'
// vue-sonner(Sonner) 기본 스타일 — 없으면 토스트가 스타일 없이 깨져서 뜨고 위치 지정도 안 먹는다
import 'vue-sonner/style.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { setUnauthorizedHandler } from './common/api/client'
import { useAuthStore } from './modules/auth/stores/auth.store'

const app = createApp(App)

app.use(createPinia())
app.use(router)

// 401(세션 만료/미인증) 전역 처리: auth store를 비우고 로그인 화면으로 유도.
// (pinia 설치 이후이므로 useAuthStore 호출 가능)
setUnauthorizedHandler(() => {
    const auth = useAuthStore()
    auth.clear()
    // 이미 공개 화면(로그인·회원가입)이면 리다이렉트하지 않는다 (stray 401로 튕기는 것 방지)
    const current = router.currentRoute.value
    if (current.meta.public !== true) {
        router.replace({ name: 'login' })
    }
})

app.mount('#app')
