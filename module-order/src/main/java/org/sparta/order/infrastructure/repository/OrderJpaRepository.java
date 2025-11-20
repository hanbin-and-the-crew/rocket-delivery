package org.sparta.order.infrastructure.repository;

import org.sparta.order.domain.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Order JPA Repository
 */
@Repository
public interface OrderJpaRepository extends JpaRepository<Order, UUID> {

    /**
     * Soft Delete 고려한 단건 조회
     */
    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.deletedAt IS NULL")
    Optional<Order> findByIdAndDeletedAtIsNull(@Param("id") UUID id);

    /**
     * Soft Delete 고려한 페이징 조회
     * Spring Data JPA가 자동으로 페이징 쿼리 생성
     */
    @Query("SELECT o FROM Order o WHERE o.deletedAt IS NULL")
    Page<Order> findAllByDeletedAtIsNull(Pageable pageable);
}
