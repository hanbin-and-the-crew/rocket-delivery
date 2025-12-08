package org.sparta.payment.presentation;

import lombok.extern.slf4j.Slf4j;
import org.sparta.common.api.ApiResponse;
import org.sparta.common.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "org.sparta.payment")
public class PaymentExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex) {
        log.warn("[BusinessException] type={}, message={}", ex.getErrorType(), ex.getMessage());

        String errorCode = ex.getErrorType().getCode();
        String message = (ex.getMessage() != null)
                ? ex.getMessage()
                : ex.getErrorType().getMessage();

        ApiResponse<Object> body = ApiResponse.fail(errorCode, message);
        return ResponseEntity.status(ex.getErrorType().getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다.");

        log.warn("[ValidationException] {}", message);

        ApiResponse<Object> body = ApiResponse.fail("VALIDATION_ERROR", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {
        log.error("[UnexpectedException]", ex);

        ApiResponse<Object> body = ApiResponse.fail(
                "INTERNAL_ERROR",
                "알 수 없는 오류가 발생했습니다."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
