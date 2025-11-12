package org.sparta.delivery.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public sealed interface DeliveryRequest permits
        DeliveryRequest.Create,
        DeliveryRequest.UpdateAddress,
        DeliveryRequest.AssignDeliveryMan,
        DeliveryRequest.StartCompanyMoving {

    record Create(
            @NotNull(message = "주문 ID는 필수입니다")
            UUID orderId,

            @NotNull(message = "출발 허브 ID는 필수입니다")
            UUID departureHubId,

            @NotNull(message = "목적지 허브 ID는 필수입니다")
            UUID destinationHubId,

            @NotBlank(message = "배송지 주소는 필수입니다")
            String deliveryAddress,

            @NotBlank(message = "수령인 이름은 필수입니다")
            String recipientName,

            String recipientSlackId
    ) implements DeliveryRequest {}

    record UpdateAddress(
            @NotBlank(message = "배송지 주소는 필수입니다")
            String deliveryAddress
    ) implements DeliveryRequest {}

    record AssignDeliveryMan(
            @NotNull(message = "업체 배송 담당자 ID는 필수입니다")
            UUID companyDeliveryManId,

            @NotNull(message = "허브 배송 담당자 ID는 필수입니다")
            UUID hubDeliveryManId
    ) implements DeliveryRequest {}

    record StartCompanyMoving(
            @NotNull(message = "업체 배송 담당자 ID는 필수입니다")
            UUID companyDeliveryManId
    ) implements DeliveryRequest {}
}
