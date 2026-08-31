package com.workmate.was.voice.service.impl;

import com.workmate.was.voice.dao.VoiceRecordRepository;
import com.workmate.was.voice.service.VoiceAudioStore;
import com.workmate.was.voice.service.VoiceService;
import com.workmate.was.voice.service.VoiceTranscriber;
import com.workmate.was.voice.vo.VoiceAnalysisResultVo;
import com.workmate.was.voice.vo.VoiceAudioVo;
import com.workmate.was.voice.vo.VoiceRecord;
import com.workmate.was.voice.vo.VoiceRecordPageVo;
import com.workmate.was.voice.vo.VoiceRecordSummaryVo;
import lombok.extern.slf4j.Slf4j;
import com.workmate.was.usage.service.LlmUsageService;
import com.workmate.was.usage.vo.LlmFeature;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final LlmUsageService llmUsageService;

    public VoiceServiceImpl(VoiceTranscriber transcriber,
                            VoiceRecordRepository voiceRecordRepository,
                            VoiceAudioStore audioStore,
                            ChatClient.Builder chatClientBuilder,
                            LlmUsageService llmUsageService) {
        this.transcriber = transcriber;
        this.voiceRecordRepository = voiceRecordRepository;
        this.audioStore = audioStore;
        this.chatClient = chatClientBuilder.build();
        this.llmUsageService = llmUsageService;
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
        String sttText = transcriber.transcribe(userSeq, file.getResource(), file.getContentType());
        if (sttText.isBlank()) {
            throw new IllegalStateException("음성에서 텍스트를 추출하지 못했습니다. 오디오 품질을 확인해주세요.");
        }

        // 2) 구조화 요약 — 전사문을 Gemini 텍스트 모델로 3단 마크다운 요약
        String summaryMd = summarize(userSeq, sttText);

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
    public VoiceRecordPageVo getHistoryPage(Long userSeq, Integer page, Integer size) {
        Page<VoiceRecord> result;
        if (page != null && size != null) {
            // 페이징 파라미터가 오면 그 값으로 페이징 조회 (최신 등록순)
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            result = voiceRecordRepository.findByUserSeq(userSeq, pageable);
        } else {
            // page·size 가 없으면 전체를 한 페이지로 담아 반환 (totalPages=1)
            result = new PageImpl<>(voiceRecordRepository.findByUserSeqOrderByCreatedAtDesc(userSeq));
        }
        return VoiceRecordPageVo.builder()
                .content(result.getContent().stream().map(VoiceRecordSummaryVo::new).toList())
                .page(result.getNumber())
                .totalPages(result.getTotalPages())
                .totalElements(result.getTotalElements())
                .build();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public VoiceAnalysisResultVo getRecord(Long userSeq, Long recordSeq) {
        return new VoiceAnalysisResultVo(findOwnedRecord(userSeq, recordSeq));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public VoiceAudioVo getAudio(Long userSeq, Long recordSeq) {
        VoiceRecord record = findOwnedRecord(userSeq, recordSeq);
        if (record.getAudioFileName() == null) {
            throw new IllegalArgumentException("이 회의록에는 저장된 오디오가 없습니다.");
        }
        Resource resource = audioStore.load(record.getAudioFileName())
                .orElseThrow(() -> new IllegalArgumentException("오디오 파일을 찾을 수 없습니다."));
        // Content-Type 이 비어 있던 과거 업로드는 범용 바이너리로 내려 브라우저가 판단하게 한다
        String contentType = record.getContentType() != null
                ? record.getContentType() : "application/octet-stream";
        return new VoiceAudioVo(resource, contentType, record.getOriginFileName());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteRecord(Long userSeq, Long recordSeq) {
        VoiceRecord record = findOwnedRecord(userSeq, recordSeq);
        // 파일이 이미 없어도(수동 삭제·유실) DB 행 삭제는 진행한다 — store 가 경고 로그만 남긴다
        if (record.getAudioFileName() != null) {
            audioStore.delete(record.getAudioFileName());
        }
        voiceRecordRepository.delete(record);
        log.info("회의록 삭제 완료 - userSeq: {}, recordSeq: {}", userSeq, recordSeq);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public VoiceAnalysisResultVo updateTitle(Long userSeq, Long recordSeq, String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("회의 제목을 입력해주세요.");
        }
        VoiceRecord record = findOwnedRecord(userSeq, recordSeq);
        // JPA 더티 체킹 — 조회된 엔티티의 제목만 바꾸면 트랜잭션 커밋 시 UPDATE 가 자동 반영된다
        record.changeTitle(title.trim());
        log.info("회의록 제목 수정 완료 - userSeq: {}, recordSeq: {}", userSeq, recordSeq);
        return new VoiceAnalysisResultVo(record);
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
    protected String summarize(Long userSeq, String sttText) {
        // content() 대신 chatResponse() — 사용량(usage) 을 함께 받기 위함 (F-OBS)
        ChatResponse response = chatClient.prompt()
                .system(SUMMARY_SYSTEM_PROMPT)
                .user(sttText)
                .call()
                .chatResponse();

        if (response != null && response.getMetadata() != null) {
            llmUsageService.record(userSeq, LlmFeature.SUMMARY,
                    response.getMetadata().getModel(), response.getMetadata().getUsage());
        }
        String md = (response == null || response.getResult() == null
                || response.getResult().getOutput() == null)
                ? null : response.getResult().getOutput().getText();
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
