package org.sparta.hub.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.sparta.jpa.entity.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "hub_routes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HubRoute extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "route_id", nullable = false, updatable = false)
    private UUID routeId;

    @Column(name = "source_hub_id", nullable = false)
    private UUID sourceHubId;

    @Column(name = "target_hub_id", nullable = false)
    private UUID targetHubId;

    @Column(nullable = false)
    private Integer duration; // 이동 소요 시간 (분)

    @Column(nullable = false)
    private Integer distance; // 이동 거리 (km)

    // ===== 도메인 규칙 메서드 =====

    /** 출발지와 도착지가 동일할 수 없음 */
    public void validateRoute() {
        if (sourceHubId.equals(targetHubId)) {
            throw new IllegalArgumentException("Source and target hubs cannot be the same");
        }
    }

    /** 거리와 시간이 0 이하일 수 없음 */
    public void update(Integer duration, Integer distance) {
        if (duration == null || distance == null || duration <= 0 || distance <= 0) {
            throw new IllegalArgumentException("Duration and distance must be positive");
        }
        this.duration = duration;
        this.distance = distance;
    }

    /** 논리 삭제 처리 */
    public void markAsDeleted() {
        super.markAsDeleted();
    }

    /** 삭제 복구 (Soft Delete 해제) */
    public void restore() {
        super.restore();
    }
}
