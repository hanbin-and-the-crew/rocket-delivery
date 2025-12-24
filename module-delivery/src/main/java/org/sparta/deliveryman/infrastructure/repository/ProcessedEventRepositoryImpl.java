package org.sparta.deliveryman.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.deliveryman.domain.entity.ProcessedEvent;
import org.sparta.deliveryman.domain.repository.ProcessedEventRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
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

    // ✅ 테스트용 메서드 추가
    @Override
    public Optional<ProcessedEvent> findByEventId(UUID eventId){return jpaRepository.findByEventId(eventId);}

    // ✅ 테스트용 메서드 추가 (중복 체크)
    @Override
    public long countByEventId(UUID eventId){return jpaRepository.countByEventId(eventId);}
}