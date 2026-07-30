package com.workmate.was.voice.service.impl;

import com.workmate.was.guide.service.GuideService;
import com.workmate.was.guide.vo.GuideResponseVo;
import com.workmate.was.guide.vo.GuideSaveRequestVo;
import com.workmate.was.voice.dao.VoiceRecordRepository;
import com.workmate.was.voice.service.VoiceService;
import com.workmate.was.voice.service.VoiceTranscriber;
import com.workmate.was.voice.vo.VoiceAnalysisResultVo;
import com.workmate.was.voice.vo.VoiceRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/**
 * 음성 회의록 서비스 구현체 (F8-1).
 * (1) VoiceTranscriber 로 전사(STT) → (2) Gemini 텍스트로 구조화 요약 → (3) 이력 저장.
 * 전사와 요약을 분리해, STT 품질이 병목이면 VoiceTranscriber 구현체만 교체하면 된다.
 */
@Slf4j
@Service
public class VoiceServiceImpl implements VoiceService {

    private final VoiceTranscriber transcriber;
    private final VoiceRecordRepository voiceRecordRepository;
    private final GuideService guideService;
    private final ChatClient chatClient;

    public VoiceServiceImpl(VoiceTranscriber transcriber,
                            VoiceRecordRepository voiceRecordRepository,
                            GuideService guideService,
                            ChatClient.Builder chatClientBuilder) {
        this.transcriber = transcriber;
        this.voiceRecordRepository = voiceRecordRepository;
        this.guideService = guideService;
        this.chatClient = chatClientBuilder.build();
    }

    /** 회의록 요약 시스템 지시 — 3단 구조(핵심 요약/결정 사항/Action Items)의 마크다운을 강제한다 */
    private static final String SUMMARY_SYSTEM_PROMPT =
            "당신은 회의록 정리 전문가입니다. 주어진 회의 전사문을 아래 3개 섹션의 한국어 마크다운으로 구조화하세요.\n"
            + "### 📌 핵심 요약\n- 회의의 주요 논의를 3~5개 불릿으로.\n"
            + "### 💡 주요 결정 사항\n- 확정된 결정만 불릿으로. 없으면 '없음'.\n"
            + "### 📝 Action Items\n- '담당자 - 할 일 - (마감)' 형식 불릿으로. 없으면 '없음'.\n"
            + "전사문에 없는 내용을 지어내지 말고, 위 마크다운 외의 사족은 붙이지 마세요.";

    /** {@inheritDoc} */
    @Override
    @Transactional
    public VoiceAnalysisResultVo analyze(Long userSeq, String title, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("오디오 파일을 첨부해주세요.");
        }
        log.info("음성 회의록 분석 요청 - userSeq: {}, 파일: {}", userSeq, file.getOriginalFilename());

        // 1) 전사(STT) — 교체 가능한 VoiceTranscriber 에 위임
        String sttText = transcriber.transcribe(file.getResource(), file.getContentType());
        if (sttText.isBlank()) {
            throw new IllegalStateException("음성에서 텍스트를 추출하지 못했습니다. 오디오 품질을 확인해주세요.");
        }

        // 2) 구조화 요약 — 전사문을 Gemini 텍스트 모델로 3단 마크다운 요약
        String summaryMd = summarize(sttText);

        // 3) 저장 (오디오 원본은 저장하지 않음 — 텍스트만)
        VoiceRecord saved = voiceRecordRepository.save(VoiceRecord.builder()
                .userSeq(userSeq)
                .title(resolveTitle(title))
                .sttText(sttText)
                .summaryMd(summaryMd)
                .build());

        log.info("음성 회의록 저장 완료 - recordSeq: {}", saved.getRecordSeq());
        return new VoiceAnalysisResultVo(saved);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Long convertToGuide(Long userSeq, Long recordSeq) {
        // 본인 소유의 회의록만 가이드로 등록 가능 (타인 회의록 등록 차단)
        VoiceRecord record = voiceRecordRepository.findById(recordSeq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회의록입니다."));
        if (!record.getUserSeq().equals(userSeq)) {
            log.warn("회의록 접근 거부 - userSeq: {}, recordSeq: {}", userSeq, recordSeq);
            throw new IllegalArgumentException("본인의 회의록만 가이드로 등록할 수 있습니다.");
        }

        // 요약 마크다운을 가이드 본문으로 등록 — 기본은 비공개(본인만). 등록 즉시 RAG 임베딩 대상이 된다.
        GuideResponseVo guide = guideService.createGuide(userSeq, GuideSaveRequestVo.builder()
                .title(record.getTitle())
                .content(record.getSummaryMd())
                .isPublic(false)
                .build());

        log.info("회의록을 가이드로 등록 완료 - recordSeq: {} → guideSeq: {}", recordSeq, guide.getGuideSeq());
        return guide.getGuideSeq();
    }

    /** 전사문을 3단 구조 마크다운 회의록으로 요약한다 */
    private String summarize(String sttText) {
        String md = chatClient.prompt()
                .system(SUMMARY_SYSTEM_PROMPT)
                .user(sttText)
                .call()
                .content();
        return md != null ? md : "";
    }

    /** 제목이 비어 있으면 날짜 기반 기본 제목을 만든다 */
    private String resolveTitle(String title) {
        if (title != null && !title.isBlank()) {
            return title.trim();
        }
        return "회의록 " + LocalDate.now();
    }
}
