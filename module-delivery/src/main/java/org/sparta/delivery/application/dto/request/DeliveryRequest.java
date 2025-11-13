package org.sparta.delivery.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;

import java.util.UUID;

/**
 * 배송 관련 Request DTO
 */
public class DeliveryRequest {

    @Schema(description = "배송 생성 요청")
    public record Create(
            @Schema(description = "주문 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @NotNull(message = "주문 ID는 필수입니다")
            UUID orderId,

            @Schema(description = "출발 허브 ID", example = "550e8400-e29b-41d4-a716-446655440001")
            @NotNull(message = "출발 허브 ID는 필수입니다")
            UUID departureHubId,

            @Schema(description = "도착 허브 ID", example = "550e8400-e29b-41d4-a716-446655440002")
            @NotNull(message = "도착 허브 ID는 필수입니다")
            UUID destinationHubId,

            @Schema(description = "배송지 주소", example = "서울특별시 강남구 테헤란로 123")
            @NotBlank(message = "배송지 주소는 필수입니다")
            String deliveryAddress,

            @Schema(description = "수령인 이름", example = "홍길동")
            @NotBlank(message = "수령인 이름은 필수입니다")
            String recipientName,

            @Schema(description = "수령인 슬랙 ID", example = "@홍길동")
            String recipientSlackId
    ) {
    }

    @Schema(description = "배송 상태 변경 요청")
    public record UpdateStatus(
            @Schema(description = "변경할 배송 상태", example = "DELIVERING")
            @NotNull(message = "배송 상태는 필수입니다")
            DeliveryStatus deliveryStatus
    ) {
    }

    @Schema(description = "배송 주소 변경 요청")
    public record UpdateAddress(
            @Schema(description = "변경할 배송지 주소", example = "서울특별시 강남구 역삼로 456")
            @NotBlank(message = "배송지 주소는 필수입니다")
            String deliveryAddress
    ) {
    }

    @Schema(description = "배송 담당자 배정 요청")
    public record AssignDeliveryMan(
            @Schema(description = "업체 배송 담당자 ID", example = "550e8400-e29b-41d4-a716-446655440003")
            UUID companyDeliveryManId,

            @Schema(description = "허브 배송 담당자 ID", example = "550e8400-e29b-41d4-a716-446655440004")
            UUID hubDeliveryManId
    ) {
    }

    @Schema(description = "업체 배송 시작 요청")
    public record StartCompanyMoving(
            @Schema(description = "업체 배송 담당자 ID", example = "550e8400-e29b-41d4-a716-446655440003")
            @NotNull(message = "업체 배송 담당자 ID는 필수입니다")
            UUID companyDeliveryManId
    ) {
    }
}
