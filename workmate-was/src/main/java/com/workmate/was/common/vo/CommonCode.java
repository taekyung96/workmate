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

    /**
     * 그룹별 부가 속성 — 의미는 그룹마다 다르다.
     * AI_MODEL 에서는 LLM 제공자(google-genai | openai)를 담는다.
     * 다른 그룹은 쓰지 않으므로 null 이다.
     */
    @Column(name = "attr1", length = 100)
    private String attr1;

    @Builder
    public CommonCode(String groupCode, String code, String codeName, int sortOrder, boolean useYn,
                      String attr1) {
        this.groupCode = groupCode;
        this.code = code;
        this.codeName = codeName;
        this.sortOrder = sortOrder;
        this.useYn = useYn;
        this.attr1 = attr1;
    }

    /** 이름·정렬·사용여부 수정 (group_code·code 는 복합 PK 라 불변 — 값 변경은 삭제 후 재생성) */
    public void update(String codeName, int sortOrder, boolean useYn) {
        this.codeName = codeName;
        this.sortOrder = sortOrder;
        this.useYn = useYn;
    }
}
