package org.sparta.deliverylog.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sparta.deliverylog.domain.entity.DeliveryLog;
import org.sparta.deliverylog.domain.enumeration.DeliveryLogStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class DeliveryLogResponse {

    @Schema(description = "배송 로그 상세 응답")
    public record Detail(

            @Schema(description = "배송 로그 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID id,

            @Schema(description = "배송 ID", example = "550e8400-e29b-41d4-a716-446655440001")
            UUID deliveryId,

            @Schema(description = "시퀀스 (해당 배송 내 허브→허브 구간 번호)", example = "1")
            int sequence,

            @Schema(description = "출발 허브 ID", example = "550e8400-e29b-41d4-a716-446655440002")
            UUID sourceHubId,

            @Schema(description = "도착 허브 ID", example = "550e8400-e29b-41d4-a716-446655440003")
            UUID targetHubId,

            @Schema(description = "예상 거리(km)", example = "12.5")
            double estimatedKm,

            @Schema(description = "예상 소요 시간(분)", example = "30")
            int estimatedMinutes,

            @Schema(description = "실제 거리(km)", example = "13.2")
            Double actualKm,

            @Schema(description = "실제 소요 시간(분)", example = "32")
            Integer actualMinutes,

            @Schema(description = "배송 로그 상태", example = "HUB_WAITING")
            DeliveryLogStatus status,

            @Schema(description = "배송 담당자 ID", example = "550e8400-e29b-41d4-a716-446655440004")
            UUID deliveryManId,

            @Schema(description = "생성 일시")
            LocalDateTime createdAt,

            @Schema(description = "수정 일시")
            LocalDateTime updatedAt
    ) {
        public static Detail from(DeliveryLog log) {
            return new Detail(
                    log.getId(),
                    log.getDeliveryId(),
                    log.getSequence(),
                    log.getSourceHubId(),
                    log.getTargetHubId(),
                    log.getEstimatedKm(),
                    log.getEstimatedMinutes(),
                    log.getActualKm(),
                    log.getActualMinutes(),
                    log.getStatus(),
                    log.getDeliveryManId(),
                    log.getCreatedAt(),
                    log.getUpdatedAt()
            );
        }
    }

    @Schema(description = "배송 로그 요약 응답")
    public record Summary(

            @Schema(description = "배송 로그 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID id,

            @Schema(description = "배송 ID", example = "550e8400-e29b-41d4-a716-446655440001")
            UUID deliveryId,

            @Schema(description = "시퀀스 번호", example = "1")
            int sequence,

            @Schema(description = "출발 허브 ID", example = "550e8400-e29b-41d4-a716-446655440002")
            UUID sourceHubId,

            @Schema(description = "도착 허브 ID", example = "550e8400-e29b-41d4-a716-446655440003")
            UUID targetHubId,

            @Schema(description = "배송 로그 상태", example = "HUB_MOVING")
            DeliveryLogStatus status,

            @Schema(description = "배송 담당자 ID", example = "550e8400-e29b-41d4-a716-446655440004")
            UUID deliveryManId
    ) {
        public static Summary from(DeliveryLog log) {
            return new Summary(
                    log.getId(),
                    log.getDeliveryId(),
                    log.getSequence(),
                    log.getSourceHubId(),
                    log.getTargetHubId(),
                    log.getStatus(),
                    log.getDeliveryManId()
            );
        }
    }

    @Schema(description = "배송 로그 페이징 결과")
    public record PageResult(

            @Schema(description = "콘텐츠 목록")
            List<Summary> content,

            @Schema(description = "전체 개수", example = "35")
            long totalElements,

            @Schema(description = "전체 페이지 수", example = "4")
            int totalPages
    ) { }
}
