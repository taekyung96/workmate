package com.workmate.was.common.vo;

/**
 * 공통코드 등록/수정 요청 VO.
 * 등록 시 code 를 사용하고, 수정 시 code 는 경로에서 받으므로 본문 값은 무시된다.
 * 필수값(codeName) 검증은 서비스에서 수행한다. sortOrder 가 null 이면 0, useYn 이 null 이면 true 로 간주한다.
 */
public record CommonCodeSaveVo(
        String code,
        String codeName,
        Integer sortOrder,
        Boolean useYn) {

    /** null-safe 정렬순 (기본 0) */
    public int sortOrderOrDefault() {
        return sortOrder == null ? 0 : sortOrder;
    }

    /** null-safe 사용여부 (기본 true) */
    public boolean useYnOrDefault() {
        return useYn == null || useYn;
    }
}
