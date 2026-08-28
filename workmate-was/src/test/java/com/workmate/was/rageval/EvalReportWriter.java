package com.workmate.was.rageval;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * 스윕 결과를 마크다운 리포트로 렌더하고 파일로 저장한다.
 * render(순수)와 write(IO)를 분리해 렌더는 결정적으로 테스트 가능하게 한다.
 */
public class EvalReportWriter {

    /**
     * 한 (topK,threshold) 조합의 결과 행.
     *
     * @param topK             상위 K
     * @param threshold        최소 유사도 임계값
     * @param metrics          검색 품질 지표
     * @param avgContextChars  문항당 평균 RAG 자료 블록 길이(문자) — 프롬프트 비용에 비례한다.
     *                         실제 전송값과 어긋나지 않도록 프로덕션 {@code RagPromptBuilder} 로 조립해 잰다.
     */
    public record SweepResult(int topK, double threshold, RetrievalMetrics.ComboMetrics metrics,
                              double avgContextChars) {
    }

    /**
     * 리포트 상단 코퍼스 메타.
     */
    public record CorpusMeta(int guideCount, int queryCount, LocalDate runDate) {
    }

    /**
     * 마크다운 문자열 생성 (파일 IO 없음).
     *
     * @param results 스윕 결과 리스트
     * @param meta 코퍼스 메타데이터
     * @return 마크다운 문자열
     */
    public String render(List<SweepResult> results, CorpusMeta meta) {
        StringBuilder sb = new StringBuilder();
        sb.append("# RAG 검색 품질 평가 리포트\n\n");
        sb.append("- 실행일: ").append(meta.runDate()).append("\n");
        sb.append("- 가이드 개수: ").append(meta.guideCount()).append("\n");
        sb.append("- 평가 문항 수: ").append(meta.queryCount()).append("\n\n");
        sb.append("| topK | threshold | Hit@K | MRR | Miss rate | 평균 컨텍스트(자) |\n");
        sb.append("| ---: | ---: | ---: | ---: | ---: | ---: |\n");
        for (SweepResult r : results) {
            // Locale.ROOT 로 소수점(.) 고정 — 지역설정이 콤마여도 표가 깨지지 않게
            sb.append(String.format(Locale.ROOT, "| %d | %.2f | %.1f%% | %.3f | %.1f%% | %,.0f |\n",
                    r.topK(), r.threshold(),
                    r.metrics().hitRate() * 100, r.metrics().mrr(), r.metrics().missRate() * 100,
                    r.avgContextChars()));
        }
        return sb.toString();
    }

    /**
     * 렌더된 마크다운을 `dir/REPORT-<date>.md` 로 저장한다.
     *
     * @param markdown 렌더된 마크다운 문자열
     * @param dir 저장할 디렉토리
     * @param date 리포트 날짜 (파일명에 포함)
     * @return 저장된 파일 경로
     * @throws IOException 파일 저장 중 IO 예외
     */
    public Path write(String markdown, Path dir, LocalDate date) throws IOException {
        Files.createDirectories(dir);
        Path file = dir.resolve("REPORT-" + date + ".md");
        Files.writeString(file, markdown);
        return file;
    }
}
