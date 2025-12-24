package org.sparta.delivery.infrastructure.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;
import org.sparta.delivery.domain.repository.DeliveryRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryRepositoryImpl implements DeliveryRepository {

    private final DeliveryJpaRepository deliveryJpaRepository;
    private final EntityManager em;

    @Override
    public Delivery save(Delivery delivery) {
        return deliveryJpaRepository.save(delivery);
    }

    @Override
    public Optional<Delivery> findByIdAndDeletedAtIsNull(UUID id) {
        return deliveryJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public boolean existsByOrderIdAndDeletedAtIsNull(UUID orderId) {
        return deliveryJpaRepository.existsByOrderIdAndDeletedAtIsNull(orderId);
    }

    @Override
    public Page<Delivery> search(
            DeliveryStatus status,
            UUID hubId,
            UUID companyId,
            Pageable pageable,
            Sort.Direction direction
    ) {
        StringBuilder jpql = new StringBuilder(
                "SELECT d FROM Delivery d " +
                        "WHERE d.deletedAt IS NULL"
        );
        StringBuilder countJpql = new StringBuilder(
                "SELECT COUNT(d) FROM Delivery d " +
                        "WHERE d.deletedAt IS NULL"
        );

        Map<String, Object> params = new HashMap<>();

        if (status != null) {
            jpql.append(" AND d.status = :status");
            countJpql.append(" AND d.status = :status");
            params.put("status", status);
        }

        // hubId: 공급 허브 또는 수령 허브 중 하나라도 해당되면 포함
        if (hubId != null) {
            jpql.append(" AND (d.supplierHubId = :hubId OR d.receiveHubId = :hubId)");
            countJpql.append(" AND (d.supplierHubId = :hubId OR d.receiveHubId = :hubId)");
            params.put("hubId", hubId);
        }

        // companyId: 공급 업체 또는 수령 업체 중 하나라도 해당되면 포함
        if (companyId != null) {
            jpql.append(" AND (d.supplierCompanyId = :companyId OR d.receiveCompanyId = :companyId)");
            countJpql.append(" AND (d.supplierCompanyId = :companyId OR d.receiveCompanyId = :companyId)");
            params.put("companyId", companyId);
        }

        // 정렬: createdAt 기준 asc/desc
        jpql.append(" ORDER BY d.createdAt ")
                .append(direction == Sort.Direction.DESC ? "DESC" : "ASC");

        TypedQuery<Delivery> contentQuery = em.createQuery(jpql.toString(), Delivery.class);
        TypedQuery<Long> countQuery = em.createQuery(countJpql.toString(), Long.class);

        params.forEach((key, value) -> {
            contentQuery.setParameter(key, value);
            countQuery.setParameter(key, value);
        });

        contentQuery.setFirstResult((int) pageable.getOffset());
        contentQuery.setMaxResults(pageable.getPageSize());

        List<Delivery> content = contentQuery.getResultList();
        long total = countQuery.getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Optional<Delivery> findByOrderIdAndDeletedAtIsNull(UUID orderId) {
        return deliveryJpaRepository.findByOrderIdAndDeletedAtIsNull(orderId);
    }

    @Override
    public List<Delivery> findByStatusNotAndCreatedAtAfterAndDeletedAtIsNull(
            DeliveryStatus status,
            LocalDateTime createdAfter) {
        return deliveryJpaRepository.findByStatusNotAndCreatedAtAfterAndDeletedAtIsNull(status, createdAfter);
    }
}
