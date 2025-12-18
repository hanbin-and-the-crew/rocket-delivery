package org.sparta.payment.infrastructure.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.domain.PaymentType;
import org.sparta.common.domain.PgProvider;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.order.OrderCancelledEvent;
import org.sparta.common.event.order.OrderCreatedEvent;


import org.sparta.common.event.payment.PaymentFailedEvent;
import org.sparta.payment.application.command.payment.PaymentCreateCommand;
import org.sparta.payment.application.command.payment.PaymentCancelCommand;
import org.sparta.payment.application.command.payment.PaymentGetByOrderIdCommand;
import org.sparta.payment.application.dto.PaymentDetailResult;
import org.sparta.payment.application.service.PaymentService;
import org.sparta.payment.domain.error.PaymentErrorType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.EnumUtils.isValidEnum;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOrderEventConsumer {

    private static final String[] TOPICS = {
        "order.orderCreate",
        "order.orderCancel",
        "order.orderCreateFail",
        "order.orderCancelFail",
        "product.orderCreate",
        "product.orderCancel",
        "product.orderCreateFail",
        "product.orderCancelFail"
    };

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    @KafkaListener(
            topics = {
                    "order.orderCreate"
            },
            groupId = "payment-order-consumer",
            containerFactory = "paymentKafkaListenerContainerFactory"
    )
    public void orderCreateConsume(OrderCreatedEvent event) {
        log.info("[PaymentOrderEventConsumer] ORDER_CREATE 이벤트 수신 orderId={}, paymentKey={}",
                event.orderId(), event.paymentKey());

        try {
                        // 1) ENUM 유효성 검증
           if (!isValidEnum(PaymentType.class, event.methodType().toUpperCase())) {
                throw new BusinessException(
                           PaymentErrorType.INVALID_PAYMENT_METHOD,
                            "Invalid methodType: " + event.methodType()
                                );
            }
            if (!isValidEnum(PgProvider.class, event.pgProvider().toUpperCase())) {
               throw new BusinessException(
                            PaymentErrorType.INVALID_PG_PROVIDER,
                            "Invalid pgProvider: " + event.pgProvider()
                                );
            }
            // 2) ENUM 변환 (String → Enum)
            PaymentType methodType = PaymentType.valueOf(event.methodType().toUpperCase());
            PgProvider pgProvider = PgProvider.valueOf(event.pgProvider().toUpperCase());

            // 3) PaymentCreateCommand 구성
            PaymentCreateCommand command = new PaymentCreateCommand(
                    event.orderId(),
                    event.amountTotal(),
                    event.amountCoupon(),
                    event.amountPoint(),
                    event.amountPayable(),
                    methodType,
                    pgProvider,
                    event.currency(),
                    event.couponId(),
                    event.pointUsageId()
            );

            // ⭐ 결제 생성 로직 수행
            paymentService.storeCompletedPayment(command, event.paymentKey());

            log.info("[PaymentOrderEventConsumer] 결제 생성 성공! orderId={}", event.orderId());

        } catch (BusinessException e) {
            log.warn("[PaymentOrderEventConsumer] 결제 생성 실패(비즈니스 예외). orderId={}, reason={}",
                    event.orderId(), e.getMessage());

        } catch (Exception e) {
            // 시스템 예외는 DLQ 또는 재시도가 필요함
            log.error("[PaymentOrderEventConsumer] 결제 생성 중 시스템 예외 발생! orderId={}", event.orderId(), e);

            // PaymentKafkaConfig에서 DLQ로 처리함

            throw e; // 재처리 되도록 예외 재발생
        }
    }

    @KafkaListener(
            topics = {
                    "order.orderCancel"
            },
            groupId = "payment-order-consumer",
            containerFactory = "paymentKafkaListenerContainerFactory"
    )
    public void orderCancelConsume(OrderCancelledEvent event) {
        log.info("[PaymentOrderEventConsumer] ORDER_CANCEL 이벤트 수신 orderId={}", event.orderId());

        try {
            // 1) 주문 기준으로 결제 조회
            PaymentDetailResult payment = paymentService.getPaymentByOrderId(
                    new PaymentGetByOrderIdCommand(event.orderId())
            );

            // 2) 결제 취소 커맨드 생성 (전체 취소 가정)
            PaymentCancelCommand command = new PaymentCancelCommand(
                    payment.paymentId(),
                    "ORDER_CANCELLED" // 주문 취소로 인한 결제 취소
            );

            // 3) 결제 취소 수행
            paymentService.cancelPayment(command);

            log.info("[PaymentOrderEventConsumer] 결제 취소 성공. orderId={}, paymentId={}",
                    event.orderId(), payment.paymentId());

        } catch (BusinessException e) {
            // 비즈니스 예외는 재시도하면 안되므로 처리 완료로 간주
            log.warn("[PaymentOrderEventConsumer] 결제 취소 실패(비즈니스 예외). orderId={}, reason={}",
                    event.orderId(), e.getMessage());
        } catch (Exception e) {
            // 시스템 예외는 DLQ 또는 재시도 대상
            log.error("[PaymentOrderEventConsumer] 결제 취소 중 시스템 예외 발생! orderId={}", event.orderId(), e);
            throw e;
        }
    }

    @KafkaListener(
            topics = {
                    "order.orderCreateFail",
                    "product.orderCreateFail",
            },
            groupId = "payment-order-consumer",
            containerFactory = "paymentKafkaListenerContainerFactory"
    )
    public void orderCreateFailSagaConsume(OrderCreatedEvent event) {

        log.info("[PaymentSaga] CREATE FAIL 수신 → 보상 트랜잭션 시작 orderId={}", event.orderId());

        try {
            PaymentDetailResult payment = paymentService.getPaymentByOrderId(
                            new PaymentGetByOrderIdCommand(event.orderId())
            );

            // 2) 결제 취소 커맨드 생성 (전체 취소 가정)
            PaymentCancelCommand command = new PaymentCancelCommand(
                    payment.paymentId(),
                    "SAGA_ROLLBACK" // SAGA ROLLBACK으로 인한 결제 취소
            );

            // 이미 취소된 경우는 무시 (멱등)
            paymentService.cancelPayment(command);

            log.info("[PaymentSaga] 결제 보상 완료 orderId={}", event.orderId());

        } catch (BusinessException e) {
            // 결제가 없으면 이미 Payment 이전 단계에서 실패한 것
            log.warn("[PaymentSaga] 보상 불필요 orderId={}, reason={}",
                    event.orderId(), e.getMessage());
        } catch (Exception e) {
            // 시스템 예외는 DLQ 또는 재시도 대상
            log.error("[PaymentSaga] SAGA 중 시스템 예외 발생! orderId={}", event.orderId(), e);
            throw e;
        }

    }

    @KafkaListener(
            topics = {
                    "order.orderCancelFail",
                    "product.orderCancelFail"
            },
            groupId = "payment-order-consumer",
            containerFactory = "paymentKafkaListenerContainerFactory"
    )
    public void orderCancelFailSagaConsume(OrderCreatedEvent event) {
        log.error(
                "[PaymentSaga] 결제 보상 실패 감지! orderId={}",
                event.orderId()
        );

        // "주문 취소의 실패"같은 경우는 로그+모니터링 정도로만 확인하면 충분하다.
    }
}
