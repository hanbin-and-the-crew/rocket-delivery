package org.sparta.deliverylog.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.sparta.deliverylog.application.dto.DeliveryLogRequest;
import org.sparta.deliverylog.application.dto.DeliveryLogResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

/**
 * 배송 경로 관리 API 명세 (Controller와 1:1 매핑)
 */
@Tag(name = "배송 경로", description = "배송 경로 관리 API")
public interface DeliveryLogApiSpec {

    @Operation(summary = "배송 경로 생성")
    ApiResponse<DeliveryLogResponse.Detail> createDeliveryLog(
            @Valid @RequestBody DeliveryLogRequest.Create request,
            @Parameter(hidden = true) String userId,
            @Parameter(hidden = true) String userRole
    );

    @Operation(summary = "배송 담당자 배정")
    ApiResponse<DeliveryLogResponse.Detail> assignDeliveryMan(
            @Parameter(description = "배송 경로 ID") @PathVariable UUID deliveryLogId,
            @Valid @RequestBody DeliveryLogRequest.Assign request,
            @Parameter(hidden = true) String userId,
            @Parameter(hidden = true) String userRole
    );

    @Operation(summary = "배송 시작")
    ApiResponse<DeliveryLogResponse.Detail> startDelivery(
            @Parameter(description = "배송 경로 ID") @PathVariable UUID deliveryLogId,
            @Parameter(hidden = true) String userId,
            @Parameter(hidden = true) String userRole
    );

    @Operation(summary = "배송 완료(실제 거리/시간 기록)")
    ApiResponse<DeliveryLogResponse.Detail> completeDelivery(
            @Parameter(description = "배송 경로 ID") @PathVariable UUID deliveryLogId,
            @Valid @RequestBody DeliveryLogRequest.Complete request,
            @Parameter(hidden = true) String userId,
            @Parameter(hidden = true) String userRole
    );

    @Operation(summary = "배송 경로 단건 조회")
    ApiResponse<DeliveryLogResponse.Detail> getDeliveryLog(
            @Parameter(description = "배송 경로 ID") @PathVariable UUID deliveryLogId,
            @Parameter(hidden = true) String userId,
            @Parameter(hidden = true) String userRole
    );

    @Operation(summary = "배송 ID로 전체 경로 조회")
    ApiResponse<List<DeliveryLogResponse.Summary>> getDeliveryLogsByDeliveryId(
            @Parameter(description = "배송 ID") @PathVariable UUID deliveryId,
            @Parameter(hidden = true) String userId,
            @Parameter(hidden = true) String userRole
    );

    @Operation(summary = "배송 담당자의 진행 중인 경로 조회")
    ApiResponse<List<DeliveryLogResponse.Summary>> getDeliveryManInProgressLogs(
            @Parameter(description = "배송 담당자 ID") @PathVariable UUID deliveryManId,
            @Parameter(hidden = true) String userId,
            @Parameter(hidden = true) String userRole
    );

    @Operation(summary = "허브의 대기 중인 경로 조회")
    ApiResponse<List<DeliveryLogResponse.Summary>> getHubWaitingLogs(
            @Parameter(description = "허브 ID") @PathVariable UUID hubId,
            @Parameter(hidden = true) String userId,
            @Parameter(hidden = true) String userRole
    );

    @Operation(summary = "전체 경로 목록 조회")
    ApiResponse<Page<DeliveryLogResponse.Summary>> getAllDeliveryLogs(
            @ParameterObject Pageable pageable,
            @Parameter(hidden = true) String userId,
            @Parameter(hidden = true) String userRole
    );

    @Operation(summary = "배송 경로 취소(Soft Cancel)")
    ApiResponse<Void> cancelDeliveryLog(
            @Parameter(description = "배송 경로 ID") @PathVariable UUID deliveryLogId,
            @Parameter(hidden = true) String userId,
            @Parameter(hidden = true) String userRole
    );

    @Operation(summary = "배송 경로 삭제(Soft Delete)")
    ApiResponse<Void> deleteDeliveryLog(
            @Parameter(description = "배송 경로 ID") @PathVariable UUID deliveryLogId,
            @Parameter(hidden = true) String userId,
            @Parameter(hidden = true) String userRole
    );
}
