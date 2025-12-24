package org.sparta.delivery.infrastructure.repository;

import org.sparta.delivery.domain.entity.DeliveryCancelRequest;
import lombok.RequiredArgsConstructor;
import org.sparta.delivery.domain.enumeration.CancelRequestStatus;
import org.sparta.delivery.domain.repository.DeliveryCancelRequestRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryCancelRequestRepositoryImpl implements DeliveryCancelRequestRepository {

    private final DeliveryCancelRequestJpaRepository jpaRepository;

    @Override
    public DeliveryCancelRequest save(DeliveryCancelRequest cancelRequest) {
        return jpaRepository.save(cancelRequest);
    }

    @Override
    public boolean existsByCancelEventIdAndDeletedAtIsNull(UUID cancelEventId){
        return jpaRepository.existsByCancelEventIdAndDeletedAtIsNull(cancelEventId);
    }

    @Override
    public Optional<DeliveryCancelRequest> findByOrderIdAndDeletedAtIsNull(UUID orderId){
        return jpaRepository.findByOrderIdAndDeletedAtIsNull(orderId);
    }

    @Override
    public Optional<DeliveryCancelRequest> findWithLockByOrderId(UUID orderId) {
        return jpaRepository.findWithLockByOrderId(orderId);
    }

    @Override
    public List<DeliveryCancelRequest> findAllByStatusAndDeletedAtIsNull(CancelRequestStatus status) {
        return jpaRepository.findAllByStatusAndDeletedAtIsNull(status);
    }

    @Override
    public DeliveryCancelRequest saveAndFlush(DeliveryCancelRequest entity) {
        return jpaRepository.saveAndFlush(entity);
    }

    @Override
    public long countPendingPaymentCancelDlt(CancelRequestStatus status, LocalDateTime cutoffTime) {
        return jpaRepository.countPendingPaymentCancelDlt(status, cutoffTime);
    }

    // 수정: 구현 변경
    @Override
    public boolean existsByOrderIdAndDeletedAtIsNull(UUID orderId){
        return jpaRepository.existsByOrderIdAndDeletedAtIsNull(orderId);
    }
}
