package org.sparta.order.infrastructure.client;

import org.sparta.common.api.ApiResponse;
import org.sparta.order.infrastructure.client.dto.response.UserResponse;
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
    *  특정 사용자 정보를 userId로 조회 (시스템에서 확인용)
    * */
    @GetMapping("/bos/{userId}")
    ApiResponse<UserResponse> getSpecificUserInfo(
            @PathVariable UUID userId
    );
}