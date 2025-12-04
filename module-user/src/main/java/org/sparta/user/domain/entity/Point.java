package org.sparta.user.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sparta.jpa.entity.BaseEntity;
import org.sparta.user.domain.enums.PointStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_points")
@Getter
@Setter
@NoArgsConstructor//(access = AccessLevel.PROTECTED)
public class Point extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Long amount; // 시작 포인트 (현재 가능한 포인트는 아래 getAvailableAmount로 따로 구함)

    @Column(nullable = false)
    private Long usedAmount; // 사용된 포인트

    @Column(nullable = false)
    private Long reservedAmount; // 예약된 포인트

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PointStatus status;

    public Long getAvailableAmount() {
        return this.amount - this.reservedAmount - this.usedAmount;
    }

    public static Point create(
            UUID userId, Long amount, Long usedAmount, Long reservedAmount,
            LocalDateTime expiryDate, PointStatus status) {
        Point point = new Point();
        point.userId = userId;
        point.amount = amount;
        point.usedAmount = usedAmount;
        point.reservedAmount = reservedAmount;
        point.expiryDate = expiryDate;
        point.status = status;
        return point;
    }
}