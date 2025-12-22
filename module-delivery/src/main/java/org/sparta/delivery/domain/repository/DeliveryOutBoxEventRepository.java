package org.sparta.delivery.domain.repository;

import org.sparta.delivery.domain.entity.DeliveryOutboxEvent;

import java.util.List;

public interface DeliveryOutBoxEventRepository {
    DeliveryOutboxEvent save(DeliveryOutboxEvent event);

    List<DeliveryOutboxEvent> findReadyEvents(int batchSize);
}
