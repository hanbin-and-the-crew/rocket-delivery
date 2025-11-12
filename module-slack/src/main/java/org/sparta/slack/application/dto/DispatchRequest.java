package org.sparta.slack.application.dto;

import java.time.LocalDate;

public record DispatchRequest(
        LocalDate date
) {
}
