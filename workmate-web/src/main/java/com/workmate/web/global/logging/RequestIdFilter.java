package com.workmate.web.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 요청 추적 ID 필터 (F-OBS) — 브라우저 요청 하나에 ID 를 붙여 WEB·WAS 로그를 잇는다.
 *
 * <p>WEB 이 시작점이다. 헤더가 없으면 새로 만들고, 있으면(리버스 프록시가 붙였을 수 있다)
 * 그대로 이어받는다. MDC 에 넣어두면 이 요청 스레드에서 찍히는 모든 로그에 자동으로 따라붙는다.
 *
 * <p>응답 헤더로도 돌려준다 — 사용자가 오류를 제보할 때 이 값만 알려주면 로그를 바로 찾을 수 있다.
 *
 * <p><b>주의</b>: MDC 는 ThreadLocal 이라 리액터 스레드로는 넘어가지 않는다.
 * SSE 중계(채팅)는 컨트롤러가 이 값을 읽어 파라미터로 넘긴다 — {@code X-User-Seq} 와 같은 방식이다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    /** 요청 추적 ID 헤더 — WEB 이 만들고 WAS 로 전파한다 */
    public static final String HEADER = "X-Request-Id";

    /** MDC 키 — 로그 패턴(logging.pattern.level)에서 %X{requestId} 로 참조한다 */
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader(HEADER);
        if (requestId == null || requestId.isBlank()) {
            // 앞단 프록시가 안 붙였으면 여기서 만든다. 로그에서 눈으로 좇을 수 있게 8자로 줄인다
            requestId = UUID.randomUUID().toString().substring(0, 8);
        }
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 스레드는 풀에서 재사용되므로 반드시 지운다 — 안 지우면 다음 요청 로그에 남의 ID 가 찍힌다
            MDC.remove(MDC_KEY);
        }
    }
}
