package com.workmate.was.rageval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workmate.was.guide.dao.GuideRepository;
import com.workmate.was.guide.service.GuideService;
import com.workmate.was.guide.vo.Guide;
import com.workmate.was.guide.vo.GuideSaveRequestVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * [수동 실행] AI 개발자 면접 가이드 코퍼스를 dev DB 에 시딩한다.
 * 실제 생성 경로(GuideService.createGuide)로 넣어 vector_store 임베딩까지 함께 적재한다.
 * 이미 같은 제목이 있으면 건너뛰므로(idempotent) 재실행해도 중복 생성되지 않는다.
 *
 * 실행: ./gradlew :workmate-was:seedGuides   (GEMINI_API_KEY + 실행 중인 dev DB 필요)
 */
@Tag("seed-guides")
@SpringBootTest
class GuideCorpusSeeder {

    /** 시드 문서를 소유할 사용자 seq (dev DB 에 존재하는 계정) */
    private static final Long OWNER_SEQ = 1L;

    /** interview-guides.json 한 항목 */
    private record SeedGuide(String title, String content, boolean isPublic) {
    }

    @Autowired
    private GuideService guideService;
    @Autowired
    private GuideRepository guideRepository;

    @Test
    @DisplayName("면접 가이드 코퍼스를 등록(+임베딩)한다")
    void seed() throws Exception {
        List<SeedGuide> seeds;
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("rageval/interview-guides.json")) {
            if (in == null) {
                throw new IllegalStateException("interview-guides.json 리소스를 찾을 수 없습니다.");
            }
            seeds = new ObjectMapper().readValue(in, new TypeReference<List<SeedGuide>>() {});
        }

        // 이미 등록된 제목은 재등록하지 않는다(중복 임베딩 방지)
        Set<String> existing = guideRepository.findAll().stream()
                .map(Guide::getTitle)
                .collect(Collectors.toSet());

        int created = 0, skipped = 0;
        for (SeedGuide s : seeds) {
            if (existing.contains(s.title())) {
                skipped++;
                continue;
            }
            guideService.createGuide(OWNER_SEQ, GuideSaveRequestVo.builder()
                    .title(s.title())
                    .content(s.content())
                    .isPublic(s.isPublic())
                    .build());
            created++;
        }
        System.out.println("면접 가이드 시딩 완료 — 생성: " + created + ", 건너뜀(이미 존재): " + skipped);
    }
}
