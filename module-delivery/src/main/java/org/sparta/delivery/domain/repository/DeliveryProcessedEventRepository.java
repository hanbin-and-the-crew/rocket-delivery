
package org.sparta.delivery.domain.repository;

import org.sparta.delivery.domain.entity.DeliveryProcessedEvent;

import java.util.Optional;
import java.util.UUID;

/**
 * 처리된 이벤트 Repository
 *
 * 멱등성 체크용
 */
public interface DeliveryProcessedEventRepository {

    /**
     * 이벤트 ID로 처리 여부 확인
     */
    boolean existsByEventId(UUID eventId);

    /**
     * 처리된 이벤트 저장
     */
    DeliveryProcessedEvent save(DeliveryProcessedEvent deliveryProcessedEvent);


    Optional<DeliveryProcessedEvent> findByEventId(UUID eventId);

}