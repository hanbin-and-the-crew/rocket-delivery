package org.sparta.slack.application.service.route;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.slack.domain.entity.CompanyDeliveryRoute;
import org.sparta.slack.domain.service.AiRoutePlanner;
import org.sparta.slack.domain.repository.CompanyDeliveryRouteRepository;
import org.sparta.slack.domain.vo.RoutePlanningResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates AI 기반 경로 계획과 영속화(단일 책임).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutePlanningService {

    private final AiRoutePlanner aiRoutePlanner;
    private final CompanyDeliveryRouteRepository routeRepository;

    @Transactional
    public RoutePlanningResult plan(CompanyDeliveryRoute route) {
        RoutePlanningResult planningResult = aiRoutePlanner.plan(route);
        route.applyPlanningResult(planningResult);
        routeRepository.save(route);
        log.info("Route planning completed routeId={} distanceMeters={} durationMinutes={}",
                route.getId(), planningResult.expectedDistanceMeters(), planningResult.expectedDurationMinutes());
        return planningResult;
    }
}
