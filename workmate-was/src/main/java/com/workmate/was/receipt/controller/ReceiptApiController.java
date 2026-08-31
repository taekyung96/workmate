package com.workmate.was.receipt.controller;

import com.workmate.was.global.response.ApiResponse;
import com.workmate.was.receipt.service.ReceiptService;
import com.workmate.was.receipt.vo.Receipt;
import com.workmate.was.receipt.vo.ReceiptAnalysisResponseVo;
import com.workmate.was.receipt.vo.ReceiptPageVo;
import com.workmate.was.receipt.vo.ReceiptSaveRequestVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 영수증 처리 관련 REST API 컨트롤러.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/receipts")
@RequiredArgsConstructor
public class ReceiptApiController {

    private final ReceiptService receiptService;

    /**
     * 업로드된 영수증 이미지를 분석하여 데이터 후보군 및 도메인 검증 결과를 반환한다.
     *
     * @param userSeq 인증 세션에서 WEB 이 주입한 사용자 식별자 (X-User-Seq 헤더) — 사용량 기록 귀속 대상
     * @param file 영수증 이미지 파일
     * @return 분석 결과 DTO 공통 응답
     * @throws IOException 파일 처리 중 에러
     */
    @PostMapping("/analyze")
    public ApiResponse<ReceiptAnalysisResponseVo> analyzeReceipt(
            @RequestHeader("X-User-Seq") Long userSeq,
            @RequestParam("file") MultipartFile file) throws IOException {
        log.info("영수증 이미지 분석 API 호출. userSeq: {}, 파일명: {}", userSeq, file.getOriginalFilename());
        ReceiptAnalysisResponseVo response = receiptService.analyzeUploadedReceipt(userSeq, file);
        return ApiResponse.success(response);
    }

    /**
     * 사용자가 확인하고 수정한 영수증 최종 데이터를 데이터베이스에 영구 저장한다.
     *
     * @param userSeq 인증 세션에서 WEB 인터셉터가 주입한 사용자 식별자 (X-User-Seq 헤더)
     * @param request 최종 저장 요청 DTO
     * @return 저장 성공 응답
     */
    @PostMapping
    public ApiResponse<Receipt> saveReceipt(
            @RequestHeader("X-User-Seq") Long userSeq,
            @Valid @RequestBody ReceiptSaveRequestVo request) {
        log.info("영수증 최종 저장 API 호출. UserSeq: {}", userSeq);
        Receipt saved = receiptService.saveConfirmedReceipt(userSeq, request);
        return ApiResponse.success(saved);
    }

    /**
     * 특정 사용자의 영수증 등록 목록을 최신순으로 조회한다.
     * page·size 가 오면 그 값으로 페이징하고, 없으면 전체를 한 페이지로 반환한다.
     *
     * @param userSeq 사용자 식별자
     * @param page    0-based 페이지 (선택)
     * @param size    페이지 크기 (선택)
     * @return 영수증 이력 페이지 공통 응답
     */
    @GetMapping
    public ApiResponse<ReceiptPageVo> getReceiptHistory(
            @RequestHeader("X-User-Seq") Long userSeq,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        log.info("영수증 이력 조회 API 호출. UserSeq: {}, page: {}, size: {}", userSeq, page, size);
        return ApiResponse.success(receiptService.getReceiptHistoryPage(userSeq, page, size));
    }

    /**
     * 영수증 등록 목록 데이터를 CSV 파일로 내보낸다.
     *
     * @param userSeq 사용자 식별자
     * @return CSV 파일 응답
     */
    @GetMapping("/csv")
    public ResponseEntity<byte[]> downloadReceiptCsv(
            @RequestHeader("X-User-Seq") Long userSeq) {
        log.info("영수증 이력 CSV 다운로드 API 호출. UserSeq: {}", userSeq);
        byte[] csvData = receiptService.exportReceiptHistoryToCsv(userSeq);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipts.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}
