package org.sparta.order.infrastructure.repository;

import org.sparta.order.domain.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 순수 Spring Data JPA Repository
 * - 도메인 포트(OrderRepository)와는 분리
 * - 구현체(OrderRepositoryImpl)가 이 인터페이스를 사용해 실제 DB 접근
 */
public interface OrderJpaRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdAndDeletedAtIsNull(UUID id);

    Page<Order> findByCustomerIdAndDeletedAtIsNull(UUID customerId, Pageable pageable);

    List<Order> findAll();
}
