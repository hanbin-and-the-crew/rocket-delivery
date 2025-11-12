package org.sparta.slack.application.service.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.slack.presentation.SlackMessageRequest;
import org.sparta.slack.presentation.SlackMessageResponse;
import org.sparta.slack.application.port.out.SlackNotificationSender;
import org.sparta.slack.application.port.out.SlackTemplateRepository;
import org.sparta.slack.domain.entity.Message;
import org.sparta.slack.domain.entity.Template;
import org.sparta.slack.domain.repository.MessageRepository;
import org.sparta.slack.error.SlackErrorType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlackMessageCommandService {

    private final MessageRepository messageRepository;
    private final SlackTemplateRepository slackTemplateRepository;
    private final SlackNotificationSender slackNotificationSender;
    private final ObjectMapper objectMapper;

    @Transactional
    public SlackMessageResponse.Detail create(SlackMessageRequest.Create request) {
        Template template = slackTemplateRepository.findActiveByCode(request.templateCode())
                .orElseThrow(() -> new BusinessException(SlackErrorType.SLACK_TEMPLATE_NOT_FOUND));

        String payloadJson = serializePayload(request.payload());
        String messageBody = template.render(payloadJson);

        Message message = Message.create(
                request.slackId(),
                template.getTemplateCode(),
                payloadJson,
                messageBody
        );

        messageRepository.save(message);
        sendAndMark(message);
        messageRepository.save(message);

        return SlackMessageResponse.Detail.from(message);
    }

    @Transactional
    public SlackMessageResponse.Detail update(UUID messageId, SlackMessageRequest.Update request) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(SlackErrorType.SLACK_MESSAGE_NOT_FOUND));

        Template template = slackTemplateRepository.findActiveByCode(request.templateCode())
                .orElseThrow(() -> new BusinessException(SlackErrorType.SLACK_TEMPLATE_NOT_FOUND));

        String payloadJson = serializePayload(request.payload());
        String messageBody = template.render(payloadJson);

        message.updateContent(template.getTemplateCode(), payloadJson, messageBody);
        messageRepository.save(message);
        return SlackMessageResponse.Detail.from(message);
    }

    @Transactional
    public void delete(UUID messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(SlackErrorType.SLACK_MESSAGE_NOT_FOUND));
        messageRepository.delete(message);
    }

    private void sendAndMark(Message message) {
        try {
            slackNotificationSender.send(message);
            message.markAsSent();
        } catch (Exception ex) {
            log.error("Slack 메시지 전송 실패 - messageId={}", message.getId(), ex);
            message.markAsFailed("SLACK-SENDER-001", ex.getMessage());
        }
    }

    private String serializePayload(Map<String, Object> payload) {
        Map<String, Object> safePayload = CollectionUtils.isEmpty(payload) ? Map.of() : payload;
        try {
            return objectMapper.writeValueAsString(safePayload);
        } catch (JsonProcessingException e) {
            throw new BusinessException(SlackErrorType.SLACK_PAYLOAD_SERIALIZATION_FAILED, e.getMessage());
        }
    }
}
