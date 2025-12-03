package org.sparta.user.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.user.domain.enums.ReservationStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long pointId;

    @Column(nullable = false)
    private String paymentId;

    @Column(nullable = false)
    private Integer reservedAmount;

    @Column(nullable = false)
    private LocalDateTime reservedAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    @PrePersist
    protected void onCreate() {
        this.reservedAt = LocalDateTime.now();
    }
}