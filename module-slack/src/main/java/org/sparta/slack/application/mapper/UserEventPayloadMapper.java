package org.sparta.slack.application.mapper;

import org.sparta.common.error.BusinessException;
import org.sparta.common.event.slack.UserDomainEvent;
import org.sparta.slack.domain.enums.UserRole;
import org.sparta.slack.domain.enums.UserStatus;
import org.sparta.slack.error.SlackErrorType;
import org.springframework.stereotype.Component;

@Component
public class UserEventPayloadMapper {

    public UserEventPayload map(UserDomainEvent event) {
        if (event == null || !event.hasPayload()) {
            throw new BusinessException(SlackErrorType.USER_SLACK_VIEW_PAYLOAD_MISSING);
        }

        UserDomainEvent.Payload payload = event.payload();
        UserRole role = parseEnum(payload.role(), UserRole.class);
        UserStatus status = parseEnum(payload.status(), UserStatus.class);

        return new UserEventPayload(
                payload.userId(),
                payload.userName(),
                payload.realName(),
                payload.slackId(),
                role,
                status,
                payload.hubId()
        );
    }

    private <T extends Enum<T>> T parseEnum(String value, Class<T> enumType) {
        try {
            return Enum.valueOf(enumType, value);
        } catch (Exception ex) {
            throw new BusinessException(SlackErrorType.USER_SLACK_VIEW_PAYLOAD_MISSING);
        }
    }

    public record UserEventPayload(
            java.util.UUID userId,
            String userName,
            String realName,
            String slackId,
            UserRole role,
            UserStatus status,
            java.util.UUID hubId
    ) {
    }
}
