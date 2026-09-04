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
     * 오탐 스윕 결과 한 줄 — 코퍼스에 답이 없는 질문들을 같은 파라미터로 돌린 결과.
     *
     * @param topK      상위 K
     * @param threshold 최소 유사도 임계값
     * @param metrics   오탐 지표
     */
    public record NegativeSweepResult(int topK, double threshold,
                                      RetrievalMetrics.NegativeMetrics metrics) {
    }

    /**
     * 마크다운 문자열 생성 (파일 IO 없음).
     *
     * @param results 스윕 결과 리스트
     * @param meta 코퍼스 메타데이터
     * @return 마크다운 문자열
     */
    public String render(List<SweepResult> results, CorpusMeta meta) {
        return render(results, List.of(), meta);
    }

    /**
     * 마크다운 문자열 생성 — 오탐 절을 함께 낸다.
     *
     * <p>표를 하나로 합치지 않고 절을 나눈 이유: 위 표는 리포트끼리 비교하는 기준이라
     * 열 구성이 바뀌면 예전 리포트와 나란히 읽기 어려워진다. 오탐은 뒤에 덧붙인다.
     *
     * @param results   정답이 있는 문항의 스윕 결과
     * @param negatives 답이 없는 문항의 스윕 결과 (비어 있으면 절을 만들지 않는다)
     * @param meta      코퍼스 메타데이터
     * @return 마크다운 문자열
     */
    public String render(List<SweepResult> results, List<NegativeSweepResult> negatives,
                         CorpusMeta meta) {
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
        appendNegativeSection(sb, negatives);
        return sb.toString();
    }

    /**
     * 오탐 절을 덧붙인다 — "답이 없는 질문에 근거를 몇 건이나 내놓는가".
     *
     * <p>Hit@K·MRR 만으로는 이 축을 못 본다. 정답이 있는 질문만 재기 때문이다.
     * 임계값을 올리면 오탐률은 내려가고 재현율(Hit@K)은 깎이므로, 두 표를 함께 봐야
     * 임계값을 어디에 둘지 판단할 수 있다.
     *
     * @param sb        누적 중인 마크다운
     * @param negatives 오탐 스윕 결과 (비어 있으면 아무것도 하지 않는다)
     */
    private void appendNegativeSection(StringBuilder sb, List<NegativeSweepResult> negatives) {
        if (negatives.isEmpty()) {
            return;
        }
        sb.append("\n\n## 오탐 — 코퍼스에 답이 없는 질문\n\n");
        sb.append("정답이 코퍼스에 **없는** 질문 ").append(negatives.get(0).metrics().total())
                .append("건을 같은 파라미터로 돌린 결과다. 근거를 한 건이라도 돌려주면 화면에 문서 목록이 붙으므로, ")
                .append("답변은 자료에 없다고 말하는데 목록만 딸려 나오는 상태가 된다.\n\n");
        sb.append("| topK | threshold | 오탐률 | 평균 반환 건수 |\n");
        sb.append("| ---: | ---: | ---: | ---: |\n");
        for (NegativeSweepResult n : negatives) {
            sb.append(String.format(Locale.ROOT, "| %d | %.2f | %.1f%% | %.2f |\n",
                    n.topK(), n.threshold(),
                    n.metrics().falsePositiveRate() * 100, n.metrics().avgReturned()));
        }
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
