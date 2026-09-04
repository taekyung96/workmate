<script setup lang="ts">
/**
 * 앱 기본 레이아웃 — 좌측 공통 사이드바 + 본문(RouterView) + 우측 도우미 패널.
 */
import { ref } from 'vue'
import { Menu } from 'lucide-vue-next'
import AppSidebar from './AppSidebar.vue'
import AssistantToggle from '@/common/components/assistant/AssistantToggle.vue'
import AssistantPanel from '@/common/components/assistant/AssistantPanel.vue'
import BrandMark from '@/common/components/BrandMark.vue'
import { Button } from '@/common/components/ui/button'

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

/**
 * 모바일 사이드바(서랍) 열림 상태.
 *
 * 사이드바가 아니라 레이아웃이 드는 이유: 여는 버튼(햄버거)이 사이드바 <b>바깥</b>
 * 상단 바에 있어야 한다. 닫혀 있을 때 사이드바는 화면 밖이라 스스로를 열 수 없다.
 *
 * md 이상에서는 사이드바가 항상 보이므로 이 값은 쓰이지 않는다.
 */
const sidebarOpen = ref(false)
</script>

<template>
    <div class="flex h-screen">
        <AppSidebar :open="sidebarOpen" @close="sidebarOpen = false" />

        <!-- 모바일 서랍 배경 덮개 — 바깥을 누르면 닫힌다. 서랍(z-50)보다 뒤(z-40)에 둔다 -->
        <div
            v-if="sidebarOpen"
            class="fixed inset-0 z-40 bg-black/50 md:hidden"
            aria-hidden="true"
            @click="sidebarOpen = false"
        />

        <!-- 본문 열 — 모바일에서는 위에 상단 바가 얹히므로 세로로 쌓는다.
             min-w-0 는 도우미 패널이 열릴 때 본문이 실제로 줄어들게 하고,
             min-h-0 는 안쪽 화면(h-full)이 상단 바를 뺀 높이를 정확히 받게 한다 -->
        <div class="flex min-w-0 min-h-0 flex-1 flex-col">
            <!-- 모바일 상단 바 — 사이드바가 접혀 있어 로고와 메뉴 진입점이 여기 필요하다 -->
            <header
                class="flex h-14 shrink-0 items-center gap-2 border-b bg-background px-3 md:hidden"
            >
                <Button
                    variant="ghost"
                    size="icon"
                    aria-label="메뉴 열기"
                    @click="sidebarOpen = true"
                >
                    <Menu class="size-5" />
                </Button>
                <BrandMark :sparkle="false" class="size-6 shrink-0" />
                <span class="font-semibold">Workmate</span>
            </header>

            <!-- 패널이 열리면 flex 가 남은 폭으로 줄여 준다(min-w-0 이 있어야 실제로 줄어든다).
                 패널 폭이 애니메이션되는 동안 본문 폭도 매 프레임 다시 계산돼 함께 부드럽게 밀린다 -->
            <main class="min-h-0 min-w-0 flex-1 overflow-hidden">
                <slot />
            </main>
        </div>

        <!--
            페이지 인식 도우미 — 로그인 후 모든 화면에 뜬다 (로그인 화면은 이 레이아웃을 쓰지 않는다).

            데스크탑(md↑): static 이라 레이아웃의 한 칸이 된다 → 본문이 왼쪽으로 밀린다.
            모바일: 나눠 쓸 폭이 없어 전체 화면 오버레이로 덮는다.

            v-if 로 마운트/언마운트하는 이유: 닫을 때 useAssistant 의 상태가 함께 사라져야
            "닫으면 대화가 비워진다"는 규칙이 컴포넌트 생명주기로 보장된다.
        -->
        <Transition name="assistant-dock">
            <div
                v-if="assistantOpen"
                class="fixed inset-0 z-50 md:static md:z-auto md:w-96 md:shrink-0"
            >
                <!--
                    안쪽 폭을 24rem 으로 고정한다. 바깥 폭이 0 → 24rem 으로 애니메이션되는 동안
                    안쪽까지 같이 줄었다 늘어나면 글자가 매 프레임 재배치돼 덜컹거린다.
                    안쪽을 고정해 두면 '가려져 있던 패널이 드러나는' 움직임이 된다.
                -->
                <div class="h-full w-full md:w-96">
                    <AssistantPanel @close="assistantOpen = false" />
                </div>
            </div>
        </Transition>

        <!-- 토글 버튼: 패널이 열려 있으면 숨긴다(패널 안에 닫기 버튼이 있다) -->
        <AssistantToggle v-if="!assistantOpen" @open="assistantOpen = true" />
    </div>
</template>

<style scoped>
/*
 * 데스크탑: 패널이 차지하는 '폭'을 애니메이션한다.
 * 폭이 변하면 flex 가 본문 폭을 다시 계산하므로, 본문이 따라서 부드럽게 밀린다.
 * (transform 으로 밀면 패널만 움직이고 본문은 그대로라 목적을 이루지 못한다.)
 */
@media (min-width: 768px) {
    .assistant-dock-enter-active,
    .assistant-dock-leave-active {
        /* 폭이 줄어드는 동안만 잘라낸다. 평상시에도 걸어 두면 패널 안에서 바깥으로
           떠야 하는 요소(드롭다운·툴팁)가 나중에 잘리게 된다 */
        overflow: hidden;
        transition: width 220ms ease-out;
    }

    .assistant-dock-enter-from,
    .assistant-dock-leave-to {
        width: 0;
    }
}

/* 모바일: 전체 화면 오버레이라 폭이 아니라 오른쪽에서 밀려 들어오게 한다 */
@media (max-width: 767px) {
    .assistant-dock-enter-active,
    .assistant-dock-leave-active {
        transition:
            transform 200ms ease-out,
            opacity 200ms ease-out;
    }

    .assistant-dock-enter-from,
    .assistant-dock-leave-to {
        opacity: 0;
        transform: translateX(100%);
    }
}

/* 움직임을 줄이도록 설정한 사용자에게는 애니메이션을 걸지 않는다 */
@media (prefers-reduced-motion: reduce) {
    .assistant-dock-enter-active,
    .assistant-dock-leave-active {
        transition: none;
    }
}
</style>
