package org.sparta.user.infrastructure.jpa;

import jakarta.persistence.LockModeType;
import org.sparta.user.domain.entity.Point;
import org.sparta.user.domain.enums.PointStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PointJpaRepository extends JpaRepository<Point, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Point> findByUserIdAndStatusAndExpiryDateAfter(
            UUID userId,
            PointStatus status,
            LocalDateTime now,
            Sort sort
    );

    List<Point> findByUserIdAndStatusAndExpiryDateAfter(
            UUID userId,
            PointStatus status,
            LocalDateTime now
    );
}
