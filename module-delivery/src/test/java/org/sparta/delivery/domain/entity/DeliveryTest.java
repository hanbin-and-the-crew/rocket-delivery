package org.sparta.delivery.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Delivery 도메인 테스트")
class DeliveryTest {

    @Test
    @DisplayName("배송 생성 - 초기 상태는 HUB_WAITING")
    void create_delivery() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID departureHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        String deliveryAddress = "서울시 강남구";
        String recipientName = "홍길동";
        String recipientSlackId = "@hong";

        // when
        Delivery delivery = Delivery.create(
                orderId,
                departureHubId,
                destinationHubId,
                deliveryAddress,
                recipientName,
                recipientSlackId
        );

        // then
        assertThat(delivery).isNotNull();
        assertThat(delivery.getOrderId()).isEqualTo(orderId);
        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.HUB_WAITING);
        assertThat(delivery.getDepartureHubId()).isEqualTo(departureHubId);
        assertThat(delivery.getDestinationHubId()).isEqualTo(destinationHubId);
        assertThat(delivery.getDeliveryAddress()).isEqualTo(deliveryAddress);
        assertThat(delivery.getRecipientName()).isEqualTo(recipientName);
        assertThat(delivery.getRecipientSlackId()).isEqualTo(recipientSlackId);
        assertThat(delivery.getCompanyDeliveryManId()).isNull();
        assertThat(delivery.getHubDeliveryManId()).isNull();
    }

    @Test
    @DisplayName("허브 대기 상태로 변경")
    void hub_waiting() {
        // given
        Delivery delivery = createDelivery();

        // when
        delivery.hubWaiting();

        // then
        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.HUB_WAITING);
    }

    @Test
    @DisplayName("허브 이동 중 상태로 변경")
    void hub_moving() {
        // given
        Delivery delivery = createDelivery();

        // when
        delivery.hubMoving();

        // then
        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.HUB_MOVING);
    }

    @Test
    @DisplayName("목적지 허브 도착 상태로 변경")
    void arrive_at_destination_hub() {
        // given
        Delivery delivery = createDelivery();

        // when
        delivery.arriveAtDestinationHub();

        // then
        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.DEST_HUB_ARRIVED);
    }

    @Test
    @DisplayName("업체 이동 시작 - 배송 담당자 배정")
    void start_company_moving() {
        // given
        Delivery delivery = createDelivery();
        UUID companyDeliveryManId = UUID.randomUUID();

        // when
        delivery.startCompanyMoving(companyDeliveryManId);

        // then
        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.COMPANY_MOVING);
        assertThat(delivery.getCompanyDeliveryManId()).isEqualTo(companyDeliveryManId);
    }

    @Test
    @DisplayName("배송 완료 상태로 변경")
    void complete_delivery() {
        // given
        Delivery delivery = createDelivery();

        // when
        delivery.completeDelivery();

        // then
        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
    }

    @Test
    @DisplayName("배송지 주소 변경")
    void update_address() {
        // given
        Delivery delivery = createDelivery();
        String newAddress = "서울시 서초구";

        // when
        delivery.updateAddress(newAddress);

        // then
        assertThat(delivery.getDeliveryAddress()).isEqualTo(newAddress);
    }

    @Test
    @DisplayName("배송 담당자 배정")
    void save_delivery_man() {
        // given
        Delivery delivery = createDelivery();
        UUID companyDeliveryManId = UUID.randomUUID();
        UUID hubDeliveryManId = UUID.randomUUID();

        // when
        delivery.saveDeliveryMan(companyDeliveryManId, hubDeliveryManId);

        // then
        assertThat(delivery.getCompanyDeliveryManId()).isEqualTo(companyDeliveryManId);
        assertThat(delivery.getHubDeliveryManId()).isEqualTo(hubDeliveryManId);
    }

    // Helper
    private Delivery createDelivery() {
        return Delivery.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "서울시 강남구",
                "홍길동",
                "@hong"
        );
    }
}
