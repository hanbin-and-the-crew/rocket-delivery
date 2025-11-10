package org.sparta.order.infrastructure.client;

import org.sparta.common.api.ApiResponse;
import org.sparta.order.infrastructure.client.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * User 서비스와 통신하는 Feign Client
 */
@FeignClient(name = "user-service")
public interface UserClient {

    /**
     * 사용자 조회
     */
    @GetMapping("/api/users/{userId}")
    ApiResponse<UserResponse> getUser(@PathVariable UUID userId);
}