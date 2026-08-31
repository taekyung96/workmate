package com.workmate.was.guide.service;

import com.workmate.was.guide.vo.GuideSourceChunk;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RAG 검색기 (F4-06·08) — 채팅 RAG 모드에서 질문과 유사한 가이드 청크를 찾는다.
 * 본인 문서 + 공개 문서만 대상으로 하고(비공개 타인 문서 제외), 최소 유사도 임계값 미달은 버린다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuideRetriever {

    private final VectorStore vectorStore;
    private final MeterRegistry meterRegistry;

    /** RAG 검색 결과 카운터 이름 (F-OBS) — "RAG 를 켰는데 근거를 못 찾은 비율"을 본다 */
    private static final String METRIC_NAME = "workmate.rag.retrieval";

    /** 유사도 상위 K개 (F4-06) */
    @Value("${app.chat.rag-top-k:4}")
    private int topK;

    /** 최소 코사인 유사도 임계값 (F4-06) — 미달 청크 제외 */
    @Value("${app.chat.rag-threshold:0.4}")
    private double threshold;

    /**
     * 질문과 유사한 접근 가능 가이드 청크를 반환한다.
     *
     * @param userSeq 요청 사용자 (본인·공개 필터용)
     * @param query   사용자 질문
     * @return 접근 가능한 유사 청크 (없으면 빈 리스트)
     */
    public List<GuideSourceChunk> retrieve(Long userSeq, String query) {
        try {
            List<Document> docs = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(threshold)
                    .build());
            List<GuideSourceChunk> chunks = docs.stream()
                    .filter(doc -> isAccessible(doc, userSeq))
                    .map(doc -> new GuideSourceChunk(
                            toLong(doc.getMetadata().get("guideSeq")),
                            String.valueOf(doc.getMetadata().get("title")),
                            doc.getText()))
                    .toList();
            // 검색은 성공했지만 근거를 못 찾은 경우(miss)와 찾은 경우(hit)를 구분해서 잰다 (F-OBS)
            meterRegistry.counter(METRIC_NAME, "result", chunks.isEmpty() ? "miss" : "hit").increment();
            return chunks;
        } catch (Exception e) {
            // 검색 실패는 RAG 미적용(일반 답변)으로 폴백 — 채팅 자체를 막지 않는다
            // hit/miss 와는 원인이 다른 상태(검색 자체가 죽음)라 error 로 별도 태그를 붙인다
            meterRegistry.counter(METRIC_NAME, "result", "error").increment();
            log.error("RAG 검색 실패 — 일반 답변으로 폴백", e);
            return List.of();
        }
    }

    /** 공개 문서이거나 본인 문서인 청크만 접근 허용 (F4-08) */
    private boolean isAccessible(Document doc, Long userSeq) {
        boolean isPublic = Boolean.parseBoolean(String.valueOf(doc.getMetadata().get("isPublic")));
        Object owner = doc.getMetadata().get("userSeq");
        boolean owned = owner != null && owner.toString().equals(userSeq.toString());
        return isPublic || owned;
    }

    private Long toLong(Object value) {
        return value == null ? null : Long.valueOf(value.toString());
    }
}
