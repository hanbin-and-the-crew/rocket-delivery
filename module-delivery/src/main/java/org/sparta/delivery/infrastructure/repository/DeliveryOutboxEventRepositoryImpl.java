package org.sparta.delivery.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.delivery.domain.entity.DeliveryOutboxEvent;
import org.sparta.delivery.domain.enumeration.OutboxStatus;
import org.sparta.delivery.domain.repository.DeliveryOutBoxEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DeliveryOutboxEventRepositoryImpl implements DeliveryOutBoxEventRepository {

    private final DeliveryOutboxEventJpaRepository jpaRepository;

    @Override
    public DeliveryOutboxEvent save(DeliveryOutboxEvent event) {
        return jpaRepository.save(event);
    }

    @Override
    public List<DeliveryOutboxEvent> findReadyEvents(int batchSize) {
        Page<DeliveryOutboxEvent> page = jpaRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.READY,
                PageRequest.of(0, batchSize)
        );
        return page.getContent();
    }
}