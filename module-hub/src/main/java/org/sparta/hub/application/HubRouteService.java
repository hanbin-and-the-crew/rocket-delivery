package org.sparta.hub.application;

import lombok.RequiredArgsConstructor;
import org.sparta.hub.domain.entity.HubRoute;
import org.sparta.hub.domain.repository.HubRouteRepository;
import org.sparta.hub.exception.HubRouteNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubRouteService {

    private final HubRouteRepository hubRouteRepository;

    @Transactional
    public HubRoute createRoute(UUID sourceHubId, UUID targetHubId, int duration, int distance) {
        HubRoute route = HubRoute.builder()
                .sourceHubId(sourceHubId)
                .targetHubId(targetHubId)
                .duration(duration)
                .distance(distance)
                .build();

        route.validateRoute();
        return hubRouteRepository.save(route);
    }

    public HubRoute getRoute(UUID routeId) {
        return hubRouteRepository.findById(routeId)
                .orElseThrow(() -> new HubRouteNotFoundException(routeId));
    }

    @CacheEvict(cacheNames = "routePlan", allEntries = true)
    @Transactional
    public HubRoute updateRoute(UUID routeId, int duration, int distance) {
        HubRoute route = getRoute(routeId);
        route.update(duration, distance);
        return hubRouteRepository.save(route);
    }

    @Transactional
    public HubRoute deleteRoute(UUID routeId) {
        HubRoute route = getRoute(routeId);
        route.markAsDeleted();
        return hubRouteRepository.save(route);
    }

    public List<HubRoute> getAllActiveRoutes() {
        return hubRouteRepository.findAllActive();
    }

    @Transactional(readOnly = true)
    public List<String> calculateIntermediateHubs(UUID sourceHubId, UUID targetHubId, int distance) {
        if (distance < 200) return List.of(); // 직항 가능
        int stops = distance / 200; // 200km마다 1개 경유지
        return IntStream.rangeClosed(1, stops)
                .mapToObj(i -> "경유허브-" + i)
                .toList();
    }


}
