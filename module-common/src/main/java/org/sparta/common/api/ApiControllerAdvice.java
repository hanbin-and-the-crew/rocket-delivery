package org.sparta.common.api;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.common.error.CommonErrorType;
import org.sparta.common.error.ErrorType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.time.DateTimeException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;

/**
 * Spring 애플리케이션에서 발생하는 모든 예외를 한 곳에서 잡아서 통일된 JSON 응답으로 변환하는 전역 예외 처리기입니다.
 * ---------
 * 처리하는 예외 종류
 * BusinessException, @Valid 검증 실패, 타입 불일치, 헤더 누락, JSON 파싱 실패, 404 에러, 기타 모든 예외
 */
@Slf4j
@RestControllerAdvice
public class ApiControllerAdvice {

    private static final Pattern PARAMETER_PATTERN = Pattern.compile("'(.+?)'");
    private static final List<String> SENSITIVE_FIELDS = List.of("password", "token", "secret", "auth");
    private static final int MAX_VALUE_LENGTH = 100;

    @ExceptionHandler
    public ResponseEntity<ApiResponse<?>> handle(BusinessException e) {
        log.warn("BusinessException : {}", e.getCustomMessage() != null ? e.getCustomMessage() : e.getMessage(), e);
        return failureResponse(e.getErrorType(), e.getCustomMessage());
    }

    @ExceptionHandler
    public ResponseEntity<ApiResponse<?>> handleBadRequest(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();

        if (fieldErrors.isEmpty()) {
            return failureResponse(CommonErrorType.INVALID, "유효성 검증에 실패했습니다.");
        }

        FieldError fieldError = fieldErrors.get(0);
        String name = fieldError.getField();
        String value = sanitizeValue(name, fieldError.getRejectedValue());
        String message = String.format("요청 파라미터 '%s'의 값 '%s'이(가) 잘못되었습니다.", name, value);

        return failureResponse(CommonErrorType.INVALID, message);
    }

    @ExceptionHandler
    public ResponseEntity<ApiResponse<?>> handleBadRequest(MethodArgumentTypeMismatchException e) {
        String name = e.getName();
        String type = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown";
        String value = sanitizeValue(name, e.getValue());
        String message = String.format("요청 파라미터 '%s' (타입: %s)의 값 '%s'이(가) 잘못되었습니다.", name, type, value);
        return failureResponse(CommonErrorType.BAD_REQUEST, message);
    }

    @ExceptionHandler
    public ResponseEntity<ApiResponse<?>> handleBadRequest(MissingRequestHeaderException e) {
        String name = e.getHeaderName();
        String message = String.format("필수 요청 헤더 '%s'가 누락되었습니다.", name);
        return failureResponse(CommonErrorType.BAD_REQUEST, message);
    }

    @ExceptionHandler
    public ResponseEntity<ApiResponse<?>> handleBadRequest(MissingServletRequestParameterException e) {
        String name = e.getParameterName();
        String type = e.getParameterType();
        String message = String.format("필수 요청 파라미터 '%s' (타입: %s)가 누락되었습니다.", name, type);
        return failureResponse(CommonErrorType.BAD_REQUEST, message);
    }

    @ExceptionHandler
    public ResponseEntity<ApiResponse<?>> handleBadRequest(HttpMessageNotReadableException e) {
        String errorMessage;
        Throwable rootCause = e.getRootCause();

        if (rootCause instanceof InvalidFormatException invalidFormat) {
            String fieldName = invalidFormat.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "?")
                    .collect(joining("."));

            String valueIndicationMessage = "";
            if (invalidFormat.getTargetType().isEnum()) {
                Class<?> enumClass = invalidFormat.getTargetType();
                String enumValues = Arrays.stream(enumClass.getEnumConstants())
                        .map(Object::toString)
                        .collect(joining(", "));
                valueIndicationMessage = "사용 가능한 값 : [" + enumValues + "]";
            }

            String expectedType = invalidFormat.getTargetType().getSimpleName();
            Object value = invalidFormat.getValue();

            errorMessage = String.format("필드 '%s'의 값 '%s'이(가) 예상 타입(%s)과 일치하지 않습니다. %s",
                    fieldName, value, expectedType, valueIndicationMessage);

        } else if (rootCause instanceof MismatchedInputException mismatchedInput) {
            String fieldPath = mismatchedInput.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "?")
                    .collect(joining("."));
            errorMessage = String.format("필수 필드 '%s'이(가) 누락되었습니다.", fieldPath);

        } else if (rootCause instanceof JsonMappingException jsonMapping) {
            String fieldPath = jsonMapping.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "?")
                    .collect(joining("."));
            errorMessage = String.format("필드 '%s'에서 JSON 매핑 오류가 발생했습니다: %s",
                    fieldPath, jsonMapping.getOriginalMessage());

        } else if (rootCause instanceof DateTimeException) {
            return failureResponse(CommonErrorType.INVALID, "올바르지 않은 날짜 형식입니다.");

        } else if (rootCause instanceof BusinessException business) {
            return failureResponse(business.getErrorType(), business.getCustomMessage());

        } else {
            errorMessage = "요청 본문을 처리하는 중 오류가 발생했습니다. JSON 메시지 규격을 확인해주세요.";
        }

        return failureResponse(CommonErrorType.BAD_REQUEST, errorMessage);
    }

    @ExceptionHandler
    public ResponseEntity<ApiResponse<?>> handleBadRequest(ServerWebInputException e) {
        String missingParams = extractMissingParameter(e.getReason() != null ? e.getReason() : "");
        if (!missingParams.isEmpty()) {
            String message = String.format("필수 요청 값 '%s'가 누락되었습니다.", missingParams);
            return failureResponse(CommonErrorType.BAD_REQUEST, message);
        } else {
            return failureResponse(CommonErrorType.BAD_REQUEST, null);
        }
    }

    @ExceptionHandler
    public ResponseEntity<ApiResponse<?>> handleNotFound(NoResourceFoundException e) {
        return failureResponse(CommonErrorType.NOT_FOUND, null);
    }

    @ExceptionHandler
    public ResponseEntity<ApiResponse<?>> handle(Throwable e) {
        log.error("Exception : {}", e.getMessage(), e);
        return failureResponse(CommonErrorType.INTERNAL_ERROR, null);
    }

    private String extractMissingParameter(String message) {
        Matcher matcher = PARAMETER_PATTERN.matcher(message);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String sanitizeValue(String fieldName, Object value) {
        if (value == null) return "null";
        if (SENSITIVE_FIELDS.stream().anyMatch(fieldName.toLowerCase()::contains)) {
            return "***";
        }
        String strValue = value.toString();
        return strValue.length() > MAX_VALUE_LENGTH ? strValue.substring(0, MAX_VALUE_LENGTH) + "..." : strValue;
    }

    private ResponseEntity<ApiResponse<?>> failureResponse(ErrorType errorType, String errorMessage) {
        return ResponseEntity.status(errorType.getStatus())
                .body(ApiResponse.fail(errorType.getCode(), errorMessage != null ? errorMessage : errorType.getMessage()));
    }

}
