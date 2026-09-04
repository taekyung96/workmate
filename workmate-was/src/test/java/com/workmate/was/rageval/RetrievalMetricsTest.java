package com.workmate.was.rageval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RetrievalMetricsTest {

    @Test
    @DisplayName("Hit·MRR·Miss 를 문항 집합에 대해 집계한다")
    void computes_aggregate() {
        // 1) 1위에 정답 → hit, rr=1.0
        var c1 = new RetrievalMetrics.EvalCase(List.of("RAG란 무엇인가", "Docker vs Kubernetes"), Set.of("RAG란 무엇인가"));
        // 2) 2위에 정답 → hit, rr=0.5
        var c2 = new RetrievalMetrics.EvalCase(List.of("Docker vs Kubernetes", "RAG란 무엇인가"), Set.of("RAG란 무엇인가"));
        // 3) 빈 결과 → miss, hit 아님, rr=0
        var c3 = new RetrievalMetrics.EvalCase(List.of(), Set.of("LangChain 개요"));

        RetrievalMetrics.ComboMetrics m = RetrievalMetrics.compute(List.of(c1, c2, c3));

        assertThat(m.total()).isEqualTo(3);
        assertThat(m.hitRate()).isCloseTo(2.0 / 3, within(1e-9));  // 2/3 hit
        assertThat(m.mrr()).isCloseTo((1.0 + 0.5 + 0.0) / 3, within(1e-9));
        assertThat(m.missRate()).isCloseTo(1.0 / 3, within(1e-9));
    }

    @Test
    @DisplayName("빈 입력은 0 으로 방어한다")
    void empty_input_is_zero() {
        RetrievalMetrics.ComboMetrics m = RetrievalMetrics.compute(List.of());
        assertThat(m.total()).isZero();
        assertThat(m.hitRate()).isZero();
    }

    @Test
    @DisplayName("답이 없는 질문에 근거를 돌려주면 오탐으로 센다")
    void counts_false_positives_on_negative_queries() {
        // 4문항 중 3문항이 근거를 물고 왔다 → 오탐률 75%
        var returned = List.of(
                List.of("무관한 가이드 A", "무관한 가이드 B"),
                List.of("무관한 가이드 C"),
                List.<String>of(),
                List.of("무관한 가이드 D"));

        var m = RetrievalMetrics.computeNegative(returned);

        assertThat(m.total()).isEqualTo(4);
        assertThat(m.falsePositiveRate()).isEqualTo(0.75);
        // 반환 건수 합 4건 / 4문항
        assertThat(m.avgReturned()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("답이 없는 질문에 아무것도 안 돌려주면 오탐이 0이다")
    void no_false_positive_when_nothing_returned() {
        var m = RetrievalMetrics.computeNegative(List.of(List.<String>of(), List.<String>of()));

        assertThat(m.falsePositiveRate()).isZero();
        assertThat(m.avgReturned()).isZero();
        assertThat(m.total()).isEqualTo(2);
    }

    @Test
    @DisplayName("오탐 입력이 비면 전부 0이다")
    void empty_negative_input_is_all_zero() {
        var m = RetrievalMetrics.computeNegative(List.of());

        assertThat(m.falsePositiveRate()).isZero();
        assertThat(m.avgReturned()).isZero();
        assertThat(m.total()).isZero();
    }
}
