package org.sparta.user.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.EventPublisher;
import org.sparta.common.event.order.OrderApprovedEvent;
import org.sparta.common.event.order.OrderCancelledEvent;
import org.sparta.user.application.command.PointCommand;
import org.sparta.user.application.dto.PointServiceResult;
import org.sparta.user.application.service.PointService;
import org.sparta.user.domain.entity.ProcessedEvent;
import org.sparta.user.domain.repository.ProcessedEventRepository;
import org.sparta.user.infrastructure.event.publisher.PointConfirmedEvent;
import org.sparta.user.infrastructure.event.publisher.PointReservationCancelledEvent;
import org.sparta.user.presentation.dto.PointMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User 모듈 Point 결제 관련 이벤트 핸들러
 * 주문 승인 or 취소 이벤트 수신
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointEventHandler {

    private final ProcessedEventRepository processedEventRepository;
    private final EventPublisher eventPublisher;
    private final PointService pointService;
    private final PointMapper pointMapper;

    /**
     * 주문 승인 이벤트 처리
     */
    @KafkaListener(topics = "order.orderApprove", groupId = "user-service", containerFactory = "pointKafkaListenerContainerFactory")
    @Transactional
    public void handleOrderApproved(OrderApprovedEvent event) {
        log.info("주문 승인 이벤트 수신: orderId={}, eventId={}", event.orderId(), event.eventId());

        // 멱등성 체크
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("이미 처리된 이벤트 - eventId: {}", event.eventId());
            return;
        }

        try {
            // 포인트 사용 확정
            PointCommand.ConfirmPoint command = pointMapper.toCommand(event);
            PointServiceResult.Confirm result = pointService.confirmPointUsage(command);

            // 이벤트 처리 기록
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "OrderApprovedEvent")
            );

            // 포인트 확정 이벤트 발행
            PointConfirmedEvent confirmedEvent = PointConfirmedEvent.of(
                    event.orderId(),
                    result.discountAmount()
            );
            eventPublisher.publishExternal(confirmedEvent);

            log.info("포인트 사용 확정 완료: orderId={}, discountAmount={}",
                    event.orderId(), result.discountAmount());

        } catch (BusinessException e) {
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "OrderApprovedEvent")
            );
            log.warn("포인트 사용 확정 실패 (예약 없음 또는 만료): orderId={}, error={}",
                    event.orderId(), e.getMessage());
        }
    }

    /**
     * 주문 취소 이벤트 처리
     */
    @KafkaListener(topics = "order.orderCancel", groupId = "user-service", containerFactory = "pointKafkaListenerContainerFactory")
    @Transactional
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("주문 취소 이벤트 수신: orderId={}, eventId={}", event.orderId(), event.eventId());

        // 멱등성 체크
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("이미 처리된 이벤트 - eventId: {}", event.eventId());
            return;
        }

        try {
            // 포인트 예약 취소
            pointService.rollbackReservations(event.orderId());

            // 이벤트 처리 기록
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "OrderCancelledEvent")
            );

            // 포인트 예약 취소 이벤트 발행
            PointReservationCancelledEvent cancelledEvent = PointReservationCancelledEvent.of(
                    event.orderId()
            );
            eventPublisher.publishExternal(cancelledEvent);

            log.info("포인트 예약 취소 완료: orderId={}", event.orderId());

        } catch (BusinessException e) {
            // 비즈니스 예외는 이벤트 처리 완료로 간주 (재시도 방지)
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "OrderCancelledEvent")
            );
            log.warn("포인트 예약 취소 실패 (예약 없음): orderId={}, error={}",
                    event.orderId(), e.getMessage());
        }
    }
}