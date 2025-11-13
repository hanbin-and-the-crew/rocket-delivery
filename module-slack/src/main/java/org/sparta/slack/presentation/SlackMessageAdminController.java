package org.sparta.slack.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.slack.application.service.message.SlackMessageCommandService;
import org.sparta.slack.application.service.message.SlackMessageQueryService;
import org.sparta.slack.domain.enums.MessageStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 슬랙 메시지 관리(보관/템플릿 발송) 전용 관리자 API.
 * - 기능: 수신자 ID, 메시지 본문, 발송 시각 등을 가진 Message 엔티티를 생성/조회/검색/삭제하고 즉시 Slack API로 전송한다.
 * - 권한: 로그인한 내부 사용자 및 시스템이 필요 시 메시지를 발송할 수 있도록 설계되었으며, 역할 기반 제어를 통해 운영자가 관리한다.
 */
@RestController
@RequestMapping("/api/slack/messages")
@RequiredArgsConstructor
public class SlackMessageAdminController implements SlackMessageAdminApiSpec {

    private final SlackMessageCommandService commandService;
    private final SlackMessageQueryService queryService;

    @Override
    @PostMapping
    @PreAuthorize("hasAnyRole('MASTER','HUB_MANAGER','DELIVERY_MANAGER','COMPANY_MANAGER')")
    public ApiResponse<SlackMessageResponse.Detail> createMessage(
            @Valid @RequestBody SlackMessageRequest.Create request
    ) {
        return ApiResponse.success(commandService.create(request));
    }

    @Override
    @PatchMapping("/{messageId}")
    @PreAuthorize("hasRole('MASTER')")
    public ApiResponse<SlackMessageResponse.Detail> updateMessage(
            @PathVariable UUID messageId,
            @Valid @RequestBody SlackMessageRequest.Update request
    ) {
        return ApiResponse.success(commandService.update(messageId, request));
    }

    @Override
    @DeleteMapping("/{messageId}")
    @PreAuthorize("hasRole('MASTER')")
    public ApiResponse<Void> deleteMessage(
            @PathVariable UUID messageId
    ) {
        commandService.delete(messageId);
        return ApiResponse.success(null);
    }

    @Override
    @GetMapping("/{messageId}")
    @PreAuthorize("hasRole('MASTER')")
    public ApiResponse<SlackMessageResponse.Detail> getMessage(
            @PathVariable UUID messageId
    ) {
        return ApiResponse.success(queryService.get(messageId));
    }

    @Override
    @GetMapping
    @PreAuthorize("hasRole('MASTER')")
    public ApiResponse<PageResponse<SlackMessageResponse.Summary>> searchMessages(
            @RequestParam(required = false) String templateCode,
            @RequestParam(required = false) MessageStatus status,
            @RequestParam(required = false) String slackId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime sentFrom,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime sentTo,
            Pageable pageable
    ) {
        SlackMessageSearchRequest.Query request = toSearchRequest(templateCode, status, slackId, sentFrom, sentTo);
        return ApiResponse.success(queryService.search(request, pageable));
    }
}
