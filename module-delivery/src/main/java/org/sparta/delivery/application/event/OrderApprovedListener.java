package org.sparta.delivery.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.delivery.application.service.DeliveryService;
import org.sparta.delivery.domain.entity.DeliveryProcessedEvent;
import org.sparta.delivery.domain.error.DeliveryCancelledException;
import org.sparta.delivery.domain.repository.DeliveryProcessedEventRepository;
import org.sparta.delivery.infrastructure.event.OrderApprovedEvent;
import org.sparta.delivery.presentation.dto.response.DeliveryResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [OrderApprovedEvent 수신]
 * => 주문 승인 시 배송 생성
 * => 허브 경로 계산 및 DeliveryLog 생성
 *
 * 멱등성 보장:
 * - eventId 기반 중복 이벤트 체크
 * - 동일 주문으로 배송 중복 생성 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderApprovedListener {

    private final DeliveryService deliveryService;
    private final DeliveryProcessedEventRepository deliveryProcessedEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * 주문 승인 이벤트 처리
     * - 배송 생성
     * - deliveryKafkaListenerContainerFactory 사용
     */
    @KafkaListener(
            topics = "order.orderApprove",
            groupId = "delivery-service",
            containerFactory = "deliveryKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleOrderApproved(OrderApprovedEvent event) {
        log.info("OrderApprovedEvent received: orderId={}, eventId={}",
                event.orderId(), event.eventId());

        // 1. 멱등성 체크: eventId로 중복 이벤트 확인 / try 밖으로 뺐음
        if (deliveryProcessedEventRepository.existsByEventId(event.eventId())) {
            log.info("Event already processed, skipping: eventId={}, orderId={}",
                    event.eventId(), event.orderId());
            return;
        }

        boolean deliveryCreated = false;

        try {

            // 2. 배송 생성 시도 (허브 경로 계산 + DeliveryLog 생성)
            //  (내부에서 Cancel Request 가드 수행)
            DeliveryResponse.Detail delivery = deliveryService.createWithRoute(event);
            deliveryCreated = true;
            log.info("Delivery created successfully: orderId={}, eventId={}, deliveryId={}",
                    event.orderId(), event.eventId(), delivery.id());

            // 정상 케이스의 processedEvent(멱등성)저장은 try 밖으로 뻄

        }  catch (DeliveryCancelledException e) {
            // Cancel Request로 인한 생성 중단 (정상 케이스)
            log.warn("Delivery creation cancelled due to cancel request: orderId={}, message={}",
                    event.orderId(), e.getMessage());
            deliveryCreated = false;    // delivery 생성 안 됨

            // delivery가 Cancel된 경우에도 이벤트는 처리된 것으로 간주하고 기록
            deliveryProcessedEventRepository.save(
                    DeliveryProcessedEvent.of(event.eventId(), "ORDER_APPROVED")
            );

            log.info("OrderApprovedEvent processing completed: orderId={}, eventId={}, status=CANCELLED",
                    event.orderId(), event.eventId());

        }
        catch (DataIntegrityViolationException e) {
            // 중복 키 에러 처리: 다른 트랜잭션이 이미 처리 완료했을 경우 에러가 발생하지 않게 처리
            if (e.getMessage() != null && e.getMessage().contains("idx_event_id")) {
                log.warn("Event already processed by another transaction: eventId={}, orderId={}. " +
                                "This is expected in concurrent scenarios.",
                        event.eventId(), event.orderId());
                // 예외를 던지지 않고 정상 종료 → Kafka ACK 처리 (kafka 재전송 X)
                return;
            }
            // 다른 종류의 데이터 무결성 위반은 재시도
            log.error("Data integrity violation during delivery creation: orderId={}, eventId={}",
                    event.orderId(), event.eventId(), e);
            throw new RuntimeException("Data integrity violation during delivery creation", e);

        } catch (Exception e) {
            // 나머지 예외는 재시도
            log.error("Failed to handle order approved event: orderId={}, eventId={}",
                    event.orderId(), event.eventId(), e);
            // 예외 발생 시 전체 트랜잭션 롤백 + Kafka 재시도
            throw new RuntimeException("Order approved event processing failed", e);
        }

        // 이벤트 처리 완료 기록 (같은 트랜잭션)
        deliveryProcessedEventRepository.save(
                DeliveryProcessedEvent.of(event.eventId(), "ORDER_APPROVED")
        );

        log.info("OrderApprovedEvent processing completed: orderId={}, eventId={}",
                event.orderId(), event.eventId());

        if (deliveryCreated) {
            log.info("Order approved processing completed: orderId={}, eventId={}, status=CREATED",
                    event.orderId(), event.eventId());
        } else {
            log.info("Order approved processing completed: orderId={}, eventId={}, status=CANCELLED",
                    event.orderId(), event.eventId());
        }
    }
}
