package org.sparta.slack.presentation.dto.route;

import java.time.LocalDate;

public record DispatchRequest(
        LocalDate date
) {
}
