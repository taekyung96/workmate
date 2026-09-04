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
     * 코퍼스에 답이 없는 질문에 대한 집계 지표 — <b>오탐(false positive)</b>을 잰다.
     *
     * <p>Hit@K·MRR 은 "정답이 있는 질문"만 다루므로 반대편을 못 본다. 실제로 겪은 문제가
     * 그쪽이었다 — 사내 가이드에 없는 질문("연차 휴가 며칠?")에 임계값을 넘긴 무관한 청크가
     * 4건 딸려 나왔고, 모델은 "자료에 없습니다"라고 답하는데 화면에는 출처가 붙었다.
     *
     * @param falsePositiveRate 근거를 한 건이라도 반환한 질문의 비율 (낮을수록 좋다)
     * @param avgReturned       질문당 평균 반환 청크 수 (0에 가까울수록 좋다)
     * @param total             평가한 질문 수
     */
    public record NegativeMetrics(double falsePositiveRate, double avgReturned, int total) {
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

    /**
     * 답이 없는 질문들에 대해 오탐을 집계한다.
     *
     * <p>여기서는 "무엇을 돌려줬는가"만 본다 — 정답 집합이 비어 있어 순위를 따질 대상이 없다.
     * 한 건이라도 돌려주면 화면에 출처가 붙으므로 그 자체가 오탐이다.
     *
     * @param returnedPerQuery 질문별 반환 청크 title 목록
     * @return 집계 지표 (빈 입력이면 전부 0)
     */
    public static NegativeMetrics computeNegative(List<List<String>> returnedPerQuery) {
        if (returnedPerQuery.isEmpty()) {
            return new NegativeMetrics(0, 0, 0);
        }
        int n = returnedPerQuery.size();
        double withAny = 0, returnedSum = 0;
        for (List<String> returned : returnedPerQuery) {
            if (!returned.isEmpty()) {
                withAny++;
            }
            returnedSum += returned.size();
        }
        return new NegativeMetrics(withAny / n, returnedSum / n, n);
    }
}
