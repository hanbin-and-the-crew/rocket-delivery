package org.sparta.hub.domain.repository;

import org.sparta.hub.domain.entity.HubRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface HubRouteRepository extends JpaRepository<HubRoute, UUID> {

    @Query("SELECT r FROM HubRoute r WHERE r.status = 'ACTIVE'")
    List<HubRoute> findAllActive();
}
