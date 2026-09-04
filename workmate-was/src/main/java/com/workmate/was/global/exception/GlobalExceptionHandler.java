package com.workmate.was.global.exception;

import com.workmate.was.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * WAS 전역 예외 핸들러.
 * 예외 타입별로 HTTP 상태코드를 구분해 공통 응답 포맷으로 반환한다.
 * - BusinessException: 409 (비즈니스 규칙 위반)
 * - IllegalArgumentException: 400 (입력값 오류)
 * - AuthenticationFailedException: 401 (인증 실패 — 자격 불일치)
 * - RateLimitExceededException: 429 (요청 제한 초과)
 * - NoResourceFound·NoHandlerFound: 404 (없는 경로)
 * - 그 외: 500 (서버 내부 오류 — 상세 메시지는 로그에만 남김)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        log.warn("비즈니스 예외 - {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("입력값 예외 - {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationFailed(AuthenticationFailedException e) {
        log.warn("인증 실패 - {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimit(RateLimitExceededException e) {
        log.warn("요청 제한 초과 - {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ApiResponse.error(e.getMessage()));
    }

    /**
     * 없는 경로 요청 — 404 로 답한다.
     *
     * <p>이 핸들러가 없으면 아래 {@code Exception} 캐치올이 삼켜 <b>500</b> 이 나간다.
     * 없는 주소를 부른 것은 서버 잘못이 아니라 요청 잘못이므로 404 가 맞다.
     *
     * <p>로그 수준을 낮춘 것도 같은 이유다. 캐치올은 {@code log.error} 로 스택트레이스까지
     * 남기는데, 스캐너나 오타 요청 하나하나가 ERROR 로 쌓이면 <b>진짜 오류가 묻힌다</b>.
     *
     * @param e 정적 리소스를 못 찾았거나(NoResourceFound) 매핑된 핸들러가 없을 때(NoHandlerFound)
     * @return 404 와 공통 에러 포맷
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception e) {
        log.debug("없는 경로 요청 - {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("요청한 경로를 찾을 수 없습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("서버 내부 오류", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("서버 내부 오류가 발생했습니다."));
    }
}
