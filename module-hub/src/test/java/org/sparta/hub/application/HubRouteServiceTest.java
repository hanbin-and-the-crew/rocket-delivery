package org.sparta.hub.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.hub.domain.entity.HubRoute;
import org.sparta.hub.domain.model.HubRouteStatus;
import org.sparta.hub.domain.repository.HubRouteRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class HubRouteServiceTest {

    @Mock
    private HubRouteRepository hubRouteRepository;

    @InjectMocks
    private HubRouteService hubRouteService;

    @Test
    @DisplayName("허브 경로 생성 시 기본 상태는 ACTIVE")
    void createRoute_success() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        HubRoute route = HubRoute.builder()
                .sourceHubId(sourceId)
                .targetHubId(targetId)
                .distance(100)
                .duration(80)
                .status(HubRouteStatus.ACTIVE)
                .build();

        given(hubRouteRepository.save(any())).willReturn(route);

        HubRoute result = hubRouteService.createRoute(sourceId, targetId, 80, 100);
        assertThat(result.getStatus()).isEqualTo(HubRouteStatus.ACTIVE);
    }

    @Test
    @DisplayName("삭제 요청 시 markAsDeleted 호출 후 INACTIVE로 변경된다")
    void deleteRoute_success() {
        UUID id = UUID.randomUUID();
        HubRoute route = HubRoute.builder()
                .routeId(id)
                .sourceHubId(UUID.randomUUID())
                .targetHubId(UUID.randomUUID())
                .status(HubRouteStatus.ACTIVE)
                .build();

        given(hubRouteRepository.findById(id)).willReturn(Optional.of(route));
        given(hubRouteRepository.save(any())).willReturn(route);

        HubRoute result = hubRouteService.deleteRoute(id);

        assertThat(result.getStatus()).isEqualTo(HubRouteStatus.INACTIVE);
        then(hubRouteRepository).should().save(route);
    }
}
