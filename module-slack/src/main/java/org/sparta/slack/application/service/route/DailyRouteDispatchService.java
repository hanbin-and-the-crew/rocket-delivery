package org.sparta.slack.application.service.route;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.slack.application.dto.route.DailyDispatchResult;
import org.sparta.slack.domain.entity.CompanyDeliveryRoute;
import org.sparta.slack.domain.enums.RouteStatus;
import org.sparta.slack.domain.repository.CompanyDeliveryRouteRepository;
import org.sparta.slack.domain.vo.RoutePlanningResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyRouteDispatchService {

    private static final List<RouteStatus> PLANNABLE_STATUSES = List.of(
            RouteStatus.ASSIGNED,
            RouteStatus.PLANNED,
            RouteStatus.FAILED
    );

    private final CompanyDeliveryRouteRepository routeRepository;
    private final DeliveryAssignmentService assignmentService;
    private final RoutePlanningService routePlanningService;
    private final RouteNotificationService routeNotificationService;

    @Transactional
    public List<DailyDispatchResult> dispatch(LocalDate date) {
        log.info("Starting daily route dispatch for date={}", date);
        assignmentService.assign(date);
        List<CompanyDeliveryRoute> routes = routeRepository.findAllByScheduledDateAndStatusIn(date, PLANNABLE_STATUSES);
        log.info("Dispatch candidates fetched count={}", routes.size());

        List<DailyDispatchResult> results = new ArrayList<>();
        for (CompanyDeliveryRoute route : routes) {
            processRoute(route).ifPresent(results::add);
        }
        return results;
    }

    private java.util.Optional<DailyDispatchResult> processRoute(CompanyDeliveryRoute route) {
        if (route.getDeliveryManagerSlackId() == null) {
            log.warn("Slack ID가 없어 경로 알림을 건너뜁니다 - routeId={}", route.getId());
            return java.util.Optional.empty();
        }
        try {
            log.info("Planning route routeId={} deliveryId={} currentStatus={} stopCount={}",
                    route.getId(), route.getDeliveryId(), route.getStatus(), route.getStops().size());
            RoutePlanningResult planningResult = routePlanningService.plan(route);
            UUID messageId = routeNotificationService.notifyManager(route, planningResult);
            log.info("Slack DM sent routeId={} managerSlackId={} messageId={}",
                    route.getId(), route.getDeliveryManagerSlackId(), messageId);
            route.markDispatched(messageId);
            routeRepository.save(route);
            return java.util.Optional.of(DailyDispatchResult.success(route.getId(), messageId));
        } catch (Exception ex) {
            log.error("일일 경로 발송 실패 routeId={}", route.getId(), ex);
            route.markPlanningFailed(ex.getMessage());
            routeRepository.save(route);
            return java.util.Optional.of(DailyDispatchResult.failure(route.getId(), ex.getMessage()));
        }
    }
}
