<script setup lang="ts">
/**
 * 앱 기본 레이아웃 — 좌측 공통 사이드바 + 우측 본문(RouterView).
 */
import AppSidebar from './AppSidebar.vue'
import AssistantToggle from '@/common/components/assistant/AssistantToggle.vue'
</script>

<template>
    <div class="flex h-screen">
        <AppSidebar />
        <main class="min-w-0 flex-1 overflow-hidden">
            <slot />
        </main>
        <!-- 페이지 인식 도우미 — 로그인 후 모든 화면에 뜬다 (로그인 화면은 이 레이아웃을 쓰지 않는다) -->
        <AssistantToggle />
    </div>
</template>
