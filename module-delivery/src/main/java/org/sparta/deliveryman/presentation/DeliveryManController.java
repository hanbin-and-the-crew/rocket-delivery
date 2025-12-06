package org.sparta.deliveryman.application.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.deliveryman.application.dto.DeliveryManRequest;
import org.sparta.deliveryman.application.dto.DeliveryManResponse;
import org.sparta.deliveryman.application.service.DeliveryManService;
import org.sparta.deliveryman.presentation.DeliveryManApiSpec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DeliveryManController implements DeliveryManApiSpec {

    private final DeliveryManService deliveryManService;

    @Override
    public ResponseEntity<ApiResponse<DeliveryManResponse.Detail>> create(@Valid @RequestBody DeliveryManRequest.Create request) {
        return ResponseEntity.ok(ApiResponse.success(deliveryManService.createDeliveryMan(request)));
    }

    @Override
    public ResponseEntity<ApiResponse<DeliveryManResponse.Detail>> findById(UUID id) {
        return ResponseEntity.ok(ApiResponse.success(deliveryManService.getDeliveryMan(id)));
    }

    @Override
    public ResponseEntity<ApiResponse<Page<DeliveryManResponse.Summary>>> search(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(deliveryManService.getAllDeliveryMen(pageable)));
    }

    @Override
    public ResponseEntity<ApiResponse<DeliveryManResponse.Detail>> update(UUID id, @Valid DeliveryManRequest.Update request) {
        return ResponseEntity.ok(ApiResponse.success(deliveryManService.updateDeliveryMan(id, request)));
    }

    @Override
    public ResponseEntity<ApiResponse<Object>> delete(UUID id) {
        deliveryManService.deleteDeliveryMan(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
