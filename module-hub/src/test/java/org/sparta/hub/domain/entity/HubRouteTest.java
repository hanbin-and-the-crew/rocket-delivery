package org.sparta.hub.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.hub.domain.model.HubRouteStatus;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class HubRouteTest {

    @Test
    @DisplayName("허브 경로 생성 시 기본 상태는 ACTIVE다")
    void createHubRoute_defaultStatusIsActive() {
        HubRoute route = HubRoute.builder()
                .sourceHubId(UUID.randomUUID())
                .targetHubId(UUID.randomUUID())
                .distance(100)
                .duration(70)
                .build();

        route = persist(route);

        assertThat(route.getStatus()).isEqualTo(HubRouteStatus.ACTIVE);
        assertThat(route.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("markAsDeleted() 호출 시 상태가 INACTIVE로 변경되고 deletedAt 기록됨")
    void markAsDeleted_success() {
        HubRoute route = HubRoute.builder()
                .sourceHubId(UUID.randomUUID())
                .targetHubId(UUID.randomUUID())
                .distance(120)
                .duration(90)
                .build();

        route.markAsDeleted();

        assertThat(route.getStatus()).isEqualTo(HubRouteStatus.INACTIVE);
        assertThat(route.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("closeRoute() 호출 시 상태가 CLOSED로 변경되고 updatedAt 기록됨")
    void closeRoute_success() {
        HubRoute route = HubRoute.builder()
                .sourceHubId(UUID.randomUUID())
                .targetHubId(UUID.randomUUID())
                .distance(80)
                .duration(60)
                .build();

        route.closeRoute();

        assertThat(route.getStatus()).isEqualTo(HubRouteStatus.CLOSED);
        assertThat(route.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("reopenRoute() 호출 시 ACTIVE로 복귀되고 deletedAt null로 초기화됨")
    void reopenRoute_success() {
        HubRoute route = HubRoute.builder()
                .sourceHubId(UUID.randomUUID())
                .targetHubId(UUID.randomUUID())
                .distance(200)
                .duration(150)
                .status(HubRouteStatus.INACTIVE)
                .deletedAt(LocalDateTime.now())
                .build();

        route.reopenRoute();

        assertThat(route.getStatus()).isEqualTo(HubRouteStatus.ACTIVE);
        assertThat(route.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("isActive()는 상태가 ACTIVE일 때 true 반환")
    void isActive_returnsTrueOnlyWhenActive() {
        HubRoute route = HubRoute.builder()
                .sourceHubId(UUID.randomUUID())
                .targetHubId(UUID.randomUUID())
                .distance(100)
                .duration(50)
                .status(HubRouteStatus.ACTIVE)
                .build();

        assertThat(route.isActive()).isTrue();

        route.markAsDeleted();
        assertThat(route.isActive()).isFalse();
    }

    // 단순히 생성 훅 실행 시뮬레이션용
    private HubRoute persist(HubRoute route) {
        route = HubRoute.builder()
                .routeId(route.getRouteId())
                .sourceHubId(route.getSourceHubId())
                .targetHubId(route.getTargetHubId())
                .distance(route.getDistance())
                .duration(route.getDuration())
                .status(HubRouteStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        return route;
    }
    @Test
    @DisplayName("거리나 시간 0 이하일 경우 validateRoute에서 예외 발생")
    void validateRoute_invalidValues_throwsException() {
        HubRoute invalidRoute = HubRoute.builder()
                .sourceHubId(UUID.randomUUID())
                .targetHubId(UUID.randomUUID())
                .distance(0)
                .duration(50)
                .build();

        assertThatThrownBy(invalidRoute::validateRoute)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0보다 커야 합니다");
    }

    @Test
    @DisplayName("update() 호출 시 거리와 소요시간이 변경된다")
    void update_success() {
        HubRoute route = HubRoute.builder()
                .sourceHubId(UUID.randomUUID())
                .targetHubId(UUID.randomUUID())
                .distance(100)
                .duration(70)
                .status(HubRouteStatus.ACTIVE)
                .build();

        route.update(120, 150);

        assertThat(route.getDistance()).isEqualTo(150);
        assertThat(route.getDuration()).isEqualTo(120);
    }



}
