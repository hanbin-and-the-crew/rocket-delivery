package org.sparta.deliverylog.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.deliverylog.domain.entity.DeliveryLog;
import org.sparta.deliverylog.domain.repository.DeliveryLogRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryLogRepositoryImpl implements DeliveryLogRepository {

    private final DeliveryLogJpaRepository deliveryLogJpaRepository;
    private final EntityManager em;

    @Override
    public DeliveryLog save(DeliveryLog deliveryLog) {
        return deliveryLogJpaRepository.save(deliveryLog);
    }

    @Override
    public Optional<DeliveryLog> findByIdAndDeletedAtIsNull(UUID id) {
        return deliveryLogJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public List<DeliveryLog> findAllByDeliveryIdOrderBySequenceAsc(UUID deliveryId) {
        return deliveryLogJpaRepository
                .findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(deliveryId);
    }

    @Override
    public boolean existsByDeliveryIdAndSequenceAndDeletedAtIsNull(UUID deliveryId, int sequence) {
        return deliveryLogJpaRepository
                .existsByDeliveryIdAndSequenceAndDeletedAtIsNull(deliveryId, sequence);
    }

    /**
     * - 검색 로직 설계
     * hubId ⇒ source OR target
     * deliveryManId / deliveryId ⇒ AND 조건
     * deletedAt IS NULL 공통
     * createdAt asc/desc + 페이징(10/30/50)는 Pageable/Sort.Direction으로 제어
     * */
    @Override
    public Page<DeliveryLog> search(
            UUID hubId,
            UUID deliveryManId,
            UUID deliveryId,
            Pageable pageable,
            Sort.Direction direction
    ) {
        StringBuilder jpql = new StringBuilder(
                "SELECT dl FROM DeliveryLog dl " +
                        "WHERE dl.deletedAt IS NULL"
        );
        StringBuilder countJpql = new StringBuilder(
                "SELECT COUNT(dl) FROM DeliveryLog dl " +
                        "WHERE dl.deletedAt IS NULL"
        );

        Map<String, Object> params = new HashMap<>();

        // hubId: source 또는 target에 하나라도 해당되면 포함
        if (hubId != null) {
            jpql.append(" AND (dl.sourceHubId = :hubId OR dl.targetHubId = :hubId)");
            countJpql.append(" AND (dl.sourceHubId = :hubId OR dl.targetHubId = :hubId)");
            params.put("hubId", hubId);
        }

        if (deliveryManId != null) {
            jpql.append(" AND dl.deliveryManId = :deliveryManId");
            countJpql.append(" AND dl.deliveryManId = :deliveryManId");
            params.put("deliveryManId", deliveryManId);
        }

        if (deliveryId != null) {
            jpql.append(" AND dl.deliveryId = :deliveryId");
            countJpql.append(" AND dl.deliveryId = :deliveryId");
            params.put("deliveryId", deliveryId);
        }

        // 정렬: createdAt 기준 asc/desc
        jpql.append(" ORDER BY dl.createdAt ")
                .append(direction == Sort.Direction.DESC ? "DESC" : "ASC");

        TypedQuery<DeliveryLog> contentQuery = em.createQuery(jpql.toString(), DeliveryLog.class);
        TypedQuery<Long> countQuery = em.createQuery(countJpql.toString(), Long.class);

        params.forEach((key, value) -> {
            contentQuery.setParameter(key, value);
            countQuery.setParameter(key, value);
        });

        // 페이징
        contentQuery.setFirstResult((int) pageable.getOffset());
        contentQuery.setMaxResults(pageable.getPageSize());

        List<DeliveryLog> content = contentQuery.getResultList();
        long total = countQuery.getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }
}
