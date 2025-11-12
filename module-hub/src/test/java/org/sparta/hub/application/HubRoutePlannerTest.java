package org.sparta.hub.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.common.event.EventPublisher;
import org.sparta.hub.domain.entity.Hub;
import org.sparta.hub.domain.repository.HubRepository;
import org.sparta.hub.presentation.dto.response.RoutePlanResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HubRoutePlannerTest {

    @Autowired private HubRoutePlanner hubRoutePlanner;
    @Autowired private HubRepository hubRepository;
    @Autowired private CacheManager cacheManager;

    private Hub seoul, incheon, daejeon, busan;

    @MockBean
    private EventPublisher eventPublisher;

    @BeforeEach
    void init() {
        hubRepository.deleteAll();

        // 좌표를 "도메인 규칙에 맞게" 단순화한 테스트 전용 허브들

        seoul = hubRepository.save(Hub.create(
                "서울 허브",
                "테스트용 주소 1",
                0.0, 0.0));     // (0, 0)

        incheon = hubRepository.save(Hub.create(
                "인천 허브",
                "테스트용 주소 2",
                0.0, 0.5));     // (0, 0.5)  -> 서울과 약 55km

        daejeon = hubRepository.save(Hub.create(
                "대전 허브",
                "테스트용 주소 3",
                0.0, 1.0));     // (0, 1.0)  -> 서울/부산과 각각 약 111km

        busan = hubRepository.save(Hub.create(
                "부산 허브",
                "테스트용 주소 4",
                0.0, 2.0));     // (0, 2.0)  -> 서울과 약 222km (200km 초과)
    }

    @Test
    @DisplayName("200km 미만의 인접 허브는 직통 경로를 반환한다 (서울→인천)")
    void directRoute_under200km() {
        RoutePlanResponse route = hubRoutePlanner.plan(seoul.getHubId(), incheon.getHubId());

        assertThat(route.totalDistanceKm()).isLessThan(200);
        assertThat(route.legs()).hasSize(1);

        var leg = route.legs().get(0);
        assertThat(leg.sourceHubId()).isEqualTo(seoul.getHubId());
        assertThat(leg.targetHubId()).isEqualTo(incheon.getHubId());
    }

    @Test
    @DisplayName("200km 이상은 중간 허브를 포함한 릴레이 경로를 반환한다 (서울→부산)")
    void relayRoute_over200km() {
        RoutePlanResponse route = hubRoutePlanner.plan(seoul.getHubId(), busan.getHubId());

        assertThat(route.totalDistanceKm()).isGreaterThan(200);
        assertThat(route.legs().size()).isGreaterThan(1);

        // 경유 허브로 대전이 포함되어야 함
        boolean containsDaejeon = route.legs().stream()
                .anyMatch(l -> l.sourceHubId().equals(daejeon.getHubId())
                        || l.targetHubId().equals(daejeon.getHubId()));
        assertThat(containsDaejeon).as("경유지로 대전이 포함되어야 함").isTrue();
    }

    @Test
    @DisplayName("같은 허브로 출발/도착 요청 시 예외가 발생한다")
    void sameSourceAndTarget_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> hubRoutePlanner.plan(seoul.getHubId(), seoul.getHubId()));
    }

    @Test
    @DisplayName("존재하지 않는 허브 ID로 요청 시 예외 발생")
    void invalidHubId_shouldThrow() {
        UUID fakeId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> hubRoutePlanner.plan(seoul.getHubId(), fakeId));
    }

    @Test
    @DisplayName("동일한 경로 재요청 시 Redis 캐시가 사용된다 (서울→대전)")
    void cachedRoute_isReused() {
        RoutePlanResponse first = hubRoutePlanner.plan(seoul.getHubId(), daejeon.getHubId());
        RoutePlanResponse second = hubRoutePlanner.plan(seoul.getHubId(), daejeon.getHubId());

        // 동일한 결과
        assertThat(second).usingRecursiveComparison().isEqualTo(first);

        // 캐시 확인
        var cache = cacheManager.getCache("routePlan");
        assertThat(cache).isNotNull();

        String key = "routePlan::" + seoul.getHubId() + "::" + daejeon.getHubId();
        Object cached = cache.get(key, RoutePlanResponse.class);
        assertThat(cached).isNotNull();
    }
}
