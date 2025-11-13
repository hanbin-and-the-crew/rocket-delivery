package org.sparta.delivery.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;
import org.sparta.delivery.fixture.DeliveryFixture;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Delivery Entity 테스트")
class DeliveryTest {

    @Test
    @DisplayName("배송 생성 성공")
    void create_delivery_success() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID departureHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        String deliveryAddress = "서울특별시 강남구 테헤란로 123";
        String recipientName = "홍길동";
        String recipientSlackId = "@홍길동";

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
        assertThat(delivery.getDepartureHubId()).isEqualTo(departureHubId);
        assertThat(delivery.getDestinationHubId()).isEqualTo(destinationHubId);
        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.HUB_WAITING);
        assertThat(delivery.getDeliveryAddress()).isEqualTo(deliveryAddress);
        assertThat(delivery.getRecipientName()).isEqualTo(recipientName);
        assertThat(delivery.getRecipientSlackId()).isEqualTo(recipientSlackId);
    }

    @Test
    @DisplayName("허브 대기 상태로 변경")
    void hub_waiting_success() {
        // given
        Delivery delivery = createTestDelivery();
        delivery.hubMoving();

        // when
        delivery.hubWaiting();

        // then
        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.HUB_WAITING);
    }

    @Test
    @DisplayName("허브 이동 중 상태로 변경")
    void hub_moving_success() {
        // given
        Delivery delivery = createTestDelivery();

        // when
        delivery.hubMoving();

        // then
        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.HUB_MOVING);
    }

    @Test
    @DisplayName("목적지 허브 도착 상태로 변경")
    void arrive_at_destination_hub_success() {
        // given
        Delivery delivery = createTestDelivery();
        delivery.hubMoving();

        // when
        delivery.arriveAtDestinationHub();

        // then
        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.DEST_HUB_ARRIVED);
    }

    @Test
    @DisplayName("업체 배송 시작")
    void start_company_moving_success() {
        // given
        Delivery delivery = createTestDelivery();
        UUID companyDeliveryManId = UUID.randomUUID();

        // when
        delivery.startCompanyMoving(companyDeliveryManId);

        // then
        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.COMPANY_MOVING);
        assertThat(delivery.getCompanyDeliveryManId()).isEqualTo(companyDeliveryManId);
    }

    @Test
    @DisplayName("배송 완료")
    void complete_delivery_success() {
        // given
        Delivery delivery = createTestDelivery();
        delivery.startCompanyMoving(UUID.randomUUID());

        // when
        delivery.completeDelivery();

        // then
        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
    }

    @Test
    @DisplayName("주소 변경 성공")
    void update_address_success() {
        // given
        Delivery delivery = createTestDelivery();
        String newAddress = "서울특별시 강남구 역삼로 456";

        // when
        delivery.updateAddress(newAddress);

        // then
        assertThat(delivery.getDeliveryAddress()).isEqualTo(newAddress);
    }

    @Test
    @DisplayName("배송 담당자 배정 성공")
    void save_delivery_man_success() {
        // given
        Delivery delivery = createTestDelivery();
        UUID companyDeliveryManId = UUID.randomUUID();
        UUID hubDeliveryManId = UUID.randomUUID();

        // when
        delivery.saveDeliveryMan(companyDeliveryManId, hubDeliveryManId);

        // then
        assertThat(delivery.getCompanyDeliveryManId()).isEqualTo(companyDeliveryManId);
        assertThat(delivery.getHubDeliveryManId()).isEqualTo(hubDeliveryManId);
    }

    @Test
    @DisplayName("업체 배송 담당자만 배정")
    void assign_company_delivery_man_success() {
        // given
        Delivery delivery = createTestDelivery();
        UUID companyDeliveryManId = UUID.randomUUID();

        // when
        delivery.assignCompanyDeliveryMan(companyDeliveryManId);

        // then
        assertThat(delivery.getCompanyDeliveryManId()).isEqualTo(companyDeliveryManId);
    }

    @Test
    @DisplayName("허브 배송 담당자만 배정")
    void assign_hub_delivery_man_success() {
        // given
        Delivery delivery = createTestDelivery();
        UUID hubDeliveryManId = UUID.randomUUID();

        // when
        delivery.assignHubDeliveryMan(hubDeliveryManId);

        // then
        assertThat(delivery.getHubDeliveryManId()).isEqualTo(hubDeliveryManId);
    }

    @Test
    @DisplayName("배송 삭제 성공")
    void delete_delivery_success() {
        // given
        Delivery delivery = createTestDelivery();
        UUID userId = UUID.randomUUID();

        // when
        delivery.delete(userId);

        // then
        assertThat(delivery.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("배송 취소 가능 여부 확인 - HUB_WAITING")
    void is_cancellable_hub_waiting() {
        // given
        Delivery delivery = createTestDelivery();

        // when & then
        assertThat(delivery.isCancellable()).isTrue();
    }

    @Test
    @DisplayName("배송 취소 가능 여부 확인 - HUB_MOVING")
    void is_cancellable_hub_moving() {
        // given
        Delivery delivery = createTestDelivery();
        delivery.hubMoving();

        // when & then
        assertThat(delivery.isCancellable()).isTrue();
    }

    @Test
    @DisplayName("배송 취소 불가능 - DELIVERED")
    void is_not_cancellable_delivered() {
        // given
        Delivery delivery = DeliveryFixture.createDelivery();
        delivery.startCompanyMoving(UUID.randomUUID());
        delivery.completeDelivery();

        // when & then
        assertThat(delivery.isCancellable()).isFalse();
    }

    @Test
    @DisplayName("배송 완료 여부 확인")
    void is_completed_success() {
        // given
        Delivery delivery = DeliveryFixture.createDelivery();

        // when
        delivery.startCompanyMoving(UUID.randomUUID());
        delivery.completeDelivery();

        // then
        assertThat(delivery.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("배송 담당자 배정 여부 확인")
    void has_delivery_man_success() {
        // given
        Delivery delivery = createTestDelivery();
        delivery.assignCompanyDeliveryMan(UUID.randomUUID());

        // when & then
        assertThat(delivery.hasDeliveryMan()).isTrue();
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
