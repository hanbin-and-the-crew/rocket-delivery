package org.sparta.slack.application.dto.route;

import java.time.LocalDate;

public record DispatchRequest(
        LocalDate date
) {
}
