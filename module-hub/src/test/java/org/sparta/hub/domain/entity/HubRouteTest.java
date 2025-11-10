package org.sparta.hub.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.jpa.entity.BaseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class HubRouteTest {

    @Test
    @DisplayName("HubRoute 생성 시 필드가 정상적으로 설정된다")
    void createHubRoute_success() {
        // given
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        // when
        HubRoute route = HubRoute.builder()
                .sourceHubId(sourceId)
                .targetHubId(targetId)
                .duration(180)
                .distance(120)
                .build();

        // then
        assertThat(route.getSourceHubId()).isEqualTo(sourceId);
        assertThat(route.getTargetHubId()).isEqualTo(targetId);
        assertThat(route.getDuration()).isEqualTo(180);
        assertThat(route.getDistance()).isEqualTo(120);
        assertThat(route.getCreatedAt()).isNull(); // 아직 persist 전이므로 null
        assertThat(route.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("출발 허브와 도착 허브가 같을 경우 validateRoute()에서 예외가 발생한다")
    void validateRoute_sameSourceAndTarget_fail() {
        // given
        UUID sameId = UUID.randomUUID();

        HubRoute route = HubRoute.builder()
                .sourceHubId(sameId)
                .targetHubId(sameId)
                .duration(100)
                .distance(70)
                .build();

        // when & then
        assertThatThrownBy(route::validateRoute)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source and target hubs cannot be the same");
    }

    @Test
    @DisplayName("update() 호출 시 거리와 소요시간이 변경된다")
    void update_success() {
        // given
        HubRoute route = HubRoute.builder()
                .sourceHubId(UUID.randomUUID())
                .targetHubId(UUID.randomUUID())
                .duration(100)
                .distance(50)
                .build();

        // when
        route.update(120, 80);

        // then
        assertThat(route.getDuration()).isEqualTo(120);
        assertThat(route.getDistance()).isEqualTo(80);
    }

    @Test
    @DisplayName("update() 시 거리나 소요시간이 0 이하이면 예외가 발생한다")
    void update_invalidValues_fail() {
        HubRoute route = HubRoute.builder()
                .sourceHubId(UUID.randomUUID())
                .targetHubId(UUID.randomUUID())
                .duration(100)
                .distance(50)
                .build();

        // duration이 0인 경우
        assertThatThrownBy(() -> route.update(0, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");

        // distance가 음수인 경우
        assertThatThrownBy(() -> route.update(100, -10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");
    }

    @Test
    @DisplayName("markAsDeleted() 호출 시 deletedAt이 기록된다")
    void markAsDeleted_success() {
        // given
        HubRoute route = HubRoute.builder()
                .sourceHubId(UUID.randomUUID())
                .targetHubId(UUID.randomUUID())
                .duration(200)
                .distance(150)
                .build();

        // when
        route.markAsDeleted();

        // then
        assertThat(route.getDeletedAt()).isNotNull();
    }


    @Test
    @DisplayName("유효성 검증 통과 시 validateRoute()가 정상 수행된다")
    void validateRoute_success() {
        // given
        HubRoute route = HubRoute.builder()
                .sourceHubId(UUID.randomUUID())
                .targetHubId(UUID.randomUUID())
                .duration(180)
                .distance(90)
                .build();

        // when & then
        assertThatCode(route::validateRoute).doesNotThrowAnyException();
    }
}
