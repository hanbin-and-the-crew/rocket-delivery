package org.sparta.slack.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "naver.maps.directions")
public record NaverDirectionsProperties(
        String baseUrl,
        String clientId,
        String clientSecret,
        String endpointPath
) {

    public NaverDirectionsProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://naveropenapi.apigw.ntruss.com";
        }
        if (endpointPath == null) {
            endpointPath = "/map-direction/v1/driving";
        } else if (endpointPath.isBlank()) {
            endpointPath = "";
        } else if (!endpointPath.startsWith("/")) {
            endpointPath = "/" + endpointPath;
        }
    }

    public boolean isEnabled() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }
}
