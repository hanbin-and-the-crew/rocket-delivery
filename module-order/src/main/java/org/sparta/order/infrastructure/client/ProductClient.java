package org.sparta.order.infrastructure.client;

import org.sparta.common.api.ApiResponse;
import org.sparta.order.infrastructure.client.dto.ProductDetailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Product 서비스와 통신하는 Feign Client
 */
@FeignClient(name = "product-service")
public interface ProductClient {

    /**
     * 상품 상세 조회
     */
    @GetMapping("/api/products/{productId}")
    ApiResponse<ProductDetailResponse> getProduct(@PathVariable UUID productId);
}