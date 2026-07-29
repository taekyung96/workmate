<script setup lang="ts">
/**
 * 관리자 영역 상단 탭 네비게이션.
 * "사용자 관리"·"감사 로그" 두 하위 화면을 오가는 공용 탭 (사이드바는 관리자 진입점 하나만 유지).
 * 현재 라우트 이름으로 활성 탭을 표시한다.
 */
import { RouterLink, useRoute } from 'vue-router'

const route = useRoute()

// 탭 정의 — 라우트 name 으로 활성 여부를 판정한다
const tabs = [
    { name: 'admin-users', label: '사용자 관리' },
    { name: 'admin-audit-logs', label: '감사 로그' },
    { name: 'admin-common-codes', label: '공통코드' },
]
</script>

<template>
    <nav class="mb-6 flex gap-1 border-b">
        <RouterLink
            v-for="tab in tabs"
            :key="tab.name"
            :to="{ name: tab.name }"
            class="-mb-px border-b-2 px-4 py-2 text-sm font-medium"
            :class="
                route.name === tab.name
                    ? 'border-primary text-foreground'
                    : 'border-transparent text-muted-foreground hover:text-foreground'
            "
        >
            {{ tab.label }}
        </RouterLink>
    </nav>
</template>
