package org.sparta.order.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.order.domain.entity.Order;
import org.sparta.order.domain.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * OrderRepository 구현체
 * - 도메인 레이어에서 정의한 Repository 포트의 실제 구현
 * - 내부에서 Spring Data JPA(OrderJpaRepository)에 위임
 */
@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    /**
     * Persist the given order.
     *
     * @param order the order to persist
     * @return the persisted order with any persistence-managed updates (e.g., generated id or timestamps)
     */
    @Override
    public Order save(Order order) {
        return jpaRepository.save(order);
    }

    /**
     * Retrieves an order by ID only if it has not been soft-deleted.
     *
     * @param id the UUID of the order to retrieve
     * @return an Optional containing the order with the given id whose `deletedAt` is null, or empty if not found
     */
    @Override
    public Optional<Order> findByIdAndDeletedAtIsNull(UUID id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    /**
     * Retrieve a page of orders for the specified customer that are not marked deleted.
     *
     * @param customerId the UUID of the customer whose orders are requested
     * @param pageable pagination and sorting parameters
     * @return a page of Orders for the given customer where DeletedAt is null
     */
    @Override
    public Page<Order> findByCustomerIdAndDeletedAtIsNull(UUID customerId, Pageable pageable) {
        return jpaRepository.findByCustomerIdAndDeletedAtIsNull(customerId, pageable);
    }
}