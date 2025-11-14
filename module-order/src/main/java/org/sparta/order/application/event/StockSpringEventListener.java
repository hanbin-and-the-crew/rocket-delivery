package org.sparta.order.application.event;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.order.application.service.StockService;
import org.sparta.order.infrastructure.event.publisher.PaymentCompletedSpringEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/*
 * 과제용 Spring Event 코드임. Kafka 아님!
 * */
@Component
@Slf4j
@RequiredArgsConstructor
public class StockSpringEventListener {
    private final StockService stockService;

    //@Async
    @EventListener
    public void handlePaymentCompleted(PaymentCompletedSpringEvent event) {
        log.info("결제 완료 이벤트 수신 - 재고 차감 시작");

        // 재고 차감
        stockService.decreaseStock(
                event.productId(),
                event.quantity()
        );

        log.info("재고 차감 완료");
    }
}