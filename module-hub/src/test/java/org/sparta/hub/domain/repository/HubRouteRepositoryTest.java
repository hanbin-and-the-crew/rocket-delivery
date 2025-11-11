package org.sparta.hub.domain.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.hub.domain.model.HubRouteStatus;
import org.sparta.hub.domain.entity.HubRoute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class HubRouteRepositoryTest {

    @Autowired
    private HubRouteRepository hubRouteRepository;

    @Test
    @DisplayName("허브 경로 저장 및 조회 성공")
    void saveAndFind_success() {
        HubRoute route = HubRoute.builder()
                .sourceHubId(UUID.randomUUID())
                .targetHubId(UUID.randomUUID())
                .distance(120)
                .duration(90)
                .status(HubRouteStatus.ACTIVE)
                .build();

        HubRoute saved = hubRouteRepository.save(route);
        Optional<HubRoute> found = hubRouteRepository.findById(saved.getRouteId());

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(HubRouteStatus.ACTIVE);
    }

    @Test
    @DisplayName("삭제된 경로는 findAllActive() 결과에서 제외된다")
    void findAllActive_excludesDeleted() {
        HubRoute active = HubRoute.builder()
                .sourceHubId(UUID.randomUUID())
                .targetHubId(UUID.randomUUID())
                .distance(80)
                .duration(60)
                .status(HubRouteStatus.ACTIVE)
                .build();

        HubRoute deleted = HubRoute.builder()
                .sourceHubId(UUID.randomUUID())
                .targetHubId(UUID.randomUUID())
                .distance(200)
                .duration(150)
                .status(HubRouteStatus.INACTIVE)
                .build();

        hubRouteRepository.saveAll(List.of(active, deleted));

        List<HubRoute> result = hubRouteRepository.findAllActive();

        assertThat(result)
                .hasSize(1)
                .extracting(HubRoute::getStatus)
                .containsExactly(HubRouteStatus.ACTIVE);
    }

    @Test
    @DisplayName("markAsDeleted() 후 저장하면 status가 INACTIVE로 남는다")
    void markAsDeleted_persistsInactive() {
        HubRoute route = HubRoute.builder()
                .sourceHubId(UUID.randomUUID())
                .targetHubId(UUID.randomUUID())
                .distance(90)
                .duration(70)
                .build();

        route.markAsDeleted();
        hubRouteRepository.save(route);

        HubRoute found = hubRouteRepository.findById(route.getRouteId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(HubRouteStatus.INACTIVE);
        assertThat(found.getDeletedAt()).isNotNull();
    }
}
