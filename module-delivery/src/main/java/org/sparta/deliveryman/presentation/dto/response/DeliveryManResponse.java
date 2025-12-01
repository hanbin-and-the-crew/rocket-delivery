package org.sparta.deliveryman.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sparta.deliveryman.domain.entity.DeliveryMan;
import org.sparta.deliveryman.domain.enumeration.DeliveryManStatus;
import org.sparta.deliveryman.domain.enumeration.DeliveryManType;

import java.time.LocalDateTime;
import java.util.UUID;

public class DeliveryManResponse {

    @Schema(description = "배송 담당자 상세 응답")
    public record Detail(
            @Schema(description = "배송 담당자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID id,

            @Schema(description = "User 서비스의 사용자 ID", example = "550e8400-e29b-41d4-a716-446655440001")
            UUID userId,

            @Schema(description = "허브 ID (HUB 타입이면 null)", example = "550e8400-e29b-41d4-a716-446655440002")
            UUID hubId,

            @Schema(description = "배송 담당자 타입", example = "HUB")
            DeliveryManType type,

            @Schema(description = "담당자 실명", example = "홍길동")
            String realName,

            @Schema(description = "담당자 Slack ID", example = "@honggildong")
            String slackId,

            @Schema(description = "사용자 권한 (스냅샷)", example = "HUB_MANAGER")
            String userRole,

            @Schema(description = "사용자 상태 (스냅샷)", example = "ACTIVE")
            String userStatus,

            @Schema(description = "배송 담당자 상태", example = "WAITING")
            DeliveryManStatus status,

            @Schema(description = "라운드 로빈 순번", example = "3")
            int sequence,

            @Schema(description = "누적 담당 배송 수", example = "42")
            int deliveryCount,

            @Schema(description = "생성 일시")
            LocalDateTime createdAt,

            @Schema(description = "수정 일시")
            LocalDateTime updatedAt
    ) {
        public static Detail from(DeliveryMan deliveryMan) {
            return new Detail(
                    deliveryMan.getId(),
                    deliveryMan.getUserId(),
                    deliveryMan.getHubId(),
                    deliveryMan.getType(),
                    deliveryMan.getRealName(),
                    deliveryMan.getSlackId(),
                    deliveryMan.getUserRole(),
                    deliveryMan.getUserStatus(),
                    deliveryMan.getStatus(),
                    deliveryMan.getSequence(),
                    deliveryMan.getDeliveryCount(),
                    deliveryMan.getCreatedAt(),
                    deliveryMan.getUpdatedAt()
            );
        }
    }

    @Schema(description = "배송 담당자 요약 응답")
    public record Summary(
            @Schema(description = "배송 담당자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID id,

            @Schema(description = "배송 담당자 타입", example = "COMPANY")
            DeliveryManType type,

            @Schema(description = "허브 ID (HUB 타입이면 null)", example = "550e8400-e29b-41d4-a716-446655440002")
            UUID hubId,

            @Schema(description = "담당자 실명", example = "홍길동")
            String realName,

            @Schema(description = "배송 담당자 상태", example = "DELIVERING")
            DeliveryManStatus status,

            @Schema(description = "누적 담당 배송 수", example = "10")
            int deliveryCount
    ) {
        public static Summary from(DeliveryMan deliveryMan) {
            return new Summary(
                    deliveryMan.getId(),
                    deliveryMan.getType(),
                    deliveryMan.getHubId(),
                    deliveryMan.getRealName(),
                    deliveryMan.getStatus(),
                    deliveryMan.getDeliveryCount()
            );
        }
    }
    @Schema(description = "담당자 배정 결과")
    public record AssignResult (

            @Schema(description = "배송 담당자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID id,

            @Schema(description = "User 서비스의 사용자 ID", example = "550e8400-e29b-41d4-a716-446655440001")
            UUID userId,

            @Schema(description = "담당자 실명", example = "홍길동")
            String realName,

            @Schema(description = "담당자 Slack ID", example = "@honggildong")
            String slackId,

            @Schema(description = "라운드 로빈 순번", example = "3")
            int sequence,

            @Schema(description = "누적 담당 배송 수", example = "42")
            int deliveryCount,

            @Schema(description = "배송 담당자 상태", example = "DELIVERING")
            DeliveryManStatus status,

            @Schema(description = "배송 담당자 이전 상태", example = "WAITING")
            DeliveryManStatus beforeStatus
    ) {
        public static AssignResult from(DeliveryMan deliveryMan) {
            return new AssignResult(
                    deliveryMan.getId(),
                    deliveryMan.getUserId(),
                    deliveryMan.getRealName(),
                    deliveryMan.getSlackId(),
                    deliveryMan.getSequence(),
                    deliveryMan.getDeliveryCount(),
                    deliveryMan.getStatus(),
                    deliveryMan.getBeforeStatus()
            );
        }
    }
//    @Schema(description = "페이징 결과")
//    public record PageResult (
//
//            @Schema(description = "콘텐츠 내용", example = "담당자 요약 내용")
//            List<Summary> content,
//
//            @Schema(description = "전체 목록 수", example = "35")
//            Long totalElements,
//
//            @Schema(description = "전체 페이지 수", example = "4")
//            int totalPage
//    ) {
//        public static PageResult from(DeliveryMan deliveryMan) {
//            return new PageResult(
//
//            );
//        }
//    }

}
