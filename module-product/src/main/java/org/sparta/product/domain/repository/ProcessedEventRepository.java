package org.sparta.product.domain.repository;

import org.sparta.product.domain.entity.ProcessedEvent;

import java.util.UUID;

/**
 * Product 모듈 이벤트 멱등성 보장을 위한 처리 이력 저장소
 */
public interface ProcessedEventRepository {

    boolean existsByEventId(UUID eventId);

    ProcessedEvent save(ProcessedEvent processedEvent);
}
