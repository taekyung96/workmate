<script setup lang="ts">
/**
 * 공통 앱 사이드바 쉘 (모든 앱 화면 공유) — Gemini 스타일.
 * 상단: 로고·새 채팅 / 메뉴 / 최근 채팅 목록 / 하단: 사용자 계정 메뉴(내 사용량·로그아웃).
 *
 * 참고(모듈 경계): "최근 채팅"은 chat 데이터라 chat.store를 읽는다.
 * 채팅이 주인공인 제품의 쉘이라 상시 노출하는 의도된 결합이다(데이터 소유는 chat 모듈).
 */
import { ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import {
    BarChart3,
    BookText,
    ChevronsUpDown,
    LogOut,
    MessageSquare,
    Mic,
    Receipt,
    Settings,
    SquarePen,
    Trash2,
} from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/common/components/ui/dropdown-menu'
import ConfirmDialog from '@/common/components/ConfirmDialog.vue'
import BrandMark from '@/common/components/BrandMark.vue'
import { useAuthStore } from '@/modules/auth/stores/auth.store'
import { useAuth } from '@/modules/auth/composables/useAuth'
import { useChatStore } from '@/modules/chat/stores/chat.store'
import type { ChatRoom } from '@/modules/chat/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { logout } = useAuth()
const chat = useChatStore()

// 인증 상태를 감시해 방 목록을 불러온다.
// onMounted 1회로는 새로고침 시 사이드바가 세션 복원(가드의 /me)보다 먼저 마운트돼
// isAuthenticated=false 인 순간을 놓쳐 목록이 비는 문제가 있다. 세션이 뒤늦게 복원돼
// 인증 상태가 되는 순간에도 로드되도록 watch(immediate)로 처리한다.
watch(
    () => auth.isAuthenticated,
    (authed) => {
        // 인증 상태에서만 호출 (로그아웃 상태의 불필요한 401 방지)
        if (authed && !chat.roomsLoaded) chat.loadRooms()
    },
    { immediate: true },
)

function newChat(): void {
    chat.startNewChat()
    if (route.name !== 'chat') router.push({ name: 'chat' })
}

function openRoom(roomSeq: number): void {
    chat.selectRoom(roomSeq)
    if (route.name !== 'chat') router.push({ name: 'chat' })
}

// 삭제 확인창 — 열림 상태와 대상 방을 분리 보관(공용 ConfirmDialog 하나로 목록 전체를 제어)
const deleteOpen = ref(false)
const deleteTarget = ref<ChatRoom | null>(null)

/** 특정 방의 삭제 확인창 열기 */
function askDeleteRoom(room: ChatRoom): void {
    deleteTarget.value = room
    deleteOpen.value = true
}

/** 확인창에서 삭제 확정 — store 액션은 예외를 던질 수 있어 결과를 toast 로 알린다 */
async function confirmDeleteRoom(): Promise<void> {
    const target = deleteTarget.value
    if (!target) return
    try {
        await chat.deleteRoom(target.roomSeq)
        toast.success('채팅을 삭제했습니다.')
    } catch {
        toast.error('채팅 삭제에 실패했습니다.')
    } finally {
        deleteTarget.value = null
    }
}
</script>

<template>
    <aside class="flex h-screen w-64 shrink-0 flex-col border-r bg-muted/30">
        <!-- 로고 + 새 채팅 -->
        <div class="p-3">
            <div class="mb-3 flex items-center gap-2 px-2">
                <!-- 작게 들어가므로 반짝임은 빼고 W 만 남긴다 -->
                <BrandMark :sparkle="false" class="size-6 shrink-0" />
                <span class="text-lg font-semibold">Workmate</span>
            </div>
            <Button variant="outline" class="w-full justify-start gap-2" @click="newChat">
                <SquarePen class="size-4" />
                새 채팅
            </Button>
        </div>

        <!-- 메뉴 -->
        <nav class="flex flex-col gap-1 px-3">
            <RouterLink
                :to="{ name: 'chat' }"
                class="flex items-center gap-2 rounded-md px-2 py-1.5 text-sm text-muted-foreground hover:bg-accent hover:text-foreground"
                :class="{ 'bg-accent font-medium text-foreground': route.name === 'chat' }"
            >
                <MessageSquare class="size-4" />
                채팅
            </RouterLink>
            <RouterLink
                :to="{ name: 'receipt' }"
                class="flex items-center gap-2 rounded-md px-2 py-1.5 text-sm text-muted-foreground hover:bg-accent hover:text-foreground"
                :class="{
                    'bg-accent font-medium text-foreground': route.path.startsWith('/receipt'),
                }"
            >
                <Receipt class="size-4" />
                영수증
            </RouterLink>
            <RouterLink
                :to="{ name: 'guide-list' }"
                class="flex items-center gap-2 rounded-md px-2 py-1.5 text-sm text-muted-foreground hover:bg-accent hover:text-foreground"
                :class="{
                    'bg-accent font-medium text-foreground': route.path.startsWith('/guide'),
                }"
            >
                <BookText class="size-4" />
                가이드
            </RouterLink>
            <RouterLink
                :to="{ name: 'voice' }"
                class="flex items-center gap-2 rounded-md px-2 py-1.5 text-sm text-muted-foreground hover:bg-accent hover:text-foreground"
                :class="{
                    'bg-accent font-medium text-foreground': route.path.startsWith('/voice'),
                }"
            >
                <Mic class="size-4" />
                회의록
            </RouterLink>
            <!-- 관리자 메뉴 — ROLE_ADMIN에게만 노출 (F6-04). 서버 접근 차단은 별도 -->
            <RouterLink
                v-if="auth.isAdmin"
                :to="{ name: 'admin-users' }"
                class="flex items-center gap-2 rounded-md px-2 py-1.5 text-sm text-muted-foreground hover:bg-accent hover:text-foreground"
                :class="{
                    'bg-accent font-medium text-foreground': route.path.startsWith('/admin'),
                }"
            >
                <Settings class="size-4" />
                관리자
            </RouterLink>
        </nav>

        <!-- 최근 채팅 -->
        <div class="mt-4 min-h-0 flex-1 overflow-y-auto slim-scroll px-3">
            <p class="px-2 py-1 text-xs text-muted-foreground">최근</p>
            <ul class="flex flex-col gap-0.5">
                <li
                    v-for="room in chat.rooms"
                    :key="room.roomSeq"
                    class="group flex items-center rounded-md hover:bg-accent"
                    :class="{ 'bg-accent': room.roomSeq === chat.currentRoomSeq }"
                >
                    <button
                        class="min-w-0 flex-1 truncate px-2 py-1.5 text-left text-sm"
                        @click="openRoom(room.roomSeq)"
                    >
                        {{ room.title }}
                    </button>
                    <button
                        class="mr-1 hidden shrink-0 rounded p-1 text-muted-foreground hover:text-destructive group-hover:block"
                        title="삭제"
                        @click="askDeleteRoom(room)"
                    >
                        <Trash2 class="size-3.5" />
                    </button>
                </li>
            </ul>
        </div>

        <!-- 채팅 삭제 확인 (목록 공용 하나로 제어) -->
        <ConfirmDialog
            v-model:open="deleteOpen"
            title="채팅을 삭제할까요?"
            :description="`&quot;${deleteTarget?.title}&quot; 대화가 삭제됩니다.`"
            @confirm="confirmDeleteRoom"
        />

        <!--
            하단 사용자 — 이름을 누르면 계정 메뉴가 열린다(흔한 사용자 메뉴 패턴).
            사이드바 메뉴를 늘리지 않으면서 계정 관련 항목을 한곳에 모은다.
        -->
        <div class="border-t p-3">
            <DropdownMenu>
                <DropdownMenuTrigger as-child>
                    <button
                        class="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-left hover:bg-accent"
                        :class="{ 'bg-accent': route.path.startsWith('/usage') }"
                    >
                        <span class="min-w-0 flex-1">
                            <span class="block truncate text-sm font-medium">
                                {{ auth.user?.userName }}
                            </span>
                            <span class="block text-xs text-muted-foreground">
                                {{ auth.isAdmin ? '관리자' : '사용자' }}
                            </span>
                        </span>
                        <ChevronsUpDown class="size-4 shrink-0 text-muted-foreground" />
                    </button>
                </DropdownMenuTrigger>
                <!-- 사이드바가 화면 왼쪽 끝이라 위쪽으로 펼친다 -->
                <DropdownMenuContent align="start" side="top" class="w-56">
                    <DropdownMenuLabel class="font-normal">
                        <span class="block truncate text-sm font-medium">
                            {{ auth.user?.userName }}
                        </span>
                        <span class="block text-xs text-muted-foreground">
                            {{ auth.isAdmin ? '관리자' : '사용자' }}
                        </span>
                    </DropdownMenuLabel>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem @select="router.push({ name: 'my-usage' })">
                        <BarChart3 class="size-4" />
                        내 사용량
                    </DropdownMenuItem>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem variant="destructive" @select="logout">
                        <LogOut class="size-4" />
                        로그아웃
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>
        </div>
    </aside>
</template>
