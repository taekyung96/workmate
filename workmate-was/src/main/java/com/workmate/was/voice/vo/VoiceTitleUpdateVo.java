package com.workmate.was.voice.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회의록 제목 수정 요청 VO (F8-1).
 * 분석이 끝난 뒤 결과 화면에서 사용자가 입력한 회의 제목을 저장할 때 쓴다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceTitleUpdateVo {

    /** 사용자가 입력한 회의 제목 */
    @NotBlank(message = "회의 제목을 입력해주세요.")
    @Size(max = 255, message = "회의 제목은 255자 이하여야 합니다.")
    private String title;
}
