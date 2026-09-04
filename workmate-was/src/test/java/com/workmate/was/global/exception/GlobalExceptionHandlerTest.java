package com.workmate.was.global.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 전역 예외 핸들러가 예외 타입별로 상태코드와 공통 포맷을 반환하는지 검증.
 */
@WebMvcTest(ExceptionTestController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("BusinessException 은 409와 에러 메시지를 반환한다")
    void businessException_returns409() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("비즈니스 규칙 위반"));
    }

    @Test
    @DisplayName("IllegalArgumentException 은 400과 에러 메시지를 반환한다")
    void illegalArgument_returns400() throws Exception {
        mockMvc.perform(get("/test/illegal"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("입력값 오류"));
    }

    @Test
    @DisplayName("없는 경로는 500이 아니라 404를 반환한다")
    void unknownPath_returns404() throws Exception {
        // 캐치올(Exception)이 NoResourceFoundException 을 삼켜 500 을 내던 결함을 지킨다.
        // 없는 주소를 부른 것은 요청 잘못이지 서버 잘못이 아니다.
        mockMvc.perform(get("/이런경로는없다"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("요청한 경로를 찾을 수 없습니다."));
    }
}
