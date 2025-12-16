package org.sparta.user.domain.repository;

import org.sparta.user.domain.entity.ProcessedEvent;

import java.util.UUID;

public interface ProcessedEventRepository {

    ProcessedEvent save(ProcessedEvent processedEvent);

    boolean existsByEventId(UUID eventId);
}