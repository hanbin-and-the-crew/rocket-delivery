package org.sparta.order.domain.saga;

import org.sparta.order.domain.saga.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, UUID> {

    // 1. 주문ID로 단일 조회 (가장 중요)
    Optional<SagaState> findByOrderId(UUID orderId);

    // 2. 상태별 조회 (운영용)
    List<SagaState> findAllByOverallStatus(String overallStatus);

    // 3. 실패한 사가 조회 (자동 복구용)
    @Query("SELECT s FROM SagaState s WHERE s.overallStatus IN ('FAILED', 'COMPENSATING') AND s.updatedAt < :threshold")
    List<SagaState> findStuckSagas(@Param("threshold") LocalDateTime threshold);

    // 4. 주문 상태별 조회
    List<SagaState> findAllByOrderStatus(String orderStatus);

    // 5. 전체 진행중인 사가 개수 (대시보드용)
    @Query("SELECT COUNT(s) FROM SagaState s WHERE s.overallStatus = 'IN_PROGRESS'")
    long countInProgress();
}
