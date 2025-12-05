package org.sparta.deliverylog.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.sparta.deliverylog.domain.enumeration.DeliveryLogStatus;

import java.util.UUID;

public class DeliveryLogRequest {

    @Schema(description = "배송 로그 생성 요청")
    public record Create(

            @Schema(description = "배송 ID", example = "550e8400-e29b-41d4-a716-446655440001")
            @NotNull(message = "deliveryId는 필수입니다.")
            UUID deliveryId,

            @Schema(description = "시퀀스 번호(해당 배송 내 허브→허브 구간 번호)", example = "1")
            @Min(value = 0, message = "sequence는 1 이상이어야 합니다.")
            int sequence,

            @Schema(description = "출발 허브 ID", example = "550e8400-e29b-41d4-a716-446655440002")
            @NotNull(message = "sourceHubId는 필수입니다.")
            UUID sourceHubId,

            @Schema(description = "도착 허브 ID", example = "550e8400-e29b-41d4-a716-446655440003")
            @NotNull(message = "targetHubId는 필수입니다.")
            UUID targetHubId,

            @Schema(description = "예상 거리(km)", example = "12.5")
            @Min(value = 0, message = "estimatedKm는 0 이상이어야 합니다.")
            double estimatedKm,

            @Schema(description = "예상 소요 시간(분)", example = "30")
            @Min(value = 0, message = "estimatedMinutes는 0 이상이어야 합니다.")
            int estimatedMinutes

//            @Schema(description = "실제 거리(km)", example = "13.2")
//            Double actualKm,
//
//            @Schema(description = "실제 소요 시간(분)", example = "32")
//            Integer actualMinutes
    ) { }

    @Schema(description = "배송 로그 상태 변경 요청")
    public record UpdateStatus(

            @Schema(description = "변경할 배송 로그 상태", example = "HUB_MOVING")
            @NotNull(message = "status는 필수입니다.")
            DeliveryLogStatus status
    ) { }

    @Schema(description = "배송 로그 검색 요청")
    public record Search(

            @Schema(description = "허브 ID (source/target 둘 다에서 검색)", example = "550e8400-e29b-41d4-a716-446655440002")
            UUID hubId,

            @Schema(description = "배송 담당자 ID", example = "550e8400-e29b-41d4-a716-446655440004")
            UUID deliveryManId,

            @Schema(description = "배송 ID", example = "550e8400-e29b-41d4-a716-446655440001")
            UUID deliveryId,

            @Schema(description = "정렬 방향 (ASC/DESC)", example = "ASC")
            String sortDirection
    ) { }

    @Schema(description = "허브 도착 처리 요청")
    public record Arrive(
            @Schema(description = "실제 거리(km)", example = "13.2")
            @Min(value = 0, message = "actualKm는 0 이상이어야 합니다.")
            double actualKm,

            @Schema(description = "실제 소요 시간(분)", example = "32")
            @Min(value = 0, message = "actualMinutes는 0 이상이어야 합니다.")
            int actualMinutes
    ) { }

    @Schema(description = "배송 로그 담당자 배정 요청")
    public record AssignDeliveryMan(
            @Schema(description = "배송 담당자 ID", example = "550e8400-e29b-41d4-a716-446655440004")
            @NotNull(message = "deliveryManId는 필수입니다.")
            UUID deliveryManId
    ) { }
}
