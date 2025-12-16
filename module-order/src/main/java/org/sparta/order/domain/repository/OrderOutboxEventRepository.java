package org.sparta.order.domain.repository;

import org.sparta.order.domain.entity.OrderOutboxEvent;

import java.util.List;

public interface OrderOutboxEventRepository {
    OrderOutboxEvent save(OrderOutboxEvent event);

    List<OrderOutboxEvent> findReadyEvents(int batchSize);
}