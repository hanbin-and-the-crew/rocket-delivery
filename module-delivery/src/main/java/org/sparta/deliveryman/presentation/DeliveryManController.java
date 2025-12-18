package org.sparta.deliveryman.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.deliveryman.application.service.DeliveryManService;
import org.sparta.deliveryman.domain.entity.DeliveryMan;
import org.sparta.deliveryman.domain.enumeration.DeliveryManStatus;
import org.sparta.deliveryman.domain.enumeration.DeliveryManType;
import org.sparta.deliveryman.presentation.dto.request.DeliveryManRequest;
import org.sparta.deliveryman.presentation.dto.response.DeliveryManResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/delivery-men")
@RequiredArgsConstructor
public class DeliveryManController implements DeliveryManApiSpec {

    private final DeliveryManService deliveryManService;

    @Override
    @PostMapping
    public ApiResponse<DeliveryManResponse.Detail> createDeliveryMan(
            @Valid @RequestBody DeliveryManRequest.Create request
    ) {
        DeliveryManResponse.Detail response = deliveryManService.createManually(request);
        return ApiResponse.success(response);
    }

    @Override
    @PatchMapping("/{deliveryManId}/status")
    public ApiResponse<DeliveryManResponse.Detail> updateStatus(
            @PathVariable UUID deliveryManId,
            @Valid @RequestBody DeliveryManRequest.UpdateStatus request
    ) {
        DeliveryManResponse.Detail response =
                deliveryManService.changeStatus(deliveryManId, request);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/{deliveryManId}")
    public ApiResponse<DeliveryManResponse.Detail> getDetail(
            @PathVariable UUID deliveryManId
    ) {
        DeliveryManResponse.Detail response = deliveryManService.getDetail(deliveryManId);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping
    public ApiResponse<List<DeliveryManResponse.Summary>> search(
            @RequestParam(required = false) UUID hubId,
            @RequestParam(required = false) DeliveryManType type,
            @RequestParam(required = false) DeliveryManStatus status,
            @RequestParam(required = false) String realName
    ) {
        DeliveryManRequest.Search request = new DeliveryManRequest.Search(
                hubId,
                type,
                status,
                realName
        );

        List<DeliveryManResponse.Summary> response = deliveryManService.search(request);
        return ApiResponse.success(response);
    }


    @Override
    @PostMapping("/assign/hub")
    public ApiResponse<DeliveryManResponse.AssignResult> assignHubDeliveryMan(
    ) {
        DeliveryMan deliveryMan = deliveryManService.assignHubDeliveryMan();
        DeliveryManResponse.AssignResult response = DeliveryManResponse.AssignResult.from(deliveryMan);
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/assign/company")
    public ApiResponse<DeliveryManResponse.AssignResult> assignCompanyDeliveryMan(
            @RequestParam UUID hubId
    ) {
        DeliveryMan deliveryMan = deliveryManService.assignCompanyDeliveryMan(hubId);
        DeliveryManResponse.AssignResult response = DeliveryManResponse.AssignResult.from(deliveryMan);
        return ApiResponse.success(response);
    }

}
