package org.sparta.slack.application.dto.route;

import java.util.UUID;

public record DailyDispatchResult(
        UUID routeId,
        UUID messageId,
        boolean success,
        String message
) {

    public static DailyDispatchResult success(UUID routeId, UUID messageId) {
        return new DailyDispatchResult(routeId, messageId, true, "DISPATCHED");
    }

    public static DailyDispatchResult failure(UUID routeId, String reason) {
        return new DailyDispatchResult(routeId, null, false, reason);
    }
}
