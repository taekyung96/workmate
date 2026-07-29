package com.workmate.was.common.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공통코드 그룹 Entity (common_code_group, 04 §4.5). group_code 단일 PK.
 */
@Entity
@Table(name = "common_code_group")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommonCodeGroup {

    @Id
    @Column(name = "group_code", length = 30)
    private String groupCode;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "use_yn", nullable = false)
    private boolean useYn;

    @Builder
    public CommonCodeGroup(String groupCode, String groupName, String description, boolean useYn) {
        this.groupCode = groupCode;
        this.groupName = groupName;
        this.description = description;
        this.useYn = useYn;
    }

    /** 그룹 속성 수정 (group_code 는 PK 라 불변) */
    public void update(String groupName, String description, boolean useYn) {
        this.groupName = groupName;
        this.description = description;
        this.useYn = useYn;
    }
}
