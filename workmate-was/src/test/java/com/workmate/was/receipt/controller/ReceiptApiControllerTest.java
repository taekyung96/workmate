package com.workmate.was.receipt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workmate.was.ocr.service.OcrService;
import com.workmate.was.ocr.vo.OcrResultVo;
import com.workmate.was.receipt.dao.ReceiptRepository;
import com.workmate.was.receipt.service.ReceiptService;
import com.workmate.was.receipt.vo.Receipt;
import com.workmate.was.receipt.vo.ReceiptSaveRequestVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReceiptApiController.class)
class ReceiptApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReceiptService receiptService;

    @MockBean
    private OcrService ocrService; // 추가 의존성 주입 방지용 모킹

    @MockBean
    private ReceiptRepository receiptRepository; // JPA 리포지토리 모킹

    @Test
    @DisplayName("영수증 이미지 분석 API는 업로드 파일을 분석하여 성공 응답을 반환한다")
    void analyzeReceipt_success() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.png",
                MediaType.IMAGE_PNG_VALUE,
                "test image content".getBytes()
        );

        var mockResponse = com.workmate.was.receipt.vo.ReceiptAnalysisResponseVo.builder()
                .imagePath("uploads/test-uuid.png")
                .payAmount(15000)
                .bizNo("2208162517")
                .payDate("20260713")
                .cardName("롯데법인카드")
                .bizNoValid(true)
                .selectType("AUTO")
                .rawJson("[]")
                .items(List.of(new OcrResultVo("롯데법인카드", 15000, "20260713", "2208162517")))
                .build();

        given(receiptService.analyzeUploadedReceipt(any(), any())).willReturn(mockResponse);

        // when & then
        // 분석 API 도 사용량 기록 귀속을 위해 X-User-Seq 를 요구한다 (F-OBS)
        mockMvc.perform(multipart("/api/v1/receipts/analyze").file(file)
                        .header("X-User-Seq", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.payAmount").value(15000))
                .andExpect(jsonPath("$.result.bizNo").value("2208162517"))
                .andExpect(jsonPath("$.result.cardName").value("롯데법인카드"))
                .andExpect(jsonPath("$.result.selectType").value("AUTO"));
    }

    @Test
    @DisplayName("영수증 최종 저장 API는 유효한 요청에 대해 저장된 객체를 반환한다")
    void saveReceipt_success() throws Exception {
        // given
        ReceiptSaveRequestVo request = ReceiptSaveRequestVo.builder()
                .imagePath("uploads/test-uuid.png")
                .payAmount(15000)
                .bizNo("2208162517")
                .payDate("20260713")
                .cardName("롯데법인카드")
                .selectType("AUTO")
                .rawJson("[]")
                .build();

        Receipt mockSaved = Receipt.builder()
                .receiptSeq(1L)
                .userSeq(1L)
                .imagePath("uploads/test-uuid.png")
                .payAmount(15000)
                .bizNo("2208162517")
                .payDate("20260713")
                .cardName("롯데법인카드")
                .bizNoValid(true)
                .selectType("AUTO")
                .rawJson("[]")
                .createdAt(LocalDateTime.now())
                .build();

        given(receiptService.saveConfirmedReceipt(any(Long.class), any(ReceiptSaveRequestVo.class)))
                .willReturn(mockSaved);

        // when & then
        mockMvc.perform(post("/api/v1/receipts")
                        .header("X-User-Seq", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.receiptSeq").value(1))
                .andExpect(jsonPath("$.result.payAmount").value(15000))
                .andExpect(jsonPath("$.result.bizNo").value("2208162517"));
    }
}
