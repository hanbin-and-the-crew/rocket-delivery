package org.sparta.deliveryman.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.deliveryman.domain.entity.DeliveryMan;
import org.sparta.deliveryman.domain.enumeration.DeliveryManStatus;
import org.sparta.deliveryman.domain.enumeration.DeliveryManType;
import org.sparta.deliveryman.domain.repository.DeliveryManRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryManRepositoryImpl implements DeliveryManRepository {

    private final DeliveryManJpaRepository jpaRepository;

    @Override
    public DeliveryMan save(DeliveryMan deliveryMan) {
        return jpaRepository.save(deliveryMan);
    }

    @Override
    public Optional<DeliveryMan> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<DeliveryMan> findByIdAndNotDeleted(UUID id) {
        return jpaRepository.findByIdAndNotDeleted(id);
    }

    @Override
    public Page<DeliveryMan> findAllActive(Pageable pageable) {
        return jpaRepository.findAllActive(pageable);
    }

    @Override
    public void softDelete(DeliveryMan deliveryMan) {
        deliveryMan.delete();
        jpaRepository.save(deliveryMan);
    }

    @Override
    public List<DeliveryMan> findIdleDeliveryMenOrderByLastDeliveryCompletedAtAsc(DeliveryManStatus status) {
        return jpaRepository.findByStatusOrderByLastDeliveryCompletedAtAsc(status);
    }

    // === 허브 배송 담당자 대상 조회 ===
    public List<DeliveryMan> findHubDeliveryManagersByAffiliationHubOrderByAssignedDeliveryCountAsc(
            UUID hubId, DeliveryManStatus status) {
        return jpaRepository.findByAffiliationHubIdAndDeliveryManTypeAndStatusOrderByAssignedDeliveryCountAsc(
                hubId, DeliveryManType.HUB_DELIVERY_MAN, status);
    }

    // === 업체 배송 담당자 대상 조회 ===
    public List<DeliveryMan> findPartnerDeliveryManagersByAffiliationHubOrderByAssignedDeliveryCountAsc(
            UUID hubId, DeliveryManStatus status) {
        return jpaRepository.findByAffiliationHubIdAndDeliveryManTypeAndStatusOrderByAssignedDeliveryCountAsc(
                hubId, DeliveryManType.COMPANY_DELIVERY_MAN, status);
    }

    @Override
    public List<DeliveryMan> findByAffiliationHubIdAndDeliveryManTypeAndStatusOrderByAssignedDeliveryCountAsc(
            UUID affiliationHubId, DeliveryManType deliveryManType, DeliveryManStatus status) {
        return jpaRepository.findByAffiliationHubIdAndDeliveryManTypeAndStatusOrderByAssignedDeliveryCountAsc(
                affiliationHubId, deliveryManType, status);
    }

}
