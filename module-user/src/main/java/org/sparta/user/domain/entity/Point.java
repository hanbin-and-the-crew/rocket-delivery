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
@Table(name = "points")
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
    private Long amount;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PointStatus status;

    @Column
    private LocalDateTime usedAt;

    @Column
    private LocalDateTime reservedAt;

    @PrePersist
    protected void onCreate() {
        this.status = PointStatus.AVAILABLE;
    }
}