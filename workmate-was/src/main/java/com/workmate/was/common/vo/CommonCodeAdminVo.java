package com.workmate.was.common.vo;

/**
 * 공통코드 관리 응답 VO — 정렬·사용여부 포함(관리자용). 조회용 CommonCodeVo 와 달리 비활성 코드도 노출.
 */
public record CommonCodeAdminVo(String code, String codeName, int sortOrder, boolean useYn) {

    public static CommonCodeAdminVo from(CommonCode c) {
        return new CommonCodeAdminVo(c.getCode(), c.getCodeName(), c.getSortOrder(), c.isUseYn());
    }
}
