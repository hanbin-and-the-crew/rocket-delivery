package org.sparta.order.domain.repository;

import org.sparta.order.application.dto.response.OrderSearchCondition;
import org.sparta.order.domain.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * 주문 Repository 인터페이스
 * 도메인 계층에서 정의하여 도메인의 독립성 유지
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(UUID id);

    /**
     * Soft Delete 고려한 조회
     */
    Optional<Order> findByIdAndDeletedAtIsNull(UUID id);


    /**
     * 검색 (Soft Delete 고려)
     */
    Page<Order> searchOrders(OrderSearchCondition condition, Pageable pageable);

    /**
     * Soft Delete
     */
    void delete(Order order);
}