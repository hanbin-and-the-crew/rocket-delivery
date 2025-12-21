package org.sparta.delivery.domain.error;

import java.util.UUID;

/**
 * 배송 생성 중단 예외
 * - Cancel Intent가 존재하여 배송 생성을 중단할 때 발생
 * - 정상적인 비즈니스 플로우 (에러 아님)
 */
public class DeliveryCancelledException extends RuntimeException {

    private final UUID orderId;

    public DeliveryCancelledException(UUID orderId) {
        super("Delivery creation cancelled due to cancel intent: orderId=" + orderId);
        this.orderId = orderId;
    }

    public UUID getOrderId() {
        return orderId;
    }
}
