package org.sparta.order.infrastructure.client;

import org.sparta.common.api.ApiResponse;
import org.sparta.order.infrastructure.client.dto.request.DeliveryCreateRequest;
import org.sparta.order.infrastructure.client.dto.response.DeliveryCreateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Delivery 서비스와 통신하는 Feign Client
 */
@FeignClient(name = "delivery-service")
public interface DeliveryClient {

    /**
     * 배송 생성
     */
    @PostMapping("/api/deliveries")
    ApiResponse<DeliveryCreateResponse> createDelivery(@RequestBody DeliveryCreateRequest request);

    /**
     * 배송로그 저장
     * */
    @PostMapping("/api/deliveries/{deliveryLogId}")
    ApiResponse<DeliveryCreateResponse> saveDeliveryLog(@PathVariable("deliveryLogId") UUID deliveryLogId, @RequestBody DeliveryCreateRequest request);

    @DeleteMapping("/api/deliveries/{deliveryId}")
    ApiResponse<Void> deleteDelivery(
            @PathVariable("deliveryId") UUID deliveryId,
            @RequestBody Map<String, UUID> request
    );


}