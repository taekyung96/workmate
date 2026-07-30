package com.workmate.was.voice.service;

import com.workmate.was.voice.vo.VoiceAnalysisResultVo;
import org.springframework.web.multipart.MultipartFile;

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
     * 저장된 회의록 요약을 사내 가이드 문서로 등록한다 (F8-1-6, 지식 공유).
     * 등록된 가이드는 이후 RAG 검색 대상이 된다.
     *
     * @param userSeq   요청 사용자 식별자 (본인 회의록만 허용)
     * @param recordSeq 회의록 식별자
     * @return 새로 생성된 가이드 식별자
     */
    Long convertToGuide(Long userSeq, Long recordSeq);
}
