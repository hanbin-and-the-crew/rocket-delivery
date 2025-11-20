package org.sparta.order.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.order.application.dto.response.OrderSearchCondition;
import org.sparta.order.domain.entity.Order;
import org.sparta.order.domain.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Order Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return orderJpaRepository.findById(id);
    }

    @Override
    public Optional<Order> findByIdAndDeletedAtIsNull(UUID id) {
        return orderJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Page<Order> searchOrders(OrderSearchCondition condition, Pageable pageable) {
        // JpaRepository의 메서드를 사용하여 DB에서 직접 페이징 처리
        // 훨씬 효율적입니다 (메모리에 전체 데이터를 로드하지 않음)
        return orderJpaRepository.findAllByDeletedAtIsNull(pageable);
    }

    @Override
    public void delete(Order order) {
        // Soft Delete는 이미 order.delete()에서 처리되므로
        // save만 호출하면 됩니다
        orderJpaRepository.save(order);
    }
}
