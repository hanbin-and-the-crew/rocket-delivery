package org.sparta.slack.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.slack.config.properties.AiPlanningProperties;
import org.sparta.slack.domain.vo.GeoPoint;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

/**
 * 주소 -> 위경도 좌표 변환
 * - 1순위: Gemini API
 * - 2순위: Nominatim 공개 API
 * - 3순위: 해시 기반 의사 좌표
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeoCoordinateResolver {

    private static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org";
    private final AiPlanningProperties aiPlanningProperties;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public GeoPoint resolve(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("주소는 필수입니다");
        }

        return requestViaAi(address)
                .or(() -> requestViaNominatim(address))
                .orElseGet(() -> fallback(address));
    }

    private Optional<GeoPoint> requestViaAi(String address) {
        if (!aiPlanningProperties.isEnabled()) {
            return Optional.empty();
        }
        try {
            RestClient client = restClientBuilder
                    .baseUrl(aiPlanningProperties.baseUrl())
                    .build();

            String prompt = aiPlanningProperties.coordinatePromptTemplate().formatted(address);
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
                return Optional.empty();
            }

            JsonNode textNode = response.at("/candidates/0/content/parts/0/text");
            if (textNode.isMissingNode()) {
                return Optional.empty();
            }

            JsonNode json = objectMapper.readTree(textNode.asText());
            double lat = json.path("lat").asDouble();
            double lng = json.path("lng").asDouble();
            return Optional.of(new GeoPoint(lat, lng));
        } catch (Exception ex) {
            log.warn("Gemini 좌표 변환 실패 - {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<GeoPoint> requestViaNominatim(String address) {
        try {
            RestClient client = restClientBuilder
                    .baseUrl(NOMINATIM_BASE_URL)
                    .build();

            GeoResponse[] result = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("format", "json")
                            .queryParam("limit", "1")
                            .queryParam("q", address)
                            .build())
                    .header("User-Agent", "slack-module/1.0 (+ai-route)")
                    .retrieve()
                    .body(GeoResponse[].class);

            if (result == null || result.length == 0) {
                return Optional.empty();
            }

            return Optional.of(new GeoPoint(
                    Double.parseDouble(result[0].lat()),
                    Double.parseDouble(result[0].lon())
            ));
        } catch (Exception ex) {
            log.warn("Nominatim 좌표 변환 실패 - {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private GeoPoint fallback(String address) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(address.getBytes(StandardCharsets.UTF_8));
            double lat = 30 + (hash[0] & 0xFF) / 255.0 * 20;
            double lng = 120 + (hash[1] & 0xFF) / 255.0 * 10;
            return new GeoPoint(lat, lng);
        } catch (Exception ex) {
            return new GeoPoint(37.5665, 126.9780); // 서울 좌표
        }
    }

    private record GeoResponse(String lat, String lon) {
    }
}
