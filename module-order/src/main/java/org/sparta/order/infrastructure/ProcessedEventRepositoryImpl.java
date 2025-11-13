package org.sparta.order.infrastructure;

import lombok.RequiredArgsConstructor;
import org.sparta.order.domain.entity.ProcessedEvent;
import org.sparta.order.domain.repository.ProcessedEventRepository;
import org.sparta.order.infrastructure.repository.ProcessedEventJpaRepository;
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