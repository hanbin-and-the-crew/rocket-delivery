package org.sparta.delivery.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.delivery.application.service.DeliveryService;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;
import org.sparta.delivery.infrastructure.event.OrderApprovedEvent;
import org.sparta.delivery.presentation.dto.request.DeliveryRequest;
import org.sparta.delivery.presentation.dto.response.DeliveryResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController implements DeliveryApiSpec {

    private final DeliveryService deliveryService;

    @Override
    @PostMapping("/simple")
    public ApiResponse<DeliveryResponse.Detail> createSimple(
            @Valid @RequestBody DeliveryRequest.Create request
    ) {
        DeliveryResponse.Detail response = deliveryService.createSimple(request);
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/with-route")
    public ApiResponse<DeliveryResponse.Detail> createWithRoute(
            @Valid @RequestBody OrderApprovedEvent event
    ) {
        DeliveryResponse.Detail response = deliveryService.createWithRoute(event);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/{deliveryId}")
    public ApiResponse<DeliveryResponse.Detail> getDetail(
            @PathVariable UUID deliveryId
    ) {
        DeliveryResponse.Detail response = deliveryService.getDetail(deliveryId);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping
    public ApiResponse<DeliveryResponse.PageResult> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID hubId,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        DeliveryStatus parsedStatus = null;
        if (status != null && !status.isBlank()) {
            parsedStatus = DeliveryStatus.valueOf(status);
        }

        DeliveryRequest.Search request = new DeliveryRequest.Search(
                parsedStatus,
                hubId,
                companyId,
                sortDirection
        );

        DeliveryResponse.PageResult response = deliveryService.search(request, pageable);
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/{deliveryId}/assign/hub-delivery-man")
    public ApiResponse<DeliveryResponse.Detail> assignHubDeliveryMan(
            @PathVariable UUID deliveryId,
            @Valid @RequestBody DeliveryRequest.AssignHubDeliveryMan request
    ) {
        DeliveryResponse.Detail response =
                deliveryService.assignHubDeliveryMan(deliveryId, request);
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/{deliveryId}/assign/company-delivery-man")
    public ApiResponse<DeliveryResponse.Detail> assignCompanyDeliveryMan(
            @PathVariable UUID deliveryId,
            @Valid @RequestBody DeliveryRequest.AssignCompanyDeliveryMan request
    ) {
        DeliveryResponse.Detail response =
                deliveryService.assignCompanyDeliveryMan(deliveryId, request);
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/{deliveryId}/hub/start")
    public ApiResponse<DeliveryResponse.Detail> startHubMoving(
            @PathVariable UUID deliveryId,
            @Valid @RequestBody DeliveryRequest.StartHubMoving request
    ) {
        DeliveryResponse.Detail response =
                deliveryService.startHubMoving(deliveryId, request);
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/{deliveryId}/hub/complete")
    public ApiResponse<DeliveryResponse.Detail> completeHubMoving(
            @PathVariable UUID deliveryId,
            @Valid @RequestBody DeliveryRequest.CompleteHubMoving request
    ) {
        DeliveryResponse.Detail response =
                deliveryService.completeHubMoving(deliveryId, request);
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/{deliveryId}/company/start")
    public ApiResponse<DeliveryResponse.Detail> startCompanyMoving(
            @PathVariable UUID deliveryId
    ) {
        DeliveryResponse.Detail response =
                deliveryService.startCompanyMoving(deliveryId);
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/{deliveryId}/company/complete")
    public ApiResponse<DeliveryResponse.Detail> completeDelivery(
            @PathVariable UUID deliveryId
    ) {
        DeliveryResponse.Detail response =
                deliveryService.completeDelivery(deliveryId);
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/{deliveryId}/cancel")
    public ApiResponse<DeliveryResponse.Detail> cancel(
            @PathVariable UUID deliveryId
    ) {
        DeliveryResponse.Detail response =
                deliveryService.cancel(deliveryId);
        return ApiResponse.success(response);
    }

    @Override
    @DeleteMapping("/{deliveryId}")
    public ApiResponse<Void> delete(
            @PathVariable UUID deliveryId
    ) {
        deliveryService.delete(deliveryId);
        return ApiResponse.success(null);
    }
}
