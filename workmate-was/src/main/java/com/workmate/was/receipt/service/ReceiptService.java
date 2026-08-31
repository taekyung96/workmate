package com.workmate.was.receipt.service;

import com.workmate.was.receipt.vo.Receipt;
import com.workmate.was.receipt.vo.ReceiptAnalysisResponseVo;
import com.workmate.was.receipt.vo.ReceiptPageVo;
import com.workmate.was.receipt.vo.ReceiptSaveRequestVo;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

/** 영수증 비즈니스 로직 처리 서비스 인터페이스. */
public interface ReceiptService {

    ReceiptAnalysisResponseVo analyzeUploadedReceipt(Long userSeq, MultipartFile file) throws IOException;

    Receipt saveConfirmedReceipt(Long userSeq, ReceiptSaveRequestVo request);

    /** 전체 이력(최신순) — CSV 내보내기 등 전량이 필요할 때 사용 */
    List<Receipt> getReceiptHistory(Long userSeq);

    /** 이력 페이지 조회 — page·size 가 없으면(null) 전체를 한 페이지로 반환 */
    ReceiptPageVo getReceiptHistoryPage(Long userSeq, Integer page, Integer size);

    byte[] exportReceiptHistoryToCsv(Long userSeq);
}
