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

@Repository
@RequiredArgsConstructor
public class PointRepositoryImpl implements PointRepository {

    private PointJpaRepository jpaRepository;

    Sort sort = Sort.by(Sort.Direction.ASC, "expiryDate");

    @Override
    public List<Point> findUsablePoints(Long userId, LocalDateTime now) {
        return jpaRepository.findByUserIdAndStatusAndExpiryDateAfter(
                userId,
                PointStatus.AVAILABLE,
                now,
                sort
        );
    }
}
