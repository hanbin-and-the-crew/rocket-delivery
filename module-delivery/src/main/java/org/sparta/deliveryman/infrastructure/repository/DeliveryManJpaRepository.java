package org.sparta.deliveryman.infrastructure.repository;

import org.sparta.deliveryman.domain.entity.DeliveryMan;
import org.sparta.deliveryman.domain.enumeration.DeliveryManStatus;
import org.sparta.deliveryman.domain.enumeration.DeliveryManType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryManJpaRepository extends JpaRepository<DeliveryMan, UUID> {

    Optional<DeliveryMan> findByIdAndDeletedAtIsNull(UUID id);

    Optional<DeliveryMan> findByUserIdAndDeletedAtIsNull(UUID userId);

    boolean existsByUserIdAndDeletedAtIsNull(UUID userId);

    // 타입 기준 max(sequence)
    @Query("""
           SELECT MAX(d.sequence)
           FROM DeliveryMan d
           WHERE d.type = :type
             AND d.deletedAt IS NULL
           """)
    Integer findMaxSequenceByTypeAndDeletedAtIsNull(@Param("type") DeliveryManType type);

    // 허브 + 타입 기준 max(sequence)
    @Query("""
           SELECT MAX(d.sequence)
           FROM DeliveryMan d
           WHERE d.hubId = :hubId
             AND d.type = :type
             AND d.deletedAt IS NULL
           """)
    Integer findMaxSequenceByHubIdAndTypeAndDeletedAtIsNull(@Param("hubId") UUID hubId,
                                                            @Param("type") DeliveryManType type);

    // 허브 공용(HUB 타입) 배정 후보 – sequence ASC
    List<DeliveryMan> findAllByTypeAndDeletedAtIsNullOrderBySequenceAsc(DeliveryManType type);

    // 허브별 COMPANY 타입 배정 후보 – sequence ASC
    List<DeliveryMan> findAllByHubIdAndTypeAndDeletedAtIsNullOrderBySequenceAsc(UUID hubId,
                                                                                DeliveryManType type);

    // 검색용 – 동적 조건을 JPQL로 처리 (nullable 파라미터 허용)
    @Query("""
           SELECT d
           FROM DeliveryMan d
           WHERE d.deletedAt IS NULL
             AND (:hubId IS NULL OR d.hubId = :hubId)
             AND (:type IS NULL OR d.type = :type)
             AND (:status IS NULL OR d.status = :status)
             AND (:realName IS NULL OR d.realName LIKE CONCAT('%', :realName, '%'))
           """)
    List<DeliveryMan> search(@Param("hubId") UUID hubId,
                             @Param("type") DeliveryManType type,
                             @Param("status") DeliveryManStatus status,
                             @Param("realName") String realName);
}
