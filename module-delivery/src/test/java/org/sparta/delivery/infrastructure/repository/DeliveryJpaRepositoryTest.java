package org.sparta.delivery.infrastructure.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.fixture.DeliveryFixture;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("DeliveryJpaRepository 테스트")
class DeliveryJpaRepositoryTest {

    @Autowired
    private DeliveryJpaRepository deliveryJpaRepository;

    @Test
    @DisplayName("배송 저장")
    void save_delivery() {
        // given
        Delivery delivery = DeliveryFixture.createDelivery();

        // when
        Delivery savedDelivery = deliveryJpaRepository.save(delivery);

        // then
        assertThat(savedDelivery).isNotNull();
        assertThat(savedDelivery.getId()).isNotNull();
    }

    @Test
    @DisplayName("배송 ID로 조회")
    void find_by_id() {
        // given
        Delivery delivery = DeliveryFixture.createDelivery();
        Delivery savedDelivery = deliveryJpaRepository.save(delivery);

        // when
        Optional<Delivery> foundDelivery = deliveryJpaRepository.findById(savedDelivery.getId());

        // then
        assertThat(foundDelivery).isPresent();
        assertThat(foundDelivery.get().getId()).isEqualTo(savedDelivery.getId());
    }

    @Test
    @DisplayName("삭제되지 않은 배송 조회")
    void find_by_id_and_deleted_at_is_null() {
        // given
        Delivery delivery = DeliveryFixture.createDelivery();
        Delivery savedDelivery = deliveryJpaRepository.save(delivery);

        // when
        Optional<Delivery> foundDelivery = deliveryJpaRepository.findByIdAndDeletedAtIsNull(savedDelivery.getId());

        // then
        assertThat(foundDelivery).isPresent();
        assertThat(foundDelivery.get().getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("주문 ID로 배송 조회")
    void find_by_order_id() {
        // given
        UUID orderId = UUID.randomUUID();
        Delivery delivery = DeliveryFixture.createDelivery(orderId);
        deliveryJpaRepository.save(delivery);

        // when
        Optional<Delivery> foundDelivery = deliveryJpaRepository.findByOrderIdAndDeletedAtIsNull(orderId);

        // then
        assertThat(foundDelivery).isPresent();
        assertThat(foundDelivery.get().getOrderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("배송 삭제")
    void delete_delivery() {
        // given
        Delivery delivery = DeliveryFixture.createDelivery();
        Delivery savedDelivery = deliveryJpaRepository.save(delivery);

        // when
        deliveryJpaRepository.delete(savedDelivery);

        // then
        Optional<Delivery> foundDelivery = deliveryJpaRepository.findById(savedDelivery.getId());
        assertThat(foundDelivery).isEmpty();
    }
}
