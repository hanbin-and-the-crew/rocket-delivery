package org.sparta.slack.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class SlackMessageRequest {

    @Schema(description = "Slack 메시지 생성 요청")
    public record Create(

            @Schema(description = "Slack 수신자 ID", example = "U12345678")
            @NotBlank String slackId,

            @Schema(description = "템플릿 코드", example = "ORDER_DEADLINE_ALERT")
            @NotBlank String templateCode,

            @Schema(description = "템플릿 Payload(JSON)")
            @NotNull Map<String, Object> payload
    ) {
    }

    @Schema(description = "Slack 메시지 수정 요청")
    public record Update(

            @Schema(description = "템플릿 코드", example = "ORDER_DEADLINE_ALERT")
            @NotBlank String templateCode,

            @Schema(description = "템플릿 Payload(JSON)")
            @NotNull Map<String, Object> payload

    ) {
    }
}
