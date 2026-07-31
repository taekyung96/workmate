package com.workmate.was.voice.service.impl;

import com.workmate.was.voice.dao.VoiceRecordRepository;
import com.workmate.was.voice.service.VoiceAudioStore;
import com.workmate.was.voice.service.VoiceService;
import com.workmate.was.voice.service.VoiceTranscriber;
import com.workmate.was.voice.vo.VoiceAnalysisResultVo;
import com.workmate.was.voice.vo.VoiceRecord;
import com.workmate.was.voice.vo.VoiceRecordSummaryVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/**
 * 음성 회의록 서비스 구현체 (F8-1).
 * (1) VoiceTranscriber 로 전사(STT) → (2) Gemini 텍스트로 구조화 요약 → (3) 오디오 저장 → (4) 이력 저장.
 * 전사와 요약을 분리해, STT 품질이 병목이면 VoiceTranscriber 구현체만 교체하면 된다.
 */
@Slf4j
@Service
public class VoiceServiceImpl implements VoiceService {

    private final VoiceTranscriber transcriber;
    private final VoiceRecordRepository voiceRecordRepository;
    private final VoiceAudioStore audioStore;
    private final ChatClient chatClient;

    public VoiceServiceImpl(VoiceTranscriber transcriber,
                            VoiceRecordRepository voiceRecordRepository,
                            VoiceAudioStore audioStore,
                            ChatClient.Builder chatClientBuilder) {
        this.transcriber = transcriber;
        this.voiceRecordRepository = voiceRecordRepository;
        this.audioStore = audioStore;
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

        // 3) 오디오 원본 저장 — 이력에서 다시 재생할 수 있어야 하므로 파일을 보관한다
        String audioFileName = audioStore.store(file);

        // 4) 회의록 저장 (DB 에는 파일명만, 저장 루트는 설정값)
        VoiceRecord saved = voiceRecordRepository.save(VoiceRecord.builder()
                .userSeq(userSeq)
                .title(resolveTitle(title))
                .sttText(sttText)
                .summaryMd(summaryMd)
                .audioFileName(audioFileName)
                .originFileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .build());

        log.info("음성 회의록 저장 완료 - recordSeq: {}", saved.getRecordSeq());
        return new VoiceAnalysisResultVo(saved);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<VoiceRecordSummaryVo> getHistory(Long userSeq) {
        return voiceRecordRepository.findByUserSeqOrderByCreatedAtDesc(userSeq).stream()
                .map(VoiceRecordSummaryVo::new)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public VoiceAnalysisResultVo getRecord(Long userSeq, Long recordSeq) {
        return new VoiceAnalysisResultVo(findOwnedRecord(userSeq, recordSeq));
    }

    /**
     * 본인 소유의 회의록을 찾는다. 없거나 타인 소유면 예외.
     * 상세·오디오·삭제가 공유하는 검증 지점이다.
     *
     * @param userSeq   요청자 식별자
     * @param recordSeq 회의록 식별자
     * @return 검증된 회의록
     */
    private VoiceRecord findOwnedRecord(Long userSeq, Long recordSeq) {
        VoiceRecord record = voiceRecordRepository.findById(recordSeq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회의록입니다."));
        if (!record.getUserSeq().equals(userSeq)) {
            log.warn("회의록 접근 거부 - userSeq: {}, recordSeq: {}", userSeq, recordSeq);
            throw new IllegalArgumentException("본인의 회의록만 조회할 수 있습니다.");
        }
        return record;
    }

    /** 전사문을 3단 구조 마크다운 회의록으로 요약한다 */
    protected String summarize(String sttText) {
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
