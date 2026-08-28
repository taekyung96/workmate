package com.workmate.was.guide.service.impl;

import com.workmate.was.guide.dao.GuideRepository;
import com.workmate.was.guide.service.GuideService;
import com.workmate.was.guide.vo.Guide;
import com.workmate.was.guide.vo.GuidePageVo;
import com.workmate.was.guide.vo.GuideResponseVo;
import com.workmate.was.guide.vo.GuideSaveRequestVo;
import com.workmate.was.guide.vo.GuideSummaryVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 가이드 문서 처리 및 벡터 스토어 RAG 적재 서비스 구현체.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuideServiceImpl implements GuideService {

    private final GuideRepository guideRepository;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 새로운 가이드 문서를 등록하고 본문을 청크 분할하여 벡터 스토어에 적재한다.
     *
     * @param userSeq 작성자 식별자
     * @param request 등록 요청 정보
     * @return 등록 완료된 가이드 문서 정보 VO
     */
    @Override
    @Transactional
    public GuideResponseVo createGuide(Long userSeq, GuideSaveRequestVo request) {
        log.info("가이드 문서 등록 요청. Title: {}, UserSeq: {}", request.getTitle(), userSeq);

        Guide guide = Guide.builder()
                .userSeq(userSeq)
                .title(request.getTitle())
                .content(request.getContent())
                .isPublic(request.getIsPublic())
                .build();

        Guide saved = guideRepository.save(guide);

        // 벡터 스토어 임베딩 적재
        saveEmbeddings(saved);

        return new GuideResponseVo(saved);
    }

    /**
     * 기존 가이드 문서를 수정하고, 벡터 스토어의 기존 임베딩을 지운 뒤 새로 적재한다.
     *
     * @param guideSeq 가이드 문서 식별자
     * @param request 수정 요청 정보
     * @return 수정 완료된 가이드 문서 정보 VO
     */
    @Override
    @Transactional
    public GuideResponseVo updateGuide(Long userSeq, boolean isAdmin, Long guideSeq, GuideSaveRequestVo request) {
        log.info("가이드 문서 수정 요청. GuideSeq: {}, UserSeq: {}, isAdmin: {}", guideSeq, userSeq, isAdmin);

        Guide guide = findEditableGuide(userSeq, isAdmin, guideSeq);

        // 본문 변경 여부는 반드시 update() 로 덮어쓰기 '전에' 판단한다.
        boolean contentChanged = !Objects.equals(guide.getContent(), request.getContent());

        guide.update(request.getTitle(), request.getContent(), request.getIsPublic());

        if (contentChanged) {
            // 본문이 바뀌었을 때만 재임베딩한다 (선삭제 → 후적재)
            deleteEmbeddings(guideSeq);
            saveEmbeddings(guide);
        } else if (updateEmbeddingMetadata(guide) == 0) {
            // 본문이 그대로면 임베딩 API 를 부르지 않는다 — title·isPublic 은 청크 메타데이터라
            // 벡터에 영향을 주지 않기 때문이다. 무료 티어 RPM 한도가 낮아 불필요한 재임베딩이
            // 곧 429 로 이어진다(docs/architecture/RAG_VECTORSTORE_EMBEDDING_QUOTA_GUIDE.md).
            // 다만 갱신된 청크가 0건이면 과거 적재가 실패해 벡터가 비어 있다는 뜻이므로 이때는 적재한다.
            log.info("갱신할 임베딩 청크가 없어 신규 적재로 전환한다. GuideSeq: {}", guideSeq);
            saveEmbeddings(guide);
        }

        return new GuideResponseVo(guide);
    }

    /**
     * 가이드 문서를 삭제하고 벡터 스토어에 적재된 관련 임베딩 청크들도 모두 삭제한다.
     *
     * @param guideSeq 가이드 문서 식별자
     */
    @Override
    @Transactional
    public void deleteGuide(Long userSeq, boolean isAdmin, Long guideSeq) {
        log.info("가이드 문서 삭제 요청. GuideSeq: {}, UserSeq: {}, isAdmin: {}", guideSeq, userSeq, isAdmin);

        Guide guide = findEditableGuide(userSeq, isAdmin, guideSeq);

        // 벡터 스토어 임베딩 데이터 제거
        deleteEmbeddings(guideSeq);

        // 엔티티 삭제
        guideRepository.delete(guide);
    }

    /**
     * 가이드 문서 상세 단건 조회.
     *
     * @param guideSeq 가이드 문서 식별자
     * @return 가이드 문서 정보 VO
     */
    @Override
    @Transactional(readOnly = true)
    public GuideResponseVo getGuide(Long userSeq, Long guideSeq) {
        Guide guide = guideRepository.findById(guideSeq)
                .orElseThrow(() -> new IllegalArgumentException("해당 가이드 문서를 찾을 수 없습니다. ID: " + guideSeq));
        // 접근 제어: 비공개 문서는 본인만 열람 가능 (F4-08)
        if (!Boolean.TRUE.equals(guide.getIsPublic()) && !guide.getUserSeq().equals(userSeq)) {
            throw new IllegalArgumentException("해당 가이드 문서에 접근할 수 없습니다.");
        }
        return new GuideResponseVo(guide);
    }

    /**
     * 접근 가능한 가이드(본인 + 공개)를 키워드로 검색해 페이징 조회한다 (G1, F4-08).
     * 단일 OR 쿼리로 조회하므로 이전처럼 두 목록을 Java 에서 병합·중복제거할 필요가 없다.
     *
     * @param userSeq 요청 사용자 식별자
     * @param keyword 검색어 (null·공백이면 전체)
     * @param page    0-based 페이지 번호 (null 이면 전체 조회)
     * @param size    페이지 크기 (null 이면 전체 조회)
     * @return 가이드 목록 페이지 VO (카드 표시용 요약 + 페이징 메타)
     */
    @Override
    @Transactional(readOnly = true)
    public GuidePageVo searchGuides(Long userSeq, String keyword, Integer page, Integer size) {
        // 최신 수정순 정렬. page·size 가 모두 오면 그 값으로 페이징하고,
        // 하나라도 없으면 전체를 한 페이지로 담아 반환한다(영수증·회의록 이력과 동일 정책).
        Sort sort = Sort.by(Sort.Direction.DESC, "updatedAt");
        Pageable pageable = (page != null && size != null)
                ? PageRequest.of(page, size, sort)
                : Pageable.unpaged(sort);
        // 키워드가 비면 빈 문자열("")로 넘겨 JPQL 의 ':keyword = '' ' 분기(전체 조회)를 타게 한다.
        // null 을 넘기면 PostgreSQL 이 파라미터 타입을 bytea 로 추론해 lower(bytea) 오류가 나므로 주의.
        String kw = (keyword == null || keyword.isBlank()) ? "" : keyword.trim();

        Page<Guide> result = guideRepository.searchAccessible(userSeq, kw, pageable);
        return GuidePageVo.builder()
                .content(result.getContent().stream().map(this::toSummary).toList())
                .page(result.getNumber())
                .totalPages(result.getTotalPages())
                .totalElements(result.getTotalElements())
                .build();
    }

    /** 목록 카드용 요약 VO 변환 — 본문은 미리보기(excerpt)로 축약해 담는다 */
    private GuideSummaryVo toSummary(Guide guide) {
        return GuideSummaryVo.builder()
                .guideSeq(guide.getGuideSeq())
                .title(guide.getTitle())
                .excerpt(buildExcerpt(guide.getContent()))
                .isPublic(guide.getIsPublic())
                .updatedAt(guide.getUpdatedAt())
                .build();
    }

    /** 미리보기 길이 — 카드 2줄 분량 */
    private static final int EXCERPT_LENGTH = 120;

    /**
     * 마크다운 본문에서 카드 미리보기용 평문을 만든다.
     * 링크는 표시 텍스트만 남기고, 헤더·강조·코드·인용·리스트 기호와 연속 공백을 정리한 뒤 앞부분만 자른다.
     *
     * @param content 마크다운 본문
     * @return 정리된 미리보기 문자열 (길이 초과 시 말줄임표 추가)
     */
    private String buildExcerpt(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String plain = content
                .replaceAll("!?\\[([^\\]]*)\\]\\([^)]*\\)", "$1") // 이미지/링크 → 표시 텍스트만
                .replaceAll("(?m)^\\s{0,3}#{1,6}\\s*", "")          // 헤더 기호
                .replaceAll("(?m)^\\s*[-*+>]\\s+", "")               // 리스트·인용 접두 기호
                .replaceAll("[*_`~#>]", "")                          // 인라인 강조/코드 기호
                .replaceAll("\\s+", " ")                              // 연속 공백·줄바꿈 축약
                .trim();
        return plain.length() <= EXCERPT_LENGTH ? plain : plain.substring(0, EXCERPT_LENGTH).trim() + "…";
    }

    /**
     * 수정·삭제 대상 가이드를 조회한다. 없으면 예외 (F4-01).
     * 일반 사용자는 본인 문서만, 관리자(isAdmin)는 타인 문서도 허용한다.
     */
    private Guide findEditableGuide(Long userSeq, boolean isAdmin, Long guideSeq) {
        Guide guide = guideRepository.findById(guideSeq)
                .orElseThrow(() -> new IllegalArgumentException("해당 가이드 문서를 찾을 수 없습니다. ID: " + guideSeq));
        // 관리자는 모든 문서를 수정·삭제할 수 있고, 일반 사용자는 본인 문서만 가능하다.
        if (!isAdmin && !guide.getUserSeq().equals(userSeq)) {
            throw new IllegalArgumentException("본인의 가이드 문서만 수정·삭제할 수 있습니다.");
        }
        return guide;
    }

    /**
     * 가이드 문서를 청크로 쪼개어 임베딩을 생성한 후 벡터 스토어에 저장한다.
     */
    private void saveEmbeddings(Guide guide) {
        try {
            log.info("가이드 문서 본문 청크 분할 및 임베딩 생성 시작 (GuideSeq: {})", guide.getGuideSeq());

            // 기본 설정의 토큰 기반 텍스트 스플리터 생성
            TokenTextSplitter splitter = new TokenTextSplitter();

            // 문서 본문 생성 및 메타데이터 추가 (userSeq 는 RAG 검색 시 본인·공개 필터에 사용, F4-08)
            Document doc = new Document(guide.getContent(), Map.of(
                    "guideSeq", guide.getGuideSeq(),
                    "userSeq", guide.getUserSeq(),
                    "title", guide.getTitle(),
                    "isPublic", guide.getIsPublic()
            ));

            List<Document> chunks = splitter.split(List.of(doc));
            log.info("문서 분할 완료. 생성된 청크 수: {}개", chunks.size());

            // 벡터 스토어에 적재 (EmbeddingModel 호출 및 DB 저장 자동 수행)
            vectorStore.add(chunks);
            log.info("벡터 스토어 임베딩 적재 완료. (GuideSeq: {})", guide.getGuideSeq());

        } catch (Exception e) {
            log.error("벡터 스토어 임베딩 적재 실패 (GuideSeq: {}): {}", guide.getGuideSeq(), e.getMessage(), e);
            // 비즈니스 트랜잭션 전체가 롤백되지 않도록 선택할 수도 있으나,
            // 지식 검색의 데이터 정합성을 위해 런타임 예외로 위임하여 롤백 처리
            throw new RuntimeException("RAG 벡터 DB 적재에 실패했습니다. 가이드 문서 저장을 취소합니다.", e);
        }
    }

    /**
     * 벡터 스토어 테이블에서 특정 가이드 문서에 속한 모든 청크를 직접 제거한다.
     */
    /**
     * 본문이 그대로일 때 쓰는 <b>메타데이터 전용</b> 갱신 — 임베딩 API 를 호출하지 않는다.
     *
     * <p>{@code title}·{@code isPublic} 은 청크 메타데이터일 뿐 임베딩 벡터에 반영되지 않으므로,
     * 이 둘만 바뀐 수정에서 벡터를 다시 만드는 것은 순수한 낭비다(429 쿼터 소모의 주원인).
     * jsonb 병합(||)으로 해당 키만 덮어써 본문 청크와 벡터는 그대로 둔다.
     *
     * @param guide 수정된 가이드 문서
     * @return 갱신된 청크 수 (0이면 적재된 임베딩이 없다는 뜻)
     */
    private int updateEmbeddingMetadata(Guide guide) {
        try {
            String sql = "UPDATE vector_store"
                    + " SET metadata = metadata || jsonb_build_object('title', ?::text, 'isPublic', ?::boolean)"
                    + " WHERE (metadata->>'guideSeq')::bigint = ?";
            int rows = jdbcTemplate.update(sql, guide.getTitle(), guide.getIsPublic(), guide.getGuideSeq());
            log.info("본문 무변경 — 임베딩 없이 메타데이터만 갱신했다. 갱신 청크 수: {}개 (GuideSeq: {})",
                    rows, guide.getGuideSeq());
            return rows;
        } catch (Exception e) {
            log.error("벡터 스토어 메타데이터 갱신 실패 (GuideSeq: {}): {}", guide.getGuideSeq(), e.getMessage(), e);
            throw new RuntimeException("RAG 벡터 DB 메타데이터 갱신에 실패했습니다.", e);
        }
    }

    private void deleteEmbeddings(Long guideSeq) {
        try {
            log.info("벡터 스토어 내 기존 청크 삭제 시작 (GuideSeq: {})", guideSeq);
            // jsonb 타입의 metadata 필드 내 guideSeq 값을 참조하여 매칭되는 레코드 제거
            String sql = "DELETE FROM vector_store WHERE (metadata->>'guideSeq')::bigint = ?";
            int rows = jdbcTemplate.update(sql, guideSeq);
            log.info("벡터 스토어 기존 청크 삭제 완료. 삭제된 청크 수: {}개", rows);
        } catch (Exception e) {
            log.error("벡터 스토어 청크 삭제 실패 (GuideSeq: {}): {}", guideSeq, e.getMessage(), e);
            throw new RuntimeException("RAG 벡터 DB 기존 데이터 갱신에 실패했습니다.", e);
        }
    }
}
