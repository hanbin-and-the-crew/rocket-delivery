package org.sparta.slack.application.service.route;

import lombok.RequiredArgsConstructor;
import org.sparta.slack.application.dto.route.DailyRouteMessagePayload;
import org.sparta.slack.application.service.notification.SlackDirectMessageSender;
import org.sparta.slack.domain.entity.CompanyDeliveryRoute;
import org.sparta.slack.domain.vo.RoutePlanningResult;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Slack 알림 메시지 생성 및 발송.
 */
@Service
@RequiredArgsConstructor
public class RouteNotificationService {

    private static final String TEMPLATE_CODE = "ROUTE_DAILY_SUMMARY";

    private final SlackDirectMessageSender slackDirectMessageSender;

    public UUID notifyManager(CompanyDeliveryRoute route, RoutePlanningResult planningResult) {
        DailyRouteMessagePayload payload = buildPayload(route, planningResult);
        String fallback = planningResult.routeSummary();
        return slackDirectMessageSender.send(
                route.getDeliveryManagerSlackId(),
                TEMPLATE_CODE,
                payload,
                fallback
        );
    }

    private DailyRouteMessagePayload buildPayload(CompanyDeliveryRoute route, RoutePlanningResult planningResult) {
        double distanceKm = planningResult.expectedDistanceMeters() != null
                ? Math.round(planningResult.expectedDistanceMeters() / 100.0) / 10.0
                : 0.0;
        int durationMinutes = planningResult.expectedDurationMinutes() != null
                ? planningResult.expectedDurationMinutes()
                : 0;
        List<String> orderedStops = planningResult.orderedStops()
                .stream()
                .map(stop -> "%s (%s)".formatted(stop.label(), stop.address()))
                .toList();
        LocalDate dispatchDate = route.getScheduledDate();

        return new DailyRouteMessagePayload(
                route.getDeliveryManagerName(),
                dispatchDate,
                route.getOriginHubName(),
                orderedStops,
                distanceKm,
                durationMinutes,
                planningResult.routeSummary(),
                planningResult.aiReason()
        );
    }
}
