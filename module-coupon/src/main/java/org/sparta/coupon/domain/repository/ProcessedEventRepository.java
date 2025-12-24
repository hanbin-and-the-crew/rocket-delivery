package org.sparta.coupon.domain.repository;

import org.sparta.coupon.domain.entity.ProcessedEvent;

import java.util.UUID;

public interface ProcessedEventRepository {

    ProcessedEvent save(ProcessedEvent processedEvent);

    boolean existsByEventId(UUID eventId);
}