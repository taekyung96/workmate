import { mount, type VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter, type RouteRecordRaw, type Router } from 'vue-router'
import { authRoutes } from '../../modules/auth/routes'
import { chatRoutes } from '../../modules/chat/routes'
import { guideRoutes } from '../../modules/guide/routes'
import { receiptRoutes } from '../../modules/receipt/routes'
import { voiceRoutes } from '../../modules/voice/routes'
import { usageRoutes } from '../../modules/usage/routes'
import { adminRoutes } from '../../modules/admin/routes'
import { nextTick, type Component } from 'vue'

/**
 * 페이지 스모크 테스트용 마운트 헬퍼.
 *
 * <p><b>왜 api 층만 목으로 잡나.</b> 이 프로젝트의 계층은 view → composable/store → api 다.
 * composable 을 통째로 목으로 바꾸면 정작 화면과 composable 을 잇는 배선이 검증되지 않는다.
 * api 만 막고 그 위는 실제 코드를 태워야, 응답 모양이 바뀌었을 때 어느 화면이 깨지는지 드러난다.</p>
 *
 * <p>자식 컴포넌트도 stub 하지 않는다. 여기서 잡고 싶은 첫 번째 결함이 <b>렌더 도중 터지는 것</b>이라
 * 실제로 그려 봐야 의미가 있다.</p>
 */

/** 빈 화면 — 라우트 해석에만 쓰인다 */
const BlankRoute = { template: '<div />' }

/**
 * 라우트 정의에서 컴포넌트만 빈 것으로 바꾼다 (경로·이름·중첩 구조는 유지).
 *
 * @param routes 실제 모듈이 내보낸 라우트 정의
 * @returns 컴포넌트가 비워진 같은 모양의 라우트 정의
 */
function stubComponents(routes: RouteRecordRaw[]): RouteRecordRaw[] {
    return routes.map((route) => {
        const next = { ...route } as Record<string, unknown>
        if ('component' in next) next.component = BlankRoute
        if ('components' in next) delete next.components
        if (Array.isArray(next.children)) {
            next.children = stubComponents(next.children as RouteRecordRaw[])
        }
        // RouteRecordRaw 는 여러 형태(컴포넌트형·리다이렉트형)의 합집합이라 부분 수정한
        // 객체가 어느 한쪽과도 정확히 겹치지 않는다. 모양은 원본 그대로 유지하므로 unknown 을 거친다
        return next as unknown as RouteRecordRaw
    })
}

/** 페이지가 useRoute 로 읽는 값 */
export interface RouteStub {
    /** 경로 파라미터 (예: 가이드 상세의 id) */
    params?: Record<string, string>
    /** 쿼리스트링 */
    query?: Record<string, string>
}

export interface MountPageResult {
    wrapper: VueWrapper
    router: Router
}

/**
 * 페이지 컴포넌트를 라우터·Pinia 와 함께 마운트한다.
 *
 * 마운트 후 두 번 tick 을 돌리는 이유: 페이지 대부분이 onMounted 에서 비동기 조회를 시작한다.
 * 한 번만 돌리면 로딩 상태에서 멈춰 데이터가 그려지기 전에 단언하게 된다.
 *
 * @param component 마운트할 페이지 컴포넌트
 * @param route     페이지가 읽을 라우트 파라미터·쿼리
 * @returns 마운트된 래퍼와 라우터
 */
export async function mountPage(
    component: Component,
    route: RouteStub = {},
): Promise<MountPageResult> {
    setActivePinia(createPinia())

    const router = createRouter({
        history: createMemoryHistory(),
        // 실제 라우트의 경로·이름을 그대로 쓰되 컴포넌트만 빈 것으로 바꾼다.
        //  - 경로·이름이 필요한 이유: 화면들이 RouterLink 에 이름(:to="{ name: 'admin-users' }")을
        //    쓰고 있어 catch-all 하나로는 해석되지 않는다. 이름이 바뀌면 여기서 드러난다.
        //  - 컴포넌트를 비우는 이유: 진짜 컴포넌트를 두면 이동 한 번에 그 화면의 모듈이 통째로
        //    딸려 오고, 그 화면의 api 는 이 테스트에서 목이 아니라서 실패한다.
        //    여기서 보고 싶은 것은 '어디로 갔는가'지 '거기가 잘 그려지는가'가 아니다.
        // 가드는 붙이지 않는다 — 인증 규칙은 guards.spec.ts 가 따로 검증한다
        routes: stubComponents([
            ...authRoutes,
            ...chatRoutes,
            ...guideRoutes,
            ...receiptRoutes,
            ...voiceRoutes,
            ...usageRoutes,
            ...adminRoutes,
            { path: '/:pathMatch(.*)*', name: 'not-found', component: { template: '<div />' } },
        ]),
    })

    await router.push({ path: '/test', query: route.query })
    await router.isReady()
    // useRoute().params 는 라우트 정의에서 오므로, 테스트가 원하는 값을 직접 얹는다
    if (route.params) {
        Object.assign(router.currentRoute.value.params, route.params)
    }

    const wrapper = mount(component, {
        global: {
            plugins: [router],
            stubs: {
                // Teleport 대상(body)은 jsdom 에 없어 경고만 남기므로 인라인으로 그린다
                teleport: true,
            },
        },
    })

    await flush()
    return { wrapper, router }
}

/**
 * 마이크로태스크 큐를 비운다 — onMounted 의 비동기 조회가 끝나 화면에 반영될 때까지.
 *
 * @param times 반복 횟수. 조회가 여러 단계로 이어지는 화면은 늘린다
 */
export async function flush(times = 3): Promise<void> {
    for (let i = 0; i < times; i++) {
        await nextTick()
        // 매크로태스크까지 한 번 넘긴다. 라우트 컴포넌트가 동적 import 라
        // 마이크로태스크만 비우면 화면 이동이 끝나기 전에 단언하게 된다
        await new Promise((resolve) => setTimeout(resolve, 0))
    }
}
