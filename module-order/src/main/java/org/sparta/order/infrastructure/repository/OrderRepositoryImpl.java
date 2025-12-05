package org.sparta.order.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.order.domain.entity.Order;
import org.sparta.order.domain.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    @Override
    public Order save(Order order) {
        return jpaRepository.save(order);
    }

    @Override
    public Optional<Order> findByIdAndDeletedAtIsNull(UUID id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Page<Order> findByCustomerIdAndDeletedAtIsNull(UUID customerId, Pageable pageable) {
        return jpaRepository.findByCustomerIdAndDeletedAtIsNull(customerId, pageable);
    }

    @Override
    public List<Order> findAll(){
        return jpaRepository.findAll();
    }
}
