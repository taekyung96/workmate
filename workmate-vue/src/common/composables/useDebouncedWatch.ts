import { watch, type Ref } from 'vue'

/**
 * source 값이 바뀌면 delay(ms) 뒤 콜백을 실행한다. 값이 계속 바뀌면(연타) 직전 타이머를 취소해,
 * 마지막 변경 후 delay 만큼 잠잠할 때 한 번만 실행한다(디바운스).
 * 검색어 입력 자동조회처럼 "입력이 멈춘 뒤 한 번만" 실행하고 싶을 때 쓰는 공통 부품.
 *
 * @param source   감시할 반응형 값 (예: 검색어 ref)
 * @param callback 디바운스 후 실행할 함수 (예: 첫 페이지부터 재검색)
 * @param delay    지연(ms), 기본 300
 */
export function useDebouncedWatch<T>(source: Ref<T>, callback: () => void, delay = 300): void {
    let timer: ReturnType<typeof setTimeout> | undefined
    watch(source, () => {
        clearTimeout(timer)
        timer = setTimeout(callback, delay)
    })
}
