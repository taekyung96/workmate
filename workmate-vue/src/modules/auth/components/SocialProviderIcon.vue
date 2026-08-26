<script setup lang="ts">
/**
 * 소셜 로그인 버튼 안에 들어가는 제공자 마크 (F1-1).
 *
 * 제공자마다 마크 형태가 달라(글자·단색 도형·다색 로고) 로그인 화면에 v-if 를 늘어놓는 대신
 * 여기로 분리했다. 제공자를 늘릴 때 이 파일만 손보면 된다.
 *
 * @prop provider - 제공자 registrationId ('naver' · 'kakao' · 'google')
 */
defineProps<{ provider: string }>()

// v-if/v-else-if 체인이라 루트가 fragment 로 잡혀 class 가 자동 상속되지 않는다.
// 각 분기에 $attrs 를 직접 넘긴다 (안 그러면 부모가 준 size-4 가 먹지 않는다)
defineOptions({ inheritAttrs: false })
</script>

<template>
    <!-- 네이버는 워드마크 N 자체가 상징이라 글자로 둔다 -->
    <span v-if="provider === 'naver'" v-bind="$attrs" class="text-base font-bold leading-none"
        >N</span
    >

    <!-- 카카오는 말풍선 마크 — 버튼 글자색을 그대로 따라가게 fill-current 를 쓴다 -->
    <svg
        v-else-if="provider === 'kakao'"
        v-bind="$attrs"
        viewBox="0 0 24 24"
        class="fill-current"
        aria-hidden="true"
    >
        <path
            d="M12 3C6.99 3 3 6.2 3 10.14c0 2.5 1.65 4.7 4.14 5.96l-.9 3.3c-.09.32.27.57.55.39l3.94-2.6c.42.05.84.08 1.27.08 5.01 0 9-3.2 9-7.13S17.01 3 12 3z"
        />
    </svg>

    <!-- 구글은 4색 G 마크가 규정이라 색을 각 path 에 직접 지정한다 (fill-current 를 쓰면 안 된다) -->
    <svg v-else-if="provider === 'google'" v-bind="$attrs" viewBox="0 0 24 24" aria-hidden="true">
        <path
            fill="#4285F4"
            d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
        />
        <path
            fill="#34A853"
            d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
        />
        <path
            fill="#FBBC05"
            d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
        />
        <path
            fill="#EA4335"
            d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
        />
    </svg>
</template>
