package org.sparta.order.domain.repository;

import org.sparta.order.domain.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findByIdAndDeletedAtIsNull(UUID id);

    Page<Order> findByCustomerIdAndDeletedAtIsNull(UUID customerId, Pageable pageable);

    List<Order> findAll();
}
