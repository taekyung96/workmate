<script setup lang="ts">
/**
 * 화면 상단 공통 탭 네비게이션 (밑줄형, 라우터 이동).
 * 관리자·영수증·회의록이 같은 모양·동작을 공유한다.
 * 탭 전환이 실제 라우트 이동이므로 새로고침·뒤로가기·링크 공유가 모두 동작한다.
 */
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { isTabActive, type PageTab } from './pageTabs'

const props = defineProps<{ tabs: PageTab[] }>()

const route = useRoute()
const currentName = computed(() => (route.name ? String(route.name) : ''))
</script>

<template>
    <nav class="mb-6 flex gap-1 border-b">
        <RouterLink
            v-for="tab in props.tabs"
            :key="tab.name"
            :to="{ name: tab.name }"
            class="-mb-px border-b-2 px-4 py-2 text-sm font-medium"
            :class="
                isTabActive(tab, currentName)
                    ? 'border-primary text-foreground'
                    : 'border-transparent text-muted-foreground hover:text-foreground'
            "
        >
            {{ tab.label }}
        </RouterLink>
    </nav>
</template>
