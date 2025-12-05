package org.sparta.coupon.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.EventPublisher;
import org.sparta.coupon.application.dto.CouponServiceResult;
import org.sparta.coupon.application.service.CouponService;
import org.sparta.coupon.domain.entity.ProcessedEvent;
import org.sparta.coupon.domain.repository.ProcessedEventRepository;
import org.sparta.coupon.infrastructure.event.OrderApprovedEvent;
import org.sparta.coupon.infrastructure.event.OrderCancelledEvent;
import org.sparta.coupon.infrastructure.event.publisher.CouponConfirmedEvent;
import org.sparta.coupon.infrastructure.event.publisher.CouponReservationCancelledEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * - 주문 승인/취소 이벤트 수신
 * - 멱등성 보장 (ProcessedEvent)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponEventHandler {

    private final CouponService couponService;
    private final ProcessedEventRepository processedEventRepository;
    private final EventPublisher eventPublisher;

    /**
     * 주문 승인 이벤트 처리
     * - 쿠폰 사용 확정 (RESERVED → PAID)
     */
    @KafkaListener(
            topics = "payment-completed",
            groupId = "coupon-service",
            containerFactory = "couponKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleOrderApproved(OrderApprovedEvent event) {
        log.info("주문 승인 이벤트 수신: orderId={}, eventId={}", event.orderId(), event.eventId());

        // 멱등성 체크
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("이미 처리된 이벤트 - eventId: {}", event.eventId());
            return;
        }

        try {
            // 쿠폰 사용 확정
            CouponServiceResult.Confirm result = couponService.confirmCouponByOrderId(event.orderId());

            // 이벤트 처리 기록
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "OrderApprovedEvent")
            );

            // 쿠폰 확정 이벤트 발행
            CouponConfirmedEvent confirmedEvent = CouponConfirmedEvent.of(
                    event.orderId(),
                    result.couponId(),
                    result.discountAmount()
            );
            eventPublisher.publishExternal(confirmedEvent);

            log.info("쿠폰 사용 확정 완료: orderId={}, couponId={}, discountAmount={}",
                    event.orderId(), result.couponId(), result.discountAmount());

        } catch (BusinessException e) {
            // 비즈니스 예외는 이벤트 처리 완료로 간주 (재시도 방지)
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "OrderApprovedEvent")
            );
            log.warn("쿠폰 사용 확정 실패 (예약 없음 또는 만료): orderId={}, error={}",
                    event.orderId(), e.getMessage());
        }
    }

    /**
     * 주문 취소 이벤트 처리
     * - 쿠폰 예약 취소 (RESERVED → AVAILABLE)
     */
    @KafkaListener(
            topics = "order-cancelled",
            groupId = "coupon-service",
            containerFactory = "couponKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("주문 취소 이벤트 수신: orderId={}, eventId={}", event.orderId(), event.eventId());

        // 멱등성 체크
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("이미 처리된 이벤트 - eventId: {}", event.eventId());
            return;
        }

        try {
            // 쿠폰 예약 취소
            java.util.UUID couponId = couponService.cancelReservationByOrderId(event.orderId());

            // 이벤트 처리 기록
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "OrderCancelledEvent")
            );

            // 쿠폰 예약 취소 이벤트 발행
            CouponReservationCancelledEvent cancelledEvent = CouponReservationCancelledEvent.of(
                    event.orderId(),
                    couponId
            );
            eventPublisher.publishExternal(cancelledEvent);

            log.info("쿠폰 예약 취소 완료: orderId={}, couponId={}", event.orderId(), couponId);

        } catch (BusinessException e) {
            // 비즈니스 예외는 이벤트 처리 완료로 간주 (재시도 방지)
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "OrderCancelledEvent")
            );
            log.warn("쿠폰 예약 취소 실패 (예약 없음): orderId={}, error={}",
                    event.orderId(), e.getMessage());
        }
    }
}