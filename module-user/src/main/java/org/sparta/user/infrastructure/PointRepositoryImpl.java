package org.sparta.user.infrastructure;

import lombok.RequiredArgsConstructor;
import org.sparta.user.domain.entity.Point;
import org.sparta.user.domain.enums.PointStatus;
import org.sparta.user.domain.repository.PointRepository;
import org.sparta.user.infrastructure.jpa.PointJpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PointRepositoryImpl implements PointRepository {

    private final PointJpaRepository pointJpaRepository;

    @Override
    public List<Point> findUsablePoints(UUID userId, PointStatus status,
                                        LocalDateTime now, Sort sort) {

        return pointJpaRepository.findByUserIdAndStatusAndExpiryDateAfter(
                userId, status, now, sort
        );
    }

    @Override
    public List<Point> findUsablePoints(UUID userId, PointStatus status,
                                        LocalDateTime now) {

        return pointJpaRepository.findByUserIdAndStatusAndExpiryDateAfter(
                userId, status, now
        );
    }

    @Override
    public Optional<Point> findById(UUID id) {
        return pointJpaRepository.findById(id);
    }

    @Override
    public Point save(Point point) {
        return pointJpaRepository.save(point);
    }

    @Override
    public long count() {
        return pointJpaRepository.count();
    }
}
