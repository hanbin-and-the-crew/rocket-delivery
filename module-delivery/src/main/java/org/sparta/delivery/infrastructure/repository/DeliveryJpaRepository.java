package org.sparta.delivery.infrastructure.repository;

import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Delivery JPA Repository
 */
public interface DeliveryJpaRepository extends JpaRepository<Delivery, UUID> {

    /**
     * ID로 삭제되지 않은 배송 조회
     */
    Optional<Delivery> findByIdAndDeletedAtIsNull(UUID id);

    /**
     * 주문 ID로 삭제되지 않은 배송 조회
     */
    Optional<Delivery> findByOrderIdAndDeletedAtIsNull(UUID orderId);

    /**
     * 삭제되지 않은 모든 배송 목록 조회 (페이징, 정렬 지원)
     */
    @Query("SELECT d FROM Delivery d WHERE d.deletedAt IS NULL")
    Page<Delivery> findAllNotDeleted(Pageable pageable);

    /**
     * 배송 담당자 ID와 상태로 삭제되지 않은 배송 조회
     */
    @Query("""
        SELECT d FROM Delivery d 
        WHERE d.companyDeliveryManId = :deliveryManId 
        AND d.deliveryStatus = :status 
        AND d.deletedAt IS NULL
        """)
    List<Delivery> findByCompanyDeliveryManIdAndStatus(
            @Param("deliveryManId") UUID deliveryManId,
            @Param("status") DeliveryStatus status
    );

    /**
     * 허브 배송 담당자 ID와 상태로 삭제되지 않은 배송 조회
     */
    @Query("""
        SELECT d FROM Delivery d 
        WHERE d.hubDeliveryManId = :deliveryManId 
        AND d.deliveryStatus = :status 
        AND d.deletedAt IS NULL
        """)
    List<Delivery> findByHubDeliveryManIdAndStatus(
            @Param("deliveryManId") UUID deliveryManId,
            @Param("status") DeliveryStatus status
    );

    /**
     * 출발 허브 ID와 상태로 삭제되지 않은 배송 조회
     */
    @Query("""
        SELECT d FROM Delivery d 
        WHERE d.departureHubId = :hubId 
        AND d.deliveryStatus = :status 
        AND d.deletedAt IS NULL
        """)
    List<Delivery> findByDepartureHubIdAndStatus(
            @Param("hubId") UUID hubId,
            @Param("status") DeliveryStatus status
    );

    /**
     * 도착 허브 ID와 상태로 삭제되지 않은 배송 조회
     */
    @Query("""
        SELECT d FROM Delivery d 
        WHERE d.destinationHubId = :hubId 
        AND d.deliveryStatus = :status 
        AND d.deletedAt IS NULL
        """)
    List<Delivery> findByArrivalHubIdAndStatus(
            @Param("hubId") UUID hubId,
            @Param("status") DeliveryStatus status
    );

    /**
     * 특정 상태의 모든 배송 조회 (삭제된 것 제외)
     */
    @Query("""
        SELECT d FROM Delivery d 
        WHERE d.deliveryStatus = :status 
        AND d.deletedAt IS NULL
        """)
    List<Delivery> findByStatus(@Param("status") DeliveryStatus status);

    /**
     * 업체 배송 담당자가 배정된 모든 배송 조회
     */
    @Query("""
        SELECT d FROM Delivery d 
        WHERE d.companyDeliveryManId = :deliveryManId 
        AND d.deletedAt IS NULL
        ORDER BY d.createdAt DESC
        """)
    List<Delivery> findByCompanyDeliveryManId(@Param("deliveryManId") UUID deliveryManId);

    /**
     * 허브 배송 담당자가 배정된 모든 배송 조회
     */
    @Query("""
        SELECT d FROM Delivery d 
        WHERE d.hubDeliveryManId = :deliveryManId 
        AND d.deletedAt IS NULL
        ORDER BY d.createdAt DESC
        """)
    List<Delivery> findByHubDeliveryManId(@Param("deliveryManId") UUID deliveryManId);
}
