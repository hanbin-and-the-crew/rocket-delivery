package org.sparta.order.infrastructure.client;

import org.sparta.common.api.ApiResponse;
import org.sparta.order.infrastructure.client.dto.request.DeliveryLogCreateRequest;
import org.sparta.order.infrastructure.client.dto.response.DeliveryLogCreateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Delivery-log 서비스와 통신하는 Feign Client
 */
@FeignClient(name = "delivery-log-service")
public interface DeliveryLogClient {

    /**
     * 배송로그 생성
     */
    @PostMapping("/api/deliveries")
    ApiResponse<DeliveryLogCreateResponse> createDeliveryLog(@RequestBody DeliveryLogCreateRequest request);
}
