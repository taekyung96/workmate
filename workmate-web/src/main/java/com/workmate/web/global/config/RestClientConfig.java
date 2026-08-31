package com.workmate.web.global.config;

import com.workmate.web.global.logging.RequestIdFilter;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * WEB → WAS 통신용 RestClient 설정.
 * WEB 레이어는 DB에 직접 접근하지 않고 이 클라이언트를 통해서만 WAS REST API 를 호출한다.
 */
@Configuration
@EnableConfigurationProperties(WasProperties.class)
public class RestClientConfig {

    /**
     * WAS 호출 전용 RestClient 빈.
     *
     * @param wasProperties WAS 베이스 URL 프로퍼티
     * @return WAS 베이스 URL 이 적용된 RestClient
     */
    @Bean
    public RestClient wasRestClient(WasProperties wasProperties) {
        return RestClient.builder()
                .baseUrl(wasProperties.getBaseUrl())
                .requestFactory(wasRequestFactory())
                // 로그인 사용자의 userSeq/role 을 모든 WAS 호출에 자동 주입 (04 문서 §1.4)
                // 브라우저는 userSeq 를 보내지 않는다 — 세션이 유일한 출처
                .requestInterceptor((request, body, execution) -> {
                    org.springframework.security.core.Authentication auth =
                            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && auth.getPrincipal() instanceof com.workmate.web.global.security.LoginUser loginUser) {
                        request.getHeaders().set("X-User-Seq", String.valueOf(loginUser.getUserSeq()));
                        request.getHeaders().set("X-User-Role", loginUser.getRole());
                    }
                    // 요청 추적 ID 전파 (F-OBS) — RestClient 는 블로킹이라 인터셉터가 요청 스레드에서
                    // 실행되므로 MDC 가 살아 있다. WAS 로 그대로 이어 보내 WEB·WAS 로그를 잇는다.
                    String requestId = MDC.get(RequestIdFilter.MDC_KEY);
                    if (requestId != null) {
                        request.getHeaders().set(RequestIdFilter.HEADER, requestId);
                    }
                    return execution.execute(request, body);
                })
                // WAS가 내려주는 4xx/5xx 에러 응답(ApiResponse JSON)을 예외로 바꾸지 않고
                // 상태코드·본문 그대로 화면(fetch)까지 통과시키기 위한 무동작 핸들러
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> { })
                .build();
    }

    /**
     * WAS 호출용 HTTP 요청 팩토리.
     * webflux(reactor-netty) 의존성이 클래스패스에 있으면 RestClient 기본 팩토리가 reactor-netty 가
     * 되는데, 기본 read 타임아웃이 짧아 영수증 OCR 분석 같은 느린 AI 호출이 ReadTimeoutException 으로
     * 실패한다. AI 호출 대기 시간을 고려해 read 타임아웃을 넉넉히(60초) 설정한다.
     *
     * @return 타임아웃이 조정된 ClientHttpRequestFactory
     */
    private ClientHttpRequestFactory wasRequestFactory() {
        ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60));
        return factory;
    }
}
