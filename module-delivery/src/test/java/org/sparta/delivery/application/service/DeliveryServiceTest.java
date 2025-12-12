//package org.sparta.delivery.application.service;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentCaptor;
//import org.sparta.common.error.BusinessException;
//import org.sparta.common.event.EventPublisher;
//import org.sparta.delivery.domain.entity.Delivery;
//import org.sparta.delivery.domain.entity.DeliveryProcessedEvent;
//import org.sparta.delivery.domain.error.DeliveryErrorType;
//import org.sparta.delivery.domain.repository.DeliveryProcessedEventRepository;
//import org.sparta.delivery.domain.repository.DeliveryRepository;
//import org.sparta.delivery.infrastructure.client.HubRouteFeignClient;
//import org.sparta.delivery.infrastructure.event.OrderApprovedEvent;
//import org.sparta.delivery.infrastructure.event.publisher.DeliveryFailedEvent;
//import org.sparta.delivery.presentation.dto.request.DeliveryRequest;
//import org.sparta.delivery.presentation.dto.response.DeliveryResponse;
//import org.sparta.delivery.presentation.dto.response.HubLegResponse;
//import org.sparta.deliverylog.application.service.DeliveryLogService;
//import org.sparta.deliverylog.presentation.dto.request.DeliveryLogRequest;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.*;
//import static org.mockito.Mockito.mock;
//
//class DeliveryServiceImplTest {
//
//    // 테스트용 서브클래스: afterCommit 훅을 즉시 실행하도록 override
//    static class TestableDeliveryServiceImpl extends DeliveryServiceImpl {
//        public TestableDeliveryServiceImpl(
//                DeliveryRepository deliveryRepository,
//                DeliveryProcessedEventRepository deliveryProcessedEventRepository,
//                DeliveryLogService deliveryLogService,
//                HubRouteFeignClient hubRouteFeignClient,
//                EventPublisher eventPublisher
//        ) {
//            super(deliveryRepository, deliveryProcessedEventRepository,
//                    deliveryLogService, hubRouteFeignClient, eventPublisher);
//        }
//
//        @Override
//        protected void registerAfterCommit(Runnable task) {
//            // 테스트에서는 트랜잭션 동기화 없이 바로 실행
//            task.run();
//        }
//    }
//
//    private DeliveryServiceImpl deliveryService;
//
//    private DeliveryRepository deliveryRepository;
//    private DeliveryProcessedEventRepository deliveryProcessedEventRepository;
//    private DeliveryLogService deliveryLogService;
//    private HubRouteFeignClient hubRouteFeignClient;
//    private EventPublisher eventPublisher;
//
//    @BeforeEach
//    void setUp() {
//        deliveryRepository = mock(DeliveryRepository.class);
//        deliveryProcessedEventRepository = mock(DeliveryProcessedEventRepository.class);
//        deliveryLogService = mock(DeliveryLogService.class);
//        hubRouteFeignClient = mock(HubRouteFeignClient.class);
//        eventPublisher = mock(EventPublisher.class);
//
//        deliveryService = new TestableDeliveryServiceImpl(
//                deliveryRepository,
//                deliveryProcessedEventRepository,
//                deliveryLogService,
//                hubRouteFeignClient,
//                eventPublisher
//        );
//    }
//
//    // ================= createWithRoute =================
//
//    @Test
//    @DisplayName("createWithRoute - 정상 플로우: 경로 조회 + Delivery/Log 생성 + ProcessedEvent 저장")
//    void createWithRoute_success() {
//        // given
//        UUID eventId = UUID.randomUUID();
//        UUID orderId = UUID.randomUUID();
//        UUID customerId = UUID.randomUUID();
//        UUID supplierCompanyId = UUID.randomUUID();
//        UUID supplierHubId = UUID.randomUUID();
//        UUID receiveCompanyId = UUID.randomUUID();
//        UUID receiveHubId = UUID.randomUUID();
//
//        OrderApprovedEvent orderEvent = OrderApprovedEvent.of(
//                orderId,
//                customerId,
//                supplierCompanyId,
//                supplierHubId,
//                receiveCompanyId,
//                receiveHubId,
//                "서울시 어딘가",
//                "수령인",
//                "slack-id",
//                "010-0000-0000",
//                LocalDateTime.now().plusDays(1),
//                "요청 메모"
//        );
//
//        given(deliveryProcessedEventRepository.existsByEventId(eventId))
//                .willReturn(false);
//        given(deliveryRepository.findByOrderIdAndDeletedAtIsNull(orderId))
//                .willReturn(Optional.empty());
//
//        HubLegResponse leg1 = new HubLegResponse(
//                supplierHubId,
//                UUID.randomUUID(),
//                10.0,
//                20
//        );
//        HubLegResponse leg2 = new HubLegResponse(
//                leg1.targetHubId(),
//                receiveHubId,
//                5.0,
//                10
//        );
//        given(hubRouteFeignClient.getRouteLegs(supplierHubId, receiveHubId))
//                .willReturn(List.of(leg1, leg2));
//
//        Delivery savedDelivery = mock(Delivery.class);
//        UUID deliveryId = UUID.randomUUID();
//        given(savedDelivery.getId()).willReturn(deliveryId);
//        given(savedDelivery.getOrderId()).willReturn(orderId);
//        given(savedDelivery.getSupplierHubId()).willReturn(supplierHubId);
//        given(savedDelivery.getReceiveHubId()).willReturn(receiveHubId);
//        given(savedDelivery.getTotalLogSeq()).willReturn(2);
//
//        given(deliveryRepository.save(any(Delivery.class)))
//                .willReturn(savedDelivery);
//
//        // when
//        DeliveryResponse.Detail result = deliveryService.createWithRoute(orderEvent);
//
//        // then
//        assertThat(result).isNotNull();
//        assertThat(result.orderId()).isEqualTo(orderId);
////        assertThat(result.()).isEqualTo(deliveryId);
//
//        ArgumentCaptor<DeliveryLogRequest.Create> logCaptor =
//                ArgumentCaptor.forClass(DeliveryLogRequest.Create.class);
//        then(deliveryLogService).should(times(2)).create(logCaptor.capture());
//
//        List<DeliveryLogRequest.Create> createdLogs = logCaptor.getAllValues();
//        assertThat(createdLogs).hasSize(2);
//        assertThat(createdLogs).extracting(DeliveryLogRequest.Create::deliveryId)
//                .containsOnly(deliveryId);
//
//        assertThat(createdLogs.get(0).sequence()).isEqualTo(0);
//        assertThat(createdLogs.get(0).sourceHubId()).isEqualTo(supplierHubId);
//        assertThat(createdLogs.get(0).targetHubId()).isEqualTo(leg1.targetHubId());
//
//        assertThat(createdLogs.get(1).sequence()).isEqualTo(1);
//        assertThat(createdLogs.get(1).sourceHubId()).isEqualTo(leg1.targetHubId());
//        assertThat(createdLogs.get(1).targetHubId()).isEqualTo(receiveHubId);
//
//        ArgumentCaptor<DeliveryProcessedEvent> processedCaptor =
//                ArgumentCaptor.forClass(DeliveryProcessedEvent.class);
//        then(deliveryProcessedEventRepository).should().save(processedCaptor.capture());
//        assertThat(processedCaptor.getValue().getEventId()).isEqualTo(eventId);
//
//        // 실패 이벤트는 발행되지 않아야 함
//        then(eventPublisher).should(never()).publishExternal(isA(DeliveryFailedEvent.class));
//    }
//
//    @Test
//    @DisplayName("createWithRoute - 이미 처리한 eventId면 기존 Delivery 반환하고 아무것도 생성하지 않음")
//    void createWithRoute_idempotent_byEventId() {
//        // given
//        UUID eventId = UUID.randomUUID();
//        UUID orderId = UUID.randomUUID();
//
//        OrderApprovedEvent event = OrderApprovedEvent.of(
//                eventId,
//                orderId,
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                "주소",
//                "이름",
//                "slack",
//                "010",
//                LocalDateTime.now().plusDays(1),
//                "메모"
//        );
//
//        given(deliveryProcessedEventRepository.existsByEventId(eventId))
//                .willReturn(true);
//
//        Delivery existing = mock(Delivery.class);
//        given(existing.getId()).willReturn(UUID.randomUUID());
//        given(existing.getOrderId()).willReturn(orderId);
//
//        given(deliveryRepository.findByOrderIdAndDeletedAtIsNull(orderId))
//                .willReturn(Optional.of(existing));
//
//        // when
//        DeliveryResponse.Detail result = deliveryService.createWithRoute(event);
//
//        // then
//        assertThat(result.orderId()).isEqualTo(orderId);
//
//        then(hubRouteFeignClient).shouldHaveNoInteractions();
//        then(deliveryRepository).should(never()).save(any());
//        then(deliveryLogService).shouldHaveNoInteractions();
//        then(deliveryProcessedEventRepository).should(never()).save(any());
//        then(eventPublisher).shouldHaveNoInteractions();
//    }
//
//    @Test
//    @DisplayName("createWithRoute - orderId 기준 기존 Delivery 있으면 ProcessedEvent 만 저장하고 기존 Delivery 반환")
//    void createWithRoute_idempotent_byOrderId() {
//        // given
//        UUID eventId = UUID.randomUUID();
//        UUID orderId = UUID.randomUUID();
//
//        OrderApprovedEvent event = OrderApprovedEvent.of(
//                eventId,
//                orderId,
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                "주소",
//                "이름",
//                "slack",
//                "010",
//                LocalDateTime.now().plusDays(1),
//                "메모"
//        );
//
//        given(deliveryProcessedEventRepository.existsByEventId(eventId))
//                .willReturn(false);
//
//        Delivery existing = mock(Delivery.class);
//        given(existing.getId()).willReturn(UUID.randomUUID());
//        given(existing.getOrderId()).willReturn(orderId);
//
//        given(deliveryRepository.findByOrderIdAndDeletedAtIsNull(orderId))
//                .willReturn(Optional.of(existing));
//
//        // when
//        DeliveryResponse.Detail result = deliveryService.createWithRoute(event);
//
//        // then
//        assertThat(result.orderId()).isEqualTo(orderId);
//
//        then(deliveryProcessedEventRepository).should()
//                .save(any(DeliveryProcessedEvent.class));
//
//        then(hubRouteFeignClient).shouldHaveNoInteractions();
//        then(deliveryRepository).should(never()).save(any());
//        then(deliveryLogService).shouldHaveNoInteractions();
//        then(eventPublisher).shouldHaveNoInteractions();
//    }
//
//    @Test
//    @DisplayName("createWithRoute - 경로 없음이면 DeliveryFailedEvent 발행 후 NO_ROUTE_AVAILABLE 예외")
//    void createWithRoute_noRoute() {
//        // given
//        UUID eventId = UUID.randomUUID();
//        UUID orderId = UUID.randomUUID();
//        UUID supplierHubId = UUID.randomUUID();
//        UUID receiveHubId = UUID.randomUUID();
//
//        OrderApprovedEvent event = OrderApprovedEvent.of(
//                orderId,
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                supplierHubId,
//                UUID.randomUUID(),
//                receiveHubId,
//                "주소",
//                "이름",
//                "slack",
//                "010",
//                LocalDateTime.now().plusDays(1),
//                "메모"
//        );
//
//        given(deliveryProcessedEventRepository.existsByEventId(eventId))
//                .willReturn(false);
//        given(deliveryRepository.findByOrderIdAndDeletedAtIsNull(orderId))
//                .willReturn(Optional.empty());
//
//        given(hubRouteFeignClient.getRouteLegs(supplierHubId, receiveHubId))
//                .willReturn(List.of());
//
//        // when
//        Throwable thrown = catchThrowable(() -> deliveryService.createWithRoute(event));
//
//        // then
//        assertThat(thrown)
//                .isInstanceOf(BusinessException.class)
//                .extracting("errorType")
//                .isEqualTo(DeliveryErrorType.NO_ROUTE_AVAILABLE);
//
//        then(eventPublisher).should().publishExternal(
//                isA(DeliveryFailedEvent.class)
//        );
//
//        then(deliveryRepository).should(never()).save(any());
//        then(deliveryLogService).shouldHaveNoInteractions();
//        then(deliveryProcessedEventRepository).should(never()).save(any());
//    }
//
//    // ================= assignHubDeliveryMan =================
//
//    @Test
//    @DisplayName("assignHubDeliveryMan - 배송 존재 시 담당자 배정 후 Detail 반환")
//    void assignHubDeliveryMan_success() {
//        UUID deliveryId = UUID.randomUUID();
//        UUID hubDeliveryManId = UUID.randomUUID();
//
//        DeliveryRequest.AssignHubDeliveryMan request =
//                new DeliveryRequest.AssignHubDeliveryMan(hubDeliveryManId);
//
//        Delivery delivery = mock(Delivery.class);
//        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
//                .willReturn(Optional.of(delivery));
//
//        DeliveryResponse.Detail result =
//                deliveryService.assignHubDeliveryMan(deliveryId, request);
//
//        then(delivery).should().assignHubDeliveryMan(hubDeliveryManId);
//        assertThat(result).isNotNull();
//    }
//
//    @Test
//    @DisplayName("assignHubDeliveryMan - 배송 없으면 DELIVERY_NOT_FOUND 예외")
//    void assignHubDeliveryMan_notFound() {
//        UUID deliveryId = UUID.randomUUID();
//        DeliveryRequest.AssignHubDeliveryMan request =
//                new DeliveryRequest.AssignHubDeliveryMan(UUID.randomUUID());
//
//        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
//                .willReturn(Optional.empty());
//
//        Throwable thrown = catchThrowable(
//                () -> deliveryService.assignHubDeliveryMan(deliveryId, request)
//        );
//
//        assertThat(thrown)
//                .isInstanceOf(BusinessException.class)
//                .extracting("errorType")
//                .isEqualTo(DeliveryErrorType.DELIVERY_NOT_FOUND);
//    }
//}
