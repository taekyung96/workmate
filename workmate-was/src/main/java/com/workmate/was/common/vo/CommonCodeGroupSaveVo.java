package com.workmate.was.common.vo;

/**
 * 공통코드 그룹 등록/수정 요청 VO.
 * 등록 시 groupCode 를 사용하고, 수정 시 groupCode 는 경로에서 받으므로 본문 값은 무시된다.
 * 필수값(groupName) 검증은 서비스에서 수행한다. useYn 이 null 이면 true 로 간주한다.
 */
public record CommonCodeGroupSaveVo(
        String groupCode,
        String groupName,
        String description,
        Boolean useYn) {

    /** null-safe 사용여부 (기본 true) */
    public boolean useYnOrDefault() {
        return useYn == null || useYn;
    }
}
