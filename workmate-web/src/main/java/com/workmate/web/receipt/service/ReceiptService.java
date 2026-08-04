package com.workmate.web.receipt.service;

import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 * 영수증 도메인 WEB 프록시 서비스 인터페이스.
 * 화면(fetch)의 요청을 WAS REST API 로 중계한다.
 */
public interface ReceiptService {

    /**
     * 영수증 이미지 분석 요청을 WAS 로 중계한다.
     *
     * @param file 업로드된 영수증 이미지
     * @return WAS 분석 응답 (ApiResponse JSON 원문)
     * @throws IOException 파일 스트림 처리 실패 시
     */
    ResponseEntity<String> analyze(MultipartFile file) throws IOException;

    /**
     * 영수증 최종 저장 요청을 WAS 로 중계한다.
     * (사용자 식별은 RestClient 인터셉터가 세션에서 X-User-Seq 헤더로 주입한다)
     *
     * @param requestBody 저장 요청 JSON 원문
     * @return WAS 저장 응답 (ApiResponse JSON 원문)
     */
    ResponseEntity<String> save(String requestBody);

    /**
     * 영수증 등록 이력 조회 요청을 WAS 로 중계한다. page·size 가 있으면 함께 전달(페이징).
     * (사용자 식별은 인터셉터가 세션에서 X-User-Seq 헤더로 주입한다)
     *
     * @param page 0-based 페이지 (선택, null 이면 미전달)
     * @param size 페이지 크기 (선택, null 이면 미전달)
     * @return WAS 이력 응답 (ApiResponse JSON 원문)
     */
    ResponseEntity<String> getHistory(Integer page, Integer size);

    /**
     * 영수증 이력 CSV 다운로드 요청을 WAS 로 중계한다.
     * (사용자 식별은 인터셉터가 세션에서 X-User-Seq 헤더로 주입한다)
     *
     * @return CSV 파일 바이트 응답 (Content-Disposition 헤더 포함)
     */
    ResponseEntity<byte[]> downloadCsv();
}
