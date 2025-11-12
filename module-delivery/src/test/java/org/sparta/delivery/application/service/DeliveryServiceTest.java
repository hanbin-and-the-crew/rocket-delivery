package org.sparta.delivery.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.error.BusinessException;
import org.sparta.delivery.application.dto.request.DeliveryRequest;
import org.sparta.delivery.application.dto.response.DeliveryResponse;
import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;
import org.sparta.delivery.domain.error.DeliveryErrorType;
import org.sparta.delivery.fixture.DeliveryFixture;
import org.sparta.delivery.infrastructure.repository.DeliveryJpaRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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
        UUID orderId = UUID.randomUUID();
        DeliveryRequest.Create request = new DeliveryRequest.Create(
                orderId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "서울시 강남구",
                "홍길동",
                "@hong"
        );
        UUID userId = UUID.randomUUID();

        Delivery delivery = DeliveryFixture.createDelivery(orderId);

        given(deliveryRepository.findByOrderIdAndDeletedAtIsNull(orderId))
                .willReturn(Optional.empty());
        given(deliveryRepository.save(any(Delivery.class)))
                .willReturn(delivery);

        // when
        DeliveryResponse.Create response = deliveryService.createDelivery(request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.HUB_WAITING);

        verify(deliveryRepository, times(1)).findByOrderIdAndDeletedAtIsNull(orderId);
        verify(deliveryRepository, times(1)).save(any(Delivery.class));
    }

    @Test
    @DisplayName("배송 생성 실패 - 중복된 주문")
    void create_delivery_fail_duplicate() {
        // given
        UUID orderId = UUID.randomUUID();
        DeliveryRequest.Create request = new DeliveryRequest.Create(
                orderId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "서울시 강남구",
                "홍길동",
                "@hong"
        );
        UUID userId = UUID.randomUUID();

        Delivery existingDelivery = DeliveryFixture.createDelivery(orderId);
        given(deliveryRepository.findByOrderIdAndDeletedAtIsNull(orderId))
                .willReturn(Optional.of(existingDelivery));

        // when & then
        assertThatThrownBy(() -> deliveryService.createDelivery(request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(DeliveryErrorType.DELIVERY_ALREADY_EXISTS.getMessage());

        verify(deliveryRepository, times(1)).findByOrderIdAndDeletedAtIsNull(orderId);
        verify(deliveryRepository, never()).save(any(Delivery.class));
    }

    @Test
    @DisplayName("배송 조회 성공")
    void get_delivery_success() {
        // given
        UUID deliveryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Delivery delivery = DeliveryFixture.createDelivery();

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
                .willReturn(Optional.of(delivery));

        // when
        DeliveryResponse.Detail response = deliveryService.getDelivery(deliveryId, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.deliveryId()).isEqualTo(delivery.getId());

        verify(deliveryRepository, times(1)).findByIdAndDeletedAtIsNull(deliveryId);
    }

    @Test
    @DisplayName("배송 조회 실패 - 존재하지 않는 배송")
    void get_delivery_fail_not_found() {
        // given
        UUID deliveryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> deliveryService.getDelivery(deliveryId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(DeliveryErrorType.DELIVERY_NOT_FOUND.getMessage());

        verify(deliveryRepository, times(1)).findByIdAndDeletedAtIsNull(deliveryId);
    }

    @Test
    @DisplayName("배송지 주소 변경 성공")
    void update_address_success() {
        // given
        UUID deliveryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String newAddress = "서울시 서초구";
        DeliveryRequest.UpdateAddress request = new DeliveryRequest.UpdateAddress(newAddress);

        Delivery delivery = DeliveryFixture.createDelivery();
        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
                .willReturn(Optional.of(delivery));

        // when
        DeliveryResponse.Detail response = deliveryService.updateAddress(deliveryId, request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.deliveryAddress()).isEqualTo(newAddress);

        verify(deliveryRepository, times(1)).findByIdAndDeletedAtIsNull(deliveryId);
    }

    @Test
    @DisplayName("배송 담당자 배정 성공")
    void assign_delivery_man_success() {
        // given
        UUID deliveryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID companyDeliveryManId = UUID.randomUUID();
        UUID hubDeliveryManId = UUID.randomUUID();
        DeliveryRequest.AssignDeliveryMan request = new DeliveryRequest.AssignDeliveryMan(
                companyDeliveryManId,
                hubDeliveryManId
        );

        Delivery delivery = DeliveryFixture.createDelivery();
        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
                .willReturn(Optional.of(delivery));

        // when
        DeliveryResponse.Detail response = deliveryService.assignDeliveryMan(deliveryId, request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.companyDeliveryManId()).isEqualTo(companyDeliveryManId);
        assertThat(response.hubDeliveryManId()).isEqualTo(hubDeliveryManId);

        verify(deliveryRepository, times(1)).findByIdAndDeletedAtIsNull(deliveryId);
    }

    @Test
    @DisplayName("허브 이동 시작 성공")
    void hub_moving_success() {
        // given
        UUID deliveryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Delivery delivery = DeliveryFixture.createDelivery();

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
                .willReturn(Optional.of(delivery));

        // when
        DeliveryResponse.Detail response = deliveryService.hubMoving(deliveryId, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.HUB_MOVING);

        verify(deliveryRepository, times(1)).findByIdAndDeletedAtIsNull(deliveryId);
    }

    @Test
    @DisplayName("목적지 허브 도착 성공")
    void arrive_at_destination_hub_success() {
        // given
        UUID deliveryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Delivery delivery = DeliveryFixture.createDelivery();

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
                .willReturn(Optional.of(delivery));

        // when
        DeliveryResponse.Detail response = deliveryService.arriveAtDestinationHub(deliveryId, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.DEST_HUB_ARRIVED);

        verify(deliveryRepository, times(1)).findByIdAndDeletedAtIsNull(deliveryId);
    }

    @Test
    @DisplayName("업체 이동 시작 성공")
    void start_company_moving_success() {
        // given
        UUID deliveryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID companyDeliveryManId = UUID.randomUUID();
        DeliveryRequest.StartCompanyMoving request = new DeliveryRequest.StartCompanyMoving(companyDeliveryManId);

        Delivery delivery = DeliveryFixture.createDelivery();
        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
                .willReturn(Optional.of(delivery));

        // when
        DeliveryResponse.Detail response = deliveryService.startCompanyMoving(deliveryId, request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.COMPANY_MOVING);
        assertThat(response.companyDeliveryManId()).isEqualTo(companyDeliveryManId);

        verify(deliveryRepository, times(1)).findByIdAndDeletedAtIsNull(deliveryId);
    }

    @Test
    @DisplayName("배송 완료 성공")
    void complete_delivery_success() {
        // given
        UUID deliveryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Delivery delivery = DeliveryFixture.createDelivery();

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
                .willReturn(Optional.of(delivery));

        // when
        DeliveryResponse.Detail response = deliveryService.completeDelivery(deliveryId, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);

        verify(deliveryRepository, times(1)).findByIdAndDeletedAtIsNull(deliveryId);
    }

    @Test
    @DisplayName("배송 삭제 성공")
    void delete_delivery_success() {
        // given
        UUID deliveryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Delivery delivery = DeliveryFixture.createDelivery();

        given(deliveryRepository.findById(deliveryId))
                .willReturn(Optional.of(delivery));

        // when
        deliveryService.deleteDelivery(deliveryId, userId);

        // then
        verify(deliveryRepository, times(1)).findById(deliveryId);
    }
}
