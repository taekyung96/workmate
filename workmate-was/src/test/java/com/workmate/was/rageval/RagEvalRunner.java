package com.workmate.was.rageval;

import com.workmate.was.chat.service.RagPromptBuilder;
import com.workmate.was.guide.dao.GuideRepository;
import com.workmate.was.guide.vo.Guide;
import com.workmate.was.guide.vo.GuideSourceChunk;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RAG 검색 품질 평가 러너 — 실제 dev DB·실제 임베딩으로 읽기 전용 검색을 수행한다.
 * topK×threshold 를 스윕하며 검색 품질(Hit@K·MRR·Miss)을 측정하고 마크다운 리포트로 남긴다.
 *
 * <p><b>임베딩 호출 최소화(핵심 설계):</b> topK·threshold 는 임베딩 벡터에 영향을 주지 않고
 * "검색 후 자르기/거르기"에만 관여한다. 따라서 쿼리마다 <b>단 1회</b>만 임베딩·검색(topK=최대,
 * threshold=0)해 점수 포함 결과를 캐시한 뒤, 16개 파라미터 조합은 그 캐시를 메모리에서 재현한다.
 * 결과적으로 임베딩 API 호출은 (조합 수 × 쿼리 수)가 아니라 <b>쿼리 수</b>만큼만 발생해
 * 무료 티어 쿼터(429) 안에서 평가가 끝난다. (예: 16×33=528회 → 33회)
 *
 * <p>재현 순서는 프로덕션 검색기 {@code GuideRetriever} 와 동일하다:
 * DB 유사도 임계값(threshold) → 상위 K(topK) → Java 접근 필터(본인·공개 문서만).
 *
 * <p>실행: {@code ./gradlew :workmate-was:ragEval}  (GEMINI_API_KEY + dev DB 필요)
 */
@Tag("rag-eval")
@SpringBootTest
class RagEvalRunner {

    /** 스윕 격자 — 상위 K 개 */
    private static final List<Integer> TOP_KS = List.of(2, 4, 6, 8);
    /** 스윕 격자 — 최소 유사도 임계값 */
    private static final List<Double> THRESHOLDS = List.of(0.3, 0.4, 0.5, 0.6);

    /** 1회 검색 시 넉넉히 가져올 상위 개수 = 스윕 최대 topK. 이보다 큰 topK 는 쓰지 않으므로 충분하다. */
    private static final int MAX_TOP_K = TOP_KS.stream().max(Integer::compareTo).orElse(8);

    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private GuideRepository guideRepository;
    @Autowired
    private RagPromptBuilder ragPromptBuilder;

    /**
     * 검색 1회 결과의 청크 한 건 — 파라미터 스윕(threshold·topK·접근필터)을 메모리에서 재현하는 데 필요한 최소 정보.
     *
     * @param guideSeq   출처 문서 식별자(프롬프트 블록 조립용)
     * @param title      청크 제목(메트릭은 title 시퀀스로 계산)
     * @param content    청크 본문(프롬프트 컨텍스트 크기 측정용)
     * @param score      코사인 유사도 점수(threshold 재현용)
     * @param accessible 접근 가능 여부(본인·공개 문서 필터 재현용)
     */
    private record ScoredChunk(Long guideSeq, String title, String content, double score, boolean accessible) {
    }

    @Test
    @DisplayName("검색 품질 스윕 실행 후 리포트를 남긴다")
    void run() throws Exception {
        List<Guide> guides = guideRepository.findAll();
        assertThat(guides).as("평가하려면 dev DB 에 가이드가 있어야 한다").isNotEmpty();
        // 접근 필터(공개/본인) 통과를 위해 가이드 소유자 seq 로 검색한다(본인 문서는 무조건 접근 가능)
        Long ownerSeq = guides.get(0).getUserSeq();

        List<EvalQuery> queries = new GoldenSetLoader().load("rageval/queries.json");
        assertThat(queries).as("골든셋이 비어 있으면 안 된다").isNotEmpty();

        // 1) 쿼리당 1회만 임베딩·검색(topK=최대, threshold=0) → 점수 포함 결과를 캐시.
        //    파라미터 스윕은 이 캐시를 재사용하므로 임베딩 호출은 여기서 쿼리 수만큼만 발생한다.
        List<List<ScoredChunk>> perQueryChunks = new ArrayList<>(queries.size());
        for (EvalQuery q : queries) {
            List<Document> docs = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(q.question())
                    .topK(MAX_TOP_K)
                    .similarityThreshold(0.0) // 임계값은 메모리에서 적용하므로 여기선 전부 허용
                    .build());
            List<ScoredChunk> chunks = docs.stream()
                    .map(doc -> new ScoredChunk(
                            toLong(doc.getMetadata().get("guideSeq")),
                            String.valueOf(doc.getMetadata().get("title")),
                            doc.getText(),
                            doc.getScore() == null ? 0.0 : doc.getScore(),
                            isAccessible(doc, ownerSeq)))
                    .toList();
            perQueryChunks.add(chunks);
        }

        // 2) 파라미터 스윕 — 임베딩 없이 캐시된 결과만 메모리에서 자르고 걸러 재현한다.
        List<EvalReportWriter.SweepResult> results = new ArrayList<>();
        for (int topK : TOP_KS) {
            for (double threshold : THRESHOLDS) {
                List<RetrievalMetrics.EvalCase> cases = new ArrayList<>(queries.size());
                long contextCharSum = 0;
                for (int i = 0; i < queries.size(); i++) {
                    // GuideRetriever 와 동일 순서: threshold(DB) → topK(DB) → 접근필터(Java)
                    List<ScoredChunk> retained = perQueryChunks.get(i).stream()
                            .filter(c -> c.score() >= threshold)
                            .limit(topK)
                            .filter(ScoredChunk::accessible)
                            .toList();
                    cases.add(new RetrievalMetrics.EvalCase(
                            retained.stream().map(ScoredChunk::title).toList(),
                            new HashSet<>(queries.get(i).expectedTitles())));
                    // 프로덕션과 같은 빌더로 조립해, 리포트의 컨텍스트 크기가 실제 전송량과 어긋나지 않게 한다
                    contextCharSum += ragPromptBuilder.build(retained.stream()
                            .map(c -> new GuideSourceChunk(c.guideSeq(), c.title(), c.content()))
                            .toList()).length();
                }
                results.add(new EvalReportWriter.SweepResult(
                        topK, threshold, RetrievalMetrics.compute(cases),
                        (double) contextCharSum / queries.size()));
            }
        }

        EvalReportWriter writer = new EvalReportWriter();
        EvalReportWriter.CorpusMeta meta =
                new EvalReportWriter.CorpusMeta(guides.size(), queries.size(), LocalDate.now());
        String markdown = writer.render(results, meta);
        System.out.println(markdown);

        String reportDir = System.getProperty("ragEval.reportDir", "../docs/features/rag-eval");
        Path file = writer.write(markdown, Path.of(reportDir), LocalDate.now());

        assertThat(Files.exists(file)).isTrue();
        // 최소 한 조합에서는 정답을 건져야 골든셋·검색이 정상 연결된 것
        assertThat(results).anyMatch(r -> r.metrics().hitRate() > 0);
    }

    /**
     * 청크 접근 가능 여부 — 프로덕션 {@code GuideRetriever.isAccessible} 과 동일 규칙(공개이거나 본인 문서).
     *
     * @param doc     검색된 문서(메타데이터에 isPublic·userSeq 보유)
     * @param userSeq 요청 사용자 seq
     * @return 접근 가능하면 true
     */
    private boolean isAccessible(Document doc, Long userSeq) {
        boolean isPublic = Boolean.parseBoolean(String.valueOf(doc.getMetadata().get("isPublic")));
        Object owner = doc.getMetadata().get("userSeq");
        boolean owned = owner != null && owner.toString().equals(userSeq.toString());
        return isPublic || owned;
    }

    /** 메타데이터의 guideSeq 를 Long 으로 — 프로덕션 {@code GuideRetriever.toLong} 과 동일 규칙 */
    private Long toLong(Object value) {
        return value == null ? null : Long.valueOf(value.toString());
    }
}
