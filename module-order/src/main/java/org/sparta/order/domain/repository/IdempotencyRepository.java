package org.sparta.order.domain.repository;

import org.sparta.order.domain.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> {

    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query("DELETE FROM IdempotencyRecord r WHERE r.expiresAt < :now")
    int deleteExpiredRecords(LocalDateTime now);
}
