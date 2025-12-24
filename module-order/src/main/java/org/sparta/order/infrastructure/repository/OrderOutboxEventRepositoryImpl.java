package org.sparta.order.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.order.domain.entity.OrderOutboxEvent;
import org.sparta.order.domain.enumeration.OutboxStatus;
import org.sparta.order.domain.repository.OrderOutboxEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderOutboxEventRepositoryImpl implements OrderOutboxEventRepository {

    private final OrderOutboxEventJpaRepository jpaRepository;

    @Override
    public OrderOutboxEvent save(OrderOutboxEvent event) {
        return jpaRepository.save(event);
    }

    @Override
    public List<OrderOutboxEvent> findReadyEvents(int batchSize) {
        Page<OrderOutboxEvent> page = jpaRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.READY,
                PageRequest.of(0, batchSize)
        );
        return page.getContent();
    }
}