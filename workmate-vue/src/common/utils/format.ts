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
 * 사업자등록번호를 'XXX-XX-XXXXX'(3-2-5) 형식으로 표시한다.
 * 완전한 10자리가 아니어도 자리수에 맞춰 부분 포맷하므로 입력 중 실시간 표시에도 쓸 수 있다.
 * @param bizNo 숫자·하이픈이 섞인 사업자번호 문자열
 * @returns 하이픈이 들어간 문자열 (숫자가 없으면 빈 문자열)
 */
export function formatBizNo(bizNo: string | null | undefined): string {
    // 숫자만 추려 최대 10자리로 자른 뒤 자리수에 맞춰 하이픈을 넣는다
    const digits = (bizNo ?? '').replace(/\D/g, '').slice(0, 10)
    if (digits.length <= 3) return digits
    if (digits.length <= 5) return `${digits.slice(0, 3)}-${digits.slice(3)}`
    return `${digits.slice(0, 3)}-${digits.slice(3, 5)}-${digits.slice(5)}`
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
