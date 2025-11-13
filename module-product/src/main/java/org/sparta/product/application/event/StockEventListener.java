package org.sparta.product.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.product.application.service.StockService;
import org.sparta.product.infrastructure.event.PaymentCompletedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/*
* 과제용 Spring Event 코드임. Kafka 아님!
* */
@Component
@Slf4j
@RequiredArgsConstructor
public class StockEventListener {
    private final StockService stockService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("결제 완료 이벤트 수신 - 재고 차감 시작");

        // 재고 차감
        stockService.decreaseStock(
                event.productId(),
                event.quantity()
        );

        log.info("재고 차감 완료");
    }
}