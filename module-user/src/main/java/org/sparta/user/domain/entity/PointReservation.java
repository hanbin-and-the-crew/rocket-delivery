package org.sparta.user.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sparta.user.domain.enums.ReservationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "point_reservations")
@Getter
@Setter
@NoArgsConstructor//(access = AccessLevel.PROTECTED)
public class PointReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID pointId;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private Long reservedAmount;

    @Column(nullable = false)
    private LocalDateTime reservedAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    @PrePersist
    protected void onCreate() {
        this.reservedAt = LocalDateTime.now();
    }
    public static PointReservation create(
            UUID pointId, UUID orderId, Long reservedAmount, ReservationStatus status) {
        PointReservation reservation = new PointReservation();
        reservation.pointId = pointId;
        reservation.orderId = orderId;
        reservation.reservedAmount = reservedAmount;
        reservation.status = status;
        return reservation;
    }
}