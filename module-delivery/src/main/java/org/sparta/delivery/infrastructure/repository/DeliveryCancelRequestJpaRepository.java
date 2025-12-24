package org.sparta.delivery.infrastructure.repository;

import jakarta.persistence.LockModeType;
import org.sparta.delivery.domain.entity.DeliveryCancelRequest;
import org.sparta.delivery.domain.enumeration.CancelRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryCancelRequestJpaRepository extends JpaRepository<DeliveryCancelRequest, UUID> {

    /**
     * eventId로 존재 여부 확인 (멱등성 체크용)
     */
    boolean existsByCancelEventIdAndDeletedAtIsNull(UUID cancelEventId);

    /**
     * orderId로 Cancel Intent 조회 (삭제되지 않은 것만)
     */
    Optional<DeliveryCancelRequest> findByOrderIdAndDeletedAtIsNull(UUID orderId);

    /**
     * orderId로 Cancel Intent 조회 (PESSIMISTIC_WRITE 락)
     * - createWithRoute()와 handlePaymentCanceled() 동시성 보장
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM DeliveryCancelRequest c " +
            "WHERE c.orderId = :orderId AND c.deletedAt IS NULL")
    Optional<DeliveryCancelRequest> findWithLockByOrderId(@Param("orderId") UUID orderId);

    /**
     * REQUESTED 상태인 Cancel Intent 목록 조회 (스케줄러용)
     */
    @Query("SELECT c FROM DeliveryCancelRequest c " +
            "WHERE c.status = :status AND c.deletedAt IS NULL")
    List<DeliveryCancelRequest> findAllByStatusAndDeletedAtIsNull(@Param("status") CancelRequestStatus status);

    @Query("SELECT COUNT(cr) FROM DeliveryCancelRequest cr " +
            "LEFT JOIN DeliveryProcessedEvent pe ON pe.eventId = cr.cancelEventId " +
            "WHERE cr.status = :status " +
            "AND pe.id IS NULL " +
            "AND cr.deletedAt IS NULL " +
            "AND cr.createdAt > :cutoffTime")
    long countPendingPaymentCancelDlt(@Param("status") CancelRequestStatus status,
                                      @Param("cutoffTime") LocalDateTime cutoffTime);

    // 수정: 반환 타입을 boolean으로 변경
    boolean existsByOrderIdAndDeletedAtIsNull(UUID orderId);
}
