package org.sparta.slack.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.slack.application.port.out.SlackUserLookupPort;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slack 사용자 조회 + 캐시
 */
@Slf4j
@RequiredArgsConstructor
@org.springframework.stereotype.Component
public class SlackUserDirectoryAdapter {

    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final SlackUserLookupPort slackUserLookupPort;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public String resolveUserId(String email) {
        CacheEntry cached = cache.get(email);
        if (cached != null && !cached.isExpired()) {
            log.debug("Slack userId cache hit - email={}, userId={}", email, cached.userId());
            return cached.userId();
        }

        SlackUserLookupPort.SlackUser slackUser = slackUserLookupPort.lookupUserByEmail(email);
        CacheEntry entry = new CacheEntry(slackUser.id(), Instant.now().plus(CACHE_TTL));
        cache.put(email, entry);
        log.info("Slack userId cache stored - email={}, userId={}", email, slackUser.id());
        return slackUser.id();
    }

    private record CacheEntry(String userId, Instant expiration) {
        boolean isExpired() {
            return Instant.now().isAfter(expiration);
        }
    }
}
