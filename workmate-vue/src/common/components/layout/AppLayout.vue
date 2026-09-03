<script setup lang="ts">
/**
 * 앱 기본 레이아웃 — 좌측 공통 사이드바 + 본문(RouterView) + 우측 도우미 패널.
 */
import { ref } from 'vue'
import AppSidebar from './AppSidebar.vue'
import AssistantToggle from '@/common/components/assistant/AssistantToggle.vue'
import AssistantPanel from '@/common/components/assistant/AssistantPanel.vue'

/**
 * 도우미 패널 열림 상태.
 *
 * 토글 버튼이 아니라 레이아웃이 이 상태를 들고 있는 이유: 패널이 본문과 같은 flex 행에
 * 놓여야 본문이 밀려나기 때문이다. 상태가 버튼 안에 있으면 패널을 레이아웃 바깥(fixed)에
 * 그릴 수밖에 없고, 그러면 본문 오른쪽을 덮어 가린다.
 *
 * 여러 화면이 공유하는 값이 아니라 이 레이아웃 한 곳의 상태라 store 가 아니라 로컬 ref 다.
 */
const assistantOpen = ref(false)
</script>

<template>
    <div class="flex h-screen">
        <AppSidebar />

        <!-- 본문 — 패널이 열리면 flex 가 남은 폭으로 줄여 준다(min-w-0 이 있어야 실제로 줄어든다) -->
        <main class="min-w-0 flex-1 overflow-hidden">
            <slot />
        </main>

        <!--
            페이지 인식 도우미 — 로그인 후 모든 화면에 뜬다 (로그인 화면은 이 레이아웃을 쓰지 않는다).

            데스크탑(md↑): static 이라 레이아웃의 한 칸이 된다 → 본문이 왼쪽으로 밀린다.
            모바일: 나눠 쓸 폭이 없어 전체 화면 오버레이로 덮는다.

            v-if 로 마운트/언마운트하는 이유: 닫을 때 useAssistant 의 상태가 함께 사라져야
            "닫으면 대화가 비워진다"는 규칙이 컴포넌트 생명주기로 보장된다.
        -->
        <div
            v-if="assistantOpen"
            class="fixed inset-0 z-50 md:static md:z-auto md:w-96 md:shrink-0"
        >
            <AssistantPanel @close="assistantOpen = false" />
        </div>

        <!-- 토글 버튼: 패널이 열려 있으면 숨긴다(패널 안에 닫기 버튼이 있다) -->
        <AssistantToggle v-if="!assistantOpen" @open="assistantOpen = true" />
    </div>
</template>
