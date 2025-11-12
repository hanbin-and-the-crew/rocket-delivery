package org.sparta.slack.application.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.api.ApiResponse;
import org.sparta.slack.application.dto.SlackEmailMessageRequest;
import org.sparta.slack.application.dto.SlackEmailMessageResponse;
import org.sparta.slack.application.service.SlackEmailNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/slack/dm")
@RequiredArgsConstructor
public class SlackDirectMessageController {

    private final SlackEmailNotificationService slackEmailNotificationService;

    @PostMapping
    public ResponseEntity<ApiResponse<SlackEmailMessageResponse>> send(@Valid @RequestBody SlackEmailMessageRequest request) {
        SlackEmailNotificationService.SlackEmailMessageResult result =
                slackEmailNotificationService.sendDirectMessage(request.email(), request.message());

        SlackEmailMessageResponse response = new SlackEmailMessageResponse(
                result.result(),
                result.slackUserId(),
                result.slackTimestamp()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
