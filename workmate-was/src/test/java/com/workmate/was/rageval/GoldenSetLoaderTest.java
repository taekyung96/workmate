package com.workmate.was.rageval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoldenSetLoaderTest {

    @Test
    @DisplayName("classpath JSON 을 EvalQuery 리스트로 로드한다")
    void loads_json() {
        List<EvalQuery> queries = new GoldenSetLoader().load("rageval/sample-queries.json");

        assertThat(queries).hasSize(2);
        assertThat(queries.get(0).question()).isEqualTo("도커랑 쿠버네티스 차이가 뭐야?");
        assertThat(queries.get(0).expectedTitles()).containsExactly("Docker vs Kubernetes");
        assertThat(queries.get(1).expectedTitles()).hasSize(2);
    }

    @Test
    @DisplayName("리소스가 없으면 IllegalArgumentException")
    void missing_resource_throws() {
        assertThatThrownBy(() -> new GoldenSetLoader().load("rageval/none.json"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("실제 골든셋도 파싱되고 15문항 이상이다")
    void real_golden_set_parses() {
        List<EvalQuery> queries = new GoldenSetLoader().load("rageval/queries.json");
        assertThat(queries).hasSizeGreaterThanOrEqualTo(15);
        assertThat(queries).allSatisfy(q -> {
            assertThat(q.question()).isNotBlank();
            assertThat(q.expectedTitles()).isNotEmpty();
        });
    }
}
