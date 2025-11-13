package org.sparta.slack.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.gemini")
public record AiPlanningProperties(
        String apiKey,
        String baseUrl,
        String model,
        String coordinatePromptTemplate,
        String routePromptTemplate
) {

    public AiPlanningProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        }
        if (model == null || model.isBlank()) {
            model = "gemini-1.5-flash-latest";
        }
        if (coordinatePromptTemplate == null || coordinatePromptTemplate.isBlank()) {
            coordinatePromptTemplate = """
                    You are a geocoding assistant. Given the address "%s" return latitude and longitude in decimal degrees as JSON {"lat": 0.0, "lng": 0.0}.
                    """;
        }
        if (routePromptTemplate == null || routePromptTemplate.isBlank()) {
            routePromptTemplate = """
                    You are a logistics planner. Based on the following stops, suggest the optimal visiting order for a delivery truck that starts from the hub and must visit every stop before end of day.
                    Provide JSON with fields orderedStops (array of stop labels in visiting order), reason (string), recommendedStartTime (HH:mm), and summary (string).
                    Stops: %s
                    """;
        }
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
