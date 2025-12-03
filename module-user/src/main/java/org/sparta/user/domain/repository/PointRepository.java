package org.sparta.user.domain.repository;

import org.sparta.user.domain.entity.Point;

import java.time.LocalDateTime;
import java.util.List;

public interface PointRepository {
    List<Point> findUsablePoints(Long userId, LocalDateTime now);
}
