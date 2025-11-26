package org.sparta.delivery.infrastructure.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;
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

    // ========== 배송 저장 테스트 ==========

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

    // ========== ID로 배송 조회 테스트 ==========

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

    // ========== 주문 ID로 배송 조회 테스트 ==========

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

    // ========== 배송 목록 조회 테스트 (페이징) ==========

    @Test
    @DisplayName("배송 목록 조회 성공 - 조건 없음")
    void find_all_not_deleted_success() {
        // given
        Delivery delivery1 = createTestDelivery();
        Delivery delivery2 = createTestDelivery();
        deliveryRepository.saveAll(List.of(delivery1, delivery2));
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Delivery> result = deliveryRepository.findAllNotDeleted(pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("배송 목록 조회 성공 - 빈 결과")
    void find_all_not_deleted_success_empty() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Delivery> result = deliveryRepository.findAllNotDeleted(pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("배송 목록 조회 - 페이징 확인")
    void find_all_not_deleted_with_paging() {
        // given
        for (int i = 0; i < 25; i++) {
            deliveryRepository.save(createTestDelivery());
        }
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Delivery> result = deliveryRepository.findAllNotDeleted(pageable);

        // then
        assertThat(result.getContent()).hasSize(10);
        assertThat(result.getTotalElements()).isEqualTo(25);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("배송 목록 조회 - 정렬 확인 (생성일 기준)")
    void find_all_not_deleted_with_sorting() {
        // given
        Delivery delivery1 = createTestDelivery();
        deliveryRepository.save(delivery1);
        entityManager.flush();

        // 시간 차이 보장
        sleep(100);

        Delivery delivery2 = createTestDelivery();
        deliveryRepository.save(delivery2);
        entityManager.flush();

        sleep(100);

        Delivery delivery3 = createTestDelivery();
        deliveryRepository.save(delivery3);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Delivery> result = deliveryRepository.findAllNotDeleted(pageable);

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

    @Test
    @DisplayName("배송 목록 조회 - 삭제된 배송 제외")
    void find_all_not_deleted_exclude_deleted() {
        // given
        Delivery delivery1 = createTestDelivery();
        Delivery delivery2 = createTestDelivery();
        Delivery delivery3 = createTestDelivery();

        deliveryRepository.saveAll(List.of(delivery1, delivery2, delivery3));
        entityManager.flush();

        // delivery2 삭제
        delivery2.delete(UUID.randomUUID());
        deliveryRepository.save(delivery2);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Delivery> result = deliveryRepository.findAllNotDeleted(pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);

        // delivery2가 포함되지 않아야 함
        assertThat(result.getContent())
                .noneMatch(d -> d.getId().equals(delivery2.getId()));
    }

    // ========== 상태별 배송 조회 테스트 ==========

    @Test
    @DisplayName("특정 상태의 배송 조회 성공")
    void find_by_status_success() {
        // given
        Delivery delivery1 = createTestDelivery();
        delivery1.hubMoving();

        Delivery delivery2 = createTestDelivery();
        Delivery delivery3 = createTestDelivery();

        deliveryRepository.saveAll(List.of(delivery1, delivery2, delivery3));
        entityManager.flush();

        // when
        List<Delivery> result = deliveryRepository.findByStatus(DeliveryStatus.HUB_MOVING);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeliveryStatus()).isEqualTo(DeliveryStatus.HUB_MOVING);
    }

    // ========== 배송 담당자별 조회 테스트 ==========

    @Test
    @DisplayName("업체 배송 담당자 ID와 상태로 배송 조회 성공")
    void find_by_company_delivery_man_id_and_status_success() {
        // given
        UUID companyManId = UUID.randomUUID();

        Delivery delivery1 = createTestDelivery();
        delivery1.startCompanyMoving(companyManId);

        Delivery delivery2 = createTestDelivery();

        deliveryRepository.saveAll(List.of(delivery1, delivery2));
        entityManager.flush();

        // when
        List<Delivery> result = deliveryRepository.findByCompanyDeliveryManIdAndStatus(
                companyManId,
                DeliveryStatus.COMPANY_MOVING
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCompanyDeliveryManId()).isEqualTo(companyManId);
        assertThat(result.get(0).getDeliveryStatus()).isEqualTo(DeliveryStatus.COMPANY_MOVING);
    }

    @Test
    @DisplayName("업체 배송 담당자가 배정된 모든 배송 조회")
    void find_by_company_delivery_man_id_success() {
        // given
        UUID companyManId = UUID.randomUUID();

        Delivery delivery1 = createTestDelivery();
        delivery1.startCompanyMoving(companyManId);

        Delivery delivery2 = createTestDelivery();
        delivery2.startCompanyMoving(companyManId);

        Delivery delivery3 = createTestDelivery();

        deliveryRepository.saveAll(List.of(delivery1, delivery2, delivery3));
        entityManager.flush();

        // when
        List<Delivery> result = deliveryRepository.findByCompanyDeliveryManId(companyManId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(d -> d.getCompanyDeliveryManId().equals(companyManId));
    }

    // ========== Helper 메서드 ==========

    /**
     * 테스트용 Delivery 객체 생성
     */
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

    /**
     * Thread.sleep을 간단하게 사용
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
