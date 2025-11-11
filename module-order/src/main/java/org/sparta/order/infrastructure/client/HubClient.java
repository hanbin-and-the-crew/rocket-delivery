package org.sparta.order.infrastructure.client;

import org.sparta.common.api.ApiResponse;
import org.sparta.order.infrastructure.client.dto.HubResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Hub 서비스와 통신하는 Feign Client
 */
@FeignClient(name = "hub-service")
public interface HubClient {

    /**
     * 허브 조회
     */
    @GetMapping("/api/hubs/{hubId}")
    ApiResponse<HubResponse> getHub(@PathVariable UUID hubId);
}