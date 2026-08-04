package com.workmate.web.receipt.controller;

import com.workmate.web.receipt.service.ReceiptService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 영수증 도메인 WEB 프록시 컨트롤러.
 * 화면(fetch)이 호출하는 /api/v1/receipts/** 요청을 받아 WAS 로 중계한다.
 * (3-tier 원칙: 브라우저는 WEB(8080)만 바라보고, WAS(8081)는 WEB 뒤에 숨긴다)
 *
 * WAS 연결 실패 등 공통 예외는 GlobalExceptionHandler 에서 일괄 처리하므로
 * 여기서는 개별 try-catch 없이 중계 로직만 담는다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    /**
     * 영수증 이미지 분석 요청 중계.
     *
     * @param file 업로드된 영수증 이미지
     * @return WAS 분석 응답 JSON
     * @throws IOException 파일 스트림 처리 실패 시
     */
    @PostMapping("/analyze")
    public ResponseEntity<String> analyze(@RequestParam("file") MultipartFile file) throws IOException {
        return jsonPassthrough(receiptService.analyze(file));
    }

    /**
     * 영수증 최종 저장 요청 중계.
     * (사용자 식별은 RestClient 인터셉터가 세션 LoginUser에서 X-User-Seq 헤더로 주입한다)
     *
     * @param requestBody 저장 요청 JSON 원문
     * @return WAS 저장 응답 JSON
     */
    @PostMapping
    public ResponseEntity<String> save(@RequestBody String requestBody) {
        return jsonPassthrough(receiptService.save(requestBody));
    }

    /**
     * 영수증 등록 이력 조회 요청 중계.
     * (사용자 식별은 인터셉터가 세션에서 X-User-Seq 헤더로 주입한다)
     *
     * @return WAS 이력 응답 JSON
     */
    @GetMapping
    public ResponseEntity<String> getHistory() {
        return jsonPassthrough(receiptService.getHistory());
    }

    /**
     * 영수증 이력 CSV 다운로드 요청 중계.
     * (사용자 식별은 인터셉터가 세션에서 X-User-Seq 헤더로 주입한다)
     *
     * @return CSV 파일 응답
     */
    @GetMapping("/csv")
    public ResponseEntity<byte[]> downloadCsv() {
        return receiptService.downloadCsv();
    }

    /** WAS 응답의 상태코드·본문을 유지하며 JSON 컨텐츠 타입으로 화면에 전달한다. */
    private ResponseEntity<String> jsonPassthrough(ResponseEntity<String> wasResponse) {
        return ResponseEntity.status(wasResponse.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(wasResponse.getBody());
    }
}
