package com.workmate.was.rageval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvalReportWriterTest {

    @Test
    @DisplayName("스윕 결과를 마크다운 표로 렌더한다")
    void renders_markdown_table() {
        var metrics = new RetrievalMetrics.ComboMetrics(0.8, 0.65, 0.1, 20);
        var result = new EvalReportWriter.SweepResult(4, 0.4, metrics, 1234.5);
        var meta = new EvalReportWriter.CorpusMeta(18, 20, LocalDate.of(2026, 7, 29));

        String md = new EvalReportWriter().render(List.of(result), meta);

        assertThat(md).contains("가이드 개수: 18");
        assertThat(md).contains("평가 문항 수: 20");
        assertThat(md).contains("| topK | threshold | Hit@K | MRR | Miss rate | 평균 컨텍스트(자) |");
        // 4, 0.40, 80.0%, 0.650, 10.0%, 1,235
        assertThat(md).contains("| 4 | 0.40 | 80.0% | 0.650 | 10.0% | 1,235 |");
    }

    @Test
    @DisplayName("컨텍스트 크기는 천 단위 구분으로 읽기 쉽게 낸다")
    void formats_context_size_with_thousands_separator() {
        var metrics = new RetrievalMetrics.ComboMetrics(1.0, 1.0, 0.0, 33);
        var result = new EvalReportWriter.SweepResult(8, 0.4, metrics, 9876.4);
        var meta = new EvalReportWriter.CorpusMeta(34, 33, LocalDate.of(2026, 8, 28));

        String md = new EvalReportWriter().render(List.of(result), meta);

        assertThat(md).contains("| 8 | 0.40 | 100.0% | 1.000 | 0.0% | 9,876 |");
    }
}
