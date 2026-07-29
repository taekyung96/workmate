package com.workmate.was.common.vo;

/**
 * 공통코드 그룹 관리 응답 VO — 비활성(use_yn=false) 포함.
 */
public record CommonCodeGroupVo(String groupCode, String groupName, String description, boolean useYn) {

    public static CommonCodeGroupVo from(CommonCodeGroup g) {
        return new CommonCodeGroupVo(g.getGroupCode(), g.getGroupName(), g.getDescription(), g.isUseYn());
    }
}
