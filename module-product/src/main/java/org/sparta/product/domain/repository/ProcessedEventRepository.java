package org.sparta.product.domain.repository;

import org.sparta.product.domain.entity.ProcessedEvent;

import java.util.UUID;

/**
 * 처리된 이벤트 Repository
 *
 * 멱등성 체크용
 */
public interface ProcessedEventRepository {

    /**
     * 이벤트 ID로 처리 여부 확인
     */
    boolean existsByEventId(UUID eventId);

    /**
     * 처리된 이벤트 저장
     */
    ProcessedEvent save(ProcessedEvent processedEvent);
}