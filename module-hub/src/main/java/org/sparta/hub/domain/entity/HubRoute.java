package org.sparta.hub.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.sparta.hub.domain.model.HubRouteStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hub_route")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HubRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID routeId;

    @Column(nullable = false)
    private UUID sourceHubId;

    @Column(nullable = false)
    private UUID targetHubId;

    @Column(nullable = false)
    private int distance; // km 단위

    @Column(nullable = false)
    private int duration; // 분 단위

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HubRouteStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private String createdBy;
    private String updatedBy;
    private String deletedBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = HubRouteStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // === 비즈니스 로직 === //

    public void validateRoute() {
        if (distance <= 0 || duration <= 0) {
            throw new IllegalArgumentException("거리와 소요 시간은 0보다 커야 합니다.");
        }
        if (sourceHubId.equals(targetHubId)) {
            throw new IllegalArgumentException("출발 허브와 도착 허브는 같을 수 없습니다.");
        }
    }


    public void update(int duration, int distance, String updatedBy) {
        if (distance <= 0 || duration <= 0) {
            throw new IllegalArgumentException("거리와 소요 시간은 0보다 커야 합니다.");
        }
        this.distance = distance;
        this.duration = duration;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }
    public void update(int duration, int distance) {
        update(duration, distance, null);
    }



    public void markAsDeleted(String deletedBy) {
        this.status = HubRouteStatus.INACTIVE;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }
    public void markAsDeleted() {
        markAsDeleted(null);
    }




    public void closeRoute(String updatedBy) {
        this.status = HubRouteStatus.CLOSED;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }
    public void closeRoute() {
        closeRoute(null);
    }




    public void reopenRoute(String updatedBy) {
        this.status = HubRouteStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
        this.deletedAt = null;
        this.deletedBy = null;
    }
    public void reopenRoute() {
        reopenRoute(null);
    }



    public boolean isActive() {
        return this.status == HubRouteStatus.ACTIVE;
    }
}
