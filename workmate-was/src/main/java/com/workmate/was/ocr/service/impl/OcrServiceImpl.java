package com.workmate.was.ocr.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workmate.was.ocr.service.OcrService;
import com.workmate.was.ocr.vo.OcrResultVo;
import lombok.extern.slf4j.Slf4j;
import com.workmate.was.usage.service.LlmUsageService;
import com.workmate.was.global.config.ChatClientRegistry;
import com.workmate.was.usage.vo.LlmFeature;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.io.Resource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.util.Collections;
import java.util.List;

/**
 * Spring AI (Gemini) 기반 OCR 서비스 구현체.
 * 영수증 이미지를 분석하여 결제 건 정보들을 구조화된 객체로 추출한다.
 */
@Slf4j
@Service
public class OcrServiceImpl implements OcrService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final LlmUsageService llmUsageService;

    /**
     * @param registry 멀티모달(이미지) 입력이라 제공자를 고를 수 없다 — 고정 클라이언트를 받는다.
     *                 사용자가 텍스트 전용 모델을 골라 둔 채로 영수증을 올리면 조용히 실패한다.
     */
    public OcrServiceImpl(ChatClientRegistry registry, ObjectMapper objectMapper,
                          LlmUsageService llmUsageService) {
        this.chatClient = registry.multimodal();
        this.objectMapper = objectMapper;
        this.llmUsageService = llmUsageService;
    }

    /**
     * 영수증 이미지 리소스를 읽어 AI 분석 후 결제 정보를 구조화하여 반환한다.
     *
     * @param userSeq 요청 사용자 (사용량 기록 귀속 대상)
     * @param imageResource 영수증 이미지 파일 리소스
     * @param mimeType 이미지 파일의 MimeType (e.g. image/png, image/jpeg)
     * @return 추출된 결제 건 정보 리스트
     */
    @Override
    public List<OcrResultVo> analyzeReceipt(Long userSeq, Resource imageResource, String mimeType) {
        log.info("영수증 이미지 AI 분석 요청 시작 (MimeType: {})", mimeType);

        MimeType mediaType = parseMimeType(mimeType);
        String systemInstruction =
                "당신은 영수증 정보 추출 전문가입니다. 제공된 영수증 이미지에서 결제 내역들을 추출해 주세요.\n" +
                "주의 사항:\n" +
                "1. 카드사명(cardName): 영수증 결제 수단에 명시된 카드사명(예: 국민카드, 롯데법인카드 등)을 추출하세요.\n" +
                "2. 결제 금액(payAmount): 결제 총액 또는 해당 카드로 결제된 금액을 숫자로 추출하세요.\n" +
                "3. 결제일(payDate): 결제 일시에서 날짜 정보를 추출하여 반드시 'YYYYMMDD' 형식으로 변환하세요. (예: 2026.07.13 -> 20260713)\n" +
                "4. 사업자등록번호(bizNo): 영수증의 공급자 사업자등록번호를 하이픈(-) 없이 10자리 숫자로만 추출하세요. (예: 120-81-18375 -> 1208118375)\n" +
                "영수증 내에 여러 건의 결제 승인 내역이 있거나 분할 결제된 경우, 각각을 배열 아이템으로 모두 추출해 주세요.";

        try {
            // entity() 대신 responseEntity() 를 쓰면 파싱 결과와 원본 응답(usage 포함)을 함께 받는다
            ResponseEntity<ChatResponse, List<OcrResultVo>> response = chatClient.prompt()
                    .system(systemInstruction)
                    .user(userSpec -> userSpec
                            .text("이 영수증 이미지를 분석하여 구조화된 JSON 데이터로 반환해 주세요.")
                            .media(mediaType, imageResource)
                    )
                    .call()
                    .responseEntity(new ParameterizedTypeReference<List<OcrResultVo>>() {});

            recordUsage(userSeq, response.getResponse());
            List<OcrResultVo> results = response.getEntity();

            if (results == null) {
                log.warn("AI 분석 결과가 null입니다.");
                return Collections.emptyList();
            }

            log.info("영수증 AI 분석 성공. 추출 건수: {}건", results.size());
            return results;

        } catch (Exception e) {
            log.error("AI 영수증 분석 중 에러 발생: {}", e.getMessage(), e);
            // OCR 실패 시 빈 수동 입력 폼 폴백을 보장하기 위해 빈 리스트를 리턴
            return Collections.emptyList();
        }
    }

    /**
     * AI 분석 응답 데이터(results)를 JSON 형태의 문자열로 직렬화하여 rawJson으로 사용한다.
     *
     * @param results 추출된 결과 객체 목록
     * @return JSON 문자열
     */
    @Override
    public String toJsonString(List<OcrResultVo> results) {
        try {
            return objectMapper.writeValueAsString(results);
        } catch (JsonProcessingException e) {
            log.error("JSON 직렬화 실패: {}", e.getMessage(), e);
            return "[]";
        }
    }

    /** 응답 메타데이터의 usage 를 사용량 기록에 남긴다 (F-OBS) */
    private void recordUsage(Long userSeq, ChatResponse response) {
        if (response == null || response.getMetadata() == null) {
            return;
        }
        llmUsageService.record(userSeq, LlmFeature.OCR,
                response.getMetadata().getModel(), response.getMetadata().getUsage());
    }

    private MimeType parseMimeType(String mimeType) {
        try {
            return MimeType.valueOf(mimeType);
        } catch (Exception e) {
            return MimeTypeUtils.IMAGE_JPEG; // 기본값
        }
    }
}
