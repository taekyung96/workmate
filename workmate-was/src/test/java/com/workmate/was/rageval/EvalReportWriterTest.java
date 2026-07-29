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
        var result = new EvalReportWriter.SweepResult(4, 0.4, metrics);
        var meta = new EvalReportWriter.CorpusMeta(18, 20, LocalDate.of(2026, 7, 29));

        String md = new EvalReportWriter().render(List.of(result), meta);

        assertThat(md).contains("가이드 개수: 18");
        assertThat(md).contains("평가 문항 수: 20");
        assertThat(md).contains("| topK | threshold | Hit@K | MRR | Miss rate |");
        // 4, 0.40, 80.0%, 0.650, 10.0%
        assertThat(md).contains("| 4 | 0.40 | 80.0% | 0.650 | 10.0% |");
    }
}
