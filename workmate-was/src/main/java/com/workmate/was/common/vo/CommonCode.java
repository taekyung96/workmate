package com.workmate.was.common.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공통코드 Entity (common_code, 04 §4.5). 그룹코드+코드 복합 PK.
 */
@Entity
@Table(name = "common_code")
@IdClass(CommonCodeId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommonCode {

    @Id
    @Column(name = "group_code", length = 30)
    private String groupCode;

    @Id
    @Column(name = "code", length = 50)
    private String code;

    @Column(name = "code_name", nullable = false, length = 100)
    private String codeName;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "use_yn", nullable = false)
    private boolean useYn;

    @Builder
    public CommonCode(String groupCode, String code, String codeName, int sortOrder, boolean useYn) {
        this.groupCode = groupCode;
        this.code = code;
        this.codeName = codeName;
        this.sortOrder = sortOrder;
        this.useYn = useYn;
    }

    /** 이름·정렬·사용여부 수정 (group_code·code 는 복합 PK 라 불변 — 값 변경은 삭제 후 재생성) */
    public void update(String codeName, int sortOrder, boolean useYn) {
        this.codeName = codeName;
        this.sortOrder = sortOrder;
        this.useYn = useYn;
    }
}
