//package org.sparta.deliverylog.application.service;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.sparta.common.error.BusinessException;
//import org.sparta.deliverylog.application.dto.DeliveryLogRequest;
//import org.sparta.deliverylog.application.dto.DeliveryLogResponse;
//import org.sparta.deliverylog.application.event.DeliveryLogEventPublisher;
//import org.sparta.deliverylog.domain.entity.DeliveryLog;
//import org.sparta.deliverylog.domain.enumeration.DeliveryRouteStatus;
//import org.sparta.deliverylog.infrastructure.repository.DeliveryLogRepository;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("DeliveryLogService 테스트")
//class DeliveryLogServiceTest {
//
//    @Mock
//    private DeliveryLogRepository deliveryLogRepository;
//
//    @Mock
//    private DeliveryLogEventPublisher eventPublisher;
//
//    @InjectMocks
//    private DeliveryLogService deliveryLogService;
//
//    @Test
//    @DisplayName("배송 경로 생성 성공")
//    void createDeliveryLog_Success() {
//        // given
//        DeliveryLogRequest.Create request = new DeliveryLogRequest.Create(
//                UUID.randomUUID(),
//                1,
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                10.0,
//                30
//        );
//
//        DeliveryLog deliveryLog = DeliveryLog.create(
//                request.deliveryId(),
//                request.hubSequence(),
//                request.departureHubId(),
//                request.destinationHubId(),
//                request.expectedDistance(),
//                request.expectedTime()
//        );
//
//        when(deliveryLogRepository.save(any(DeliveryLog.class))).thenReturn(deliveryLog);
//        doNothing().when(eventPublisher).publishDeliveryLogCreated(any(DeliveryLog.class));
//
//        // when
//        DeliveryLogResponse.Detail response = deliveryLogService.createDeliveryLog(request);
//
//        // then
//        assertThat(response.deliveryId()).isEqualTo(request.deliveryId());
//        assertThat(response.hubSequence()).isEqualTo(request.hubSequence());
//        verify(deliveryLogRepository, times(1)).save(any(DeliveryLog.class));
//        verify(eventPublisher, times(1)).publishDeliveryLogCreated(any(DeliveryLog.class));
//    }
//
//    @Test
//    @DisplayName("배송 담당자 배정 성공")
//    void assignDeliveryMan_Success() {
//        // given
//        UUID deliveryLogId = UUID.randomUUID();
//        UUID deliveryManId = UUID.randomUUID();
//        DeliveryLog deliveryLog = createTestDeliveryLog();
//
//        when(deliveryLogRepository.findById(deliveryLogId)).thenReturn(Optional.of(deliveryLog));
//        doNothing().when(eventPublisher).publishDeliveryManAssigned(any(DeliveryLog.class));
//
//        // when
//        DeliveryLogResponse.Detail response = deliveryLogService.assignDeliveryMan(
//                deliveryLogId,
//                deliveryManId
//        );
//
//        // then
//        assertThat(response.deliveryManId()).isEqualTo(deliveryManId);
//        verify(deliveryLogRepository, times(1)).findById(deliveryLogId);
//        verify(eventPublisher, times(1)).publishDeliveryManAssigned(any(DeliveryLog.class));
//    }
//
//    @Test
//    @DisplayName("배송 담당자 배정 실패 - 존재하지 않는 경로")
//    void assignDeliveryMan_Fail_NotFound() {
//        // given
//        UUID deliveryLogId = UUID.randomUUID();
//        UUID deliveryManId = UUID.randomUUID();
//
//        when(deliveryLogRepository.findById(deliveryLogId)).thenReturn(Optional.empty());
//
//        // when & then
//        assertThatThrownBy(() -> deliveryLogService.assignDeliveryMan(deliveryLogId, deliveryManId))
//                .isInstanceOf(BusinessException.class);
//    }
//
//    @Test
//    @DisplayName("배송 시작 성공")
//    void startDelivery_Success() {
//        // given
//        UUID deliveryLogId = UUID.randomUUID();
//        DeliveryLog deliveryLog = createTestDeliveryLog();
//        deliveryLog.assignDeliveryMan(UUID.randomUUID());
//
//        when(deliveryLogRepository.findById(deliveryLogId)).thenReturn(Optional.of(deliveryLog));
//        doNothing().when(eventPublisher).publishDeliveryStarted(any(DeliveryLog.class));
//
//        // when
//        DeliveryLogResponse.Detail response = deliveryLogService.startDelivery(deliveryLogId);
//
//        // then
//        assertThat(response.status()).isEqualTo(DeliveryRouteStatus.MOVING.name());
//        verify(eventPublisher, times(1)).publishDeliveryStarted(any(DeliveryLog.class));
//    }
//
//    @Test
//    @DisplayName("배송 완료 성공")
//    void completeDelivery_Success() {
//        // given
//        UUID deliveryLogId = UUID.randomUUID();
//        DeliveryLog deliveryLog = createTestDeliveryLog();
//        deliveryLog.assignDeliveryMan(UUID.randomUUID());
//        deliveryLog.startDelivery();
//
//        DeliveryLogRequest.Complete request = new DeliveryLogRequest.Complete(11.0, 32);
//
//        when(deliveryLogRepository.findById(deliveryLogId)).thenReturn(Optional.of(deliveryLog));
//        doNothing().when(eventPublisher).publishDeliveryCompleted(any(DeliveryLog.class));
//
//        // when
//        DeliveryLogResponse.Detail response = deliveryLogService.completeDelivery(deliveryLogId, request);
//
//        // then
//        assertThat(response.status()).isEqualTo(DeliveryRouteStatus.COMPLETED.name());
//        assertThat(response.actualDistance()).isEqualTo(request.actualDistance());
//        assertThat(response.actualTime()).isEqualTo(request.actualTime());
//        verify(eventPublisher, times(1)).publishDeliveryCompleted(any(DeliveryLog.class));
//    }
//
//    @Test
//    @DisplayName("배송 경로 단건 조회 성공")
//    void getDeliveryLog_Success() {
//        // given
//        UUID deliveryLogId = UUID.randomUUID();
//        DeliveryLog deliveryLog = createTestDeliveryLog();
//
//        when(deliveryLogRepository.findById(deliveryLogId)).thenReturn(Optional.of(deliveryLog));
//
//        // when
//        DeliveryLogResponse.Detail response = deliveryLogService.getDeliveryLog(deliveryLogId);
//
//        // then
//        assertThat(response).isNotNull();
//        verify(deliveryLogRepository, times(1)).findById(deliveryLogId);
//    }
//
//    @Test
//    @DisplayName("배송 ID로 전체 경로 조회 성공")
//    void getDeliveryLogsByDeliveryId_Success() {
//        // given
//        UUID deliveryId = UUID.randomUUID();
//        List<DeliveryLog> deliveryLogs = Arrays.asList(
//                createTestDeliveryLog(),
//                createTestDeliveryLog(),
//                createTestDeliveryLog()
//        );
//
//        when(deliveryLogRepository.findByDeliveryIdOrderByHubSequence(deliveryId))
//                .thenReturn(deliveryLogs);
//
//        // when
//        List<DeliveryLogResponse.Summary> responses = deliveryLogService
//                .getDeliveryLogsByDeliveryId(deliveryId);
//
//        // then
//        assertThat(responses).hasSize(3);
//        verify(deliveryLogRepository, times(1)).findByDeliveryIdOrderByHubSequence(deliveryId);
//    }
//
//    @Test
//    @DisplayName("배송 담당자의 진행 중인 경로 조회 성공")
//    void getDeliveryManInProgressLogs_Success() {
//        // given
//        UUID deliveryManId = UUID.randomUUID();
//        DeliveryLog waiting = createTestDeliveryLog();
//        waiting.assignDeliveryMan(deliveryManId);
//
//        DeliveryLog moving = createTestDeliveryLog();
//        moving.assignDeliveryMan(deliveryManId);
//        moving.startDelivery();
//
//        List<DeliveryLog> inProgressLogs = Arrays.asList(waiting, moving);
//
//        when(deliveryLogRepository.findByDeliveryManIdAndDeliveryStatusIn(
//                eq(deliveryManId),
//                anyList()
//        )).thenReturn(inProgressLogs);
//
//        // when
//        List<DeliveryLogResponse.Summary> responses = deliveryLogService
//                .getDeliveryManInProgressLogs(deliveryManId);
//
//        // then
//        assertThat(responses).hasSize(2);
//        verify(deliveryLogRepository, times(1))
//                .findByDeliveryManIdAndDeliveryStatusIn(eq(deliveryManId), anyList());
//    }
//
//    @Test
//    @DisplayName("허브의 대기 중인 경로 조회 성공")
//    void getHubWaitingLogs_Success() {
//        // given
//        UUID hubId = UUID.randomUUID();
//        List<DeliveryLog> waitingLogs = Arrays.asList(
//                createTestDeliveryLog(),
//                createTestDeliveryLog()
//        );
//
//        when(deliveryLogRepository.findByDepartureHubIdAndDeliveryStatus(
//                hubId,
//                DeliveryRouteStatus.WAITING
//        )).thenReturn(waitingLogs);
//
//        // when
//        List<DeliveryLogResponse.Summary> responses = deliveryLogService.getHubWaitingLogs(hubId);
//
//        // then
//        assertThat(responses).hasSize(2);
//        verify(deliveryLogRepository, times(1))
//                .findByDepartureHubIdAndDeliveryStatus(hubId, DeliveryRouteStatus.WAITING);
//    }
//
//    @Test
//    @DisplayName("전체 경로 목록 조회 성공")
//    void getAllDeliveryLogs_Success() {
//        // given
//        Pageable pageable = PageRequest.of(0, 10);
//        List<DeliveryLog> deliveryLogs = Arrays.asList(
//                createTestDeliveryLog(),
//                createTestDeliveryLog()
//        );
//        Page<DeliveryLog> page = new PageImpl<>(deliveryLogs, pageable, deliveryLogs.size());
//
//        when(deliveryLogRepository.findAllActive(pageable)).thenReturn(page);
//
//        // when
//        Page<DeliveryLogResponse.Summary> responses = deliveryLogService.getAllDeliveryLogs(pageable);
//
//        // then
//        assertThat(responses.getContent()).hasSize(2);
//        verify(deliveryLogRepository, times(1)).findAllActive(pageable);
//    }
//
//    @Test
//    @DisplayName("배송 경로 취소 성공")
//    void cancelDeliveryLog_Success() {
//        // given
//        UUID deliveryLogId = UUID.randomUUID();
//        DeliveryLog deliveryLog = createTestDeliveryLog();
//
//        when(deliveryLogRepository.findById(deliveryLogId)).thenReturn(Optional.of(deliveryLog));
//        doNothing().when(eventPublisher).publishDeliveryCanceled(any(DeliveryLog.class));
//
//        // when
//        deliveryLogService.cancelDeliveryLog(deliveryLogId);
//
//        // then
//        assertThat(deliveryLog.getDeliveryStatus()).isEqualTo(DeliveryRouteStatus.CANCELED);
//        verify(eventPublisher, times(1)).publishDeliveryCanceled(any(DeliveryLog.class));
//    }
//
//    @Test
//    @DisplayName("배송 경로 삭제 성공")
//    void deleteDeliveryLog_Success() {
//        // given
//        UUID deliveryLogId = UUID.randomUUID();
//        DeliveryLog deliveryLog = createTestDeliveryLog();
//
//        when(deliveryLogRepository.findById(deliveryLogId)).thenReturn(Optional.of(deliveryLog));
//        doNothing().when(deliveryLogRepository).delete(any(DeliveryLog.class));
//
//        // when
//        deliveryLogService.deleteDeliveryLog(deliveryLogId);
//
//        // then
//        verify(deliveryLogRepository, times(1)).delete(any(DeliveryLog.class));
//    }
//
//    // ========== Helper Methods ==========
//
//    private DeliveryLog createTestDeliveryLog() {
//        return DeliveryLog.create(
//                UUID.randomUUID(),
//                1,
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                10.0,
//                30
//        );
//    }
//}
