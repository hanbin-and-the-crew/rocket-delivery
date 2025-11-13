package org.sparta.slack.application.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.slack.application.notification.dto.OrderSlackMessagePayload;
import org.sparta.slack.application.notification.dto.OrderSlackNotificationRequest;
import org.sparta.slack.application.port.out.SlackNotificationSender;
import org.sparta.slack.application.port.out.SlackRecipientFinder;
import org.sparta.slack.application.port.out.SlackTemplateRepository;
import org.sparta.slack.domain.entity.Message;
import org.sparta.slack.domain.entity.Template;
import org.sparta.slack.domain.entity.UserSlackView;
import org.sparta.slack.domain.enums.UserRole;
import org.sparta.slack.domain.repository.MessageRepository;
import org.sparta.slack.error.SlackErrorType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI Planning 결과를 기반으로 Slack 메시지를 생성/저장/전송하는 조율 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderSlackNotificationService {

    private static final String SLACK_SENDER_ERROR_CODE = "SLACK-SENDER-001";

    private final SlackRecipientFinder recipientFinder;
    private final SlackTemplateRepository templateRepository;
    private final MessageRepository messageRepository;
    private final SlackNotificationSender notificationSender;
    private final ObjectMapper objectMapper;

    @Transactional
    public void notify(OrderSlackNotificationRequest request) {
        if (isInvalidRequest(request)) {
            return;
        }

        Template template = loadTemplate(request.templateCode());
        List<UserSlackView> recipients = resolveRecipients(request.targetRoles(), request.hubId());

        if (recipients.isEmpty()) {
            log.info("Slack 수신자가 없습니다 (roles={}, hubId={})", request.targetRoles(), request.hubId());
            return;
        }

        String payloadJson = serializePayload(request.payload());
        String messageBody = template.render(payloadJson);

        deduplicateBySlackId(recipients).forEach(recipient ->
                sendMessage(recipient, template.getTemplateCode(), payloadJson, messageBody, request.payload().orderId())
        );
    }

    private boolean isInvalidRequest(OrderSlackNotificationRequest request) {
        if (request == null || request.payload() == null) {
            log.warn("Slack 알림 요청이 누락되었습니다.");
            return true;
        }
        return false;
    }

    private Template loadTemplate(String templateCode) {
        return templateRepository.findActiveByCode(templateCode)
                .orElseThrow(() -> new BusinessException(SlackErrorType.SLACK_TEMPLATE_NOT_FOUND));
    }

    private void sendMessage(
            UserSlackView recipient,
            String templateCode,
            String payloadJson,
            String messageBody,
            UUID orderId
    ) {
        Message message = Message.create(
                recipient.getSlackId(),
                templateCode,
                payloadJson,
                messageBody
        );

        messageRepository.save(message);

        try {
            notificationSender.send(message);
            message.markAsSent();
        } catch (Exception ex) {
            log.error("Slack 전송 중 실패: slackId={}, orderId={}", recipient.getSlackId(), orderId, ex);
            message.markAsFailed(SLACK_SENDER_ERROR_CODE, ex.getMessage());
        }

        messageRepository.save(message);
    }

    private List<UserSlackView> resolveRecipients(Set<UserRole> roles, UUID hubId) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return hubId == null
                ? recipientFinder.findApprovedByRoles(roles)
                : recipientFinder.findApprovedByHubAndRoles(hubId, roles);
    }

    private List<UserSlackView> deduplicateBySlackId(Collection<UserSlackView> recipients) {
        return recipients.stream()
                .filter(view -> view.getSlackId() != null && !view.getSlackId().isBlank())
                .collect(Collectors.toMap(
                        UserSlackView::getSlackId,
                        view -> view,
                        (existing, ignored) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    private String serializePayload(OrderSlackMessagePayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException(SlackErrorType.SLACK_PAYLOAD_SERIALIZATION_FAILED, e.getMessage());
        }
    }
}
