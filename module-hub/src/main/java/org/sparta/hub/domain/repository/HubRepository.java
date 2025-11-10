package org.sparta.hub.domain.repository;

import org.sparta.hub.domain.entity.Hub;
import org.sparta.hub.domain.model.HubStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HubRepository extends JpaRepository<Hub, UUID> {
    boolean existsByName(String name);
    List<Hub> findAllByStatus(HubStatus status);
}
