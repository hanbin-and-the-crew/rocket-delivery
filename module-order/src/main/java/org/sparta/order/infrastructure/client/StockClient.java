package org.sparta.order.infrastructure.client;

import org.sparta.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(
        name = "product-service"
)
public interface StockClient {

    @PostMapping("/api/product/stocks/reserve")
    ApiResponse<StockReserveResponse> reserveStock(@RequestBody StockReserveRequest request);

    record StockReserveRequest(
            UUID productId,
            String reservationKey,
            Integer quantity
    ) {}

    record StockReserveResponse(
            UUID reservationId,
            UUID stockId,
            String reservationKey,
            Integer reservedQuantity,
            String status
    ) {}

    // ===== ApiResponse 래퍼 (meta로 감싸진 구조) =====
    record ApiResponse<T>(
            StockClient.Meta meta,
            T data
    ) {
        public boolean isSuccess() {
            return meta != null && "SUCCESS".equals(meta.result());
        }

        public String result() {
            return meta != null ? meta.result() : null;
        }

        public String errorCode() {
            return meta != null ? meta.errorCode() : null;
        }

        public String message() {
            return meta != null ? meta.message() : null;
        }
    }

    record Meta(
            String result,
            String errorCode,
            String message
    ) {}

}
