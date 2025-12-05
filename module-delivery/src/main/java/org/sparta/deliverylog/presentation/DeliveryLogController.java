package org.sparta.deliverylog.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.deliverylog.application.service.DeliveryLogService;
import org.sparta.deliverylog.presentation.dto.request.DeliveryLogRequest;
import org.sparta.deliverylog.presentation.dto.response.DeliveryLogResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/delivery-logs")
@RequiredArgsConstructor
public class DeliveryLogController implements DeliveryLogApiSpec {

    private final DeliveryLogService deliveryLogService;

    @Override
    @PostMapping
    public ApiResponse<DeliveryLogResponse.Detail> create(
            @Valid @RequestBody DeliveryLogRequest.Create request
    ) {
        DeliveryLogResponse.Detail response = deliveryLogService.create(request);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/{logId}")
    public ApiResponse<DeliveryLogResponse.Detail> getDetail(
            @PathVariable UUID logId
    ) {
        DeliveryLogResponse.Detail response = deliveryLogService.getDetail(logId);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/delivery/{deliveryId}")
    public ApiResponse<List<DeliveryLogResponse.Summary>> getTimelineByDeliveryId(
            @PathVariable UUID deliveryId
    ) {
        List<DeliveryLogResponse.Summary> response =
                deliveryLogService.getTimelineByDeliveryId(deliveryId);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping
    public ApiResponse<DeliveryLogResponse.PageResult> search(
            @RequestParam(required = false) UUID hubId,
            @RequestParam(required = false) UUID deliveryManId,
            @RequestParam(required = false) UUID deliveryId,
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        DeliveryLogRequest.Search request = new DeliveryLogRequest.Search(
                hubId,
                deliveryManId,
                deliveryId,
                sortDirection
        );

        DeliveryLogResponse.PageResult response =
                deliveryLogService.search(request, pageable);
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/{logId}/assign-delivery-man")
    public ApiResponse<DeliveryLogResponse.Detail> assignDeliveryMan(
            @PathVariable UUID logId,
            @Valid @RequestBody DeliveryLogRequest.AssignDeliveryMan request
    ) {
        DeliveryLogResponse.Detail response =
                deliveryLogService.assignDeliveryMan(logId, request);
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/{logId}/start")
    public ApiResponse<DeliveryLogResponse.Detail> startLog(
            @PathVariable UUID logId
    ) {
        DeliveryLogResponse.Detail response =
                deliveryLogService.startLog(logId);
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/{logId}/arrive")
    public ApiResponse<DeliveryLogResponse.Detail> arriveLog(
            @PathVariable UUID logId,
            @Valid @RequestBody DeliveryLogRequest.Arrive request
    ) {
        DeliveryLogResponse.Detail response =
                deliveryLogService.arriveLog(logId, request);
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/{logId}/cancel")
    public ApiResponse<DeliveryLogResponse.Detail> cancelFromDelivery(
            @PathVariable UUID logId
    ) {
        DeliveryLogResponse.Detail response =
                deliveryLogService.cancelFromDelivery(logId);
        return ApiResponse.success(response);
    }

    @Override
    @DeleteMapping("/{logId}")
    public ApiResponse<Void> delete(
            @PathVariable UUID logId
    ) {
        deliveryLogService.delete(logId);
        return ApiResponse.success(null);
    }
}
