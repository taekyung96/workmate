package com.workmate.was.guide;

import com.workmate.was.guide.dao.GuideRepository;
import com.workmate.was.guide.vo.Guide;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

/**
 * 개발 환경 전용 유틸리티 테스트.
 * DB direct SQL 인서트 등으로 guide 테이블과 vector_store 간 임베딩 데이터가 누락되었을 때,
 * 1.5초 딜레이 간격을 두고 쿼터 차단 없이 안전하게 vector_store에 100% 임베딩을 채워 넣는다.
 */
@SpringBootTest
@ActiveProfiles("local")
class GuideEmbeddingSyncTest {

    private static final Logger log = LoggerFactory.getLogger(GuideEmbeddingSyncTest.class);

    @Autowired
    private GuideRepository guideRepository;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("DB guide 테이블의 누락된 가이드 문서를 탐색하여 vector_store에 1.5초 간격으로 안전하게 임베딩 적재한다")
    void syncUnembeddedGuides() {
        log.info("=== JUnit 유틸리티: RAG VectorStore 안전 임베딩 동기화 시작 ===");
        List<Guide> guides = guideRepository.findAll();
        TokenTextSplitter splitter = new TokenTextSplitter();
        int successCount = 0;

        for (Guide guide : guides) {
            // 이미 vector_store에 존재하는 guideSeq이면 스킵
            String checkSql = "SELECT COUNT(*) FROM vector_store WHERE (metadata->>'guideSeq')::bigint = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, guide.getGuideSeq());
            if (count != null && count > 0) {
                log.info("가이드 [Seq: {}, Title: {}] 이미 vector_store에 존재함 (스킵)", guide.getGuideSeq(), guide.getTitle());
                continue;
            }

            // 429 쿼터 한도 초과 방지를 위한 3회 재시도 및 1.5초 딜레이 슬립 적용
            boolean success = false;
            int retries = 0;
            while (!success && retries < 3) {
                try {
                    Document doc = new Document(guide.getContent(), Map.of(
                            "guideSeq", guide.getGuideSeq(),
                            "userSeq", guide.getUserSeq(),
                            "title", guide.getTitle(),
                            "isPublic", guide.getIsPublic()
                    ));

                    List<Document> chunks = splitter.split(List.of(doc));
                    vectorStore.add(chunks);
                    success = true;
                    successCount++;
                    log.info("가이드 [Seq: {}, Title: {}] 임베딩 적재 성공 (생성된 청크: {}개)",
                            guide.getGuideSeq(), guide.getTitle(), chunks.size());

                    // Google Gemini Free Tier 쿼터 한도 보호를 위해 1.5초 대기
                    Thread.sleep(1500);
                } catch (Exception e) {
                    retries++;
                    log.warn("가이드 [Seq: {}] 임베딩 시도 중 쿼터 지연 발생, 5초 후 재시도 ({}/3) - {}",
                            guide.getGuideSeq(), retries, e.getMessage());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) {}
                }
            }
        }
        log.info("=== JUnit 유틸리티: RAG VectorStore 임베딩 동기화 완료 (총 {}건 신규 적재) ===", successCount);
    }
}
