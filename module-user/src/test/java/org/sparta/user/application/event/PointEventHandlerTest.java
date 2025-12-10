package org.sparta.user.application.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.EventPublisher;
import org.sparta.user.application.command.PointCommand;
import org.sparta.user.application.dto.PointServiceResult;
import org.sparta.user.application.service.PointService;
import org.sparta.user.domain.error.PointErrorType;
import org.sparta.user.domain.repository.ProcessedEventRepository;
import org.sparta.user.infrastructure.event.OrderApprovedEvent;
import org.sparta.user.infrastructure.event.OrderCancelledEvent;
import org.sparta.user.infrastructure.event.publisher.PointConfirmedEvent;
import org.sparta.user.infrastructure.event.publisher.PointReservationCancelledEvent;
import org.sparta.user.presentation.dto.PointMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointEventHandlerTest {



    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private PointService pointService;

    @Mock
    private PointMapper pointMapper;

    @InjectMocks
    private PointEventHandler handler;

    private OrderApprovedEvent approvedEvent;
    private OrderCancelledEvent cancelledEvent;
    private PointCommand.ConfirmPoint confirmCommand;
    private PointServiceResult.Confirm confirmResult;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {

        approvedEvent = new OrderApprovedEvent(
                UUID.randomUUID(),       // eventId
                Instant.now(),           // occurredAt
                UUID.randomUUID(),       // orderId
                UUID.randomUUID(),       // customerId
                UUID.randomUUID(),       // receiveHubId
                UUID.randomUUID(),       // receiveCompanyId
                UUID.randomUUID(),       // supplierHubId
                UUID.randomUUID(),       // supplierCompanyId
                "서울시 강남구 테헤란로",    // address
                "010-1234-5678",         // receiverPhone
                "2025-12-30T10:00:00",   // dueAt
                "현관문 앞",              // requestedMemo
                "U1234567"               // receiverSlackId
        );

        // ConfirmPoint Command
        confirmCommand =
                new PointCommand.ConfirmPoint(approvedEvent.orderId());

        // Confirm Result (dummy로 생성)
        confirmResult = new PointServiceResult.Confirm(
                approvedEvent.orderId(),
                1000L,
                List.of(
                        new PointServiceResult.PointUsageDetail(
                                UUID.randomUUID(),
                                1000L
                        )
                )
        );

        lenient().when(pointMapper.toCommand(any(OrderApprovedEvent.class)))
                .thenReturn(confirmCommand);

        lenient().when(pointService.confirmPointUsage(confirmCommand))
                .thenReturn(confirmResult);

        // --------- Cancelled Event ---------
        cancelledEvent = new OrderCancelledEvent(
                UUID.randomUUID(),     // eventId
                UUID.randomUUID(),     // orderId
                UUID.randomUUID(),     // productId
                2,                     // quantity
                Instant.now()          // occurredAt
        );

        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        handler = new PointEventHandler(processedEventRepository, eventPublisher, pointService, pointMapper, objectMapper);
    }

    // -------------------------------------------------------
    // OrderApproved 이벤트 테스트
    // -------------------------------------------------------

    @Test
    @DisplayName("이미 처리된 주문 승인 이벤트는 재처리하지 않는다")
    void handleOrderApproved_alreadyProcessed_doNothing() throws JsonProcessingException {
        when(processedEventRepository.existsByEventId(approvedEvent.eventId()))
                .thenReturn(true);

        String message = objectMapper.writeValueAsString(approvedEvent);
        handler.handleOrderApproved(message);

        verify(pointService, never()).confirmPointUsage(any());
        verify(eventPublisher, never()).publishExternal(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("주문 승인 이벤트가 들어오면 포인트 사용을 확정하고 이벤트를 발행한다")
    void handleOrderApproved_successFlow() throws JsonProcessingException {

        when(processedEventRepository.existsByEventId(approvedEvent.eventId()))
                .thenReturn(false);
        when(pointService.confirmPointUsage(confirmCommand))
                .thenReturn(confirmResult);

        String message = objectMapper.writeValueAsString(approvedEvent);
        handler.handleOrderApproved(message);

        // 포인트 서비스 호출
        verify(pointService).confirmPointUsage(confirmCommand);

        // 멱등성 저장
        verify(processedEventRepository).save(
                argThat(e -> e.getEventId().equals(approvedEvent.eventId()))
        );

        // 외부 이벤트 발행
        verify(eventPublisher).publishExternal(
                argThat(event ->
                        event instanceof PointConfirmedEvent &&
                                ((PointConfirmedEvent) event).orderId().equals(approvedEvent.orderId())
                )
        );
    }

    @Test
    @DisplayName("포인트 사용 확정 시 비즈니스 예외가 발생하면 이벤트는 기록하지만 포인트 확정은 처리하지 않는다")
    void handleOrderApproved_businessException_stillSaveProcessedEvent() throws JsonProcessingException {

        when(processedEventRepository.existsByEventId(approvedEvent.eventId()))
                .thenReturn(false);

        when(pointService.confirmPointUsage(confirmCommand))
                .thenThrow(new BusinessException(PointErrorType.POINT_IS_INSUFFICIENT));

        String message = objectMapper.writeValueAsString(approvedEvent);
        handler.handleOrderApproved(message);

        // 멱등성 저장됨
        verify(processedEventRepository).save(
                argThat(e -> e.getEventId().equals(approvedEvent.eventId()))
        );

        // 외부 이벤트 발행 없음
        verify(eventPublisher, never()).publishExternal(any());
    }

    // -------------------------------------------------------
    // OrderCancelled 이벤트 테스트
    // -------------------------------------------------------

    @Test
    @DisplayName("이미 처리된 주문 취소 이벤트는 재처리하지 않는다")
    void handleOrderCancelled_alreadyProcessed_doNothing() throws JsonProcessingException {
        when(processedEventRepository.existsByEventId(cancelledEvent.eventId()))
                .thenReturn(true);

        String message = objectMapper.writeValueAsString(cancelledEvent);
        handler.handleOrderCancelled(message);

        verify(pointService, never()).rollbackReservations(any());
        verify(eventPublisher, never()).publishExternal(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("주문 취소 이벤트가 들어오면 포인트 예약을 취소한다")
    void handleOrderCancelled_successFlow() throws JsonProcessingException {

        when(processedEventRepository.existsByEventId(cancelledEvent.eventId()))
                .thenReturn(false);

        String message = objectMapper.writeValueAsString(cancelledEvent);
        handler.handleOrderCancelled(message);

        // 서비스 호출
        verify(pointService).rollbackReservations(cancelledEvent.orderId());

        // 멱등성 저장
        verify(processedEventRepository).save(
                argThat(e -> e.getEventId().equals(cancelledEvent.eventId()))
        );

        // 외부 이벤트 발행 확인
        verify(eventPublisher).publishExternal(
                argThat(event ->
                        event instanceof PointReservationCancelledEvent &&
                                ((PointReservationCancelledEvent) event).orderId().equals(cancelledEvent.orderId())
                )
        );
    }

    @Test
    @DisplayName("포인트 취소 처리 중 비즈니스 예외가 발생하더라도 이벤트는 기록된다")
    void handleOrderCancelled_businessException_stillSaveProcessedEvent() throws JsonProcessingException {

        when(processedEventRepository.existsByEventId(cancelledEvent.eventId()))
                .thenReturn(false);

        doThrow(new BusinessException(PointErrorType.POINT_NOT_FOUND))
                .when(pointService).rollbackReservations(cancelledEvent.orderId());

        String message = objectMapper.writeValueAsString(cancelledEvent);
        handler.handleOrderCancelled(message);

        // 멱등성 저장됨
        verify(processedEventRepository).save(
                argThat(e -> e.getEventId().equals(cancelledEvent.eventId()))
        );

        // 외부 이벤트 발행 안 됨
        verify(eventPublisher, never()).publishExternal(any());
    }
}
