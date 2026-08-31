package com.workmate.was.global.logging;

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
 * 요청 추적 ID 필터 (F-OBS) — WEB 이 붙여 보낸 ID 를 이어받아 WAS 로그에도 같은 값을 찍는다.
 *
 * <p>WAS 는 내부망이라 정상 경로에서는 항상 헤더가 온다. 헤더가 없으면(테스트·직접 호출)
 * 새로 만들어 최소한 요청 단위 묶음은 유지한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    /** 요청 추적 ID 헤더 — WEB 이 만들어 전파한다 */
    public static final String HEADER = "X-Request-Id";

    /** MDC 키 — 로그 패턴(logging.pattern.level)에서 %X{requestId} 로 참조한다 */
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader(HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString().substring(0, 8);
        }
        MDC.put(MDC_KEY, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
