package org.sparta.coupon.infrastructure;

import lombok.RequiredArgsConstructor;
import org.sparta.coupon.domain.entity.ProcessedEvent;
import org.sparta.coupon.domain.repository.ProcessedEventRepository;
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