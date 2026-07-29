package com.workmate.was.rageval;

import com.workmate.was.guide.dao.GuideRepository;
import com.workmate.was.guide.service.GuideRetriever;
import com.workmate.was.guide.vo.Guide;
import com.workmate.was.guide.vo.GuideSourceChunk;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RAG 검색 품질 평가 러너 — 실제 dev DB·실제 임베딩으로 읽기 전용 검색을 수행한다.
 * topK×threshold 를 스윕하며 GuideRetriever 의 실제 코드 경로를 그대로 호출하고,
 * 결과를 마크다운 리포트로 남긴다. 스윕은 ReflectionTestUtils 로 필드만 주입해
 * 프로덕션 코드를 바꾸지 않는다.
 *
 * 실행: ./gradlew :workmate-was:ragEval   (GEMINI_API_KEY + dev DB 필요)
 */
@Tag("rag-eval")
@SpringBootTest
class RagEvalRunner {

    /** 스윕 격자 — 상위 K 개 */
    private static final List<Integer> TOP_KS = List.of(2, 4, 6, 8);
    /** 스윕 격자 — 최소 유사도 임계값 */
    private static final List<Double> THRESHOLDS = List.of(0.3, 0.4, 0.5, 0.6);

    @Autowired
    private GuideRetriever guideRetriever;
    @Autowired
    private GuideRepository guideRepository;

    @Test
    @DisplayName("검색 품질 스윕 실행 후 리포트를 남긴다")
    void run() throws Exception {
        List<Guide> guides = guideRepository.findAll();
        assertThat(guides).as("평가하려면 dev DB 에 가이드가 있어야 한다").isNotEmpty();
        // 접근 필터(공개/본인) 통과를 위해 가이드 소유자 seq 로 검색한다(본인 문서는 무조건 접근 가능)
        Long ownerSeq = guides.get(0).getUserSeq();

        List<EvalQuery> queries = new GoldenSetLoader().load("rageval/queries.json");
        assertThat(queries).as("골든셋이 비어 있으면 안 된다").isNotEmpty();

        List<EvalReportWriter.SweepResult> results = new ArrayList<>();
        for (int topK : TOP_KS) {
            for (double threshold : THRESHOLDS) {
                // 프로덕션 코드 무변경 — @Value 필드만 반복마다 주입
                ReflectionTestUtils.setField(guideRetriever, "topK", topK);
                ReflectionTestUtils.setField(guideRetriever, "threshold", threshold);

                List<RetrievalMetrics.EvalCase> cases = new ArrayList<>();
                for (EvalQuery q : queries) {
                    List<String> titles = guideRetriever.retrieve(ownerSeq, q.question()).stream()
                            .map(GuideSourceChunk::title)
                            .toList();
                    cases.add(new RetrievalMetrics.EvalCase(titles, new HashSet<>(q.expectedTitles())));
                }
                results.add(new EvalReportWriter.SweepResult(
                        topK, threshold, RetrievalMetrics.compute(cases)));
            }
        }

        EvalReportWriter writer = new EvalReportWriter();
        EvalReportWriter.CorpusMeta meta =
                new EvalReportWriter.CorpusMeta(guides.size(), queries.size(), LocalDate.now());
        String markdown = writer.render(results, meta);
        System.out.println(markdown);

        String reportDir = System.getProperty("ragEval.reportDir", "../docs/development/rag-eval");
        Path file = writer.write(markdown, Path.of(reportDir), LocalDate.now());

        assertThat(Files.exists(file)).isTrue();
        // 최소 한 조합에서는 정답을 건져야 골든셋·검색이 정상 연결된 것
        assertThat(results).anyMatch(r -> r.metrics().hitRate() > 0);
    }
}
