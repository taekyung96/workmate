<script setup lang="ts">
/**
 * 공통 앱 사이드바 쉘 (모든 앱 화면 공유) — Gemini 스타일.
 * 상단: 로고·새 채팅 / 메뉴 / 최근 채팅 목록 / 하단: 사용자·로그아웃.
 *
 * 참고(모듈 경계): "최근 채팅"은 chat 데이터라 chat.store를 읽는다.
 * 채팅이 주인공인 제품의 쉘이라 상시 노출하는 의도된 결합이다(데이터 소유는 chat 모듈).
 */
import { onMounted } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
    BookText,
    LogOut,
    MessageSquare,
    Receipt,
    Settings,
    SquarePen,
    Trash2,
} from 'lucide-vue-next'
import { Button } from '@/common/components/ui/button'
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
    AlertDialogTrigger,
} from '@/common/components/ui/alert-dialog'
import { useAuthStore } from '@/modules/auth/stores/auth.store'
import { useAuth } from '@/modules/auth/composables/useAuth'
import { useChatStore } from '@/modules/chat/stores/chat.store'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { logout } = useAuth()
const chat = useChatStore()

onMounted(() => {
    // 인증 상태에서만 방 목록을 불러온다 (로그아웃 상태의 불필요한 401 방지)
    if (auth.isAuthenticated && !chat.roomsLoaded) chat.loadRooms()
})

function newChat(): void {
    chat.startNewChat()
    if (route.name !== 'chat') router.push({ name: 'chat' })
}

function openRoom(roomSeq: number): void {
    chat.selectRoom(roomSeq)
    if (route.name !== 'chat') router.push({ name: 'chat' })
}
</script>

<template>
    <aside class="flex h-screen w-64 shrink-0 flex-col border-r bg-muted/30">
        <!-- 로고 + 새 채팅 -->
        <div class="p-3">
            <div class="mb-3 px-2 text-lg font-semibold">Workmate</div>
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
        <div class="mt-4 min-h-0 flex-1 overflow-y-auto px-3">
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
                    <AlertDialog>
                        <AlertDialogTrigger as-child>
                            <button
                                class="mr-1 hidden shrink-0 rounded p-1 text-muted-foreground hover:text-destructive group-hover:block"
                                title="삭제"
                            >
                                <Trash2 class="size-3.5" />
                            </button>
                        </AlertDialogTrigger>
                        <AlertDialogContent>
                            <AlertDialogHeader>
                                <AlertDialogTitle>채팅을 삭제할까요?</AlertDialogTitle>
                                <AlertDialogDescription>
                                    "{{ room.title }}" 대화가 삭제됩니다.
                                </AlertDialogDescription>
                            </AlertDialogHeader>
                            <AlertDialogFooter>
                                <AlertDialogCancel>취소</AlertDialogCancel>
                                <AlertDialogAction @click="chat.deleteRoom(room.roomSeq)">
                                    삭제
                                </AlertDialogAction>
                            </AlertDialogFooter>
                        </AlertDialogContent>
                    </AlertDialog>
                </li>
            </ul>
        </div>

        <!-- 하단 사용자 -->
        <div class="flex items-center justify-between border-t p-3">
            <div class="min-w-0">
                <p class="truncate text-sm font-medium">{{ auth.user?.userName }}</p>
                <p class="text-xs text-muted-foreground">
                    {{ auth.isAdmin ? '관리자' : '사용자' }}
                </p>
            </div>
            <Button variant="ghost" size="icon" title="로그아웃" @click="logout">
                <LogOut class="size-4" />
            </Button>
        </div>
    </aside>
</template>
