<script setup lang="ts">
/**
 * 가로로 스크롤되는 표 껍데기.
 *
 * <p><b>왜 필요한가.</b> 표를 <code>overflow-x-auto</code> 안에 넣어도, 표가
 * <code>w-full</code>(=100%)이면 컨테이너보다 커질 수 없어 <b>스크롤이 생길 수 없다</b>.
 * 넘칠 수 없으니 글자를 줄바꿈해 밀어 넣는 수밖에 없고, 393px 화면에서 감사 로그 한 줄이
 * 121px, 영수증 이력 한 줄이 281px 를 차지했다(자연 크기라면 각각 43px).</p>
 *
 * <p>그래서 이 컴포넌트를 쓰는 표에는 <code>min-w-[...]</code> 를 함께 준다.
 * 넓은 화면에서는 <code>w-full</code> 로 채우고, 좁아지면 최소 폭에서 멈춰 여기서 스크롤한다.
 * <b>표를 줄이지 않고 화면을 미는 방식</b>이다.</p>
 *
 * <p><b>스크롤 힌트를 두는 이유.</b> 가로 스크롤은 잘린 화면과 구분이 안 된다. 오른쪽에
 * 옅은 그림자를 깔아 "더 있다"를 알린다. 끝까지 밀면 사라지고, 스크롤할 게 없으면 처음부터
 * 뜨지 않는다 — 항상 떠 있으면 그것대로 오해를 만든다.</p>
 *
 * <p>세로 스크롤은 페이지가 맡는다. 여기서 잡는 것은 가로뿐이다.</p>
 *
 * @example
 * <TableScroller>
 *     <table class="w-full min-w-[520px] text-sm">…</table>
 * </TableScroller>
 */
import { onBeforeUnmount, onMounted, ref, useTemplateRef } from 'vue'

const scroller = useTemplateRef<HTMLElement>('scroller')

/** 오른쪽에 더 볼 내용이 남아 있는가 (그림자 표시 여부) */
const hasMoreRight = ref(false)

/**
 * 스크롤 여지가 남았는지 다시 계산한다.
 *
 * 오차 1px 을 두는 이유: 브라우저가 스크롤 위치를 소수로 잡아, 끝까지 밀어도
 * scrollLeft 가 정수 경계에 딱 떨어지지 않는 경우가 있다. 그대로 비교하면
 * 끝에서도 그림자가 남는다.
 */
function update(): void {
    const el = scroller.value
    if (!el) return
    hasMoreRight.value = el.scrollWidth - el.clientWidth - el.scrollLeft > 1
}

/**
 * 내용이 바뀌면 다시 잰다 — 목록을 불러오기 전(빈 표)과 후는 폭이 다르다.
 * 창 크기 변경도 같은 이유로 본다.
 */
let observer: ResizeObserver | null = null

onMounted(() => {
    update()
    const el = scroller.value
    if (!el) return

    // ResizeObserver 가 없는 환경(jsdom)에서는 건너뛴다.
    // 힌트는 어디까지나 덤이고, 스크롤 자체는 CSS 가 한다. 여기서 던지면 이 컴포넌트를
    // 쓰는 화면의 유닛 테스트가 통째로 깨진다 — 실제로 6개 화면 21건이 그렇게 깨졌다.
    // @scroll 핸들러는 그대로 도므로 사용자가 미는 동안의 갱신은 유지된다.
    if (typeof ResizeObserver === 'undefined') return

    observer = new ResizeObserver(update)
    observer.observe(el)
    // 표 자체가 커지는 경우(행 추가)도 잡아야 한다
    if (el.firstElementChild) observer.observe(el.firstElementChild)
})

onBeforeUnmount(() => {
    observer?.disconnect()
    observer = null
})
</script>

<template>
    <div class="relative">
        <div ref="scroller" class="overflow-x-auto rounded-lg border" @scroll="update">
            <slot />
        </div>

        <!--
            스크롤 힌트 — 오른쪽 가장자리의 옅은 그림자.
            pointer-events-none 이 없으면 이 막이 표의 클릭을 가로챈다.
            rounded 는 아래 테두리와 모서리를 맞추기 위한 것이다.
        -->
        <div
            v-if="hasMoreRight"
            class="pointer-events-none absolute inset-y-px right-px w-8 rounded-r-lg bg-gradient-to-l from-background to-transparent"
            aria-hidden="true"
        />
    </div>
</template>
