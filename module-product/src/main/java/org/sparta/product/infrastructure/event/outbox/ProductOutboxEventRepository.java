package org.sparta.product.infrastructure.event.outbox;

import org.sparta.product.domain.event.ProductOutboxEvent;
import org.sparta.product.domain.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductOutboxEventRepository extends JpaRepository<ProductOutboxEvent, UUID> {

    // READY 상태 상위 100개만 발행 (너무 많이 들고오면 트랜잭션 비효율)
    List<ProductOutboxEvent> findTop100ByStatusOrderByOccurredAtAsc(OutboxStatus status);
}
