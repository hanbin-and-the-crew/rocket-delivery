package org.sparta.slack.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.slack.config.properties.NaverDirectionsProperties;
import org.sparta.slack.domain.vo.RouteMetrics;
import org.sparta.slack.domain.vo.RouteStopSnapshot;
import org.sparta.slack.error.SlackErrorType;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectionsEstimator {

    private static final double EARTH_RADIUS = 6371000.0;

    private final NaverDirectionsProperties properties;
    private final RestClient.Builder restClientBuilder;

    public RouteMetrics estimate(List<RouteStopSnapshot> orderedStops) {
        if (orderedStops == null || orderedStops.size() < 2) {
            return new RouteMetrics(0L, 0, "[]");
        }

        if (properties.isEnabled()) {
            try {
                return requestNaverDirections(orderedStops);
            } catch (Exception ex) {
                log.warn("Naver Directions 호출 실패 - {}", ex.getMessage());
            }
        }

        return fallback(orderedStops);
    }

    private RouteMetrics requestNaverDirections(List<RouteStopSnapshot> stops) {
        RestClient client = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();

        RouteStopSnapshot start = stops.get(0);
        RouteStopSnapshot goal = stops.get(stops.size() - 1);
        String waypointParam = buildWaypointParam(stops);
        String startPoint = formatPoint(start);
        String goalPoint = formatPoint(goal);

        log.info("Requesting Naver Directions start={} goal={} waypointCount={} option=trafast",
                startPoint, goalPoint, waypointParam.isBlank() ? 0 : waypointParam.split("\\|").length);
        JsonNode response = client.get()
                .uri(uriBuilder -> {
                    String endpointPath = properties.endpointPath();
                    if (endpointPath != null && !endpointPath.isBlank()) {
                        uriBuilder.path(endpointPath);
                    }
                    uriBuilder.queryParam("start", startPoint);
                    uriBuilder.queryParam("goal", goalPoint);
                    uriBuilder.queryParam("option", "trafast");
                    if (!waypointParam.isBlank()) {
                        uriBuilder.queryParam("waypoints", waypointParam);
                    }
                    return uriBuilder.build();
                })
                .header("X-NCP-APIGW-API-KEY-ID", properties.clientId())
                .header("X-NCP-APIGW-API-KEY", properties.clientSecret())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_STATE, "Directions 응답이 없습니다");
        }

        JsonNode routeNode = selectRouteNode(response.path("route"));
        JsonNode summary = routeNode.path("summary");
        JsonNode path = routeNode.path("path");

        if (summary.isMissingNode() || summary.isNull()) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_STATE, "Directions 응답에 summary 정보가 없습니다");
        }

        long distance = summary.path("distance").asLong();
        int duration = (int) Math.round(summary.path("duration").asDouble() / 60000.0);
        if (distance <= 0 || duration < 0) {
            log.warn("Directions 응답 값이 비정상입니다 distanceMeters={} durationMinutes={}", distance, duration);
        }
        log.info("Naver Directions success distanceMeters={} durationMinutes={}", distance, duration);

        return new RouteMetrics(distance, duration, path.toString());
    }

    private JsonNode selectRouteNode(JsonNode routeNode) {
        if (routeNode == null || routeNode.isMissingNode()) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_STATE, "Directions 응답에 route 노드가 없습니다");
        }
        List<String> preferredOrders = List.of("trafast", "traoptimal", "tracomfort", "traavoidtoll", "traavoidcaronly");
        for (String key : preferredOrders) {
            JsonNode optionArray = routeNode.path(key);
            if (optionArray.isArray() && optionArray.size() > 0) {
                return optionArray.get(0);
            }
        }
        throw new BusinessException(SlackErrorType.SLACK_INVALID_STATE, "Directions 응답에서 사용할 경로 옵션을 찾을 수 없습니다");
    }

    private RouteMetrics fallback(List<RouteStopSnapshot> stops) {
        long distance = 0L;
        for (int i = 0; i < stops.size() - 1; i++) {
            RouteStopSnapshot current = stops.get(i);
            RouteStopSnapshot next = stops.get(i + 1);
            distance += haversineMeters(
                    current.latitude(), current.longitude(),
                    next.latitude(), next.longitude()
            );
        }
        int durationMinutes = (int) Math.round(distance / 500.0); // 30km/h 가정
        log.warn("Directions fallback used stopCount={} fallbackDistanceMeters={} fallbackDurationMinutes={}",
                stops.size(), distance, durationMinutes);
        return new RouteMetrics(distance, durationMinutes, "[]");
    }

    private double haversineMeters(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return 0;
        }
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    private String formatPoint(RouteStopSnapshot stop) {
        return "%f,%f".formatted(
                stop.longitude() != null ? stop.longitude() : 0.0,
                stop.latitude() != null ? stop.latitude() : 0.0
        );
    }

    private String buildWaypointParam(List<RouteStopSnapshot> stops) {
        if (stops.size() <= 2) {
            return "";
        }
        List<String> points = new ArrayList<>();
        for (int i = 1; i < stops.size() - 1; i++) {
            points.add(formatPoint(stops.get(i)));
        }
        return points.stream().collect(Collectors.joining("|"));
    }
}
