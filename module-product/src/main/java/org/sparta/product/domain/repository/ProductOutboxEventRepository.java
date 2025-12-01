package org.sparta.product.domain.repository;

import org.sparta.product.domain.entity.ProductOutboxEvent;
import org.sparta.product.domain.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductOutboxEventRepository extends JpaRepository<ProductOutboxEvent,Long> {

    // READY 상태 상위 100개만 발행 (너무 많이 들고오면 트랜잭션 비효율)
    List<ProductOutboxEvent> findTop100ByStatusOrderByOccurredAtAsc(OutboxStatus status);
}
