package org.sparta.order.domain.circuitbreaker;

/**
 * Circuit Breaker가 OPEN 상태일 때 발생하는 예외
 *
 * 외부 서비스가 지속적으로 실패하여 Circuit이 OPEN된 상태에서
 * 추가 요청이 들어올 경우 즉시 이 예외를 발생시킵니다.
 */
public class CircuitBreakerOpenException extends RuntimeException {

    private final String serviceName;

    public CircuitBreakerOpenException(String serviceName) {
        super(String.format(
            "Circuit Breaker is OPEN for service: %s. " +
            "현재 주문 처리가 불가능합니다. 잠시 후 다시 시도해 주세요.",
            serviceName
        ));
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}