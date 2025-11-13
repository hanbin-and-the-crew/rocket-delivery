package org.sparta.slack.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "slack.notification")
public record SlackNotificationProperties(
        String botToken,
        String defaultChannel,
        String baseUrl,
        String dailyDispatchCron
) {

    public SlackNotificationProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://slack.com/api";
        }
        if (dailyDispatchCron == null || dailyDispatchCron.isBlank()) {
            dailyDispatchCron = "0 0 6 * * *";
        }
    }

    public boolean isEnabled() {
        return botToken != null && !botToken.isBlank();
    }
}
