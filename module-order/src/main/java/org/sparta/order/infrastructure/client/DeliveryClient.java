package org.sparta.order.infrastructure.client;

import org.sparta.common.api.ApiResponse;
import org.sparta.order.infrastructure.client.dto.DeliveryCreateRequest;
import org.sparta.order.infrastructure.client.dto.DeliveryCreateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
}