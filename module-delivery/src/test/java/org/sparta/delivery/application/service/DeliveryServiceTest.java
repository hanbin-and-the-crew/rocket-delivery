package org.sparta.delivery.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.error.BusinessException;
import org.sparta.delivery.application.dto.DeliverySearchCondition;
import org.sparta.delivery.application.dto.request.DeliveryRequest;
import org.sparta.delivery.application.dto.response.DeliveryResponse;
import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;
import org.sparta.delivery.fixture.DeliveryFixture;
import org.sparta.delivery.infrastructure.repository.DeliveryJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeliveryService 테스트")
class DeliveryServiceTest {

    @Mock
    private DeliveryJpaRepository deliveryRepository;

    @InjectMocks
    private DeliveryService deliveryService;

    @Test
    @DisplayName("배송 생성 성공")
    void create_delivery_success() {
        // given
        UUID userId = UUID.randomUUID();
        DeliveryRequest.Create request = new DeliveryRequest.Create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "서울특별시 강남구 테헤란로 123",
                "홍길동",
                "@홍길동"
        );

        Delivery delivery = createTestDelivery();
        given(deliveryRepository.findByOrderIdAndDeletedAtIsNull(any())).willReturn(Optional.empty());
        given(deliveryRepository.save(any(Delivery.class))).willReturn(delivery);

        // when
        DeliveryResponse.Create response = deliveryService.createDelivery(request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.deliveryId()).isEqualTo(delivery.getId());
        assertThat(response.orderId()).isEqualTo(delivery.getOrderId());
        verify(deliveryRepository).save(any(Delivery.class));
    }

    @Test
    @DisplayName("배송 생성 실패 - 중복된 주문 ID")
    void create_delivery_fail_duplicate_order() {
        // given
        UUID userId = UUID.randomUUID();
        DeliveryRequest.Create request = new DeliveryRequest.Create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "서울특별시 강남구 테헤란로 123",
                "홍길동",
                "@홍길동"
        );

        given(deliveryRepository.findByOrderIdAndDeletedAtIsNull(any())).willReturn(Optional.of(createTestDelivery()));

        // when & then
        assertThatThrownBy(() -> deliveryService.createDelivery(request, userId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("배송 목록 조회 성공 - 검색 조건 없음")
    void get_all_deliveries_success_without_condition() {
        // given
        UUID userId = UUID.randomUUID();
        DeliverySearchCondition condition = DeliverySearchCondition.empty();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        Delivery delivery = createTestDelivery();
        Page<Delivery> deliveryPage = new PageImpl<>(List.of(delivery), pageable, 1);

        given(deliveryRepository.searchDeliveries(any(), any())).willReturn(deliveryPage);

        // when
        Page<DeliveryResponse.Summary> result = deliveryService.getAllDeliveries(userId, condition, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("배송 상세 조회 성공")
    void get_delivery_success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = createTestDelivery();

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

        // when
        DeliveryResponse.Detail response = deliveryService.getDelivery(deliveryId, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.deliveryId()).isEqualTo(delivery.getId());
    }

    @Test
    @DisplayName("배송 상세 조회 실패 - 존재하지 않는 배송")
    void get_delivery_fail_not_found() {
        // given
        UUID userId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> deliveryService.getDelivery(deliveryId, userId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("배송 상태 변경 성공")
    void update_status_success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        DeliveryRequest.UpdateStatus request = new DeliveryRequest.UpdateStatus(DeliveryStatus.HUB_MOVING);
        Delivery delivery = createTestDelivery();

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

        // when
        DeliveryResponse.Update response = deliveryService.updateStatus(deliveryId, request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.HUB_MOVING);
    }

    @Test
    @DisplayName("배송 주소 변경 성공")
    void update_address_success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        String newAddress = "서울특별시 강남구 역삼로 456";
        DeliveryRequest.UpdateAddress request = new DeliveryRequest.UpdateAddress(newAddress);
        Delivery delivery = createTestDelivery();

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

        // when
        DeliveryResponse.Update response = deliveryService.updateAddress(deliveryId, request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(delivery.getDeliveryAddress()).isEqualTo(newAddress);
    }

    @Test
    @DisplayName("배송 담당자 배정 성공")
    void assign_delivery_man_success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        UUID companyManId = UUID.randomUUID();
        UUID hubManId = UUID.randomUUID();
        DeliveryRequest.AssignDeliveryMan request = new DeliveryRequest.AssignDeliveryMan(companyManId, hubManId);
        Delivery delivery = createTestDelivery();

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

        // when
        DeliveryResponse.Update response = deliveryService.assignDeliveryMan(deliveryId, request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(delivery.getCompanyDeliveryManId()).isEqualTo(companyManId);
        assertThat(delivery.getHubDeliveryManId()).isEqualTo(hubManId);
    }

    @Test
    @DisplayName("업체 배송 시작 성공")
    void start_company_moving_success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        UUID companyManId = UUID.randomUUID();
        DeliveryRequest.StartCompanyMoving request = new DeliveryRequest.StartCompanyMoving(companyManId);
        Delivery delivery = createTestDelivery();

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

        // when
        DeliveryResponse.Update response = deliveryService.startCompanyMoving(deliveryId, request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.COMPANY_MOVING);
        assertThat(delivery.getCompanyDeliveryManId()).isEqualTo(companyManId);
    }

    @Test
    @DisplayName("배송 완료 성공")
    void complete_delivery_success() {
        // given
        Delivery delivery = DeliveryFixture.createDelivery();

        // ✅ 배송을 COMPANY_MOVING 상태로 변경
        delivery.startCompanyMoving(UUID.randomUUID());

        given(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId()))
                .willReturn(Optional.of(delivery));

        // when
        DeliveryResponse.Update response = deliveryService.completeDelivery(delivery.getId(), UUID.randomUUID());

        // then
        assertThat(response.deliveryId()).isEqualTo(delivery.getId());
        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        verify(deliveryRepository).findByIdAndDeletedAtIsNull(delivery.getId());
    }


    @Test
    @DisplayName("배송 삭제 성공")
    void delete_delivery_success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = createTestDelivery();

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

        // when
        DeliveryResponse.Delete response = deliveryService.deleteDelivery(deliveryId, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(delivery.getDeletedAt()).isNotNull();
    }

    private Delivery createTestDelivery() {
        return Delivery.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "서울특별시 강남구 테헤란로 123",
                "홍길동",
                "@홍길동"
        );
    }
}
