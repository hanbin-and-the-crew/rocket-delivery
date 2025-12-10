package org.sparta.product.domain.repository;

import org.sparta.product.domain.outbox.OutboxStatus;
import org.sparta.product.domain.outbox.ProductOutboxEvent;

import java.util.List;

public interface ProductOutboxEventRepository {

    ProductOutboxEvent save(ProductOutboxEvent event);

    /**
     * 상태가 READY 인 Outbox 이벤트를 오래된 순으로 최대 batchSize 개 조회
     */
    List<ProductOutboxEvent> findReadyEvents(int batchSize);


    /**
     * DLQ 성격의 FAILED 상태 Outbox 이벤트를 오래된 순으로 최대 batchSize 개 조회
     */
    List<ProductOutboxEvent> findFailedEvents(int batchSize);

    long countByStatus(OutboxStatus status);

}
