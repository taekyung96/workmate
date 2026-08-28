package com.workmate.was.guide.service;

import com.workmate.was.guide.vo.GuideResponseVo;
import com.workmate.was.guide.vo.GuideSaveRequestVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 가이드 수정 시 임베딩 API 호출 비용 검증 (F4-06 · 429 쿼터 대응).
 *
 * <p>배경: 무료 티어 임베딩은 RPM 한도가 낮아 불필요한 재임베딩이 곧 429 로 이어진다
 * ({@code docs/architecture/RAG_VECTORSTORE_EMBEDDING_QUOTA_GUIDE.md}). 본문이 그대로인데
 * 제목·공개여부만 바꾸는 수정은 벡터를 다시 만들 이유가 없다 — 두 값은 청크 <b>메타데이터</b>라
 * 임베딩 벡터에 영향을 주지 않기 때문이다.
 *
 * <p>이 테스트는 그 비용을 <b>수치로 고정</b>한다. 가짜 임베딩 모델이 호출 횟수를 세므로
 * 실제 API 없이도 "수정 1회당 임베딩 호출 수"를 재현 가능하게 측정할 수 있다.
 *
 * <p>실행: {@code ./gradlew :workmate-was:test --tests "*GuideUpdateEmbeddingCostTest"} (dev DB 필요)
 */
@SpringBootTest(properties = "spring.ai.google.genai.api-key=dummy-key-for-test")
@Import(GuideUpdateEmbeddingCostTest.TestConfig.class)
class GuideUpdateEmbeddingCostTest {

    /** 호출 횟수를 세는 가짜 임베딩 모델 — 실제 API 없이 임베딩 비용을 측정한다. */
    static class CountingEmbeddingModel implements EmbeddingModel {

        /** 임베딩된 텍스트 조각 수 누적 (= 유료 API 라면 과금 대상이 되는 단위) */
        private final AtomicInteger embeddedChunks = new AtomicInteger();

        int embeddedChunks() {
            return embeddedChunks.get();
        }

        void reset() {
            embeddedChunks.set(0);
        }

        /** pgvector 코사인 거리 연산의 분모 0 오류를 막기 위해 첫 차원에 크기를 준 고정 벡터 */
        private static float[] fakeVector() {
            float[] vector = new float[768];
            vector[0] = 1.0f;
            return vector;
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            int n = request.getInstructions().size();
            embeddedChunks.addAndGet(n);
            List<Embedding> embeddings = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                embeddings.add(new Embedding(fakeVector(), i));
            }
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            embeddedChunks.incrementAndGet();
            return fakeVector();
        }

        @Override
        public int dimensions() {
            return 768;
        }
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public EmbeddingModel embeddingModel() {
            return new CountingEmbeddingModel();
        }
    }

    private static final Long USER_SEQ = 1L;
    private static final String CONTENT =
            "출장비 정산은 귀사 후 5영업일 안에 신청해야 합니다. 영수증 원본을 반드시 첨부하세요. "
                    + "숙박비는 1박 12만원, 식비는 1일 3만원을 상한으로 합니다.";

    @Autowired
    private GuideService guideService;
    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 공유 dev DB 를 건드리지 않도록 이 테스트가 만든 문서만 추적해 정리한다
    private final List<Long> createdGuideSeqs = new ArrayList<>();

    @AfterEach
    void tearDown() {
        for (Long guideSeq : createdGuideSeqs) {
            guideService.deleteGuide(USER_SEQ, false, guideSeq);
        }
        createdGuideSeqs.clear();
    }

    private CountingEmbeddingModel counter() {
        return (CountingEmbeddingModel) embeddingModel;
    }

    private Long createGuide(String title) {
        GuideResponseVo created = guideService.createGuide(USER_SEQ, GuideSaveRequestVo.builder()
                .title(title)
                .content(CONTENT)
                .isPublic(true)
                .build());
        createdGuideSeqs.add(created.getGuideSeq());
        return created.getGuideSeq();
    }

    @Test
    @DisplayName("제목만 바꾸면 임베딩을 다시 만들지 않는다 — 호출 0회")
    void title_only_update_costs_no_embedding() {
        Long guideSeq = createGuide("출장비 정산 규정");
        counter().reset();

        guideService.updateGuide(USER_SEQ, false, guideSeq, GuideSaveRequestVo.builder()
                .title("출장비 정산 규정 (2026 개정)")
                .content(CONTENT) // 본문 동일
                .isPublic(true)
                .build());

        assertThat(counter().embeddedChunks())
                .as("본문이 그대로면 벡터를 다시 만들 이유가 없다")
                .isZero();
    }

    @Test
    @DisplayName("제목만 바꿔도 검색 결과의 제목 메타데이터는 갱신된다")
    void title_only_update_still_refreshes_metadata() {
        Long guideSeq = createGuide("출장비 정산 규정");

        guideService.updateGuide(USER_SEQ, false, guideSeq, GuideSaveRequestVo.builder()
                .title("출장비 정산 규정 (2026 개정)")
                .content(CONTENT)
                .isPublic(true)
                .build());

        List<String> titles = jdbcTemplate.queryForList(
                "SELECT metadata->>'title' FROM vector_store WHERE (metadata->>'guideSeq')::bigint = ?",
                String.class, guideSeq);

        assertThat(titles).isNotEmpty();
        assertThat(titles).allMatch("출장비 정산 규정 (2026 개정)"::equals);
    }

    @Test
    @DisplayName("공개여부만 바꿔도 임베딩 호출 없이 메타데이터가 갱신된다 (F4-08 접근 필터 근거)")
    void visibility_only_update_costs_no_embedding() {
        Long guideSeq = createGuide("출장비 정산 규정");
        counter().reset();

        guideService.updateGuide(USER_SEQ, false, guideSeq, GuideSaveRequestVo.builder()
                .title("출장비 정산 규정")
                .content(CONTENT)
                .isPublic(false)
                .build());

        assertThat(counter().embeddedChunks()).isZero();

        List<String> flags = jdbcTemplate.queryForList(
                "SELECT metadata->>'isPublic' FROM vector_store WHERE (metadata->>'guideSeq')::bigint = ?",
                String.class, guideSeq);
        assertThat(flags).isNotEmpty();
        assertThat(flags).allMatch("false"::equals);
    }

    @Test
    @DisplayName("본문이 바뀌면 재임베딩한다 — 호출이 발생하고 검색도 새 본문을 찾는다")
    void content_update_reembeds() {
        Long guideSeq = createGuide("출장비 정산 규정");
        counter().reset();

        guideService.updateGuide(USER_SEQ, false, guideSeq, GuideSaveRequestVo.builder()
                .title("출장비 정산 규정")
                .content("숙박비 상한을 1박 15만원으로 인상합니다. 식비 상한은 1일 4만원입니다.")
                .isPublic(true)
                .build());

        assertThat(counter().embeddedChunks())
                .as("본문이 바뀌면 벡터를 다시 만들어야 한다")
                .isPositive();

        List<Document> found = vectorStore.similaritySearch(
                SearchRequest.builder().query("숙박비 상한").topK(5).build());
        assertThat(found)
                .anyMatch(d -> d.getMetadata().get("guideSeq").toString().equals(guideSeq.toString())
                        && d.getText().contains("15만원"));
    }
}
