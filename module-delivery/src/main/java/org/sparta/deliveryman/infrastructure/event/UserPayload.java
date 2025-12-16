package org.sparta.deliveryman.infrastructure.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * User 정보 DTO (DeliveryMan 모듈에서 수신용)
 *
 * 주의:
 * - User 엔티티를 직접 참조하지 않음
 * - Enum은 String으로 전달받아서 변환
 */
public record UserPayload(
        UUID userId,
        String userName,
        String realName,
        String slackId,
        String role,
        String status,
        UUID hubId
) {
    /**
     * Jackson 역직렬화를 위한 생성자
     */
    @JsonCreator
    public UserPayload(
            @JsonProperty("userId") UUID userId,
            @JsonProperty("userName") String userName,
            @JsonProperty("realName") String realName,
            @JsonProperty("slackId") String slackId,
            @JsonProperty("role") String role,
            @JsonProperty("status") String status,
            @JsonProperty("hubId") UUID hubId
    ) {
        this.userId = userId;
        this.userName = userName;
        this.realName = realName;
        this.slackId = slackId;
        this.role = role;
        this.status = status;
        this.hubId = hubId;
    }

    /**
     * Role이 DELIVERY_MANAGER인지 확인
     */
    public boolean isDeliveryManager() {
        return "DELIVERY_MANAGER".equals(role);
    }

    /**
     * Status가 APPROVE인지 확인
     */
    public boolean isApproved() {
        return "APPROVE".equals(status);
    }
}