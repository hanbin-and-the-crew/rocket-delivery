package org.sparta.slack.application.route.dto;

import java.time.LocalDate;
import java.util.List;

public record DailyRouteMessagePayload(
        String managerName,
        LocalDate dispatchDate,
        String hubName,
        List<String> orderedStops,
        double totalDistanceKm,
        int totalDurationMinutes,
        String routeSummary,
        String aiReason
) {
}
