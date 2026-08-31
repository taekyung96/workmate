package com.workmate.was.ocr.service;

import com.workmate.was.ocr.vo.OcrResultVo;
import org.springframework.core.io.Resource;
import java.util.List;

/** Spring AI (Gemini) 기반 OCR 서비스 인터페이스. */
public interface OcrService {

    /**
     * 영수증 이미지를 멀티모달로 분석해 결제 내역을 추출한다.
     *
     * @param userSeq       요청 사용자 — LLM 사용량 기록(F-OBS)의 귀속 대상
     * @param imageResource 영수증 이미지 파일 리소스
     * @param mimeType      이미지 MimeType
     * @return 추출된 결제 건 정보 리스트 (실패 시 빈 리스트)
     */
    List<OcrResultVo> analyzeReceipt(Long userSeq, Resource imageResource, String mimeType);

    String toJsonString(List<OcrResultVo> results);
}
