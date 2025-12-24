package org.sparta.order.application.error;

import org.sparta.common.error.BusinessException;
import org.sparta.common.error.ErrorType;
import org.springframework.http.HttpStatus;

/**
 * 서비스 이용 불가 예외
 *
 * Circuit Breaker가 OPEN 상태일 때 발생
 */
public class ServiceUnavailableException extends BusinessException {

    public ServiceUnavailableException(String message) {
        super(new ErrorType() {
            @Override
            public HttpStatus getStatus() {
                return HttpStatus.SERVICE_UNAVAILABLE; // 503
            }

            @Override
            public String getCode() {
                return "order:service_unavailable";
            }

            @Override
            public String getMessage() {
                return message;
            }
        });
    }
}