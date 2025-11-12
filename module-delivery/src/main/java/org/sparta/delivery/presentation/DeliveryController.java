package org.sparta.delivery.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.delivery.application.dto.request.DeliveryRequest;
import org.sparta.delivery.application.dto.response.DeliveryResponse;
import org.sparta.delivery.application.dto.DeliverySearchCondition;
import org.sparta.delivery.application.service.DeliveryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "배송 API", description = "배송 관리 API")
@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @Operation(summary = "배송 생성", description = "새로운 배송을 생성합니다.")
    @PostMapping
    public ApiResponse<DeliveryResponse.Create> createDelivery(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody DeliveryRequest.Create request
    ) {
        DeliveryResponse.Create response = deliveryService.createDelivery(request, userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "배송 목록 조회", description = "검색 조건과 페이징을 적용하여 배송 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<Page<DeliveryResponse.Summary>> getAllDeliveries(
            @RequestHeader("X-User-Id") UUID userId,
            @ModelAttribute DeliverySearchCondition condition,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<DeliveryResponse.Summary> response = deliveryService.getAllDeliveries(userId, condition, pageable);
        return ApiResponse.success(response);
    }

    @Operation(summary = "배송 상세 조회", description = "배송 ID로 배송 상세 정보를 조회합니다.")
    @GetMapping("/{deliveryId}")
    public ApiResponse<DeliveryResponse.Detail> getDelivery(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID deliveryId
    ) {
        DeliveryResponse.Detail response = deliveryService.getDelivery(deliveryId, userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "배송 상태 변경", description = "배송 상태를 변경합니다.")
    @PatchMapping("/{deliveryId}/status")
    public ApiResponse<DeliveryResponse.Update> updateStatus(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID deliveryId,
            @Valid @RequestBody DeliveryRequest.UpdateStatus request
    ) {
        DeliveryResponse.Update response = deliveryService.updateStatus(deliveryId, request, userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "배송 주소 변경", description = "배송 주소를 변경합니다.")
    @PatchMapping("/{deliveryId}/address")
    public ApiResponse<DeliveryResponse.Update> updateAddress(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID deliveryId,
            @Valid @RequestBody DeliveryRequest.UpdateAddress request
    ) {
        DeliveryResponse.Update response = deliveryService.updateAddress(deliveryId, request, userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "배송 담당자 할당", description = "배송 담당자를 할당합니다.")
    @PatchMapping("/{deliveryId}/manager")
    public ApiResponse<DeliveryResponse.Update> assignDeliveryMan(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID deliveryId,
            @Valid @RequestBody DeliveryRequest.AssignDeliveryMan request
    ) {
        DeliveryResponse.Update response = deliveryService.assignDeliveryMan(deliveryId, request, userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "배송 삭제", description = "배송을 논리 삭제합니다.")
    @DeleteMapping("/{deliveryId}")
    public ApiResponse<DeliveryResponse.Delete> deleteDelivery(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID deliveryId
    ) {
        DeliveryResponse.Delete response = deliveryService.deleteDelivery(deliveryId, userId);
        return ApiResponse.success(response);
    }

    @PatchMapping("/{deliveryId}/start-company-moving")
    @Operation(summary = "업체 배송 시작")
    public ApiResponse<DeliveryResponse.Update> startCompanyMoving(
            @PathVariable UUID deliveryId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody DeliveryRequest.StartCompanyMoving request
    ) {
        return ApiResponse.success(deliveryService.startCompanyMoving(deliveryId, request, userId));
    }

    @PatchMapping("/{deliveryId}/complete")
    @Operation(summary = "배송 완료")
    public ApiResponse<DeliveryResponse.Update> completeDelivery(
            @PathVariable UUID deliveryId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ApiResponse.success(deliveryService.completeDelivery(deliveryId, userId));
    }
}
