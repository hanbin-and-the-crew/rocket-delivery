package org.sparta.deliveryman.domain.repository;

import org.sparta.deliveryman.domain.entity.ProcessedEvent;

import java.util.Optional;
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

    // ✅ 테스트용 메서드 추가
    Optional<ProcessedEvent> findByEventId(UUID eventId);

    // ✅ 테스트용 메서드 추가 (중복 체크)
    long countByEventId(UUID eventId);
}