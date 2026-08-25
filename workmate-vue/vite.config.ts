import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
    // VITE_NO_DEVTOOLS=1 이면 Vue DevTools 플로팅 패널을 끈다(README 스크린샷 등 깔끔한 캡처용).
    plugins: [vue(), ...(process.env.VITE_NO_DEVTOOLS ? [] : [vueDevTools()]), tailwindcss()],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url)),
        },
    },
    server: {
        // 개발 서버(5173)에서 /api 요청을 얇은 WEB(BFF, 8080)으로 프록시.
        // 브라우저는 8080만 바라보는 아키텍처를 dev 환경에서도 동일하게 재현한다.
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
            // 소셜 로그인(F1-1)도 8080 으로 넘긴다. 전체 페이지 이동이라 XHR 은 아니지만,
            // 이 경로를 프록시해야 화면 코드가 '/oauth2/...' 상대경로 하나로 dev·운영을 모두 커버한다.
            '/oauth2': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
            '/login/oauth2': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
        },
    },
})
