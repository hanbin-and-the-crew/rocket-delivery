package org.sparta.slack.domain.vo;

import java.util.List;

/**
 * AI + 경로 API 계산 결과
 */
public record RoutePlanningResult(
        List<RouteStopSnapshot> orderedStops,
        Long expectedDistanceMeters,
        Integer expectedDurationMinutes,
        String routeSummary,
        String aiReason,
        String naverRouteLink,
        String rawWaypoints
) {

    public RoutePlanningResult {
        if (orderedStops == null || orderedStops.isEmpty()) {
            throw new IllegalArgumentException("경로 지점 목록은 필수입니다");
        }
    }
}
