package org.sparta.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 도메인 이벤트 마커 인터페이스
 * 모든 도메인 이벤트는 이 인터페이스를 구현해야 합니다.
 */
public interface DomainEvent {

    /**
     * 이벤트 고유 ID (멱등성 보장용)
     */
    UUID eventId();

    /**
     * 이벤트 발생 시각
     */
    Instant occurredAt();

    /**
     * 이벤트 타입 (클래스명 기반)
     */
    default String eventType() {
        return this.getClass().getSimpleName();
    }
}