package org.sparta.deliverylog.infrasturcture.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.deliverylog.domain.entity.DeliveryLog;
import org.sparta.deliverylog.domain.enumeration.DeliveryRouteStatus;
import org.sparta.deliverylog.infrastructure.repository.DeliveryLogRepository;
import org.sparta.deliverylog.infrastructure.repository.DeliveryLogRepositoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import(DeliveryLogRepositoryImpl.class)
@DisplayName("DeliveryLogRepository 테스트")
class DeliveryLogRepositoryTest {

    @Autowired
    private DeliveryLogRepository deliveryLogRepository;

    @Test
    @DisplayName("배송 경로 저장 성공")
    void save_Success() {
        // given
        DeliveryLog deliveryLog = createTestDeliveryLog();

        // when
        DeliveryLog saved = deliveryLogRepository.save(deliveryLog);

        // then
        assertThat(saved.getDeliveryLogId()).isNotNull();
        assertThat(saved.getDeliveryId()).isEqualTo(deliveryLog.getDeliveryId());
    }

    @Test
    @DisplayName("배송 경로 ID로 조회 성공")
    void findById_Success() {
        // given
        DeliveryLog deliveryLog = deliveryLogRepository.save(createTestDeliveryLog());

        // when
        Optional<DeliveryLog> found = deliveryLogRepository.findById(deliveryLog.getDeliveryLogId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getDeliveryLogId()).isEqualTo(deliveryLog.getDeliveryLogId());
    }

    @Test
    @DisplayName("배송 ID로 전체 경로 조회 - 순서대로")
    void findByDeliveryIdOrderByHubSequence_Success() {
        // given
        UUID deliveryId = UUID.randomUUID();
        DeliveryLog log1 = createTestDeliveryLogWithSequence(deliveryId, 1);
        DeliveryLog log2 = createTestDeliveryLogWithSequence(deliveryId, 2);
        DeliveryLog log3 = createTestDeliveryLogWithSequence(deliveryId, 3);

        deliveryLogRepository.save(log3);
        deliveryLogRepository.save(log1);
        deliveryLogRepository.save(log2);

        // when
        List<DeliveryLog> results = deliveryLogRepository.findByDeliveryIdOrderByHubSequence(deliveryId);

        // then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getHubSequence()).isEqualTo(1);
        assertThat(results.get(1).getHubSequence()).isEqualTo(2);
        assertThat(results.get(2).getHubSequence()).isEqualTo(3);
    }

    @Test
    @DisplayName("배송 담당자의 진행 중인 경로 조회")
    void findByDeliveryManIdAndDeliveryStatusIn_Success() {
        // given
        UUID deliveryManId = UUID.randomUUID();
        DeliveryLog waiting = createTestDeliveryLog();
        waiting.assignDeliveryMan(deliveryManId);

        DeliveryLog moving = createTestDeliveryLog();
        moving.assignDeliveryMan(deliveryManId);
        moving.startDelivery();

        DeliveryLog completed = createTestDeliveryLog();
        completed.assignDeliveryMan(deliveryManId);
        completed.startDelivery();
        completed.completeDelivery(10.0, 30);

        deliveryLogRepository.save(waiting);
        deliveryLogRepository.save(moving);
        deliveryLogRepository.save(completed);

        List<DeliveryRouteStatus> inProgressStatuses = Arrays.asList(
                DeliveryRouteStatus.WAITING,
                DeliveryRouteStatus.MOVING
        );

        // when
        List<DeliveryLog> results = deliveryLogRepository.findByDeliveryManIdAndDeliveryStatusIn(
                deliveryManId,
                inProgressStatuses
        );

        // then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(DeliveryLog::getDeliveryStatus)
                .containsExactlyInAnyOrder(DeliveryRouteStatus.WAITING, DeliveryRouteStatus.MOVING);
    }

    @Test
    @DisplayName("허브의 대기 중인 경로 조회")
    void findByDepartureHubIdAndDeliveryStatus_Success() {
        // given
        UUID hubId = UUID.randomUUID();
        DeliveryLog waiting1 = createTestDeliveryLogWithDepartureHub(hubId);
        DeliveryLog waiting2 = createTestDeliveryLogWithDepartureHub(hubId);

        DeliveryLog moving = createTestDeliveryLogWithDepartureHub(hubId);
        moving.assignDeliveryMan(UUID.randomUUID());
        moving.startDelivery();

        deliveryLogRepository.save(waiting1);
        deliveryLogRepository.save(waiting2);
        deliveryLogRepository.save(moving);

        // when
        List<DeliveryLog> results = deliveryLogRepository.findByDepartureHubIdAndDeliveryStatus(
                hubId,
                DeliveryRouteStatus.WAITING
        );

        // then
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(log -> log.getDeliveryStatus() == DeliveryRouteStatus.WAITING);
    }

    @Test
    @DisplayName("전체 목록 조회 - 페이징")
    void findAllActive_Success() {
        // given
        deliveryLogRepository.save(createTestDeliveryLog());
        deliveryLogRepository.save(createTestDeliveryLog());
        deliveryLogRepository.save(createTestDeliveryLog());

        PageRequest pageRequest = PageRequest.of(0, 2);

        // when
        Page<DeliveryLog> results = deliveryLogRepository.findAllActive(pageRequest);

        // then
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("배송 경로 논리 삭제")
    void delete_Success() {
        // given
        DeliveryLog deliveryLog = deliveryLogRepository.save(createTestDeliveryLog());

        // when
        deliveryLogRepository.delete(deliveryLog);

        // then
        Optional<DeliveryLog> found = deliveryLogRepository.findById(deliveryLog.getDeliveryLogId());
        assertThat(found).isEmpty();
    }

    // ========== Helper Methods ==========

    private DeliveryLog createTestDeliveryLog() {
        return DeliveryLog.create(
                UUID.randomUUID(),
                1,
                UUID.randomUUID(),
                UUID.randomUUID(),
                10.0,
                30
        );
    }

    private DeliveryLog createTestDeliveryLogWithSequence(UUID deliveryId, Integer sequence) {
        return DeliveryLog.create(
                deliveryId,
                sequence,
                UUID.randomUUID(),
                UUID.randomUUID(),
                10.0,
                30
        );
    }

    private DeliveryLog createTestDeliveryLogWithDepartureHub(UUID departureHubId) {
        return DeliveryLog.create(
                UUID.randomUUID(),
                1,
                departureHubId,
                UUID.randomUUID(),
                10.0,
                30
        );
    }
}
