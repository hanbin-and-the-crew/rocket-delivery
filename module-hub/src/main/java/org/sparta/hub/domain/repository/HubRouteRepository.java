package org.sparta.hub.domain.repository;

import org.sparta.hub.domain.entity.HubRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface HubRouteRepository extends JpaRepository<HubRoute, UUID> {

    /** 특정 출발 허브에서 출발하는 모든 경로 조회 */
    List<HubRoute> findBySourceHubId(UUID sourceHubId);

    /** 특정 도착 허브로 향하는 모든 경로 조회 */
    List<HubRoute> findByTargetHubId(UUID targetHubId);

    /** 삭제되지 않은 (active) 경로만 조회 */
    @Query("SELECT r FROM HubRoute r WHERE r.deletedAt IS NULL")
    List<HubRoute> findAllActive();
}
