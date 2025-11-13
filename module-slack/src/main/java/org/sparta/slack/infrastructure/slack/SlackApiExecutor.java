package org.sparta.slack.infrastructure.slack;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.slack.config.properties.SlackNotificationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
@Component
public class SlackApiExecutor {

    private static final int MAX_RETRY = 3;

    private final SlackNotificationProperties properties;
    private final RestClient restClient;

    public SlackApiExecutor(SlackNotificationProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        if (!properties.isEnabled()) {
            log.warn("Slack Bot Token이 설정되지 않았습니다. Slack API 호출이 비활성화됩니다.");
        }
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.botToken())
                .build();
    }

    public <T> T get(String path, Consumer<UriBuilder> uriCustomizer, Class<T> responseType) {
        return executeWithRetry(() -> restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(path);
                    if (uriCustomizer != null) {
                        uriCustomizer.accept(uriBuilder);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .toEntity(responseType));
    }

    public <T> T postJson(String path, Object body, Class<T> responseType) {
        return executeWithRetry(() -> restClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .body(body)
                .retrieve()
                .toEntity(responseType));
    }

    private <T> T executeWithRetry(Supplier<ResponseEntity<T>> supplier) {
        long backoff = 200;
        SlackApiTransportException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                ResponseEntity<T> entity = supplier.get();
                return entity.getBody();
            } catch (RestClientResponseException ex) {
                if (ex.getStatusCode().value() == 429 && attempt < MAX_RETRY) {
                    long retryAfter = parseRetryAfterSeconds(ex.getResponseHeaders());
                    sleep(Duration.ofSeconds(retryAfter > 0 ? retryAfter : 1L));
                    continue;
                }

                if (ex.getStatusCode().is5xxServerError() && attempt < MAX_RETRY) {
                    sleep(Duration.ofMillis(backoff));
                    backoff *= 2;
                    continue;
                }

                throw new SlackApiTransportException("Slack API 통신 오류 : " + ex.getMessage(), ex);
            } catch (ResourceAccessException ex) {
                lastException = new SlackApiTransportException("Slack API 네트워크 오류", ex);
                if (attempt == MAX_RETRY) {
                    break;
                }
                sleep(Duration.ofMillis(backoff));
                backoff *= 2;
            } catch (SlackApiTransportException ex) {
                lastException = ex;
                break;
            }
        }

        if (lastException != null) {
            throw lastException;
        }
        throw new SlackApiTransportException("Slack API 재시도 한도를 초과했습니다.");
    }

    private long parseRetryAfterSeconds(HttpHeaders headers) {
        if (headers == null) {
            return 0;
        }
        String retryAfter = headers.getFirst("Retry-After");
        if (!StringUtils.hasText(retryAfter)) {
            return 0;
        }
        try {
            return Long.parseLong(retryAfter.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
