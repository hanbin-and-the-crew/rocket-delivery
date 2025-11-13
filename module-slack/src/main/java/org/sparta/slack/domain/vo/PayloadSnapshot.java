package org.sparta.slack.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.slack.error.SlackErrorType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI 요청에 필요한 주문 정보 스냅샷 Value Object
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayloadSnapshot {

    @Column(name = "product_info", length = 500)
    private String productInfo;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "delivery_deadline")
    private LocalDateTime deliveryDeadline;

    @Column(name = "origin_hub_id")
    private UUID originHubId;

    @Column(name = "destination_hub_id")
    private UUID destinationHubId;

    @Column(name = "transit_route", length = 1000)
    private String transitRoute;

    @Column(name = "work_start_hour")
    private Integer workStartHour;

    @Column(name = "work_end_hour")
    private Integer workEndHour;

    private PayloadSnapshot(
            String productInfo,
            Integer quantity,
            LocalDateTime deliveryDeadline,
            UUID originHubId,
            UUID destinationHubId,
            String transitRoute,
            Integer workStartHour,
            Integer workEndHour
    ) {
        this.productInfo = productInfo;
        this.quantity = quantity;
        this.deliveryDeadline = deliveryDeadline;
        this.originHubId = originHubId;
        this.destinationHubId = destinationHubId;
        this.transitRoute = transitRoute;
        this.workStartHour = workStartHour;
        this.workEndHour = workEndHour;
    }

    public static PayloadSnapshot of(
            String productInfo,
            Integer quantity,
            LocalDateTime deliveryDeadline,
            UUID originHubId,
            UUID destinationHubId,
            String transitRoute
    ) {
        validateProductInfo(productInfo);
        validateQuantity(quantity);
        validateDeliveryDeadline(deliveryDeadline);
        validateHubIds(originHubId, destinationHubId);

        // 기본 근무시간: 09:00 - 18:00
        return new PayloadSnapshot(
                productInfo,
                quantity,
                deliveryDeadline,
                originHubId,
                destinationHubId,
                transitRoute,
                9,
                18
        );
    }

    public static PayloadSnapshot of(
            String productInfo,
            Integer quantity,
            LocalDateTime deliveryDeadline,
            UUID originHubId,
            UUID destinationHubId,
            String transitRoute,
            Integer workStartHour,
            Integer workEndHour
    ) {
        validateProductInfo(productInfo);
        validateQuantity(quantity);
        validateDeliveryDeadline(deliveryDeadline);
        validateHubIds(originHubId, destinationHubId);
        validateWorkHours(workStartHour, workEndHour);

        return new PayloadSnapshot(
                productInfo,
                quantity,
                deliveryDeadline,
                originHubId,
                destinationHubId,
                transitRoute,
                workStartHour,
                workEndHour
        );
    }

    private static void validateProductInfo(String productInfo) {
        if (productInfo == null || productInfo.isBlank()) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "상품 정보는 필수입니다");
        }
    }

    private static void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "수량은 1 이상이어야 합니다");
        }
    }

    private static void validateDeliveryDeadline(LocalDateTime deliveryDeadline) {
        if (deliveryDeadline == null) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "배송 기한은 필수입니다");
        }
    }

    private static void validateHubIds(UUID originHubId, UUID destinationHubId) {
        if (originHubId == null) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "출발지 허브 ID는 필수입니다");
        }
        if (destinationHubId == null) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "도착지 허브 ID는 필수입니다");
        }
        if (originHubId.equals(destinationHubId)) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "출발지와 도착지는 달라야 합니다");
        }
    }

    private static void validateWorkHours(Integer workStartHour, Integer workEndHour) {
        if (workStartHour == null || workEndHour == null) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "근무 시간은 필수입니다");
        }
        if (workStartHour < 0 || workStartHour > 23) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "시작 시간은 0-23 사이여야 합니다");
        }
        if (workEndHour < 0 || workEndHour > 23) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "종료 시간은 0-23 사이여야 합니다");
        }
        if (workStartHour >= workEndHour) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "시작 시간은 종료 시간보다 빨라야 합니다");
        }
    }
}
