package com.workmate.was.rageval;

import java.util.List;
import java.util.Set;

/**
 * 검색 품질 메트릭 계산 (순수 함수 — DB·Spring 의존 없음).
 * 메트릭은 retrieve() 가 이미 topK 로 잘라 반환한 **청크 title 시퀀스**로 계산한다.
 */
public final class RetrievalMetrics {

    private RetrievalMetrics() {
    }

    /**
     * 한 문항의 평가 입력.
     *
     * @param returnedTitles 검색 반환 청크의 title(유사도 순, 중복 가능)
     * @param expectedTitles 정답 title 집합
     */
    public record EvalCase(List<String> returnedTitles, Set<String> expectedTitles) {
    }

    /** 한 (topK,threshold) 조합의 집계 지표 */
    public record ComboMetrics(double hitRate, double mrr, double missRate, int total) {
    }

    /**
     * 문항들을 집계해 Hit@K·MRR·Miss rate 를 낸다.
     *
     * @param cases 문항별 반환/정답
     * @return 집계 지표 (빈 입력이면 전부 0)
     */
    public static ComboMetrics compute(List<EvalCase> cases) {
        if (cases.isEmpty()) {
            return new ComboMetrics(0, 0, 0, 0);
        }
        int n = cases.size();
        double hits = 0, rrSum = 0, misses = 0;
        for (EvalCase c : cases) {
            if (c.returnedTitles().isEmpty()) {
                misses++;
            }
            // 위에서부터 훑어 정답과 일치하는 첫 청크의 위치로 역순위(1/rank) 계산
            for (int i = 0; i < c.returnedTitles().size(); i++) {
                if (c.expectedTitles().contains(c.returnedTitles().get(i))) {
                    rrSum += 1.0 / (i + 1);
                    hits++;
                    break;
                }
            }
        }
        return new ComboMetrics(hits / n, rrSum / n, misses / n, n);
    }
}
