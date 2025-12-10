package org.sparta.product.infrastructure;

import lombok.RequiredArgsConstructor;
import org.sparta.product.domain.outbox.OutboxStatus;
import org.sparta.product.domain.outbox.ProductOutboxEvent;
import org.sparta.product.domain.repository.ProductOutboxEventRepository;
import org.sparta.product.infrastructure.jpa.ProductOutboxEventJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductOutboxEventRepositoryImpl implements ProductOutboxEventRepository {

    private final ProductOutboxEventJpaRepository jpaRepository;

    @Override
    public ProductOutboxEvent save(ProductOutboxEvent event) {
        return jpaRepository.save(event);
    }

    @Override
    public List<ProductOutboxEvent> findReadyEvents(int batchSize) {
        Page<ProductOutboxEvent> page = jpaRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.READY,
                PageRequest.of(0, batchSize)
        );
        return page.getContent();
    }

    @Override
    public List<ProductOutboxEvent> findFailedEvents(int batchSize) {
        Page<ProductOutboxEvent> page = jpaRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.FAILED,
                PageRequest.of(0, batchSize)
        );
        return page.getContent();
    }

    @Override
    public long countByStatus(OutboxStatus status) {
        return jpaRepository.countByStatus(status);
    }


}
