package org.sparta.order.infrastructure.repository;

import org.sparta.order.domain.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 순수 Spring Data JPA Repository
 * - 도메인 포트(OrderRepository)와는 분리
 * - 구현체(OrderRepositoryImpl)가 이 인터페이스를 사용해 실제 DB 접근
 */
public interface OrderJpaRepository extends JpaRepository<Order, UUID> {

    /**
 * Finds the Order with the given id only if it has not been soft-deleted.
 *
 * @param id the Order's UUID
 * @return an Optional containing the Order when found and its `deletedAt` is null, otherwise an empty Optional
 */
Optional<Order> findByIdAndDeletedAtIsNull(UUID id);

    /**
 * Finds orders for a given customer that have not been soft-deleted.
 *
 * @param customerId the UUID of the customer whose orders to retrieve
 * @param pageable   pagination and sorting information
 * @return           a page of Order entities for the specified customer where `deletedAt` is null
 */
Page<Order> findByCustomerIdAndDeletedAtIsNull(UUID customerId, Pageable pageable);
}