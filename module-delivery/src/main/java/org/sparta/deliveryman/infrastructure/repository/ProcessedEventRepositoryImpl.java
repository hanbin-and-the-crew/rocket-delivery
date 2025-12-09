package org.sparta.deliveryman.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.deliveryman.domain.entity.ProcessedEvent;
import org.sparta.deliveryman.domain.repository.ProcessedEventRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProcessedEventRepositoryImpl implements ProcessedEventRepository {

    private final ProcessedEventJpaRepository jpaRepository;

    @Override
    public boolean existsByEventId(UUID eventId) {
        return jpaRepository.existsByEventId(eventId);
    }

    @Override
    public ProcessedEvent save(ProcessedEvent processedEvent) {
        return jpaRepository.save(processedEvent);
    }
}