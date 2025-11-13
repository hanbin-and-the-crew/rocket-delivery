package org.sparta.slack.presentation.dto.message;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SlackEmailMessageRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(max = 2000)
        String message
) {
}
