package org.sparta.slack.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.slack.config.properties.AiPlanningProperties;
import org.sparta.slack.domain.entity.CompanyDeliveryRoute;
import org.sparta.slack.domain.vo.RouteMetrics;
import org.sparta.slack.domain.vo.RoutePlanningResult;
import org.sparta.slack.domain.vo.RouteStopSnapshot;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRoutePlanner {

    private final AiPlanningProperties aiPlanningProperties;
    private final GeoCoordinateResolver geoCoordinateResolver;
    private final DirectionsEstimator directionsEstimator;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public RoutePlanningResult plan(CompanyDeliveryRoute route) {
        List<RouteStopSnapshot> stops = prepareStops(route);
        log.info("AI planning started routeId={} deliveryId={} stopCount={}", route.getId(), route.getDeliveryId(), stops.size());
        List<RouteStopSnapshot> enriched = enrichCoordinates(stops);
        log.debug("Coordinates enriched routeId={} enrichedStopCount={}", route.getId(), enriched.size());
        OrderedResult orderedResult = determineOrder(route, enriched);
        log.debug("Stop ordering decided routeId={} orderedLabels={}", route.getId(), orderedResult.orderedLabels());
        List<RouteStopSnapshot> orderedStops = reorder(enriched, orderedResult.orderedLabels());
        RouteMetrics metrics = directionsEstimator.estimate(orderedStops);
        log.info("Directions estimated routeId={} distanceMeters={} durationMinutes={}",
                route.getId(), metrics.distanceMeters(), metrics.durationMinutes());

        String summary = orderedResult.summary() != null
                ? orderedResult.summary()
                : defaultSummary(route, orderedStops, metrics);
        String reason = orderedResult.reason() != null
                ? orderedResult.reason()
                : "허브와 목적지 간 직선거리 기반 자동 계산";

        return new RoutePlanningResult(
                orderedStops,
                metrics.distanceMeters(),
                metrics.durationMinutes(),
                summary,
                reason,
                orderedResult.naverRouteLink(),
                metrics.waypointsPayload()
        );
    }

    private List<RouteStopSnapshot> prepareStops(CompanyDeliveryRoute route) {
        if (route.getStops() != null && !route.getStops().isEmpty()) {
            return new ArrayList<>(route.getStops());
        }

        List<RouteStopSnapshot> defaults = new ArrayList<>();
        defaults.add(RouteStopSnapshot.builder()
                .deliveryId(route.getDeliveryId())
                .label(route.getOriginHubName() != null ? route.getOriginHubName() : "발송 허브")
                .address(route.getOriginAddress())
                .sequence(0)
                .build());
        defaults.add(RouteStopSnapshot.builder()
                .deliveryId(route.getDeliveryId())
                .label(route.getDestinationCompanyName())
                .address(route.getDestinationAddress())
                .sequence(1)
                .build());
        return defaults;
    }

    private List<RouteStopSnapshot> enrichCoordinates(List<RouteStopSnapshot> stops) {
        return stops.stream()
                .map(stop -> {
                    if (stop.latitude() != null && stop.longitude() != null) {
                        return stop;
                    }
                    try {
                        var point = geoCoordinateResolver.resolve(stop.address());
                        return stop.withCoordinate(point.latitude(), point.longitude());
                    } catch (Exception ex) {
                        log.warn("좌표 변환 실패 - {} ({})", stop.label(), ex.getMessage());
                        return stop;
                    }
                })
                .toList();
    }

    private OrderedResult determineOrder(CompanyDeliveryRoute route, List<RouteStopSnapshot> stops) {
        if (!aiPlanningProperties.isEnabled()) {
            return OrderedResult.fallback(stops);
        }

        try {
            RestClient client = restClientBuilder
                    .baseUrl(aiPlanningProperties.baseUrl())
                    .build();

            String prompt = buildStrictJsonPrompt(route, stops);

            JsonNode response = client.post()
                    .uri("/models/%s:generateContent?key=%s".formatted(
                            aiPlanningProperties.model(),
                            aiPlanningProperties.apiKey()
                    ))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""
                            {
                              "contents": [
                                {
                                  "parts": [
                                    {"text": "%s"}
                                  ]
                                }
                              ]
                            }
                            """.formatted(prompt.replace("\"", "\\\"")))
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                return OrderedResult.fallback(stops);
            }

            JsonNode textNode = response.at("/candidates/0/content/parts/0/text");
            if (textNode.isMissingNode()) {
                return OrderedResult.fallback(stops);
            }

            JsonNode payload = objectMapper.readTree(cleanResponse(textNode.asText()));
            List<String> orderedStops = new ArrayList<>();
            payload.path("orderedStops").forEach(node -> orderedStops.add(node.asText()));
            String reason = payload.path("reason").asText(null);
            String summary = payload.path("summary").asText(null);
            String recommendedStart = payload.path("recommendedStartTime").asText("06:00");

            return new OrderedResult(orderedStops, reasonWithStart(reason, recommendedStart), summary, null);
        } catch (Exception ex) {
            log.warn("AI 경로 계산 실패 - {}", ex.getMessage());
            return OrderedResult.fallback(stops);
        }
    }

    private Map<String, Object> describeStops(CompanyDeliveryRoute route, List<RouteStopSnapshot> stops) {
        Map<String, Object> descriptor = new LinkedHashMap<>();
        descriptor.put("date", route.getScheduledDate().toString());
        descriptor.put("manager", route.getDeliveryManagerName());
        descriptor.put("stops", stops);
        return descriptor;
    }

    private String buildStrictJsonPrompt(CompanyDeliveryRoute route, List<RouteStopSnapshot> stops) throws JsonProcessingException {
        String baseInstruction = """
                다음 정보를 이용해 배송 경로를 계획하세요.
                반드시 아래 JSON 스키마에 맞춰 순수 JSON 문자열만 응답해야 합니다.
                백틱(`), 코드블럭, 설명 문장, 주석을 절대로 넣지 마세요.
                                
                {
                  "orderedStops": ["경로 순서대로 각 지점 이름을 배열로 나열"],
                  "reason": "경로 결정 사유를 한 문장으로 작성",
                  "summary": "사람이 읽기 쉬운 요약",
                  "recommendedStartTime": "HH:mm"
                }
                """;
        String payloadJson = objectMapper.writeValueAsString(describeStops(route, stops));
        return baseInstruction + "\n입력 데이터: " + payloadJson;
    }

    private String cleanResponse(String rawText) {
        if (rawText == null) {
            return "{}";
        }
        String cleaned = rawText
                .replace("```json", "")
                .replace("```", "")
                .trim();
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace >= firstBrace) {
            return cleaned.substring(firstBrace, lastBrace + 1);
        }
        return cleaned;
    }

    private List<RouteStopSnapshot> reorder(List<RouteStopSnapshot> stops, List<String> orderedLabels) {
        if (orderedLabels.isEmpty()) {
            return stops;
        }
        Map<String, RouteStopSnapshot> byLabel = stops.stream()
                .collect(Collectors.toMap(RouteStopSnapshot::label, stop -> stop, (a, b) -> a, LinkedHashMap::new));

        List<RouteStopSnapshot> ordered = new ArrayList<>();
        for (String label : orderedLabels) {
            RouteStopSnapshot stop = byLabel.get(label);
            if (stop != null) {
                ordered.add(stop.withSequence(ordered.size()));
            }
        }

        stops.stream()
                .filter(stop -> !orderedLabels.contains(stop.label()))
                .forEach(stop -> ordered.add(stop.withSequence(ordered.size())));

        return ordered;
    }

    private String defaultSummary(CompanyDeliveryRoute route, List<RouteStopSnapshot> orderedStops, RouteMetrics metrics) {
        String path = orderedStops.stream()
                .map(RouteStopSnapshot::label)
                .collect(Collectors.joining(" → "));
        double distanceKm = metrics.distanceMeters() / 1000.0;
        return "%s 기준 %s 경로 (%.1fkm, 약 %d분)".formatted(
                route.getScheduledDate(),
                path,
                distanceKm,
                metrics.durationMinutes()
        );
    }

    private static String reasonWithStart(String reason, String startTime) {
        if (reason == null || reason.isBlank()) {
            return "추천 출발 시각 " + startTime;
        }
        return reason + " / 추천 출발 시각 " + startTime;
    }

    private record OrderedResult(
            List<String> orderedLabels,
            String reason,
            String summary,
            String naverRouteLink
    ) {
        static OrderedResult fallback(List<RouteStopSnapshot> stops) {
            List<String> labels = stops.stream()
                    .map(RouteStopSnapshot::label)
                    .toList();
            return new OrderedResult(labels, null, null, null);
        }
    }
}
