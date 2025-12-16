package org.sparta.deliveryman.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.sparta.deliveryman.domain.enumeration.DeliveryManType;
import org.sparta.deliveryman.domain.enumeration.DeliveryManStatus;

import java.util.UUID;

public class DeliveryManRequest {

    @Schema(description = "배송 담당자 생성 요청")
    public record Create(
            @Schema(description = "User 서비스의 사용자 ID", example = "550e8400-e29b-41d4-a716-446655440001")
            @NotNull(message = "userId는 필수입니다.")
            UUID userId,

            @Schema(description = "허브 ID (COMPANY 타입일 때 필수, HUB 타입일 때는 null)", example = "550e8400-e29b-41d4-a716-446655440002")
            UUID hubId,

            @Schema(description = "배송 담당자 타입 (HUB / COMPANY)", example = "HUB")
            @NotNull(message = "배송 담당자 타입은 필수입니다.")
            DeliveryManType type,

            @Schema(description = "담당자 실명", example = "홍길동")
            @NotBlank(message = "담당자 실명은 필수입니다.")
            String realName,

            @Schema(description = "담당자 Slack ID", example = "@honggildong")
            String slackId,

            @Schema(description = "사용자 권한 (스냅샷)", example = "DELIVERY_MANAGER")
            @NotBlank(message = "사용자 권한은 필수입니다.")
            String userRole,

            @Schema(description = "사용자 상태 (스냅샷)", example = "APPROVE")
            @NotBlank(message = "사용자 상태는 필수입니다.")
            String userStatus

//            @Schema(description = "라운드 로빈 순번 (1 이상, 보통 서버에서 계산)", example = "1")
//            @NotNull(message = "시퀀스 번호는 필수입니다.")
//            @Positive(message = "시퀀스 번호는 1 이상이어야 합니다.")
//            Integer sequence
    ) {
    }

    @Schema(description = "배송 담당자 상태 변경 요청")
    public record UpdateStatus(
            @Schema(description = "변경할 배송 담당자 상태", example = "WAITING")
            @NotNull(message = "변경할 상태는 필수입니다.")
            DeliveryManStatus status
    ) {
    }

    @Schema(description = "배송 담당자 검색 요청")
    public record Search(
            @Schema(description = "허브 ID (COMPANY 타입 필터용)", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID hubId,

            @Schema(description = "배송 담당자 타입 (HUB / COMPANY)", example = "HUB")
            DeliveryManType type,

            @Schema(description = "배송 담당자 상태", example = "WAITING")
            DeliveryManStatus status,

            @Schema(description = "담당자 실명(부분검색)", example = "홍길동")
            String realName
    ) {
    }
}
