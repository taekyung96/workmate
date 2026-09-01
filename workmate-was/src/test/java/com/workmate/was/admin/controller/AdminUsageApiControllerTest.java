package com.workmate.was.admin.controller;

import com.workmate.was.global.exception.GlobalExceptionHandler;
import com.workmate.was.usage.service.LlmUsageQueryService;
import com.workmate.was.usage.vo.UsagePeriodVo;
import com.workmate.was.usage.vo.UsageSummaryVo;
import com.workmate.was.usage.vo.UsageTotalVo;
import com.workmate.was.usage.vo.UserUsagePageVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminUsageApiController 단위 테스트 — 응답 형태와 기본 기간(from·to 미지정 → null 전달, 실제 30일
 * 계산은 서비스 책임) 위임을 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class AdminUsageApiControllerTest {

    @Mock
    private LlmUsageQueryService llmUsageQueryService;

    @InjectMocks
    private AdminUsageApiController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // standaloneSetup 은 Spring Boot 가 자동 구성한 ObjectMapper 를 쓰지 않는다.
        // 기본 ObjectMapper 는 LocalDate 를 [2026,8,2] 배열로 직렬화하지만, 실제 앱은 Boot 설정에 따라
        // "2026-08-02" 문자열로 내보낸다(프론트 타입도 string). 운영과 같은 직렬화를 쓰도록 맞춘다.
        MappingJackson2HttpMessageConverter jsonConverter =
                new MappingJackson2HttpMessageConverter(Jackson2ObjectMapperBuilder.json()
                        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .build());

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(jsonConverter)
                .build();
    }

    @Test
    @DisplayName("summary: from·to 미지정 시 서비스에 null 을 그대로 넘긴다(기본 기간 계산은 서비스 책임)")
    void getSummary_without_range_delegates_null_to_service() throws Exception {
        when(llmUsageQueryService.getSummary(isNull(), isNull())).thenReturn(sampleSummary());

        mockMvc.perform(get("/api/v1/admin/usage/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.period.from").value("2026-08-02"))
                .andExpect(jsonPath("$.result.total.callCount").value(32))
                .andExpect(jsonPath("$.result.total.untrackedCallCount").value(30));

        verify(llmUsageQueryService).getSummary(isNull(), isNull());
    }

    @Test
    @DisplayName("summary: from·to 지정 시 파싱된 LocalDate 를 서비스에 그대로 넘긴다")
    void getSummary_with_range_passes_parsed_dates() throws Exception {
        when(llmUsageQueryService.getSummary(any(), any())).thenReturn(sampleSummary());

        mockMvc.perform(get("/api/v1/admin/usage/summary")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31"))
                .andExpect(status().isOk());

        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(llmUsageQueryService).getSummary(fromCaptor.capture(), toCaptor.capture());
        assertThat(fromCaptor.getValue()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(toCaptor.getValue()).isEqualTo(LocalDate.of(2026, 8, 31));
    }

    @Test
    @DisplayName("by-user: page·size 기본값은 0·20 이고, 응답은 페이징 메타를 포함한다")
    void getByUser_defaults_page_and_size() throws Exception {
        when(llmUsageQueryService.getByUser(isNull(), isNull(), eq(0), eq(20)))
                .thenReturn(UserUsagePageVo.builder()
                        .content(List.of())
                        .page(0)
                        .totalPages(0)
                        .totalElements(0)
                        .build());

        mockMvc.perform(get("/api/v1/admin/usage/by-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.content").isArray())
                .andExpect(jsonPath("$.result.totalElements").value(0));

        verify(llmUsageQueryService).getByUser(isNull(), isNull(), eq(0), eq(20));
    }

    private UsageSummaryVo sampleSummary() {
        return UsageSummaryVo.builder()
                .period(new UsagePeriodVo(LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 31)))
                .total(UsageTotalVo.builder()
                        .callCount(32)
                        .inputTokens(1770)
                        .outputTokens(92)
                        .untrackedCallCount(30)
                        .unpricedCallCount(0)
                        .estimatedCostUsd(BigDecimal.ZERO)
                        .estimatedCostKrw(BigDecimal.ZERO)
                        .build())
                .byFeature(List.of())
                .daily(List.of())
                .build();
    }
}
