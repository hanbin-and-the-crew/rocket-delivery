package org.sparta.slack.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.slack.application.port.out.SlackTemplateRepository;
import org.sparta.slack.domain.entity.Message;
import org.sparta.slack.domain.entity.Template;
import org.sparta.slack.domain.repository.MessageRepository;
import org.sparta.slack.error.SlackErrorType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackDirectMessageSender {

    private final SlackTemplateRepository templateRepository;
    private final MessageRepository messageRepository;
    private final org.sparta.slack.application.port.out.SlackNotificationSender notificationSender;
    private final ObjectMapper objectMapper;

    @Transactional
    public UUID send(String slackId, String templateCode, Object payload, String fallbackBody) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            Template template = templateRepository.findActiveByCode(templateCode)
                    .orElse(null);

            String messageBody = template != null
                    ? template.render(payloadJson)
                    : fallbackBody;

            if (messageBody == null || messageBody.isBlank()) {
                messageBody = fallbackBody != null ? fallbackBody : "Slack 알림";
            }

            Message message = Message.create(slackId, templateCode, payloadJson, messageBody);
            messageRepository.save(message);

            try {
                notificationSender.send(message);
                message.markAsSent();
            } catch (Exception ex) {
                log.error("Slack 전송 실패 - slackId={}", slackId, ex);
                message.markAsFailed("SLACK-SENDER-001", ex.getMessage());
                messageRepository.save(message);
                throw new BusinessException(SlackErrorType.SLACK_DELIVERY_FAILED, ex.getMessage());
            }
            messageRepository.save(message);
            return message.getId();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(SlackErrorType.SLACK_PAYLOAD_SERIALIZATION_FAILED, ex.getMessage());
        }
    }
}
