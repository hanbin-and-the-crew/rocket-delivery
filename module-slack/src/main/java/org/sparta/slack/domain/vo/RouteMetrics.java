package org.sparta.slack.domain.vo;

public record RouteMetrics(
        long distanceMeters,
        int durationMinutes,
        String waypointsPayload
) {
}
