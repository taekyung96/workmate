package com.workmate.was.rageval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * classpath 의 골든셋 JSON 을 로드한다.
 */
public class GoldenSetLoader {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * @param classpathResource 예: "rageval/queries.json"
     * @return 파싱된 문항 리스트
     */
    public List<EvalQuery> load(String classpathResource) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalArgumentException("골든셋 리소스를 찾을 수 없습니다: " + classpathResource);
            }
            return mapper.readValue(in, new TypeReference<List<EvalQuery>>() {});
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
