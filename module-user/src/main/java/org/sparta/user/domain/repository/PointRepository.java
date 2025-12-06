package org.sparta.user.domain.repository;

import org.sparta.user.domain.entity.Point;
import org.sparta.user.domain.enums.PointStatus;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PointRepository {
    List<Point> findUsablePoints(UUID userId, PointStatus status, LocalDateTime now, Sort sort);
    List<Point> findUsablePoints(UUID userId, PointStatus status, LocalDateTime now);
    Point save(Point point);
    Optional<Point> findById(UUID id);
    long count();
    void flush();
}