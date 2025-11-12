package org.sparta.order.infrastructure.event.Listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.order.application.dto.request.PaymentRequest;
import org.sparta.order.application.service.PaymentService;
import org.sparta.order.infrastructure.event.dto.OrderCreatedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 1. TransactionalEventListener로 변경
@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventListener {
    private final PaymentService paymentService;

    // @TransactionalEventListener: 트랜잭션의 특정 단계에 이벤트를 처리합니다.
    // phase = AFTER_COMMIT: 트랜잭션이 성공적으로 커밋된 후에만 실행됩니다.
    // 만약 주문 생성 중 예외가 발생해 트랜잭션이 롤백되면, 이 리스너는 실행되지 않습니다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("결제 처리 시작 - 주문 ID: {}", event.orderId());

        // 결제 처리 로직
        paymentService.processPayment(
                new PaymentRequest.Create(
                        event.orderId(),
                        event.productId(),
                        event.quantity()
                ),
                event.userId()
        );

        log.info("결제 처리 완료 - 주문 ID: {}", event.orderId());
    }
}
