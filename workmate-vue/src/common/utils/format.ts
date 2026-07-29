/**
 * ISO 날짜 문자열을 'YYYY.MM.DD' 형식으로 표시한다.
 * @param iso 서버가 내려준 ISO 날짜 문자열
 * @returns 'YYYY.MM.DD' (파싱 실패 시 원본 반환)
 */
export function formatDate(iso: string): string {
    if (!iso) return ''
    const date = new Date(iso)
    if (Number.isNaN(date.getTime())) return iso
    const yyyy = date.getFullYear()
    const mm = String(date.getMonth() + 1).padStart(2, '0')
    const dd = String(date.getDate()).padStart(2, '0')
    return `${yyyy}.${mm}.${dd}`
}

/**
 * ISO 날짜 문자열을 'YYYY.MM.DD HH:mm' 형식(날짜+시각)으로 표시한다.
 * 감사 로그처럼 발생 '시각'이 의미 있는 곳에서 사용한다.
 * @param iso 서버가 내려준 ISO 날짜 문자열
 * @returns 'YYYY.MM.DD HH:mm' (파싱 실패 시 원본 반환)
 */
export function formatDateTime(iso: string): string {
    if (!iso) return ''
    const date = new Date(iso)
    if (Number.isNaN(date.getTime())) return iso
    const hh = String(date.getHours()).padStart(2, '0')
    const min = String(date.getMinutes()).padStart(2, '0')
    return `${formatDate(iso)} ${hh}:${min}`
}
