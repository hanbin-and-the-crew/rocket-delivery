package org.sparta.slack.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.slack.application.dto.DailyDispatchResult;
import org.sparta.slack.application.dto.DailyRouteMessagePayload;
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
public class DailyRouteDispatchService {

    private static final List<RouteStatus> PLANNABLE_STATUSES = List.of(
            RouteStatus.ASSIGNED,
            RouteStatus.PLANNED,
            RouteStatus.FAILED
    );

    private final CompanyDeliveryRouteRepository routeRepository;
    private final DeliveryAssignmentService assignmentService;
    private final AiRoutePlanner aiRoutePlanner;
    private final SlackDirectMessageSender slackDirectMessageSender;

    @Transactional
    public List<DailyDispatchResult> dispatch(LocalDate date) {
        log.info("Starting daily route dispatch for date={}", date);
        assignmentService.assign(date);
        List<CompanyDeliveryRoute> routes = routeRepository.findAllByScheduledDateAndStatusIn(date, PLANNABLE_STATUSES);
        log.info("Dispatch candidates fetched count={}", routes.size());

        List<DailyDispatchResult> results = new ArrayList<>();
        for (CompanyDeliveryRoute route : routes) {
            if (route.getDeliveryManagerSlackId() == null) {
                log.warn("Slack ID가 없어 경로 알림을 건너뜁니다 - routeId={}", route.getId());
                continue;
            }
            try {
                log.info("Planning route routeId={} deliveryId={} currentStatus={} stopCount={}",
                        route.getId(), route.getDeliveryId(), route.getStatus(), route.getStops().size());
                RoutePlanningResult planningResult = aiRoutePlanner.plan(route);
                route.applyPlanningResult(planningResult);
                routeRepository.save(route);
                log.info("Route planning completed routeId={} distanceMeters={} durationMinutes={}",
                        route.getId(), planningResult.expectedDistanceMeters(), planningResult.expectedDurationMinutes());

                double distanceKm = planningResult.expectedDistanceMeters() != null
                        ? Math.round(planningResult.expectedDistanceMeters() / 100.0) / 10.0
                        : 0.0;
                int durationMinutes = planningResult.expectedDurationMinutes() != null
                        ? planningResult.expectedDurationMinutes()
                        : 0;

                DailyRouteMessagePayload payload = new DailyRouteMessagePayload(
                        route.getDeliveryManagerName(),
                        route.getScheduledDate(),
                        route.getOriginHubName(),
                        planningResult.orderedStops().stream().map(stop -> stop.label() + " (" + stop.address() + ")").toList(),
                        distanceKm,
                        durationMinutes,
                        planningResult.routeSummary(),
                        planningResult.aiReason()
                );

                UUID messageId = slackDirectMessageSender.send(
                        route.getDeliveryManagerSlackId(),
                        "ROUTE_DAILY_SUMMARY",
                        payload,
                        planningResult.routeSummary()
                );
                log.info("Slack DM sent routeId={} managerSlackId={} messageId={}",
                        route.getId(), route.getDeliveryManagerSlackId(), messageId);
                route.markDispatched(messageId);
                routeRepository.save(route);
                results.add(DailyDispatchResult.success(route.getId(), messageId));
            } catch (Exception ex) {
                log.error("일일 경로 발송 실패 routeId={}", route.getId(), ex);
                route.markPlanningFailed(ex.getMessage());
                routeRepository.save(route);
                results.add(DailyDispatchResult.failure(route.getId(), ex.getMessage()));
            }
        }
        return results;
    }
}
