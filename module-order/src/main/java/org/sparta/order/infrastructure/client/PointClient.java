package org.sparta.order.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@FeignClient(name = "user-service")
public interface PointClient {

    @PostMapping("/api/users/point/reserve")
    ApiResponse<PointResponse.PointReservationResult> reservePoint(
            @RequestBody PointRequest.Reserve request
    );

    // ===== Request DTO =====
    class PointRequest {
        public record Reserve(
                UUID userId,
                UUID orderId,
                Long orderAmount,
                Long requestPoint
        ) {}
    }

    // ===== Response DTO =====
    class PointResponse {
        public record PointReservationResult(
                Long discountAmount,
                List<PointReservation> reservations
        ) {}

        public record PointReservation(
                UUID id,                    // 예약 ID
                UUID pointId,               // 포인트 ID
                UUID orderId,               // 주문 ID
                Long reservedAmount,        // 예약된 금액
                LocalDateTime reservedAt,   // 예약 시간
                String status               // 예약 상태 (RESERVED, CONFIRMED, CANCELLED)
        ) {}
    }

    // ===== ApiResponse 래퍼 (meta로 감싸진 구조) =====
    record ApiResponse<T>(
            Meta meta,
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
