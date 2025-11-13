package org.sparta.slack.application.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.api.ApiResponse;
import org.sparta.slack.application.dto.message.SlackEmailMessageRequest;
import org.sparta.slack.application.dto.message.SlackEmailMessageResponse;
import org.sparta.slack.application.service.notification.SlackEmailNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 발송 시한 알림과 별개로 운영자가 특정 이메일 사용자에게 즉시 DM을 보낼 수 있는 진단/임시 채널.
 * 대상: 발송 허브 담당자 등 내부 인력이 주문 발생 직후 “최종 발송 시한” 같은 메시지를 직접 전달해야 할 때 사용된다.
 * 시점/예시: 주문이 들어왔을 때 "주문번호 1번, 12월 10일 오전 9시까지 발송하세요!" 형태로 테스트/수동 공지를 보낼 수 있다.
 */
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
