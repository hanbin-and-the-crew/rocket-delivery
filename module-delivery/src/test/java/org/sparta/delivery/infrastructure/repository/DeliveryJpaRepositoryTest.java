package org.sparta.delivery.infrastructure.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.delivery.application.dto.DeliverySearchCondition;
import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
@DisplayName("DeliveryJpaRepository 테스트")
class DeliveryJpaRepositoryTest {

    @Autowired
    private DeliveryJpaRepository deliveryRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 데이터 초기화
        deliveryRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("배송 저장 성공")
    void save_delivery_success() {
        // given
        Delivery delivery = createTestDelivery();

        // when
        Delivery savedDelivery = deliveryRepository.save(delivery);
        entityManager.flush();

        // then
        assertThat(savedDelivery.getId()).isNotNull();
        assertThat(savedDelivery.getOrderId()).isEqualTo(delivery.getOrderId());
    }

    @Test
    @DisplayName("ID로 배송 조회 성공 - 삭제되지 않은 배송")
    void find_by_id_and_deleted_at_is_null_success() {
        // given
        Delivery delivery = createTestDelivery();
        Delivery savedDelivery = deliveryRepository.save(delivery);
        entityManager.flush();

        // when
        Optional<Delivery> result = deliveryRepository.findByIdAndDeletedAtIsNull(savedDelivery.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedDelivery.getId());
    }

    @Test
    @DisplayName("ID로 배송 조회 실패 - 삭제된 배송")
    void find_by_id_and_deleted_at_is_null_fail_deleted() {
        // given
        Delivery delivery = createTestDelivery();
        Delivery savedDelivery = deliveryRepository.save(delivery);
        savedDelivery.delete(UUID.randomUUID());
        deliveryRepository.save(savedDelivery);
        entityManager.flush();

        // when
        Optional<Delivery> result = deliveryRepository.findByIdAndDeletedAtIsNull(savedDelivery.getId());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("주문 ID로 배송 조회 성공")
    void find_by_order_id_and_deleted_at_is_null_success() {
        // given
        Delivery delivery = createTestDelivery();
        Delivery savedDelivery = deliveryRepository.save(delivery);
        entityManager.flush();

        // when
        Optional<Delivery> result = deliveryRepository.findByOrderIdAndDeletedAtIsNull(savedDelivery.getOrderId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getOrderId()).isEqualTo(savedDelivery.getOrderId());
    }

    @Test
    @DisplayName("배송 검색 성공 - 조건 없음")
    void search_deliveries_success_no_condition() {
        // given
        Delivery delivery1 = createTestDelivery();
        Delivery delivery2 = createTestDelivery();
        deliveryRepository.saveAll(List.of(delivery1, delivery2));
        entityManager.flush();

        DeliverySearchCondition condition = DeliverySearchCondition.empty();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Delivery> result = deliveryRepository.searchDeliveries(condition, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("배송 검색 성공 - 주문 ID 조건")
    void search_deliveries_success_with_order_id() {
        // given
        UUID orderId = UUID.randomUUID();
        Delivery delivery1 = Delivery.create(
                orderId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "서울특별시 강남구 테헤란로 123",
                "홍길동",
                "@홍길동"
        );
        Delivery delivery2 = createTestDelivery();
        deliveryRepository.saveAll(List.of(delivery1, delivery2));
        entityManager.flush();

        DeliverySearchCondition condition = DeliverySearchCondition.of(orderId, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Delivery> result = deliveryRepository.searchDeliveries(condition, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOrderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("배송 검색 성공 - 배송 상태 조건")
    void search_deliveries_success_with_status() {
        // given
        Delivery delivery1 = createTestDelivery();
        delivery1.hubMoving();
        Delivery delivery2 = createTestDelivery();
        deliveryRepository.saveAll(List.of(delivery1, delivery2));
        entityManager.flush();

        DeliverySearchCondition condition = DeliverySearchCondition.of(null, null, null, DeliveryStatus.HUB_MOVING, null);
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Delivery> result = deliveryRepository.searchDeliveries(condition, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeliveryStatus()).isEqualTo(DeliveryStatus.HUB_MOVING);
    }

    @Test
    @DisplayName("배송 검색 성공 - 수령인 이름 조건 (LIKE 검색)")
    void search_deliveries_success_with_recipient_name() {
        // given
        Delivery delivery1 = Delivery.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "서울특별시 강남구 테헤란로 123",
                "김철수",
                "@김철수"
        );
        Delivery delivery2 = createTestDelivery();
        deliveryRepository.saveAll(List.of(delivery1, delivery2));
        entityManager.flush();

        DeliverySearchCondition condition = DeliverySearchCondition.of(null, null, null, null, "김철");
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Delivery> result = deliveryRepository.searchDeliveries(condition, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRecipientName()).contains("김철");
    }

    @Test
    @DisplayName("배송 검색 성공 - 여러 조건 동시 적용")
    void search_deliveries_success_with_multiple_conditions() {
        // given
        UUID departureHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();

        Delivery delivery1 = Delivery.create(
                UUID.randomUUID(),
                departureHubId,
                destinationHubId,
                "서울특별시 강남구 테헤란로 123",
                "홍길동",
                "@홍길동"
        );
        delivery1.hubMoving();

        Delivery delivery2 = createTestDelivery();
        deliveryRepository.saveAll(List.of(delivery1, delivery2));
        entityManager.flush();

        DeliverySearchCondition condition = DeliverySearchCondition.of(
                null,
                departureHubId,
                destinationHubId,
                DeliveryStatus.HUB_MOVING,
                "홍길"
        );
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Delivery> result = deliveryRepository.searchDeliveries(condition, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDepartureHubId()).isEqualTo(departureHubId);
        assertThat(result.getContent().get(0).getDestinationHubId()).isEqualTo(destinationHubId);
        assertThat(result.getContent().get(0).getDeliveryStatus()).isEqualTo(DeliveryStatus.HUB_MOVING);
    }

    @Test
    @DisplayName("배송 검색 - 페이징 확인")
    void search_deliveries_with_paging() {
        // given
        for (int i = 0; i < 25; i++) {
            deliveryRepository.save(createTestDelivery());
        }
        entityManager.flush();

        DeliverySearchCondition condition = DeliverySearchCondition.empty();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Delivery> result = deliveryRepository.searchDeliveries(condition, pageable);

        // then
        assertThat(result.getContent()).hasSize(10);
        assertThat(result.getTotalElements()).isEqualTo(25);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("배송 검색 - 정렬 확인 (생성일 기준)")
    void search_deliveries_with_sorting() {
        // given
        Delivery delivery1 = createTestDelivery();
        deliveryRepository.save(delivery1);
        entityManager.flush();

        // 시간 차이 보장
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Delivery delivery2 = createTestDelivery();
        deliveryRepository.save(delivery2);
        entityManager.flush();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Delivery delivery3 = createTestDelivery();
        deliveryRepository.save(delivery3);
        entityManager.flush();

        DeliverySearchCondition condition = DeliverySearchCondition.empty();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Delivery> result = deliveryRepository.searchDeliveries(condition, pageable);

        // then
        assertThat(result.getContent()).hasSize(3);

        // createdAt 기준 정렬 검증
        List<Delivery> deliveries = result.getContent();
        assertThat(deliveries.get(0).getCreatedAt()).isNotNull();
        assertThat(deliveries.get(1).getCreatedAt()).isNotNull();
        assertThat(deliveries.get(2).getCreatedAt()).isNotNull();

        // 최신순으로 정렬되어 있는지 확인 (DESC)
        assertThat(deliveries.get(0).getCreatedAt())
                .isAfterOrEqualTo(deliveries.get(1).getCreatedAt());
        assertThat(deliveries.get(1).getCreatedAt())
                .isAfterOrEqualTo(deliveries.get(2).getCreatedAt());
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
