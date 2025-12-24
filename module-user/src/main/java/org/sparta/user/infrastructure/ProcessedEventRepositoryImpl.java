package org.sparta.user.infrastructure;

import lombok.RequiredArgsConstructor;
import org.sparta.user.domain.entity.ProcessedEvent;
import org.sparta.user.domain.repository.ProcessedEventRepository;
import org.sparta.user.infrastructure.jpa.ProcessedEventJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProcessedEventRepositoryImpl implements ProcessedEventRepository {

    private final ProcessedEventJpaRepository processedEventJpaRepository;

    @Override
    public ProcessedEvent save(ProcessedEvent processedEvent) {
        return processedEventJpaRepository.save(processedEvent);
    }

    @Override
    public boolean existsByEventId(UUID eventId) {
        return processedEventJpaRepository.existsByEventId(eventId);
    }
}