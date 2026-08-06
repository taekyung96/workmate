package com.workmate.was.voice.service;

import com.workmate.was.voice.vo.VoiceAnalysisResultVo;
import com.workmate.was.voice.vo.VoiceAudioVo;
import com.workmate.was.voice.vo.VoiceRecordPageVo;
import com.workmate.was.voice.vo.VoiceRecordSummaryVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 음성 회의록 서비스 (F8-1).
 * 업로드된 오디오를 전사(STT)하고 구조화 요약한 뒤 이력으로 저장한다.
 */
public interface VoiceService {

    /**
     * 오디오를 분석(전사 + 요약)하고 결과를 저장해 반환한다.
     *
     * @param userSeq 요청 사용자 식별자
     * @param title   회의 제목 (비어 있으면 기본 제목 생성)
     * @param file    업로드된 오디오 파일
     * @return 전사 원문 + 마크다운 요약 결과
     */
    VoiceAnalysisResultVo analyze(Long userSeq, String title, MultipartFile file);

    /**
     * 사용자의 회의록 이력을 최신순으로 조회한다.
     *
     * @param userSeq 사용자 식별자
     * @return 목록 요약 VO (전사문·요약 본문 제외)
     */
    List<VoiceRecordSummaryVo> getHistory(Long userSeq);

    /**
     * 회의록 이력 페이지 조회 — page·size 가 없으면(null) 전체를 한 페이지로 반환한다.
     *
     * @param userSeq 사용자 식별자
     * @param page    0-based 페이지 (선택)
     * @param size    페이지 크기 (선택)
     * @return 목록 요약 + 페이징 메타
     */
    VoiceRecordPageVo getHistoryPage(Long userSeq, Integer page, Integer size);

    /**
     * 회의록 상세를 조회한다 (본인 소유만).
     *
     * @param userSeq   사용자 식별자
     * @param recordSeq 회의록 식별자
     * @return 전사문·요약 전문을 포함한 상세
     */
    VoiceAnalysisResultVo getRecord(Long userSeq, Long recordSeq);

    /**
     * 회의록의 오디오 리소스를 가져온다 (본인 소유만).
     *
     * @param userSeq   사용자 식별자
     * @param recordSeq 회의록 식별자
     * @return 리소스 + Content-Type + 원본 파일명
     */
    VoiceAudioVo getAudio(Long userSeq, Long recordSeq);

    /**
     * 회의록을 삭제한다 (본인 소유만). DB 행과 오디오 파일을 함께 지운다.
     *
     * @param userSeq   사용자 식별자
     * @param recordSeq 회의록 식별자
     */
    void deleteRecord(Long userSeq, Long recordSeq);

    /**
     * 회의록 제목을 수정한다 (본인 소유만). 분석 후 결과 화면에서 제목을 확정할 때 쓴다.
     *
     * @param userSeq   사용자 식별자
     * @param recordSeq 회의록 식별자
     * @param title     새 회의 제목 (공백 불가)
     * @return 제목이 반영된 회의록 상세
     */
    VoiceAnalysisResultVo updateTitle(Long userSeq, Long recordSeq, String title);
}
