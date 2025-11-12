package org.sparta.deliverylog.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.deliverylog.application.dto.DeliveryLogRequest;
import org.sparta.deliverylog.application.dto.DeliveryLogResponse;
import org.sparta.deliverylog.application.service.DeliveryLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "배송 경로", description = "배송 경로 관리 API")
@RestController
@RequestMapping("/delivery-logs")
@RequiredArgsConstructor
public class DeliveryLogController {

    private final DeliveryLogService deliveryLogService;

    @Operation(summary = "배송 경로 생성", description = "새로운 배송 경로를 생성합니다")
    @PostMapping
    public ResponseEntity<ApiResponse<DeliveryLogResponse.Detail>> createDeliveryLog(
            @Valid @RequestBody DeliveryLogRequest.Create request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole
    ) {
        DeliveryLogResponse.Detail response = deliveryLogService.createDeliveryLog(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @Operation(summary = "배송 담당자 배정", description = "배송 경로에 배송 담당자를 배정합니다")
    @PatchMapping("/{deliveryLogId}/assign")
    public ResponseEntity<ApiResponse<DeliveryLogResponse.Detail>> assignDeliveryMan(
            @PathVariable UUID deliveryLogId,
            @Valid @RequestBody DeliveryLogRequest.Assign request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole
    ) {
        DeliveryLogResponse.Detail response = deliveryLogService.assignDeliveryMan(
                deliveryLogId,
                request.deliveryManId()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "배송 시작", description = "배송을 시작합니다")
    @PatchMapping("/{deliveryLogId}/start")
    public ResponseEntity<ApiResponse<DeliveryLogResponse.Detail>> startDelivery(
            @PathVariable UUID deliveryLogId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole
    ) {
        DeliveryLogResponse.Detail response = deliveryLogService.startDelivery(deliveryLogId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "배송 완료", description = "배송을 완료하고 실제 거리와 시간을 기록합니다")
    @PatchMapping("/{deliveryLogId}/complete")
    public ResponseEntity<ApiResponse<DeliveryLogResponse.Detail>> completeDelivery(
            @PathVariable UUID deliveryLogId,
            @Valid @RequestBody DeliveryLogRequest.Complete request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole
    ) {
        DeliveryLogResponse.Detail response = deliveryLogService.completeDelivery(
                deliveryLogId,
                request
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "배송 경로 단건 조회", description = "배송 경로 상세 정보를 조회합니다")
    @GetMapping("/{deliveryLogId}")
    public ResponseEntity<ApiResponse<DeliveryLogResponse.Detail>> getDeliveryLog(
            @PathVariable UUID deliveryLogId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole
    ) {
        DeliveryLogResponse.Detail response = deliveryLogService.getDeliveryLog(deliveryLogId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "배송 ID로 전체 경로 조회", description = "특정 배송의 전체 경로를 순서대로 조회합니다")
    @GetMapping("/delivery/{deliveryId}")
    public ResponseEntity<ApiResponse<List<DeliveryLogResponse.Summary>>> getDeliveryLogsByDeliveryId(
            @PathVariable UUID deliveryId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole
    ) {
        List<DeliveryLogResponse.Summary> response = deliveryLogService.getDeliveryLogsByDeliveryId(deliveryId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "배송 담당자의 진행 중인 경로 조회", description = "배송 담당자가 진행 중인 경로를 조회합니다")
    @GetMapping("/delivery-man/{deliveryManId}/in-progress")
    public ResponseEntity<ApiResponse<List<DeliveryLogResponse.Summary>>> getDeliveryManInProgressLogs(
            @PathVariable UUID deliveryManId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole
    ) {
        List<DeliveryLogResponse.Summary> response = deliveryLogService.getDeliveryManInProgressLogs(deliveryManId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "허브의 대기 중인 경로 조회", description = "특정 허브에서 대기 중인 경로를 조회합니다")
    @GetMapping("/hub/{hubId}/waiting")
    public ResponseEntity<ApiResponse<List<DeliveryLogResponse.Summary>>> getHubWaitingLogs(
            @PathVariable UUID hubId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole
    ) {
        List<DeliveryLogResponse.Summary> response = deliveryLogService.getHubWaitingLogs(hubId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "전체 경로 목록 조회", description = "모든 배송 경로를 페이징하여 조회합니다")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<DeliveryLogResponse.Summary>>> getAllDeliveryLogs(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole
    ) {
        Page<DeliveryLogResponse.Summary> response = deliveryLogService.getAllDeliveryLogs(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "배송 경로 취소", description = "배송 경로를 취소합니다")
    @PatchMapping("/{deliveryLogId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelDeliveryLog(
            @PathVariable UUID deliveryLogId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole
    ) {
        deliveryLogService.cancelDeliveryLog(deliveryLogId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "배송 경로 삭제", description = "배송 경로를 논리 삭제합니다")
    @DeleteMapping("/{deliveryLogId}")
    public ResponseEntity<ApiResponse<Void>> deleteDeliveryLog(
            @PathVariable UUID deliveryLogId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole
    ) {
        deliveryLogService.deleteDeliveryLog(deliveryLogId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
