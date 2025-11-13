package org.sparta.slack.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.sparta.slack.domain.enums.MessageStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.UUID;

@Tag(name = "Slack 메시지 관리", description = "Slack 메시지 수동 발송/조회/관리 API")
public interface SlackMessageAdminApiSpec {

    @Operation(summary = "메시지 생성 및 발송", description = "허브/배송/업체 담당자가 Slack 메시지를 수동 발송합니다.")
    ApiResponse<SlackMessageResponse.Detail> createMessage(
            @Valid @RequestBody SlackMessageRequest.Create request
    );

    @Operation(summary = "메시지 수정", description = "마스터 관리자가 메시지 내용을 수정합니다.")
    ApiResponse<SlackMessageResponse.Detail> updateMessage(
            @PathVariable UUID messageId,
            @Valid @RequestBody SlackMessageRequest.Update request
    );

    @Operation(summary = "메시지 삭제", description = "마스터 관리자가 메시지를 삭제합니다.")
    ApiResponse<Void> deleteMessage(
            @PathVariable UUID messageId
    );

    @Operation(summary = "메시지 상세 조회", description = "마스터 관리자가 Slack 메시지 상세를 조회합니다.")
    ApiResponse<SlackMessageResponse.Detail> getMessage(
            @PathVariable UUID messageId
    );

    @Operation(summary = "메시지 검색", description = "마스터 관리자가 템플릿/상태/수신자 기준으로 메시지를 검색합니다.")
    ApiResponse<PageResponse<SlackMessageResponse.Summary>> searchMessages(
            @RequestParam(required = false) String templateCode,
            @RequestParam(required = false) MessageStatus status,
            @RequestParam(required = false) String slackId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime sentFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime sentTo,
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable
    );

    default SlackMessageSearchRequest.Query toSearchRequest(
            String templateCode,
            MessageStatus status,
            String slackId,
            LocalDateTime sentFrom,
            LocalDateTime sentTo
    ) {
        return new SlackMessageSearchRequest.Query(templateCode, status, slackId, sentFrom, sentTo);
    }
}
