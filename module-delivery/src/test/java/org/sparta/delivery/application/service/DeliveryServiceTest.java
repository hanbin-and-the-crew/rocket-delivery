//package org.sparta.delivery.application.service;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.sparta.common.error.BusinessException;
//import org.sparta.delivery.application.dto.request.DeliveryRequest;
//import org.sparta.delivery.application.dto.response.DeliveryResponse;
//import org.sparta.delivery.domain.entity.Delivery;
//import org.sparta.delivery.domain.enumeration.DeliveryStatus;
//import org.sparta.delivery.fixture.DeliveryFixture;  // ← Fixture import
//import org.sparta.delivery.infrastructure.repository.DeliveryJpaRepository;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("DeliveryService 테스트")
//class DeliveryServiceTest {
//
//    @Mock
//    private DeliveryJpaRepository deliveryRepository;
//
//    @InjectMocks
//    private DeliveryService deliveryService;
//
//    // ========== 배송 생성 테스트 ==========
//
//    @Test
//    @DisplayName("배송 생성 성공")
//    void create_delivery_success() {
//        // given
//        UUID userId = UUID.randomUUID();
//        UUID orderId = UUID.randomUUID();
//        DeliveryRequest.Create request = new DeliveryRequest.Create(
//                orderId,
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                "서울특별시 강남구 테헤란로 123",
//                "홍길동",
//                "@홍길동"
//        );
//
//        // ✅ Fixture 사용!
//        Delivery delivery = DeliveryFixture.createDelivery(orderId);
//
//        given(deliveryRepository.findByOrderIdAndDeletedAtIsNull(any())).willReturn(Optional.empty());
//        given(deliveryRepository.save(any(Delivery.class))).willReturn(delivery);
//
//        // when
//        DeliveryResponse.Create response = deliveryService.createDelivery(request, userId);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.deliveryId()).isEqualTo(delivery.getId());
//        assertThat(response.orderId()).isEqualTo(delivery.getOrderId());
//        verify(deliveryRepository).save(any(Delivery.class));
//    }
//
//    @Test
//    @DisplayName("배송 생성 실패 - 중복된 주문 ID")
//    void create_delivery_fail_duplicate_order() {
//        // given
//        UUID userId = UUID.randomUUID();
//        UUID orderId = UUID.randomUUID();
//        DeliveryRequest.Create request = new DeliveryRequest.Create(
//                orderId,
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                "서울특별시 강남구 테헤란로 123",
//                "홍길동",
//                "@홍길동"
//        );
//
//        // ✅ Fixture 사용!
//        given(deliveryRepository.findByOrderIdAndDeletedAtIsNull(any()))
//                .willReturn(Optional.of(DeliveryFixture.createDelivery(orderId)));
//
//        // when & then
//        assertThatThrownBy(() -> deliveryService.createDelivery(request, userId))
//                .isInstanceOf(BusinessException.class);
//    }
//
//    // ========== 배송 목록 조회 테스트 ==========
//
//    @Test
//    @DisplayName("배송 목록 조회 성공 - 페이징만 사용")
//    void get_all_deliveries_success() {
//        // given
//        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
//
//        // ✅ Fixture 사용!
//        Delivery delivery = DeliveryFixture.createDelivery();
//        Page<Delivery> deliveryPage = new PageImpl<>(List.of(delivery), pageable, 1);
//
//        given(deliveryRepository.findAllNotDeleted(any(Pageable.class))).willReturn(deliveryPage);
//
//        // when
//        Page<DeliveryResponse.Summary> result = deliveryService.getAllDeliveries(pageable);
//
//        // then
//        assertThat(result).isNotNull();
//        assertThat(result.getContent()).hasSize(1);
//        assertThat(result.getTotalElements()).isEqualTo(1);
//        assertThat(result.getContent().get(0)).isNotNull();
//
//        verify(deliveryRepository, times(1)).findAllNotDeleted(any(Pageable.class));
//    }
//
//    @Test
//    @DisplayName("배송 목록 조회 성공 - 빈 결과")
//    void get_all_deliveries_success_empty() {
//        // given
//        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
//        Page<Delivery> emptyPage = new PageImpl<>(List.of(), pageable, 0);
//
//        given(deliveryRepository.findAllNotDeleted(any(Pageable.class))).willReturn(emptyPage);
//
//        // when
//        Page<DeliveryResponse.Summary> result = deliveryService.getAllDeliveries(pageable);
//
//        // then
//        assertThat(result).isNotNull();
//        assertThat(result.getContent()).isEmpty();
//        assertThat(result.getTotalElements()).isEqualTo(0);
//
//        verify(deliveryRepository, times(1)).findAllNotDeleted(any(Pageable.class));
//    }
//
//    @Test
//    @DisplayName("배송 목록 조회 성공 - 여러 페이지")
//    void get_all_deliveries_success_multiple_pages() {
//        // given
//        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
//
//        // ✅ Fixture 사용!
//        List<Delivery> deliveries = List.of(
//                DeliveryFixture.createDelivery(),
//                DeliveryFixture.createDelivery(),
//                DeliveryFixture.createDelivery()
//        );
//        Page<Delivery> deliveryPage = new PageImpl<>(deliveries, pageable, 25);
//
//        given(deliveryRepository.findAllNotDeleted(any(Pageable.class))).willReturn(deliveryPage);
//
//        // when
//        Page<DeliveryResponse.Summary> result = deliveryService.getAllDeliveries(pageable);
//
//        // then
//        assertThat(result).isNotNull();
//        assertThat(result.getContent()).hasSize(3);
//        assertThat(result.getTotalElements()).isEqualTo(25);
//        assertThat(result.getTotalPages()).isEqualTo(3);
//
//        verify(deliveryRepository, times(1)).findAllNotDeleted(any(Pageable.class));
//    }
//
//    @Test
//    @DisplayName("배송 목록 조회 - deletedAt이 null인 데이터만 조회")
//    void get_all_deliveries_only_not_deleted() {
//        // given
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // ✅ Fixture 사용!
//        Delivery activeDelivery = DeliveryFixture.createDelivery();
//        assertThat(activeDelivery.getDeletedAt()).isNull();
//
//        Page<Delivery> deliveryPage = new PageImpl<>(List.of(activeDelivery), pageable, 1);
//        given(deliveryRepository.findAllNotDeleted(any(Pageable.class))).willReturn(deliveryPage);
//
//        // when
//        Page<DeliveryResponse.Summary> result = deliveryService.getAllDeliveries(pageable);
//
//        // then
//        assertThat(result.getContent()).hasSize(1);
//        result.getContent().forEach(delivery -> assertThat(delivery).isNotNull());
//
//        verify(deliveryRepository, times(1)).findAllNotDeleted(any(Pageable.class));
//    }
//
//    // ========== 배송 상세 조회 테스트 ==========
//
//    @Test
//    @DisplayName("배송 상세 조회 성공")
//    void get_delivery_success() {
//        // given
//        UUID userId = UUID.randomUUID();
//        UUID deliveryId = UUID.randomUUID();
//
//        // ✅ Fixture 사용!
//        Delivery delivery = DeliveryFixture.createDelivery();
//
//        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
//                .willReturn(Optional.of(delivery));
//
//        // when
//        DeliveryResponse.Detail response = deliveryService.getDelivery(deliveryId, userId);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.deliveryId()).isEqualTo(delivery.getId());
//    }
//
//    @Test
//    @DisplayName("배송 상세 조회 실패 - 존재하지 않는 배송")
//    void get_delivery_fail_not_found() {
//        // given
//        UUID userId = UUID.randomUUID();
//        UUID deliveryId = UUID.randomUUID();
//
//        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
//                .willReturn(Optional.empty());
//
//        // when & then
//        assertThatThrownBy(() -> deliveryService.getDelivery(deliveryId, userId))
//                .isInstanceOf(BusinessException.class);
//    }
//
//    // ========== 배송 상태 변경 테스트 ==========
//
//    @Test
//    @DisplayName("배송 상태 변경 성공")
//    void update_status_success() {
//        // given
//        UUID userId = UUID.randomUUID();
//        UUID deliveryId = UUID.randomUUID();
//        DeliveryRequest.UpdateStatus request = new DeliveryRequest.UpdateStatus(DeliveryStatus.HUB_MOVING);
//
//        // ✅ Fixture 사용!
//        Delivery delivery = DeliveryFixture.createDelivery();
//
//        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
//                .willReturn(Optional.of(delivery));
//
//        // when
//        DeliveryResponse.Update response = deliveryService.updateStatus(deliveryId, request, userId);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.HUB_MOVING);
//    }
//
//    // ========== 배송 주소 변경 테스트 ==========
//
//    @Test
//    @DisplayName("배송 주소 변경 성공")
//    void update_address_success() {
//        // given
//        UUID userId = UUID.randomUUID();
//        UUID deliveryId = UUID.randomUUID();
//        String newAddress = "서울특별시 강남구 역삼로 456";
//        DeliveryRequest.UpdateAddress request = new DeliveryRequest.UpdateAddress(newAddress);
//
//        // ✅ Fixture 사용!
//        Delivery delivery = DeliveryFixture.createDelivery();
//
//        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
//                .willReturn(Optional.of(delivery));
//
//        // when
//        DeliveryResponse.Update response = deliveryService.updateAddress(deliveryId, request, userId);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(delivery.getDeliveryAddress()).isEqualTo(newAddress);
//    }
//
//    // ========== 배송 담당자 배정 테스트 ==========
//
//    @Test
//    @DisplayName("배송 담당자 배정 성공")
//    void assign_delivery_man_success() {
//        // given
//        UUID userId = UUID.randomUUID();
//        UUID deliveryId = UUID.randomUUID();
//        UUID companyManId = UUID.randomUUID();
//        UUID hubManId = UUID.randomUUID();
//        DeliveryRequest.AssignDeliveryMan request =
//                new DeliveryRequest.AssignDeliveryMan(companyManId, hubManId);
//
//        // ✅ Fixture 사용!
//        Delivery delivery = DeliveryFixture.createDelivery();
//
//        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
//                .willReturn(Optional.of(delivery));
//
//        // when
//        DeliveryResponse.Update response = deliveryService.assignDeliveryMan(deliveryId, request, userId);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(delivery.getCompanyDeliveryManId()).isEqualTo(companyManId);
//        assertThat(delivery.getHubDeliveryManId()).isEqualTo(hubManId);
//    }
//
//    // ========== 업체 배송 시작 테스트 ==========
//
//    @Test
//    @DisplayName("업체 배송 시작 성공")
//    void start_company_moving_success() {
//        // given
//        UUID userId = UUID.randomUUID();
//        UUID deliveryId = UUID.randomUUID();
//        UUID companyManId = UUID.randomUUID();
//        DeliveryRequest.StartCompanyMoving request =
//                new DeliveryRequest.StartCompanyMoving(companyManId);
//
//        // ✅ Fixture 사용!
//        Delivery delivery = DeliveryFixture.createDelivery();
//
//        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
//                .willReturn(Optional.of(delivery));
//
//        // when
//        DeliveryResponse.Update response = deliveryService.startCompanyMoving(deliveryId, request, userId);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.COMPANY_MOVING);
//        assertThat(delivery.getCompanyDeliveryManId()).isEqualTo(companyManId);
//    }
//
//    // ========== 배송 완료 테스트 ==========
//
//    @Test
//    @DisplayName("배송 완료 성공")
//    void complete_delivery_success() {
//        // given
//        UUID userId = UUID.randomUUID();
//
//        // ✅ Fixture의 Builder 사용! (상태가 필요한 경우)
//        Delivery delivery = DeliveryFixture.builder()
//                .status(DeliveryStatus.COMPANY_MOVING)
//                .companyDeliveryManId(UUID.randomUUID())
//                .build();
//
//        given(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId()))
//                .willReturn(Optional.of(delivery));
//
//        // when
//        DeliveryResponse.Update response = deliveryService.completeDelivery(delivery.getId(), userId);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.deliveryId()).isEqualTo(delivery.getId());
//        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
//
//        verify(deliveryRepository).findByIdAndDeletedAtIsNull(delivery.getId());
//    }
//
//    // ========== 배송 삭제 테스트 ==========
//
//    @Test
//    @DisplayName("배송 삭제 성공")
//    void delete_delivery_success() {
//        // given
//        UUID userId = UUID.randomUUID();
//        UUID deliveryId = UUID.randomUUID();
//
//        // ✅ Fixture 사용!
//        Delivery delivery = DeliveryFixture.createDelivery();
//
//        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
//                .willReturn(Optional.of(delivery));
//
//        // when
//        DeliveryResponse.Delete response = deliveryService.deleteDelivery(deliveryId, userId);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(delivery.getDeletedAt()).isNotNull();
//    }
//}
