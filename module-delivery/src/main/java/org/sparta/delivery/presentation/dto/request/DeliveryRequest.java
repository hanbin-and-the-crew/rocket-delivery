package org.sparta.delivery.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class DeliveryRequest {

    @Schema(description = "배송 생성 요청")
    public record Create(

            @Schema(description = "주문 ID")
            @NotNull(message = "orderId는 필수입니다.")
            UUID orderId,

            @Schema(description = "고객 ID")
            @NotNull(message = "customerId는 필수입니다.")
            UUID customerId,

            @Schema(description = "공급 업체 ID")
            @NotNull(message = "supplierCompanyId는 필수입니다.")
            UUID supplierCompanyId,

            @Schema(description = "공급 허브 ID")
            @NotNull(message = "supplierHubId는 필수입니다.")
            UUID supplierHubId,

            @Schema(description = "수령 업체 ID")
            @NotNull(message = "receiveCompanyId는 필수입니다.")
            UUID receiveCompanyId,

            @Schema(description = "수령 허브 ID")
            @NotNull(message = "receiveHubId는 필수입니다.")
            UUID receiveHubId,

            @Schema(description = "배송지 주소")
            @NotBlank(message = "address는 필수입니다.")
            String address,

            @Schema(description = "수령인 이름")
            @NotBlank(message = "receiverName은 필수입니다.")
            String receiverName,

            @Schema(description = "수령인 슬랙 ID")
            String receiverSlackId,

            @Schema(description = "수령인 전화번호")
            @NotBlank(message = "receiverPhone은 필수입니다.")
            String receiverPhone,

            @Schema(description = "납품 기한")
            LocalDateTime dueAt,

            @Schema(description = "요청 사항")
            String requestedMemo,

            @Schema(description = "전체 허브 로그 시퀀스 개수")
            @Min(value = 0, message = "totalLogSeq는 0 이상이어야 합니다.")
            Integer totalLogSeq
    ) { }

    @Schema(description = "배송 검색 요청")
    public record Search(

            @Schema(description = "배송 상태")
            DeliveryStatus status,

            @Schema(description = "허브 ID (공급/수령 허브 모두 대상)")
            UUID hubId,

            @Schema(description = "업체 ID (공급/수령 업체 모두 대상)")
            UUID companyId,

            @Schema(description = "정렬 방향 (ASC/DESC)")
            String sortDirection
    ) { }

    @Schema(description = "허브 배송 담당자 배정 요청")
    public record AssignHubDeliveryMan(

            @Schema(description = "허브 배송 담당자 ID")
            @NotNull(message = "hubDeliveryManId는 필수입니다.")
            UUID hubDeliveryManId
    ) { }

    @Schema(description = "업체 배송 담당자 배정 요청")
    public record AssignCompanyDeliveryMan(

            @Schema(description = "업체 배송 담당자 ID")
            @NotNull(message = "companyDeliveryManId는 필수입니다.")
            UUID companyDeliveryManId
    ) { }

    @Schema(description = "허브 leg 출발 요청")
    public record StartHubMoving(

            @Schema(description = "허브 로그 시퀀스 번호 (0 기반)")
            @Min(value = 0, message = "sequence는 0 이상이어야 합니다.")
            int sequence
    ) { }

    @Schema(description = "허브 leg 도착 요청")
    public record CompleteHubMoving(

            @Schema(description = "허브 로그 시퀀스 번호 (0 기반)")
            @Min(value = 0, message = "sequence는 0 이상이어야 합니다.")
            int sequence,

            @Schema(description = "실제 이동 거리(km)")
            @Min(value = 0, message = "actualKm는 0 이상이어야 합니다.")
            double actualKm,

            @Schema(description = "실제 소요 시간(분)")
            @Min(value = 0, message = "actualMinutes는 0 이상이어야 합니다.")
            int actualMinutes
    ) { }
}
