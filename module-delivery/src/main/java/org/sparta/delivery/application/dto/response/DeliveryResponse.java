package org.sparta.delivery.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;
import org.sparta.delivery.domain.entity.Delivery;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 배송 관련 Response DTO
 */
public class DeliveryResponse {

    @Schema(description = "배송 생성 응답")
    public record Create(
            @Schema(description = "배송 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID deliveryId,

            @Schema(description = "주문 ID", example = "550e8400-e29b-41d4-a716-446655440001")
            UUID orderId,

            @Schema(description = "배송 상태", example = "HUB_WAITING")
            DeliveryStatus deliveryStatus,

            @Schema(description = "생성일시", example = "2025-11-12T10:00:00")
            LocalDateTime createdAt
    ) {
        public static Create from(Delivery delivery) {
            return new Create(
                    delivery.getId(),
                    delivery.getOrderId(),
                    delivery.getDeliveryStatus(),
                    delivery.getCreatedAt()
            );
        }
    }

    @Schema(description = "배송 목록 조회용 요약 정보")
    public record Summary(
            @Schema(description = "배송 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID deliveryId,

            @Schema(description = "주문 ID", example = "550e8400-e29b-41d4-a716-446655440001")
            UUID orderId,

            @Schema(description = "출발 허브 ID", example = "550e8400-e29b-41d4-a716-446655440002")
            UUID departureHubId,

            @Schema(description = "도착 허브 ID", example = "550e8400-e29b-41d4-a716-446655440003")
            UUID destinationHubId,

            @Schema(description = "배송 상태", example = "HUB_WAITING")
            DeliveryStatus deliveryStatus,

            @Schema(description = "수령인 이름", example = "홍길동")
            String recipientName,

            @Schema(description = "생성일시", example = "2025-11-12T10:00:00")
            LocalDateTime createdAt,

            @Schema(description = "수정일시", example = "2025-11-12T10:30:00")
            LocalDateTime updatedAt
    ) {
        public static Summary from(Delivery delivery) {
            return new Summary(
                    delivery.getId(),
                    delivery.getOrderId(),
                    delivery.getDepartureHubId(),
                    delivery.getDestinationHubId(),
                    delivery.getDeliveryStatus(),
                    delivery.getRecipientName(),
                    delivery.getCreatedAt(),
                    delivery.getUpdatedAt()
            );
        }
    }

    @Schema(description = "배송 상세 조회 정보")
    public record Detail(
            @Schema(description = "배송 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID deliveryId,

            @Schema(description = "주문 ID", example = "550e8400-e29b-41d4-a716-446655440001")
            UUID orderId,

            @Schema(description = "출발 허브 ID", example = "550e8400-e29b-41d4-a716-446655440002")
            UUID departureHubId,

            @Schema(description = "도착 허브 ID", example = "550e8400-e29b-41d4-a716-446655440003")
            UUID destinationHubId,

            @Schema(description = "배송 상태", example = "HUB_WAITING")
            DeliveryStatus deliveryStatus,

            @Schema(description = "배송지 주소", example = "서울특별시 강남구 테헤란로 123")
            String deliveryAddress,

            @Schema(description = "수령인 이름", example = "홍길동")
            String recipientName,

            @Schema(description = "수령인 슬랙 ID", example = "@홍길동")
            String recipientSlackId,

            @Schema(description = "업체 배송 담당자 ID", example = "550e8400-e29b-41d4-a716-446655440004")
            UUID companyDeliveryManId,

            @Schema(description = "허브 배송 담당자 ID", example = "550e8400-e29b-41d4-a716-446655440005")
            UUID hubDeliveryManId,

            @Schema(description = "생성일시", example = "2025-11-12T10:00:00")
            LocalDateTime createdAt,

            @Schema(description = "수정일시", example = "2025-11-12T10:30:00")
            LocalDateTime updatedAt,

            @Schema(description = "삭제일시", example = "2025-11-12T11:00:00")
            LocalDateTime deletedAt
    ) {
        public static Detail from(Delivery delivery) {
            return new Detail(
                    delivery.getId(),
                    delivery.getOrderId(),
                    delivery.getDepartureHubId(),
                    delivery.getDestinationHubId(),
                    delivery.getDeliveryStatus(),
                    delivery.getDeliveryAddress(),
                    delivery.getRecipientName(),
                    delivery.getRecipientSlackId(),
                    delivery.getCompanyDeliveryManId(),
                    delivery.getHubDeliveryManId(),
                    delivery.getCreatedAt(),
                    delivery.getUpdatedAt(),
                    delivery.getDeletedAt()
            );
        }
    }

    @Schema(description = "배송 수정 응답")
    public record Update(
            @Schema(description = "배송 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID deliveryId,

            @Schema(description = "수정일시", example = "2025-11-12T10:30:00")
            LocalDateTime updatedAt
    ) {
        public static Update from(Delivery delivery) {
            return new Update(
                    delivery.getId(),
                    delivery.getUpdatedAt()
            );
        }
    }

    @Schema(description = "배송 삭제 응답")
    public record Delete(
            @Schema(description = "배송 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID deliveryId,

            @Schema(description = "삭제일시", example = "2025-11-12T11:00:00")
            LocalDateTime deletedAt
    ) {
        public static Delete from(Delivery delivery) {
            return new Delete(
                    delivery.getId(),
                    delivery.getDeletedAt()
            );
        }
    }
}
