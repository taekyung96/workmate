package com.workmate.was.receipt.service.impl;

import com.workmate.was.ocr.service.OcrService;
import com.workmate.was.ocr.vo.OcrResultVo;
import com.workmate.was.receipt.dao.ReceiptRepository;
import com.workmate.was.receipt.service.ReceiptService;
import com.workmate.was.receipt.vo.Receipt;
import com.workmate.was.receipt.vo.ReceiptAnalysisResponseVo;
import com.workmate.was.receipt.vo.ReceiptPageVo;
import com.workmate.was.receipt.vo.ReceiptSaveRequestVo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 영수증 비즈니스 로직 구현체.
 * 영수증 업로드, AI 분석 중계, 비즈니스 규칙 적용(사업자번호 검증·카드 매핑), 데이터 저장 및 이력 관리를 담당한다.
 */
@Slf4j
@Service
public class ReceiptServiceImpl implements ReceiptService {

    private final OcrService ocrService;
    private final ReceiptRepository receiptRepository;

    /** 영수증 이미지 저장 루트 — 설정값(app.upload.receipt-dir). 회의 오디오(VoiceAudioStore)와 동일하게 외부화 */
    private final Path rootDir;

    public ReceiptServiceImpl(OcrService ocrService, ReceiptRepository receiptRepository,
                              @Value("${app.upload.receipt-dir}") String receiptDir) {
        this.ocrService = ocrService;
        this.receiptRepository = receiptRepository;
        this.rootDir = Paths.get(receiptDir).toAbsolutePath().normalize();
    }

    @Override
    public ReceiptAnalysisResponseVo analyzeUploadedReceipt(Long userSeq, MultipartFile file) throws IOException {
        Path uploadPath = rootDir;
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String savedFilename = UUID.randomUUID().toString() + extension;
        Path targetFilePath = uploadPath.resolve(savedFilename);

        file.transferTo(targetFilePath.toFile());
        log.info("영수증 이미지 임시 저장 성공: {}", targetFilePath);

        Resource imageResource = new FileSystemResource(targetFilePath.toFile());
        List<OcrResultVo> ocrResults = ocrService.analyzeReceipt(userSeq, imageResource, file.getContentType());
        String rawJson = ocrService.toJsonString(ocrResults);

        List<String> cardNames = ocrResults.stream()
                .map(OcrResultVo::getCardName)
                .filter(name -> name != null && !name.trim().isEmpty())
                .collect(Collectors.toList());
        CardMatchResult cardMatch = matchCard(cardNames);

        Integer proposedAmount = 0;
        String proposedBizNo = "";
        String proposedPayDate = "";
        String proposedCardName = cardMatch.getCardName();

        if (!ocrResults.isEmpty()) {
            OcrResultVo primaryResult = ocrResults.stream()
                    .filter(item -> cardMatch.getCardName() == null || cardMatch.getCardName().equals(item.getCardName()))
                    .findFirst()
                    .orElse(ocrResults.get(0));

            proposedAmount = primaryResult.getPayAmount() != null ? primaryResult.getPayAmount() : 0;
            proposedBizNo = primaryResult.getBizNo() != null ? primaryResult.getBizNo().replaceAll("[^0-9]", "") : "";
            proposedPayDate = primaryResult.getPayDate() != null ? primaryResult.getPayDate().replaceAll("[^0-9]", "") : "";
            if (proposedCardName == null) {
                proposedCardName = primaryResult.getCardName();
            }
        }

        boolean bizNoValid = isValidBizNo(proposedBizNo);

        return ReceiptAnalysisResponseVo.builder()
                .imagePath(targetFilePath.toString())
                .payAmount(proposedAmount)
                .bizNo(proposedBizNo)
                .payDate(proposedPayDate)
                .cardName(proposedCardName)
                .bizNoValid(bizNoValid)
                .selectType(cardMatch.getSelectType())
                .rawJson(rawJson)
                .items(ocrResults)
                .build();
    }

    @Override
    @Transactional
    public Receipt saveConfirmedReceipt(Long userSeq, ReceiptSaveRequestVo request) {
        log.info("영수증 최종 저장 요청 (UserSeq: {}, Amount: {})", userSeq, request.getPayAmount());

        boolean bizNoValid = isValidBizNo(request.getBizNo());

        Receipt receipt = Receipt.builder()
                .userSeq(userSeq)
                .imagePath(request.getImagePath())
                .payAmount(request.getPayAmount())
                .bizNo(request.getBizNo())
                .payDate(request.getPayDate())
                .cardName(request.getCardName())
                .bizNoValid(bizNoValid)
                .selectType(request.getSelectType())
                .rawJson(request.getRawJson())
                .build();

        return receiptRepository.save(receipt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Receipt> getReceiptHistory(Long userSeq) {
        return receiptRepository.findByUserSeqOrderByCreatedAtDesc(userSeq);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptPageVo getReceiptHistoryPage(Long userSeq, Integer page, Integer size) {
        Page<Receipt> result;
        if (page != null && size != null) {
            // 페이징 파라미터가 오면 그 값으로 페이징 조회 (최신 등록순)
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            result = receiptRepository.findByUserSeq(userSeq, pageable);
        } else {
            // page·size 가 없으면 전체를 한 페이지로 담아 반환 (totalPages=1)
            result = new PageImpl<>(receiptRepository.findByUserSeqOrderByCreatedAtDesc(userSeq));
        }
        return ReceiptPageVo.builder()
                .content(result.getContent())
                .page(result.getNumber())
                .totalPages(result.getTotalPages())
                .totalElements(result.getTotalElements())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReceiptHistoryToCsv(Long userSeq) {
        List<Receipt> history = getReceiptHistory(userSeq);
        StringBuilder sb = new StringBuilder();
        // CSV 헤더 작성 (UTF-8 BOM 추가)
        sb.append('﻿');
        sb.append("결제일,카드사명,결제금액,사업자등록번호,검증성공여부,등록일\n");
        for (Receipt r : history) {
            sb.append(String.format("%s,%s,%d,%s,%s,%s\n",
                    r.getPayDate(),
                    r.getCardName() != null ? r.getCardName() : "",
                    r.getPayAmount(),
                    r.getBizNo(),
                    r.getBizNoValid() ? "성공" : "실패(경고)",
                    r.getCreatedAt().toString()
            ));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ===== 아래는 구 ReceiptValidator 흡수 — 순수 로직, 패키지 프라이빗(테스트 직접 호출용) =====

    /** 한국 사업자등록번호 유효성을 체크섬 알고리즘으로 검증한다. */
    boolean isValidBizNo(String bizNo) {
        if (bizNo == null || bizNo.length() != 10) {
            return false;
        }
        if (!bizNo.matches("\\d{10}")) {
            return false;
        }
        int[] weights = {1, 3, 7, 1, 3, 7, 1, 3};
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum += (bizNo.charAt(i) - '0') * weights[i];
        }
        int d9Val = (bizNo.charAt(8) - '0') * 5;
        sum += (d9Val / 10) + (d9Val % 10);
        int checksum = (10 - (sum % 10)) % 10;
        int d10Val = bizNo.charAt(9) - '0';
        return checksum == d10Val;
    }

    /**
     * OCR 이 검출한 카드명으로 매핑 타입을 결정한다.
     * 특정 카드사를 하드코딩하지 않고, 검출된 카드가 (중복 제거 후) 정확히 한 종류면 AUTO 로 자동 선택,
     * 0종이거나 2종 이상이면 MANUAL 로 두어 사용자가 드롭다운에서 고르게 한다.
     *
     * @param cardNames OCR 결과에서 뽑은 카드명 목록 (null·공백 항목 허용)
     * @return 매핑 타입(AUTO/MANUAL)과 자동 선택된 카드명(AUTO 일 때만)
     */
    CardMatchResult matchCard(List<String> cardNames) {
        if (cardNames == null) {
            return new CardMatchResult("MANUAL", null);
        }
        // null·공백 제거 후 중복 제거 — 같은 카드가 여러 항목에 나와도 한 종류로 센다
        List<String> distinctCards = cardNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        if (distinctCards.size() == 1) {
            return new CardMatchResult("AUTO", distinctCards.get(0));
        }
        return new CardMatchResult("MANUAL", null);
    }

    /** 카드 매핑 결과 (구 ReceiptValidator.CardMatchResult). */
    @Getter
    @AllArgsConstructor
    static class CardMatchResult {
        private final String selectType;
        private final String cardName;
    }
}
