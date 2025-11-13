package org.sparta.slack.application.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.sparta.slack.application.command.OrderDeadlineCommand;
import org.sparta.slack.application.service.notification.OrderDeadlinePlanResult;
import org.sparta.slack.config.properties.AiPlanningProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gemini 기반 발송 시한 계산 서비스.
 */
@Service
@RequiredArgsConstructor
public class OrderDeadlinePlanningService {

    private static final DateTimeFormatter ISO_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final Logger log = LoggerFactory.getLogger(OrderDeadlinePlanningService.class);

    private final AiPlanningProperties aiPlanningProperties;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public OrderDeadlinePlanResult plan(OrderDeadlineCommand command) {
        if (!aiPlanningProperties.isEnabled()) {
            return fallback(command, "AI 비활성화 상태");
        }

        try {
            RestClient client = restClientBuilder
                    .baseUrl(aiPlanningProperties.baseUrl())
                    .build();

            String prompt = buildPrompt(command);
            JsonNode response = client.post()
                    .uri("/models/%s:generateContent?key=%s".formatted(
                            aiPlanningProperties.model(),
                            aiPlanningProperties.apiKey()
                    ))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequestBody(prompt))
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode textNode = response != null
                    ? response.at("/candidates/0/content/parts/0/text")
                    : null;

            if (textNode == null || textNode.isMissingNode()) {
                return fallback(command, "AI 응답이 비어있습니다");
            }

            JsonNode payload = objectMapper.readTree(cleanResponse(textNode.asText()));
            LocalDateTime finalDeadline = parseDeadline(payload.path("finalDeadline").asText(null));
            if (finalDeadline == null) {
                return fallback(command, "AI가 발송 시한을 제공하지 않았습니다");
            }
            String summary = payload.path("routeSummary").asText(command.defaultRouteSummary());
            String reason = payload.path("reason").asText("AI가 계산한 발송 시한");

            return new OrderDeadlinePlanResult(finalDeadline, summary, reason, false);
        } catch (Exception ex) {
            log.warn("Order deadline AI 계산 실패 - orderId={} message={}", command.orderId(), ex.getMessage());
            return fallback(command, "AI 계산 실패");
        }
    }

    private String buildPrompt(OrderDeadlineCommand command) throws JsonProcessingException {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("orderId", command.orderId());
        context.put("orderNumber", command.orderNumber());
        context.put("orderTime", command.orderTime());
        context.put("customerName", command.customerName());
        context.put("customerEmail", command.customerEmail());
        context.put("productInfo", command.productInfo());
        context.put("quantity", command.quantity());
        context.put("requestMemo", command.requestMemo());
        context.put("origin", command.origin());
        context.put("transitPath", command.transitPath());
        context.put("destination", command.destination());
        context.put("originHubId", command.originHubId());
        context.put("destinationHubId", command.destinationHubId());
        context.put("deliveryDeadline", command.deliveryDeadline());
        context.put("workStartHour", command.workStartHour());
        context.put("workEndHour", command.workEndHour());

        String payloadJson = objectMapper.writeValueAsString(context);

        return """
                너는 국제/국내 물류 허브를 지원하는 운영 전문가이다.
                아래 주문 정보를 분석하여 허브에서 출발해야 하는 최종 발송 시한(LocalDateTime, Asia/Seoul 기준)을 계산하라.
                항상 다음 JSON 스키마로만 응답해야 하며, 설명 문장이나 코드블록을 넣지 마라.
                {
                  "finalDeadline": "YYYY-MM-DDTHH:mm",
                  "routeSummary": "사람이 읽기 쉬운 경로 요약",
                  "reason": "발송 시한 결정 사유"
                }
                입력 데이터: %s
                """.formatted(payloadJson);
    }

    private String buildRequestBody(String prompt) {
        String escaped = prompt.replace("\"", "\\\"");
        return """
                {
                  "contents": [
                    {
                      "parts": [
                        {"text": "%s"}
                      ]
                    }
                  ]
                }
                """.formatted(escaped);
    }

    private LocalDateTime parseDeadline(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return LocalDateTime.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(trimmed, ISO_MINUTE_FORMATTER);
            } catch (DateTimeParseException ex) {
                log.debug("발송 시한 파싱 실패 - value={}", trimmed);
                return null;
            }
        }
    }

    private OrderDeadlinePlanResult fallback(OrderDeadlineCommand command, String reason) {
        LocalDateTime fallbackDeadline = computeFallbackDeadline(command);
        String summary = command.defaultRouteSummary();
        String fallbackReason = "%s (근무시간 %02d~%02d 기준)".formatted(
                reason,
                command.workStartHour(),
                command.workEndHour()
        );
        return new OrderDeadlinePlanResult(fallbackDeadline, summary, fallbackReason, true);
    }

    private LocalDateTime computeFallbackDeadline(OrderDeadlineCommand command) {
        int bufferHours = Math.min(18, Math.max(4, command.quantity() / 5 + 4));
        LocalDateTime candidate = command.deliveryDeadline().minusHours(bufferHours);

        LocalDate candidateDate = candidate.toLocalDate();
        LocalDateTime latestWorkEnd = LocalDateTime.of(candidateDate, LocalTime.of(command.workEndHour(), 0));
        if (candidate.isAfter(latestWorkEnd)) {
            candidate = latestWorkEnd;
        }

        LocalDateTime earliestStart = LocalDateTime.of(candidateDate, LocalTime.of(command.workStartHour(), 0));
        if (candidate.isBefore(earliestStart)) {
            candidate = earliestStart;
        }

        // 이후에도 배송 마감보다 늦다면 30분 단위로 보정
        if (candidate.isAfter(command.deliveryDeadline())) {
            candidate = command.deliveryDeadline().minusHours(2);
        }

        return candidate.truncatedTo(ChronoUnit.MINUTES);
    }

    private String cleanResponse(String rawText) {
        if (rawText == null) {
            return "{}";
        }
        String cleaned = rawText
                .replace("```json", "")
                .replace("```", "")
                .trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end >= start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }
}
