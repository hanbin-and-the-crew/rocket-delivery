package org.sparta.order.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "user-service"
)
public interface PointClient {
    @PostMapping("/users/point/reserve")
    PointReserveResponse reservePoint(@RequestBody PointReserveRequest request);

    // ===== DTO =====
    record PointReserveRequest(
            String userId,      // 여기서는 customerId를 String으로
            String orderId,     // orderId.toString()
            Long orderAmount,   // 주문 총액
            Long requestPoint   // 사용할 포인트
    ) {}

    record PointReserveResponse(
            String reservationId
    ) {}
}
