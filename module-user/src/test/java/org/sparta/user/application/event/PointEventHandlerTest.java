package org.sparta.user.application.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.EventPublisher;
import org.sparta.common.event.order.OrderApprovedEvent;
import org.sparta.common.event.order.OrderCancelledEvent;
import org.sparta.user.application.command.PointCommand;
import org.sparta.user.application.dto.PointServiceResult;
import org.sparta.user.application.service.PointService;
import org.sparta.user.domain.error.PointErrorType;
import org.sparta.user.domain.repository.ProcessedEventRepository;
import org.sparta.common.event.user.PointConfirmedEvent;
import org.sparta.common.event.user.PointReservationCancelledEvent;
import org.sparta.user.presentation.dto.PointMapper;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.LocalDateTime;
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
    private OrderCancelledEvent canceledEvent;
    private PointCommand.ConfirmPoint confirmCommand;
    private PointServiceResult.Confirm confirmResult;

    // private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {

        approvedEvent = new OrderApprovedEvent(
                UUID.randomUUID(),            // orderId: 주문 ID
                UUID.randomUUID(),            // customerId: 고객 ID
                UUID.randomUUID(),            // supplierCompanyId: 공급 업체 회사 ID
                UUID.randomUUID(),            // supplierHubId: 공급 허브 ID
                UUID.randomUUID(),            // receiveCompanyId: 수령 업체 회사 ID
                UUID.randomUUID(),            // receiveHubId: 수령 허브 ID
                "서울시 강남구 테헤란로",       // address: 배송지 주소
                "홍길동",                       // receiverName: 수령자 이름
                "U1234567",                    // receiverSlackId: 수령자 슬랙 ID
                "010-1234-5678",              // receiverPhone: 수령자 연락처
                LocalDateTime.of(2025, 12, 30, 10, 0), // dueAt: 배송 예정 시간
                "현관문 앞에 놓아주세요",        // requestMemo: 고객 요청 사항
                UUID.randomUUID(),            // eventId: 이벤트 고유 ID
                Instant.now()                 // occurredAt: 이벤트 발생 시각
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
        canceledEvent = new OrderCancelledEvent(
                UUID.randomUUID(),     // eventId
                UUID.randomUUID(),     // orderId
                UUID.randomUUID(),     // productId
                2,                     // quantity
                Instant.now()          // occurredAt
        );

        //objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        //handler = new PointEventHandler(processedEventRepository, eventPublisher, pointService, pointMapper, objectMapper);
    }

    // -------------------------------------------------------
    // OrderApproved 이벤트 테스트
    // -------------------------------------------------------

    @Test
    @DisplayName("이미 처리된 주문 승인 이벤트는 재처리하지 않는다")
    void handleOrderApproved_alreadyProcessed_doNothing() {

        // save 시도 → Unique 제약 위반으로 간주
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(processedEventRepository)
                .save(any());

        handler.handleOrderApproved(approvedEvent);

        verify(pointService, never()).confirmPointUsage(any());
        verify(eventPublisher, never()).publishExternal(any());
    }

    @Test
    @DisplayName("주문 승인 이벤트가 들어오면 포인트 사용을 확정하고 이벤트를 발행한다")
    void handleOrderApproved_successFlow() {

        when(processedEventRepository.save(any()))
                .thenReturn(null);
        when(pointService.confirmPointUsage(confirmCommand))
                .thenReturn(confirmResult);

        handler.handleOrderApproved(approvedEvent);

        // save 호출됨
        verify(processedEventRepository).save(any());

        // 서비스 호출됨
        verify(pointService).confirmPointUsage(confirmCommand);

        // 이벤트 발행 호출됨
        verify(eventPublisher).publishExternal(any(PointConfirmedEvent.class));
    }

    @Test
    @DisplayName("포인트 사용 확정 시 비즈니스 예외가 발생하면 이벤트는 기록하지만 포인트 확정은 처리하지 않는다")
    void handleOrderApproved_businessException_stillSaveProcessedEvent() {

        // save 정상
        when(processedEventRepository.save(any()))
                .thenReturn(null);

        // service에서 비즈니스 예외 발생
        when(pointService.confirmPointUsage(confirmCommand))
                .thenThrow(new BusinessException(PointErrorType.POINT_IS_INSUFFICIENT));

        handler.handleOrderApproved(approvedEvent);

        // save는 반드시 호출됨
        verify(processedEventRepository).save(any());

        // publishExternal은 호출되지 않아야 함
        verify(eventPublisher, never()).publishExternal(any());
    }

    // -------------------------------------------------------
    // OrderCancelled 이벤트 테스트
    // -------------------------------------------------------

    @Test
    @DisplayName("이미 처리된 주문 취소 이벤트는 재처리하지 않는다")
    void handleOrderCancelled_alreadyProcessed_doNothing() {

        // save 시도 → Unique 제약 위반으로 간주
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(processedEventRepository)
                .save(any());

        handler.handleOrderCancelled(canceledEvent);

        verify(pointService, never()).rollbackReservations(any());
        verify(eventPublisher, never()).publishExternal(any());
    }

    @Test
    @DisplayName("주문 취소 이벤트가 들어오면 포인트 예약을 취소한다")
    void handleOrderCancelled_successFlow() {

        // save는 정상 호출
        when(processedEventRepository.save(any()))
                .thenReturn(null);

        handler.handleOrderCancelled(canceledEvent);

        // 저장은 반드시 호출됨
        verify(processedEventRepository).save(any());

        // 서비스 호출됨
        verify(pointService).rollbackReservations(canceledEvent.orderId());

        // 취소 이벤트 발행됨
        verify(eventPublisher)
                .publishExternal(any(PointReservationCancelledEvent.class));
    }

    @Test
    @DisplayName("포인트 취소 처리 중 비즈니스 예외가 발생하더라도 이벤트는 기록된다")
    void handleOrderCancelled_businessException_stillSaveProcessedEvent() {

        // save 정상
        when(processedEventRepository.save(any()))
                .thenReturn(null);

        // 서비스 로직에서 예외 발생
        doThrow(new BusinessException(PointErrorType.POINT_IS_INSUFFICIENT))
                .when(pointService)
                .rollbackReservations(canceledEvent.orderId());

        handler.handleOrderCancelled(canceledEvent);

        // 멱등성 기록은 됨
        verify(processedEventRepository).save(any());

        // publishExternal은 호출되지 않아야 함
        verify(eventPublisher, never())
                .publishExternal(any());
    }
}
